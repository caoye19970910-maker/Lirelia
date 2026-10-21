import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const { call } = vi.hoisted(() => ({ call: vi.fn() }));
vi.mock('@/services/lirelia/client', () => ({ lireliaCall: call }));
vi.mock('@/services/environment', () => ({ isTauriAppPlatform: () => true }));
vi.mock('@/hooks/useAppRouter', () => ({ useAppRouter: () => ({ push: vi.fn() }) }));
vi.mock('@/utils/nav', () => ({ navigateToReader: vi.fn() }));
vi.mock('@/store/readerStore', () => ({
  useReaderStore: { getState: () => ({ getView: () => null }) },
}));
import LireliaPanel from '@/components/lirelia/LireliaPanel';

const settings = {
  dailyNew: 15,
  autoSpeak: true,
  geminiModel: 'test',
  geminiSlot: 1,
  deeplSlot: 1,
  voiceFr: 'fr',
  voiceEn: 'en',
  playbackRate: 1,
  models: ['test'],
  scheduler: 'FSRS',
  credentials: { gemini: true, deepl: true, deepgram: true },
};

function select(text: string) {
  window.dispatchEvent(
    new CustomEvent('lirelia-open', {
      detail: {
        text,
        source: {
          bookId: 'b1',
          title: 'French Book',
          cfi: 'epubcfi(/6/2)',
          context: `Il disait ${text}.`,
        },
      },
    }),
  );
}

describe('Lirelia integration', () => {
  beforeEach(() => {
    call.mockReset();
    call.mockImplementation(async (method: string, params: { text?: string; kind?: string }) => {
      if (method === 'settings') return settings;
      if (method === 'book_language') return 'fr';
      if (method === 'query')
        return {
          answer: params.kind === 'sentence' ? '整句中文译文' : '完整 Gemini 释义',
          kind: params.kind ?? 'word',
          text: params.text,
        };
      if (method === 'speech') throw new Error('音频测试不联网');
      return true;
    });
  });
  afterEach(cleanup);

  it('saves the complete definition and source using the existing auto-enrollment operation', async () => {
    render(<LireliaPanel />);
    act(() => select('disait'));
    await screen.findByText('完整 Gemini 释义');
    fireEvent.click(screen.getByRole('button', { name: /^收藏$/ }));
    await waitFor(() =>
      expect(call).toHaveBeenCalledWith(
        'save',
        expect.objectContaining({
          text: 'disait',
          language: 'fr',
          kind: 'word',
          result: expect.objectContaining({ answer: '完整 Gemini 释义' }),
          source: expect.objectContaining({ bookId: 'b1', cfi: 'epubcfi(/6/2)' }),
        }),
      ),
    );
    await screen.findByText('已收藏，已自动加入复习队列。');
  });

  it('keeps the word definition when sentence translation fails', async () => {
    const original = call.getMockImplementation()!;
    call.mockImplementation(async (method: string, params: { kind?: string }) => {
      if (method === 'query' && params.kind === 'sentence') throw new Error('整句翻译失败');
      return original(method, params);
    });
    render(<LireliaPanel />);
    act(() => select('disait'));
    await screen.findByText('完整 Gemini 释义');
    await screen.findByText('整句翻译失败');
    expect(screen.getByRole('button', { name: /^收藏$/ })).toBeTruthy();
  });

  it('shows the sentence while the definition is still pending', async () => {
    const original = call.getMockImplementation()!;
    call.mockImplementation(async (method: string, params: { kind?: string }) => {
      if (method === 'query' && params.kind !== 'sentence') return new Promise(() => {});
      return original(method, params);
    });
    render(<LireliaPanel />);
    act(() => select('disait'));
    await screen.findByText('整句中文译文');
    expect(screen.queryByText('完整 Gemini 释义')).toBeNull();
  });

  it('does not play or display a stale query after the panel closes', async () => {
    let complete: (value: object) => void = () => {};
    call.mockImplementation(async (method: string) => {
      if (method === 'settings') return settings;
      if (method === 'book_language') return 'fr';
      if (method === 'query')
        return new Promise((resolve) => {
          complete = resolve;
        });
      return true;
    });
    render(<LireliaPanel />);
    act(() => select('bonjour'));
    await waitFor(() =>
      expect(call).toHaveBeenCalledWith('query', expect.objectContaining({ text: 'bonjour' })),
    );
    fireEvent.click(screen.getByRole('button', { name: '关闭 Lirelia' }));
    await act(async () => complete({ answer: '迟到的释义', kind: 'word' }));
    expect(screen.queryByText('迟到的释义')).toBeNull();
    expect(call.mock.calls.some(([method]) => method === 'speech')).toBe(false);
  });

  it('translates sentences first and only analyzes on demand without automatic speech', async () => {
    call.mockImplementation(async (method: string, params: { kind?: string }) => {
      if (method === 'settings') return settings;
      if (method === 'book_language') return 'en';
      if (method === 'query')
        return {
          answer: params.kind === 'analysis' ? '语法解析' : '中文译文',
          kind: params.kind ?? 'sentence',
        };
      return true;
    });
    render(<LireliaPanel />);
    act(() => select('This is a sentence.'));
    await screen.findByText('中文译文');
    expect(call.mock.calls.some(([method]) => method === 'speech')).toBe(false);
    expect(call.mock.calls.filter(([method]) => method === 'query')).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: '详细解析' }));
    await screen.findByText('语法解析');
    expect(call).toHaveBeenCalledWith('query', {
      text: 'This is a sentence.',
      language: 'en',
      kind: 'analysis',
    });
  });

  it('does not auto-play the next review card if the panel closed during rating', async () => {
    let finishRating: () => void = () => {};
    let rated = false;
    call.mockImplementation(async (method: string) => {
      if (method === 'settings') return settings;
      if (method === 'review_queue')
        return [{ text: rated ? 'second' : 'first', language: 'fr', result: { answer: '释义' } }];
      if (method === 'summary') return { total: 2, due_words: 2, new_words: 0, reviewed_today: 0 };
      if (method === 'rate')
        return new Promise((resolve) => {
          finishRating = () => {
            rated = true;
            resolve(true);
          };
        });
      if (method === 'speech') throw new Error('音频测试不联网');
      return true;
    });
    render(<LireliaPanel />);
    fireEvent.click(screen.getByRole('button', { name: '打开 Lirelia 学习面板' }));
    await waitFor(() => expect(call).toHaveBeenCalledWith('settings'));
    fireEvent.click(screen.getByRole('button', { name: '今日复习' }));
    await screen.findByText('first');
    fireEvent.click(screen.getByRole('button', { name: '查看释义' }));
    fireEvent.click(screen.getByRole('button', { name: '记得' }));
    await waitFor(() =>
      expect(call).toHaveBeenCalledWith('rate', expect.objectContaining({ text: 'first' })),
    );
    fireEvent.click(screen.getByRole('button', { name: '关闭 Lirelia' }));
    const before = call.mock.calls.filter(([method]) => method === 'speech').length;
    await act(async () => finishRating());
    await waitFor(() =>
      expect(call.mock.calls.filter(([method]) => method === 'review_queue')).toHaveLength(2),
    );
    expect(call.mock.calls.filter(([method]) => method === 'speech')).toHaveLength(before);
  });
});
