import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'core'))
from bridge import LireliaCore, process_line


class BridgeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.core = LireliaCore(Path(self.temp.name) / 'desktop')

    def tearDown(self):
        self.core.storage.close()
        self.temp.cleanup()

    def test_collection_enrolls_and_keeps_full_answer_and_location(self):
        answer = '完整释义\n' * 100
        self.core.call('save', {'text': 'allait', 'language': 'fr', 'kind': 'word',
            'result': {'answer': answer, 'lemma': 'aller'},
            'source': {'bookId': 'book1', 'title': 'Test', 'cfi': 'epubcfi(/6/2)', 'context': 'Il allait bien.'}})
        queue = self.core.call('review_queue', {'language': 'fr'})
        self.assertEqual(queue[0]['result']['answer'], answer)
        self.assertEqual(queue[0]['source']['context'], 'Il allait bien.')
        self.assertEqual(queue[0]['source']['cfi'], 'epubcfi(/6/2)')
        self.assertEqual(self.core.call('review_queue', {'language': 'en'}), [])

    def test_translation_cache_is_language_specific_and_never_stores_key(self):
        with patch('bridge.get_provider_key', return_value='private-key'), patch('bridge.ask_deepl') as ask:
            ask.side_effect = lambda text, kind, key, **kw: {'answer': kw['language'], 'query': text}
            for lang in ('fr', 'en', 'fr'):
                self.assertEqual(self.core.call('query', {'text': 'on arrive', 'language': lang})['answer'], lang)
            self.assertEqual(ask.call_count, 2)
        self.assertNotIn('private-key', json.dumps(self.core.call('settings', {})))

    def test_import_preserves_progress_and_is_idempotent(self):
        from storage import Storage
        oldpath = Path(self.temp.name) / 'old.sqlite3'
        old = Storage(oldpath)
        old.save_bookmark('bonjour', 'word', {'answer': '完整原释义'}, 'fr')
        old.rate_review('bonjour', 'fr', 3)
        before = old.review_card('bonjour', 'fr')
        old.close()
        for _ in range(2):
            self.core.call('import_lecturemini', {'path': str(oldpath)})
        after = self.core.storage.review_card('bonjour', 'fr')
        self.assertEqual(before['due'], after['due'])
        self.assertEqual(before['scheduler_json'], after['scheduler_json'])
        self.assertEqual(before['reviews'], after['reviews'])
        self.assertEqual(self.core.storage.conn.execute('select count(*) from review_logs').fetchone()[0], 1)

    def test_protocol_rejects_arbitrary_actions_without_leaking_input(self):
        response = json.loads(process_line(self.core, json.dumps({'id': 1, 'method': 'exec', 'params': {'key': 'private-key'}})))
        self.assertFalse(response['ok'])
        self.assertNotIn('private-key', json.dumps(response))

    def test_settings_and_book_language_persist_without_credentials(self):
        self.core.call('set_book_language', {'bookId': 'book1', 'language': 'en'})
        self.core.call('save_settings', {'dailyNew': 8, 'autoSpeak': False, 'geminiModel': 'gemini-3.8-flash'})
        self.assertEqual(self.core.call('book_language', {'bookId': 'book1'}), 'en')
        self.assertIsNone(self.core.call('book_language', {'bookId': 'unknown'}))
        self.assertEqual(self.core.call('settings', {})['dailyNew'], 8)


if __name__ == '__main__':
    unittest.main()
