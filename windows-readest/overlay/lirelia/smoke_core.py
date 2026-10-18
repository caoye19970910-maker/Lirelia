"""Exercise the actual frozen sidecar before shipping a Windows installer."""
from pathlib import Path
import json
import os
import subprocess
import sys
import tempfile


def exchange(binary, directory, commands):
    result = subprocess.run([str(binary)],
        input='\n'.join(json.dumps(command) for command in commands) + '\n',
        text=True, encoding='utf-8', capture_output=True, timeout=60,
        env={**os.environ, 'APPDATA': str(directory), 'XDG_DATA_HOME': str(directory)})
    if result.returncode:
        raise RuntimeError('Frozen Lirelia core failed to start or exit cleanly.')
    replies = [json.loads(line) for line in result.stdout.splitlines()]
    if len(replies) != len(commands) or not all(reply.get('ok') for reply in replies):
        raise RuntimeError('Frozen Lirelia core protocol check failed.')
    return {reply['id']: reply['result'] for reply in replies}


def main():
    binary = Path(sys.argv[1]).resolve(strict=True)
    with tempfile.TemporaryDirectory() as directory:
        replies = exchange(binary, directory, [
            {'id': 1, 'method': 'settings'},
            {'id': 2, 'method': 'save', 'params': {'text': 'bonjour', 'language': 'fr',
                'kind': 'word', 'result': {'answer': '完整测试释义'},
                'source': {'bookId': 'smoke', 'title': 'Smoke test', 'cfi': 'epubcfi(/6/2)'}}},
        ])
        if replies[1]['scheduler'] != 'FSRS':
            raise RuntimeError('The packaged application must include FSRS.')
        replies = exchange(binary, directory, [
            {'id': 3, 'method': 'review_queue', 'params': {'language': 'fr'}},
            {'id': 4, 'method': 'review_queue', 'params': {'language': 'en'}},
        ])
        if replies[3][0]['result']['answer'] != '完整测试释义' or replies[4] != []:
            raise RuntimeError('Packaged data persistence or language isolation failed.')
    print('Frozen Lirelia core: startup, FSRS, persistence and protocol passed.')


if __name__ == '__main__':
    main()
