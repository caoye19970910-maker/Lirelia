"""30 independent credential slots stored securely in Windows Credential Manager.

Keeps the v2.2 service identifier so the existing 20 credentials remain available.
Gemini #1 can still read the existing v2.1 key. Never persist plaintext to settings/SQLite.
"""
from __future__ import annotations

import os
from secret_store import get_key as legacy_gemini_key, delete_key as delete_legacy_gemini

PROVIDERS = ('gemini', 'groq', 'deepl')
SLOT_COUNT = 10
_SERVICE = 'LectureMini/AIKeys/v2.2'


def _validate(provider: str, slot: int) -> None:
    if provider not in PROVIDERS or type(slot) is not int or not 1 <= slot <= SLOT_COUNT:
        raise ValueError('无效的服务商或密钥编号。')


def _account(provider: str, slot: int) -> str:
    _validate(provider, slot)
    return f'{provider}/slot-{slot:02d}'


def get_provider_key(provider: str, slot: int) -> str:
    account = _account(provider, slot)
    env = {'gemini': 'GEMINI_API_KEY', 'groq': 'GROQ_API_KEY', 'deepl': 'DEEPL_API_KEY'}[provider]
    if slot == 1 and os.getenv(env, '').strip():
        return os.environ[env].strip()
    try:
        import keyring
        value = keyring.get_password(_SERVICE, account) or ''
        if value.strip():
            return value.strip()
    except Exception:
        pass
    if provider == 'gemini' and slot == 1:
        return legacy_gemini_key()  # migrate by lookup, not by exposing a plaintext file
    return ''


def save_provider_key(provider: str, slot: int, value: str) -> None:
    account = _account(provider, slot)
    if not value or not value.strip():
        raise ValueError('请输入 API Key。')
    import keyring
    keyring.set_password(_SERVICE, account, value.strip())


def delete_provider_key(provider: str, slot: int) -> None:
    account = _account(provider, slot)
    import keyring
    try:
        keyring.delete_password(_SERVICE, account)
    except keyring.errors.PasswordDeleteError:
        pass
    if provider == 'gemini' and slot == 1:
        # Delete old key too so it does not silently reappear after deletion.
        delete_legacy_gemini()


def has_provider_key(provider: str, slot: int) -> bool:
    return bool(get_provider_key(provider, slot))
