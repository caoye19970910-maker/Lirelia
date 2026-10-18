"""Optional Deepgram Aura-2 French TTS for Lecture Mini.

No API keys are stored in source code, SQLite, or QSettings. Credentials use
Windows Credential Manager via keyring. The request runs in the Qt thread pool.
"""
from __future__ import annotations
import os
import re
from pathlib import Path
from datetime import datetime
from io import BytesIO
import wave
import requests
from reading_language import DEEPGRAM_VOICES_BY_LANGUAGE, voices_for, valid_language

SERVICE = 'LectureMini/DeepgramTTS'
ACCOUNT = 'personal-desktop'
DEEPGRAM_VOICES = DEEPGRAM_VOICES_BY_LANGUAGE['fr']  # Backward compatible French map
URL = 'https://api.deepgram.com/v1/speak'


def get_deepgram_key() -> str:
    env = os.environ.get('DEEPGRAM_API_KEY', '').strip()
    if env:
        return env
    try:
        import keyring
        return (keyring.get_password(SERVICE, ACCOUNT) or '').strip()
    except Exception:
        return ''


def save_deepgram_key(value: str):
    if not value or not value.strip():
        raise ValueError('请输入 Deepgram API Key')
    import keyring
    keyring.set_password(SERVICE, ACCOUNT, value.strip())


def delete_deepgram_key():
    import keyring
    try:
        keyring.delete_password(SERVICE, ACCOUNT)
    except keyring.errors.PasswordDeleteError:
        pass


# Smaller segments improve the chances of getting every clause spoken.
# Each request is complete before the next is appended, and no partial audio
# is cached on a failed segment. More segments can consume more API requests.
MAX_TTS_SEGMENT_CHARS = 70
SILENCE_MS = 140
SAMPLE_RATE = 24000


def split_french_tts(text: str, max_chars: int = MAX_TTS_SEGMENT_CHARS) -> list[str]:
    """Split at clause/word boundaries without dropping or reordering words.

    A short word stays whole. Longer sentences are broken into smaller phrases;
    the original normalized text can be recovered by joining the fragments.
    """
    normalized = ' '.join(text.split())
    if not normalized:
        return []
    if len(normalized) <= max_chars:
        return [normalized]
    if max_chars < 20:
        raise ValueError('max_chars must be at least 20')
    words = normalized.split(' ')
    result = []
    current = ''
    for word in words:
        if len(word) > max_chars:
            raise ValueError('存在过长的单词，无法安全分段生成语音')
        combined = f'{current} {word}' if current else word
        if current and len(combined) > max_chars:
            result.append(current)
            current = word
            continue
        current = combined
        # Prefer a pause after a completed clause (but avoid tiny fragments).
        if len(current) >= 34 and re.search(r'[,;:!?。？！]$', word):
            result.append(current)
            current = ''
    if current:
        result.append(current)
    return result


def _fetch_pcm_segment(fragment: str, key: str, model: str) -> bytes:
    try:
        resp = requests.post(
            URL,
            params={'model': model, 'encoding': 'linear16', 'container': 'none', 'sample_rate': SAMPLE_RATE},
            headers={'Authorization': f'Token {key.strip()}', 'Content-Type': 'application/json'},
            json={'text': fragment},
            timeout=(10, 75),
        )
    except requests.RequestException as exc:
        raise RuntimeError('Deepgram 网络连接失败或超时，请检查网络和代理') from exc
    if resp.status_code in (401, 403):
        raise RuntimeError('Deepgram 拒绝访问：请确认密钥及语音权限')
    if resp.status_code == 429:
        raise RuntimeError('Deepgram 请求过于频繁或额度受限（429），请稍后再试')
    if resp.status_code >= 400:
        raise RuntimeError(f'Deepgram 语音请求失败（HTTP {resp.status_code}），请检查所选模型权限')
    media_type = resp.headers.get('Content-Type', '').lower().split(';', 1)[0].strip()
    raw = resp.content
    if not raw or media_type in ('application/json', 'text/plain', 'text/html'):
        raise RuntimeError('Deepgram 没有返回有效音频')
    if (len(raw) < 2 or len(raw) % 2 or len(raw) > 45_000_000 or
            raw.startswith((b'RIFF', b'ID3', b'{', b'<'))):
        raise RuntimeError('Deepgram 返回的 PCM 音频格式异常或不完整')
    seconds = len(raw) / (SAMPLE_RATE * 2)
    # Catch grossly suspicious output. No waveform inspection can prove that
    # the synthesized speech contains every written word.
    if len(fragment) >= 40 and seconds < max(0.6, len(fragment) * 0.017):
        raise RuntimeError(f'Deepgram 返回的语音异常短（{seconds:.1f} 秒），未缓存不完整音频')
    return raw


