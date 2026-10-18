"""Review-only tables added to the legacy SQLite database without rewriting old data."""
from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from review_scheduler import review


def utc_now():
    return datetime.now(timezone.utc)


def local_day(now=None):
    return (now or datetime.now().astimezone()).astimezone().date().isoformat()


class ReviewDataMixin:
    def init_reviews(self):
        # One automatic pre-upgrade backup. SQLite backup works in WAL mode.
        if not self.conn.execute("SELECT 1 FROM lecture_migrations WHERE name='review_v293_seeded'").fetchone():
            folder = Path(self.path).parent
            # Save backup before adding review tables; do not overwrite on retries.
            if self.path != ':memory:' and Path(self.path).exists() and self.conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='bookmarks_v2'").fetchone():
                backup_path = folder / 'lecture_before_review_v293.sqlite3'
                if not backup_path.exists():
                    self.backup_to(backup_path)
        with self.conn:
            self.conn.execute('''CREATE TABLE IF NOT EXISTS review_cards (
                text TEXT NOT NULL, language TEXT NOT NULL,
                brief TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'active',
                engine TEXT, scheduler_json TEXT,
                due TEXT, reviews INTEGER NOT NULL DEFAULT 0,
                last_rating INTEGER, introduced_at TEXT,
                created TEXT NOT NULL, updated TEXT NOT NULL,
                PRIMARY KEY(text,language))''')
            self.conn.execute('''CREATE TABLE IF NOT EXISTS review_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL, language TEXT NOT NULL,
                rating INTEGER NOT NULL, reviewed_at TEXT NOT NULL,
                local_date TEXT NOT NULL, engine TEXT NOT NULL,
                next_due TEXT NOT NULL)''')
            self.conn.execute('CREATE INDEX IF NOT EXISTS review_due_idx ON review_cards(language,status,due)')
            self.conn.execute('CREATE INDEX IF NOT EXISTS review_day_idx ON review_logs(language,local_date)')
            if not self.conn.execute("SELECT 1 FROM lecture_migrations WHERE name='review_v293_seeded'").fetchone():
                rows = self.conn.execute("SELECT text,language,result,created FROM bookmarks_v2 WHERE kind='word' ORDER BY created ASC").fetchall()
                for row in rows:
                    self.conn.execute('''INSERT OR IGNORE INTO review_cards(text,language,created,updated)
                        VALUES(?,?,?,?)''', (row['text'], row['language'], row['created'], self._now()))
                self.conn.execute("INSERT INTO lecture_migrations(name) VALUES('review_v293_seeded')")
            # A user may temporarily return to V2.9.2 and collect more words.
            # Reconcile active old-version bookmarks without resetting existing progress.
            missing = self.conn.execute('''SELECT b.text,b.language,b.result,b.created
                FROM bookmarks_v2 b LEFT JOIN review_cards c
                ON c.text=b.text AND c.language=b.language
                WHERE b.kind='word' AND c.text IS NULL''').fetchall()
            for row in missing:
                self.conn.execute('''INSERT OR IGNORE INTO review_cards(text,language,created,updated)
                    VALUES(?,?,?,?)''',(row['text'],row['language'],row['created'],self._now()))
            self.conn.execute('''UPDATE review_cards SET status='active'
                WHERE status='archived' AND EXISTS(SELECT 1 FROM bookmarks_v2 b
                WHERE b.text=review_cards.text AND b.language=review_cards.language AND b.kind='word')''')

    def enqueue_review(self, text, language, kind, result):
        if kind != 'word':
            return
        existing = self.conn.execute('SELECT status FROM review_cards WHERE text=? AND language=?', (text,language)).fetchone()
        if existing:
            if existing['status'] == 'archived':
                self.conn.execute("UPDATE review_cards SET status='active',updated=? WHERE text=? AND language=?", (self._now(),text,language))
        else:
            self.conn.execute('''INSERT INTO review_cards(text,language,created,updated)
                VALUES(?,?,?,?)''', (text, language, self._now(), self._now()))

    def archive_review(self, text, language):
        self.conn.execute("UPDATE review_cards SET status='archived',updated=? WHERE text=? AND language=?",(self._now(),text,language))

    def review_card(self, text, language):
        row = self.conn.execute('''SELECT c.*,b.kind,b.result FROM review_cards c
            JOIN bookmarks_v2 b ON b.text=c.text AND b.language=c.language
            WHERE c.text=? AND c.language=?''', (text,language)).fetchone()
        return dict(row) if row else None

    def review_summary(self, language, *, now=None):
        now = now or utc_now()
        due_now = now.isoformat()
        row = self.conn.execute('''SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN introduced_at IS NULL AND status='active' THEN 1 ELSE 0 END) AS new_words,
            SUM(CASE WHEN introduced_at IS NOT NULL AND due<=? AND status='active' THEN 1 ELSE 0 END) AS due_words,
            SUM(CASE WHEN status='paused' THEN 1 ELSE 0 END) AS paused,
            SUM(CASE WHEN introduced_at IS NOT NULL AND due>? AND status='active' THEN 1 ELSE 0 END) AS learning
            FROM review_cards WHERE language=? AND status!='archived' ''', (due_now, due_now, language)).fetchone()
        reviewed = self.conn.execute('SELECT COUNT(*) AS n FROM review_logs WHERE language=? AND local_date=?',
            (language, local_day(now))).fetchone()['n']
        return {**dict(row), 'reviewed_today':reviewed}

    def review_queue(self, language, daily_new=15, due_cap=None, *, now=None):
        now = now or utc_now()
        # Old overdue reviews have precedence over new introductions.
        due_rows = self.conn.execute('''SELECT c.*,b.result FROM review_cards c
            JOIN bookmarks_v2 b ON b.text=c.text AND b.language=c.language
            WHERE c.language=? AND c.status='active' AND c.introduced_at IS NOT NULL
              AND c.due <= ? AND b.kind='word' ORDER BY c.due, c.created''',
            (language,now.isoformat())).fetchall()
        due = [dict(row) for row in due_rows]
        if due_cap is not None:
            done_old = self.conn.execute('''SELECT COUNT(*) AS n FROM review_logs
                WHERE language=? AND local_date=? AND text IN (
                    SELECT text FROM review_cards WHERE language=? AND introduced_at!=?
                )''',(language,local_day(now), language,local_day(now))).fetchone()['n']
            due = due[:max(0, due_cap-done_old)]
        # Do not silently flood users who imported thousands of existing bookmarks.
        started_today = self.conn.execute("SELECT COUNT(*) FROM review_cards WHERE language=? AND introduced_at=?",
                                          (language, local_day(now))).fetchone()[0]
        new_remaining = max(0, int(daily_new)-started_today)
        new_rows = self.conn.execute('''SELECT c.*,b.result FROM review_cards c
            JOIN bookmarks_v2 b ON b.text=c.text AND b.language=c.language
            WHERE c.language=? AND c.status='active' AND c.introduced_at IS NULL
              AND b.kind='word' ORDER BY c.created ASC LIMIT ?''', (language,new_remaining)).fetchall()
        return due + [dict(row) for row in new_rows]

    def rate_review(self, text, language, rating, *, now=None):
        now = now or utc_now()
        now = now.astimezone(timezone.utc)
        with self.conn:
            existing = self.conn.execute('SELECT * FROM review_cards WHERE text=? AND language=? AND status=\'active\'',
                                         (text,language)).fetchone()
            if existing is None:
                raise ValueError('卡片不存在或已经暂停')
            if not self.conn.execute('SELECT 1 FROM bookmarks_v2 WHERE text=? AND language=? AND kind=\'word\'',
                                     (text,language)).fetchone():
                raise ValueError('原收藏已不存在')
            engine,state,due = review(existing['engine'],existing['scheduler_json'],int(rating),
                                      now=now,reviews=existing['reviews'])
            introduced = existing['introduced_at'] or local_day(now)
            self.conn.execute('''UPDATE review_cards SET engine=?,scheduler_json=?,due=?,reviews=reviews+1,
                last_rating=?,introduced_at=?,updated=? WHERE text=? AND language=?''',
                (engine,state,due,rating,introduced,now.isoformat(),text,language))
            self.conn.execute('''INSERT INTO review_logs(text,language,rating,reviewed_at,local_date,engine,next_due)
                VALUES(?,?,?,?,?,?,?)''',(text,language,rating,now.isoformat(),local_day(now),engine,due))
            return due

    def set_review_status(self,text,language,status):
        if status not in ('active','paused'):
            raise ValueError(status)
        with self.conn:
            self.conn.execute('''UPDATE review_cards SET status=?,updated=? WHERE text=? AND language=? AND status!='archived' ''',
                              (status,self._now(),text,language))
