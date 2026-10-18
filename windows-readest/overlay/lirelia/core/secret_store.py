"""Store Gemini keys in the OS credential vault (Windows Credential Manager)."""
from __future__ import annotations
import os

SERVICE = 'LectureMini/GeminiAPI'
ACCOUNT = 'personal-desktop'


def get_key() -> str:
    value = os.environ.get('GEMINI_API_KEY', '').strip()
    if value:
        return value
    try:
        import keyring
        return keyring.get_password(SERVICE, ACCOUNT) or ''
    except Exception:
        return ''


def set_key(key: str):
    import keyring
    if not key.strip():
        raise ValueError('请输入 Gemini API 密钥')
    keyring.set_password(SERVICE, ACCOUNT, key.strip())


def delete_key():
    import keyring
    try:
        keyring.delete_password(SERVICE, ACCOUNT)
    except keyring.errors.PasswordDeleteError:
        pass