def _record_synthesis_diagnostics(model: str, text_size: int, durations: list[float]):
    """Log only audio metadata, never the copied sentence or API key."""
    try:
        base = Path(os.environ.get('APPDATA', str(Path.home()))) / 'LectureMini'
        base.mkdir(parents=True, exist_ok=True)
        location = base / 'Lecture_Deepgram合成诊断.txt'
        if location.exists() and location.stat().st_size > 350_000:
            location.rename(location.with_suffix('.old.txt'))
        with location.open('a', encoding='utf-8') as out:
            out.write(f'{datetime.now().isoformat(timespec="seconds")} '
                      f'voice={model} text_chars={text_size} parts={len(durations)} '
                      f'part_seconds={[round(d,2) for d in durations]}\n')
    except OSError:
        pass


def synthesize_french(text: str, key: str, model: str = 'aura-2-agathe-fr') -> bytes:
    """Legacy French-only API preserved for earlier callers."""
    return synthesize_speech(text, key, model, 'fr')


def synthesize_speech(text: str, key: str, model: str = 'aura-2-agathe-fr',
                      language: str = 'fr') -> bytes:
    """Synthesize all fragments, then package one playable canonical WAV.

    Pre-generation is still silent: the UI's Deepgram button alone requests
    playback. Each fragment is synthesized in reading order; failure aborts the
    whole result rather than caching a partially spoken sentence.
    """
    if not key or not key.strip():
        raise ValueError('Deepgram Key 未配置')
    language = valid_language(language)
    if model not in voices_for(language).values():
        raise ValueError('Deepgram 音色与当前阅读语言不匹配')
    if not isinstance(text, str) or not text.strip():
        raise ValueError('请提供需要朗读的文本')
    if len(text) > 2000:
        raise ValueError('文本太长：每次朗读最多 2000 字符')
    segments = split_french_tts(text)
    frames = []
    durations = []
    spacer = b'\x00\x00' * (SAMPLE_RATE * SILENCE_MS // 1000)
    for index, segment in enumerate(segments):
        try:
            raw = _fetch_pcm_segment(segment, key, model)
        except RuntimeError as exc:
            raise RuntimeError(f'Deepgram 第 {index + 1}/{len(segments)} 段生成失败：{exc}') from exc
        durations.append(len(raw) / (2 * SAMPLE_RATE))
        if index:
            frames.append(spacer)
        frames.append(raw)
    output = BytesIO()
    with wave.open(output, 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(b''.join(frames))
    audio = output.getvalue()
    validate_wav(audio, text)
    _record_synthesis_diagnostics(model, len(text), durations)
    return audio


def validate_wav(raw: bytes, text: str = '') -> float:
    """Validate our completed WAV payload before passing it to Windows.

    The sample count is authoritative for playback duration. We avoid guessing
    speech length from text length because some valid pronunciations are fast;
    no byte-level check can guarantee that the TTS spoke every written word.
    """
    if len(raw) < 44 or raw[:4] != b'RIFF' or raw[8:12] != b'WAVE':
        raise RuntimeError('Deepgram 没有返回完整 WAV 音频，请重试')
    try:
        with wave.open(BytesIO(raw), 'rb') as wav:
            if wav.getcomptype() != 'NONE' or wav.getframerate() <= 0:
                raise ValueError('unsupported audio format')
            frames = wav.getnframes()
            channels = wav.getnchannels()
            sample_width = wav.getsampwidth()
            actual_bytes = len(wav.readframes(frames))
            required_bytes = frames * channels * sample_width
            seconds = frames / wav.getframerate()
            if frames <= 0 or actual_bytes < required_bytes:
                raise ValueError('truncated audio stream')
    except (EOFError, ValueError, wave.Error) as exc:
        raise RuntimeError('Deepgram WAV 数据不完整或格式不正确，未开始播放') from exc
    return seconds
