"""SQLite-only, local-first persisted data. No cloud account or telemetry."""
from __future__ import annotations
import json
import os
from pathlib import Path
import sqlite3
from datetime import datetime, timezone, timedelta
from review_data import ReviewDataMixin


def app_dir() -> Path:
    if os.name == 'nt' and os.environ.get('APPDATA'):
        base = Path(os.environ['APPDATA'])
    else:
        base = Path(os.environ.get('XDG_DATA_HOME', Path.home() / '.local' / 'share'))
    location = base / 'LectureMini'
    location.mkdir(parents=True, exist_ok=True)
    return location


class Storage(ReviewDataMixin):
    def __init__(self, path: str | Path | None = None):
        self.path = str(path or (app_dir() / 'lecture.sqlite3'))
        self.conn = sqlite3.connect(self.path)
        self.conn.row_factory = sqlite3.Row
        self.conn.execute('PRAGMA journal_mode=WAL')
        self.conn.executescript('''
            CREATE TABLE IF NOT EXISTS cache (
                text TEXT NOT NULL, kind TEXT NOT NULL, result TEXT NOT NULL,
                created TEXT NOT NULL, PRIMARY KEY(text, kind));
            CREATE TABLE IF NOT EXISTS bookmarks (
                text TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL,
                result TEXT NOT NULL, created TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL,
                kind TEXT NOT NULL, result TEXT NOT NULL, created TEXT NOT NULL);
        ''')
        # V2.8 language-aware bookmarks. Retain the legacy bookmarks table intact
        # for backups/downgrades, and import old entries as French once.
        self.conn.execute('''CREATE TABLE IF NOT EXISTS bookmarks_v2 (
            text TEXT NOT NULL, language TEXT NOT NULL, kind TEXT NOT NULL,
            result TEXT NOT NULL, created TEXT NOT NULL,
            PRIMARY KEY(text, language))''')
        self.conn.execute('''CREATE TABLE IF NOT EXISTS lecture_migrations (
            name TEXT NOT NULL PRIMARY KEY)''')
        # Run only once: a deleted French bookmark must not resurrect when
        # the app restarts and finds the old, read-only legacy table.
        imported = self.conn.execute(
            'SELECT 1 FROM lecture_migrations WHERE name=?',
            ('bookmarks_v2_seeded',)).fetchone()
        if not imported:
            with self.conn:
                self.conn.execute('''INSERT OR IGNORE INTO bookmarks_v2(text,language,kind,result,created)
                    SELECT text,'fr',kind,result,created FROM bookmarks''')
                self.conn.execute('INSERT INTO lecture_migrations(name) VALUES (?)',
                                  ('bookmarks_v2_seeded',))
        self.conn.commit()
        self.init_reviews()

    @staticmethod
    def _now():
        return datetime.now(timezone.utc).isoformat()

    def put_cache(self, text: str, kind: str, result: dict):
        with self.conn:
            self.conn.execute('INSERT OR REPLACE INTO cache VALUES (?,?,?,?)',
                              (text, kind, json.dumps(result, ensure_ascii=False), self._now()))

    def get_cache(self, text: str, kind: str, max_age_days: int = 7) -> dict | None:
        row = self.conn.execute('SELECT result, created FROM cache WHERE text=? AND kind=?',
                                (text, kind)).fetchone()
        if not row:
            return None
        try:
            if datetime.fromisoformat(row['created']) < datetime.now(timezone.utc) - timedelta(days=max_age_days):
                return None
            return json.loads(row['result'])
        except (ValueError, TypeError):
            return None

    def save_bookmark(self, text: str, kind: str, result: dict, language: str = 'fr'):
        from reading_language import valid_language
        language = valid_language(language)
        with self.conn:
            self.conn.execute('INSERT OR REPLACE INTO bookmarks_v2 VALUES (?,?,?,?,?)',
                              (text, language, kind, json.dumps(result, ensure_ascii=False), self._now()))
            self.enqueue_review(text, language, kind, result)

    def remove_bookmark(self, text: str, language: str = 'fr'):
        from reading_language import valid_language
        with self.conn:
            lang = valid_language(language)
            self.conn.execute('DELETE FROM bookmarks_v2 WHERE text=? AND language=?',
                              (text, lang))
            self.archive_review(text, lang)

    def is_bookmarked(self, text: str, language: str = 'fr') -> bool:
        from reading_language import valid_language
        return self.conn.execute('SELECT 1 FROM bookmarks_v2 WHERE text=? AND language=?',
                                 (text, valid_language(language))).fetchone() is not None

    def get_bookmark(self, text: str, language: str = 'fr') -> dict | None:
        from reading_language import valid_language
        row = self.conn.execute('SELECT result FROM bookmarks_v2 WHERE text=? AND language=?',
                                (text, valid_language(language))).fetchone()
        return json.loads(row['result']) if row else None

    def list_bookmarks(self, match: str = '', limit: int = 1000, language: str | None = None) -> list[dict]:
        from reading_language import valid_language
        if language is None:
            rows = self.conn.execute(
                'SELECT text,language,kind,result,created FROM bookmarks_v2 WHERE text LIKE ? '
                'ORDER BY created DESC LIMIT ?', (f'%{match}%', limit)).fetchall()
        else:
            rows = self.conn.execute(
                'SELECT text,language,kind,result,created FROM bookmarks_v2 '
                'WHERE text LIKE ? AND language=? ORDER BY created DESC LIMIT ?',
                (f'%{match}%', valid_language(language), limit)).fetchall()
        return [dict(row) for row in rows]

    def add_history(self, text: str, kind: str, result: dict):
        with self.conn:
            self.conn.execute('INSERT INTO history(text,kind,result,created) VALUES(?,?,?,?)',
                              (text, kind, json.dumps(result, ensure_ascii=False), self._now()))
            # Bound storage usage; keep newest 3000 queries.
            self.conn.execute('DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 3000)')
            return self.conn.execute('SELECT last_insert_rowid()').fetchone()[0]

    def update_history_result(self, row_id: int, result: dict):
        """Attach optional detailed analysis to the *same* history entry."""
        with self.conn:
            self.conn.execute('UPDATE history SET result=? WHERE id=?',
                              (json.dumps(result, ensure_ascii=False), row_id))

    def list_history(self, match: str = '', limit: int = 1000, language: str | None = None) -> list[dict]:
        rows = self.conn.execute('SELECT id, text, kind, result, created FROM history WHERE text LIKE ? '
                                 'ORDER BY id DESC LIMIT ?', (f'%{match}%', limit)).fetchall()
        result = [dict(row) for row in rows]
        if language is not None:
            from reading_language import valid_language
            language = valid_language(language)
            # Old history has no language field and therefore belongs to French.
            filtered = []
            for row in result:
                try:
                    entry = json.loads(row['result'])
                    if (entry.get('language') or 'fr') == language:
                        filtered.append(row)
                except (ValueError, TypeError, AttributeError):
                    continue
            return filtered
        return result

    def clear_history(self):
        with self.conn:
            self.conn.execute('DELETE FROM history')

    def delete_history(self, row_id: int):
        with self.conn:
            self.conn.execute('DELETE FROM history WHERE id=?', (row_id,))

    def backup_to(self, destination: str | Path):
        """SQLite online backup is safe while WAL journal mode is enabled."""
        target = sqlite3.connect(str(destination))
        try:
            with target:
                self.conn.backup(target)
        finally:
            target.close()

    def close(self):
        self.conn.close()
