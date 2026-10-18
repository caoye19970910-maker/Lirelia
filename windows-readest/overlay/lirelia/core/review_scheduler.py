"""Local spaced repetition: Py-FSRS when installed, safe simple fallback otherwise.

The fallback is explicitly NOT FSRS. Existing cards keep their original engine
so that installing/uninstalling optional dependencies cannot silently reset progress.
"""
from __future__ import annotations

import json
from datetime import datetime, timezone, timedelta

try:
    from fsrs import Scheduler, Card, Rating
    FSRS_AVAILABLE = True
except ImportError:
    FSRS_AVAILABLE = False

RATINGS = {"again": 1, "hard": 2, "good": 3, "easy": 4}


def utc_now():
    return datetime.now(timezone.utc)


def as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        raise ValueError('Review timestamps must be timezone-aware')
    return value.astimezone(timezone.utc)


def review(engine: str | None, state_json: str | None, rating: int, *, now=None, reviews=0):
    """Return (engine, serialized state, next UTC due); do not change external DB here."""
    if rating not in (1, 2, 3, 4):
        raise ValueError('Invalid review rating')
    now = as_utc(now or utc_now())
    if engine == 'fsrs' and not FSRS_AVAILABLE:
        raise RuntimeError('此卡片需要 py-fsrs。请重新运行一键启动以安装 requirements.txt。')
    if engine == 'fsrs' or (engine is None and FSRS_AVAILABLE):
        card = Card.from_json(state_json) if state_json else Card()
        card, _log = Scheduler(enable_fuzzing=False).review_card(card, Rating(rating), review_datetime=now)
        return 'fsrs', card.to_json(), card.due.astimezone(timezone.utc).isoformat()
    # Dependency-free deterministic fallback for offline/missing fsrs installs.
    state = json.loads(state_json) if state_json else {"interval": 0, "ease": 2.5, "reps": 0}
    if rating == 1:
        state['reps'] = 0
        next_interval = timedelta(minutes=1)
    else:
        state['reps'] = int(state.get('reps', 0)) + 1
        old_interval = float(state.get('interval', 0))
        ease = float(state.get('ease', 2.5))
        ease = max(1.3, min(3.0, ease + {2: -0.15, 3: 0.0, 4: 0.15}[rating]))
        state['ease'] = ease
        if rating == 2:
            next_days = max(1, round(old_interval * 1.2))
        elif old_interval <= 0:
            next_days = 1 if rating == 3 else 3
        else:
            next_days = max(1, round(old_interval * (ease if rating == 3 else ease * 1.3)))
        state['interval'] = next_days
        next_interval = timedelta(days=min(next_days, 36500))
    return 'simple', json.dumps(state, ensure_ascii=False), (now + next_interval).isoformat()


def engine_name():
    return 'FSRS' if FSRS_AVAILABLE else '基础间隔复习（未安装 fsrs）'
