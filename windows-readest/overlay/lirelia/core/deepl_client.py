"""DeepL text translation adapter for Lecture Mini (French -> Simplified Chinese).

The key stays in Windows Credential Manager and is never persisted in cache/logs.
A :fx free key uses api-free.deepl.com; other keys use api.deepl.com.
DeepL translates words and sentences; syntactic analysis uses Gemini/Groq instead.
"""
from __future__ import annotations

import requests
from gemini_client import classify
from reading_language import valid_language, DEEPL_SOURCE

DEFAULT_DEEPL_MODEL = 'deepl-translate'
FREE_ENDPOINT = 'https://api-free.deepl.com/v2/translate'
PRO_ENDPOINT = 'https://api.deepl.com/v2/translate'


class DeepLError(Exception):
    """User-facing error that does not include response bodies or API keys."""


def get_deepl_endpoint(key: str) -> str:
    """Use the official Free endpoint for legacy Free (:fx) credentials."""
    return FREE_ENDPOINT if key.strip().endswith(':fx') else PRO_ENDPOINT


def safe_deepl_http_error(status: int) -> str:
    if status == 400:
        return 'DeepL 请求参数不正确，请检查文本内容。'
    if status in (401, 403):
        return 'DeepL API Key 无效或权限不足，请检查是否已开通 API 订阅。'
    if status == 404:
        return 'DeepL 接口地址不可用，请检查密钥类型与服务商状态。'
    if status == 413:
        return 'DeepL 请求内容过大，请缩短文本后重试。'
    if status == 429:
        return 'DeepL 调用频率达到限制，请稍后重试（不会自动更换密钥）。'
    if status == 456:
        return 'DeepL 翻译字符额度已用完。请检查账户用量（不会自动更换密钥）。'
    if status in (500, 502, 503, 504, 529):
        return 'DeepL 服务暂时不可用，请稍后重试。'
    return f'DeepL 返回 HTTP {status}，翻译未成功。'


def extract_deepl_text(payload: dict) -> str:
    translations = payload.get('translations') if isinstance(payload, dict) else None
    if not isinstance(translations, list) or not translations:
        raise DeepLError('DeepL 没有返回译文。')
    text = translations[0].get('text') if isinstance(translations[0], dict) else None
    if not isinstance(text, str) or not text.strip():
        raise DeepLError('DeepL 没有返回可显示的中文内容。')
    return text


def ask_deepl(text: str, kind: str, key: str, model: str = DEFAULT_DEEPL_MODEL,
              language: str = "fr", *, session=None, timeout: int = 30) -> dict:
    language = valid_language(language)
    if not key or not key.strip():
        raise DeepLError('尚未设置 DeepL API Key。')
    if model != DEFAULT_DEEPL_MODEL:
        raise DeepLError('DeepL 不支持所选翻译模式。')
    if kind == 'analysis':
        raise DeepLError('DeepL 仅提供翻译。详细解析请使用已配置的 Gemini 或 Groq。')
    if kind not in ('word', 'sentence') or classify(text) != kind:
        raise DeepLError('待翻译内容不符合单词或句子的输入要求。')
    requester = session or requests
    try:
        response = requester.post(
            get_deepl_endpoint(key),
            headers={'Authorization': f'DeepL-Auth-Key {key.strip()}',
                     'Content-Type': 'application/json'},
            json={'text': [text], 'source_lang': DEEPL_SOURCE[language], 'target_lang': 'ZH'},
            timeout=(8, timeout),
        )
    except requests.Timeout as exc:
        raise DeepLError('连接 DeepL 超时，请检查网络。') from exc
    except requests.RequestException as exc:
        raise DeepLError('无法连接 DeepL，请检查网络或代理。') from exc
    if response.status_code >= 400:
        raise DeepLError(safe_deepl_http_error(response.status_code))
    try:
        data = response.json()
    except ValueError as exc:
        raise DeepLError('DeepL 返回的数据格式异常。') from exc
    return {'answer': extract_deepl_text(data), 'model': model,
            'source': 'DeepL API', 'provider': 'deepl', 'version': 2, 'query': text, 'language': language}
