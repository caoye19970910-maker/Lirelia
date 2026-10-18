"""A single persisted FR/EN reading preference shared by the desktop modules."""
from __future__ import annotations

DEFAULT_LANGUAGE = 'fr'
LANGUAGES = ('fr', 'en')
LABELS = {'fr': '法语', 'en': '英语'}
DEEPL_SOURCE = {'fr': 'FR', 'en': 'EN'}
GTTS_LANG = {'fr': 'fr', 'en': 'en'}
# Keep the original French female/male voices. English uses Aura-2 English voices.
DEEPGRAM_VOICES_BY_LANGUAGE = {
    'fr': {'Agathe · 法语女声': 'aura-2-agathe-fr',
           'Hector · 法语男声': 'aura-2-hector-fr'},
    'en': {'Thalia · 美式英语女声': 'aura-2-thalia-en',
           'Apollo · 美式英语男声': 'aura-2-apollo-en'},
}


def valid_language(language: str) -> str:
    if language not in LANGUAGES:
        raise ValueError('阅读语言必须是法语或英语')
    return language


def saved_language(language) -> str:
    """Invalid or missing legacy QSettings values are safely treated as French."""
    return language if language in LANGUAGES else DEFAULT_LANGUAGE


def voices_for(language: str) -> dict[str, str]:
    return DEEPGRAM_VOICES_BY_LANGUAGE[valid_language(language)]


def default_voice(language: str) -> str:
    return next(iter(voices_for(language).values()))


def voice_for_settings(language: str, saved) -> str:
    available = voices_for(language).values()
    return saved if saved in available else default_voice(language)
