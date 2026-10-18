"""Lirelia's local JSON-lines sidecar. No HTTP server or plaintext credentials."""
from __future__ import annotations

import base64
import hashlib
import io
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import threading
from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

from gemini_client import ask_gemini, classify, DEFAULT_MODEL, ALLOWED_MODELS
from deepl_client import ask_deepl
from deepgram_speech import get_deepgram_key, save_deepgram_key, synthesize_speech
from provider_keys import get_provider_key, save_provider_key
from reading_language import valid_language, default_voice
from review_scheduler import engine_name
from storage import Storage


class LireliaCore:
    def __init__(self, directory: Path | None = None):
        base = Path(os.environ.get('APPDATA') or os.environ.get('XDG_DATA_HOME') or Path.home() / '.local/share')
        self.directory = directory or base / 'LireliaDesktop'
        self.directory.mkdir(parents=True, exist_ok=True)
        self.storage = Storage(self.directory / 'lirelia.sqlite3')
        self.storage.conn.execute('PRAGMA busy_timeout=10000')
        self.storage.conn.executescript('''
            CREATE TABLE IF NOT EXISTS lirelia_settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS lirelia_books (book_id TEXT PRIMARY KEY, language TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS lirelia_sources (
                text TEXT NOT NULL, language TEXT NOT NULL, source_json TEXT NOT NULL,
                PRIMARY KEY(text, language));
            CREATE TABLE IF NOT EXISTS lirelia_cache (
                cache_key TEXT PRIMARY KEY, result TEXT NOT NULL);
        ''')
        self.audio_dir = self.directory / 'audio'
        self.audio_dir.mkdir(exist_ok=True)

    def settings(self):
        values = {'dailyNew': 15, 'autoSpeak': True, 'geminiModel': DEFAULT_MODEL,
                  'geminiSlot': 1, 'deeplSlot': 1, 'voiceFr': default_voice('fr'),
                  'voiceEn': default_voice('en'), 'playbackRate': 1.0}
        for row in self.storage.conn.execute('SELECT key,value FROM lirelia_settings'):
            if row['key'] in values:
                values[row['key']] = json.loads(row['value'])
        return values

    @staticmethod
    def text(params):
        text = params.get('text', '')
        if not isinstance(text, str) or not text.strip() or len(text) > 3000:
            raise ValueError('请选择 1–3000 字符的单词或句子。')
        return text.strip()

    def card(self, row):
        data = dict(row)
        data['result'] = json.loads(data['result']) if isinstance(data.get('result'), str) else data.get('result', {})
        source = self.storage.conn.execute('SELECT source_json FROM lirelia_sources WHERE text=? AND language=?',
            (data['text'], data['language'])).fetchone()
        data['source'] = json.loads(source[0]) if source else None
        return data

    def query(self, p):
        text, lang = self.text(p), valid_language(p['language'])
        kind = p.get('kind') or classify(text)
        if kind not in ('word', 'sentence', 'analysis'):
            raise ValueError('请选择法语或英语单词、句子。')
        settings = self.settings()
        model = settings['geminiModel'] if kind != 'sentence' else 'deepl-translate'
        cache_key = json.dumps([text, lang, kind, model], ensure_ascii=False)
        cached = self.storage.conn.execute('SELECT result FROM lirelia_cache WHERE cache_key=?', (cache_key,)).fetchone()
        if cached:
            return json.loads(cached[0])
        provider = 'deepl' if kind == 'sentence' else 'gemini'
        key = get_provider_key(provider, settings[f'{provider}Slot'])
        result = (ask_deepl if provider == 'deepl' else ask_gemini)(text, kind, key, model=model, language=lang)
        result.update({'text': text, 'kind': kind, 'language': lang})
        # Only surface an explicitly identified lemma; never guess a verb from a noun.
        found = re.search(r'(?:原形|词元)\s*(?:为|是)?\s*[:：]?\s*([A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’\x27-][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*)', result['answer'].replace('**', '').replace('`', ''))
        result['lemma'] = found.group(1) if found and found.group(1).casefold() != text.casefold() else ''
        with self.storage.conn:
            self.storage.conn.execute('INSERT OR REPLACE INTO lirelia_cache VALUES (?,?)', (cache_key, json.dumps(result, ensure_ascii=False)))
            self.storage.add_history(text, kind, result)
        return result

    def import_lecturemini(self, path):
        source_path = Path(path).resolve(strict=True)
        if source_path == Path(self.storage.path).resolve():
            raise ValueError('请选择旧版 Lecture Mini 数据库。')
        # A read-only SQLite connection leaves the source and its journal untouched.
        source = sqlite3.connect(source_path.as_uri() + '?mode=ro', uri=True)
        source.row_factory = sqlite3.Row
        imported = 0
        try:
            tables = {r[0] for r in source.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            if 'bookmarks_v2' not in tables:
                raise ValueError('未找到 Lecture Mini bookmarks_v2 表，请选择 lecture.sqlite3。')
            with self.storage.conn:
                for row in source.execute('SELECT text,language,kind,result,created FROM bookmarks_v2'):
                    valid_language(row['language'])
                    if row['kind'] not in ('word', 'sentence') or not isinstance(json.loads(row['result']), dict):
                        raise ValueError('旧版收藏格式不正确。')
                    cursor = self.storage.conn.execute('INSERT OR IGNORE INTO bookmarks_v2 VALUES (?,?,?,?,?)', tuple(row))
                    imported += cursor.rowcount
                if 'review_cards' in tables:
                    columns = ('text', 'language', 'brief', 'status', 'engine', 'scheduler_json', 'due', 'reviews', 'last_rating', 'introduced_at', 'created', 'updated')
                    for row in source.execute('SELECT ' + ','.join(columns) + ' FROM review_cards'):
                        self.storage.conn.execute('INSERT OR IGNORE INTO review_cards VALUES (?,?,?,?,?,?,?,?,?,?,?,?)', tuple(row))
                if 'review_logs' in tables:
                    for row in source.execute('SELECT text,language,rating,reviewed_at,local_date,engine,next_due FROM review_logs'):
                        exists = self.storage.conn.execute('SELECT 1 FROM review_logs WHERE text=? AND language=? AND reviewed_at=? AND rating=?',
                            (row['text'], row['language'], row['reviewed_at'], row['rating'])).fetchone()
                        if not exists:
                            self.storage.conn.execute('INSERT INTO review_logs(text,language,rating,reviewed_at,local_date,engine,next_due) VALUES (?,?,?,?,?,?,?)', tuple(row))
                for row in self.storage.conn.execute("SELECT text,language,result FROM bookmarks_v2 WHERE kind='word'").fetchall():
                    self.storage.enqueue_review(row['text'], row['language'], 'word', json.loads(row['result']))
            return {'imported': imported, 'message': f'已导入 {imported} 条新收藏；已有复习进度保留。'}
        finally:
            source.close()

    def call(self, method, p):
        if method == 'settings':
            settings = self.settings()
            return {**settings, 'models': list(ALLOWED_MODELS), 'scheduler': engine_name(),
                'credentials': {'gemini': bool(get_provider_key('gemini', settings['geminiSlot'])),
                    'deepl': bool(get_provider_key('deepl', settings['deeplSlot'])), 'deepgram': bool(get_deepgram_key())}}
        if method == 'save_settings':
            known = self.settings()
            for k, v in p.items():
                if k not in known:
                    raise ValueError('未知设置项。')
                if k in ('dailyNew', 'geminiSlot', 'deeplSlot') and (type(v) is not int or not 1 <= v <= (200 if k == 'dailyNew' else 10)):
                    raise ValueError('设置值超出范围。')
                if k == 'geminiModel' and v not in ALLOWED_MODELS:
                    raise ValueError('请选择支持的 Gemini 模型。')
                if k == 'autoSpeak' and type(v) is not bool:
                    raise ValueError('自动发音设置无效。')
                if k == 'playbackRate' and (type(v) not in (int, float) or not 0.5 <= v <= 2):
                    raise ValueError('语速范围为 0.5–2。')
                if k in ('voiceFr', 'voiceEn'):
                    from reading_language import voices_for
                    if v not in voices_for('fr' if k == 'voiceFr' else 'en').values():
                        raise ValueError('请选择对应语言的声音。')
            with self.storage.conn:
                for k, v in p.items():
                    self.storage.conn.execute('INSERT OR REPLACE INTO lirelia_settings VALUES (?,?)', (k, json.dumps(v)))
            return self.call('settings', {})
        if method == 'save_key':
            if p['provider'] == 'deepgram':
                save_deepgram_key(p['value'])
            elif p['provider'] in ('gemini', 'deepl'):
                save_provider_key(p['provider'], p.get('slot', 1), p['value'])
            else:
                raise ValueError('未知服务商。')
            return True
        if method == 'book_language':
            row = self.storage.conn.execute('SELECT language FROM lirelia_books WHERE book_id=?', (p['bookId'],)).fetchone()
            return row[0] if row else None
        if method == 'set_book_language':
            with self.storage.conn:
                self.storage.conn.execute('INSERT OR REPLACE INTO lirelia_books VALUES (?,?)', (p['bookId'], valid_language(p['language'])))
            return True
        if method == 'query':
            return self.query(p)
        if method == 'save':
            text, lang = self.text(p), valid_language(p['language'])
            result = p['result']
            if p.get('kind') not in ('word', 'sentence') or not isinstance(result.get('answer'), str):
                raise ValueError('请先完成查询，再收藏。')
            with self.storage.conn:
                self.storage.save_bookmark(text, p['kind'], result, lang)
                if p.get('source'):
                    self.storage.conn.execute('INSERT OR REPLACE INTO lirelia_sources VALUES (?,?,?)', (text, lang, json.dumps(p['source'], ensure_ascii=False)))
            return True
        if method == 'bookmarks':
            return [self.card(row) for row in self.storage.list_bookmarks(language=valid_language(p['language']))]
        if method == 'review_queue':
            return [self.card(row) for row in self.storage.review_queue(valid_language(p['language']), daily_new=self.settings()['dailyNew'])]
        if method == 'summary':
            return self.storage.review_summary(valid_language(p['language']))
        if method == 'rate':
            return self.storage.rate_review(self.text(p), valid_language(p['language']), p['rating'])
        if method == 'status':
            self.storage.set_review_status(self.text(p), valid_language(p['language']), p['status'])
            return True
        if method == 'import_lecturemini':
            return self.import_lecturemini(p['path'])
        if method == 'speech':
            text, lang = self.text(p), valid_language(p['language'])
            is_word = p.get('kind') == 'word'
            settings = self.settings()
            voice = settings['voiceFr' if lang == 'fr' else 'voiceEn']
            digest = hashlib.sha256(json.dumps([text, lang, 'gtts' if is_word else voice]).encode()).hexdigest()
            path = self.audio_dir / (digest + ('.mp3' if is_word else '.wav'))
            if not path.exists():
                if is_word:
                    from gtts import gTTS
                    output = io.BytesIO()
                    gTTS(text=text, lang=lang, timeout=(8, 25)).write_to_fp(output)
                    data = output.getvalue()
                else:
                    data = synthesize_speech(text, get_deepgram_key(), voice, language=lang)
                temp = path.with_suffix('.' + uuid4().hex + '.tmp')
                temp.write_bytes(data)
                temp.replace(path)
            return {'data': base64.b64encode(path.read_bytes()).decode(), 'mime': 'audio/mpeg' if is_word else 'audio/wav'}
        raise ValueError('不支持的 Lirelia 操作。')


def process_line(core, line):
    request_id = None
    try:
        request = json.loads(line)
        request_id = request.get('id')
        if not isinstance(request_id, (int, str)) or not isinstance(request.get('params', {}), dict):
            raise ValueError('请求格式无效。')
        result = core.call(request.get('method'), request.get('params', {}))
        return json.dumps({'id': request_id, 'ok': True, 'result': result}, ensure_ascii=False)
    except Exception as exc:
        from gemini_client import GeminiError
        from deepl_client import DeepLError
        # Never return arbitrary exception text (which can contain credentials or paths).
        safe = str(exc) if isinstance(exc, (GeminiError, DeepLError)) else '操作未完成，请检查设置、所选文件或输入后重试。'
        return json.dumps({'id': request_id, 'ok': False, 'error': safe}, ensure_ascii=False)


def main():
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', line_buffering=True)
        sys.stdin.reconfigure(encoding='utf-8')
    initial = LireliaCore()
    directory = initial.directory
    initial.storage.close()
    local = threading.local()
    output_lock = threading.Lock()

    def respond(line):
        if not hasattr(local, 'core'):
            local.core = LireliaCore(directory)
        response = process_line(local.core, line)
        with output_lock:
            print(response, flush=True)

    # SQLite connections stay on their owning thread. Slow speech must not block
    # a new lookup or the settings screen; response ids handle out-of-order replies.
    with ThreadPoolExecutor(max_workers=4) as executor:
        for line in sys.stdin:
            executor.submit(respond, line)


if __name__ == '__main__':
    main()
