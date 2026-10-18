import { act, cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
import '@/styles/globals.css';

vi.mock('@/services/lirelia/client', () => ({
  lireliaCall: async (method: string) => {
    if (method === 'settings')
      return { autoSpeak: false, dailyNew: 15, models: [], credentials: {} };
    if (method === 'book_language') return 'fr';
    if (method === 'query')
      return {
        kind: 'word',
        lemma: 'aller',
        answer:
          'allait\n\n词性：动词\n原形：aller\n\n这是 aller 的直陈式未完成过去时第三人称单数形式。\n\n常见义项：\n1. 去；前往。\n2. 进展；情况如何。\n3. 适合；相配。',
      };
    return true;
  },
}));
vi.mock('@/services/environment', () => ({ isTauriAppPlatform: () => true }));
vi.mock('@/hooks/useAppRouter', () => ({ useAppRouter: () => ({ push: () => {} }) }));
vi.mock('@/utils/nav', () => ({ navigateToReader: () => {} }));
vi.mock('@/store/readerStore', () => ({
  useReaderStore: { getState: () => ({ getView: () => null }) },
}));
import LireliaPanel from '@/components/lirelia/LireliaPanel';

afterEach(() => {
  cleanup();
  document.documentElement.removeAttribute('data-eink');
});

describe('Lirelia real-browser layout', () => {
  it('keeps the full definition readable in the original theme and eink mode', async () => {
    document.documentElement.setAttribute('data-theme', 'light');
    render(<LireliaPanel />);
    act(() =>
      window.dispatchEvent(
        new CustomEvent('lirelia-open', {
          detail: {
            text: 'allait',
            source: {
              bookId: 'demo',
              title: '示例书籍 · La Cinquième République',
              cfi: 'epubcfi(/6/2)',
              context: 'La situation allait changer.',
            },
          },
        }),
      ),
    );
    await screen.findByText(/这是 aller/);
    const panel = screen.getByRole('region', { name: 'Lirelia 学习面板' });
    expect(panel.getBoundingClientRect().width).toBe(440);
    expect(panel.scrollWidth).toBeLessThanOrEqual(panel.clientWidth);
    expect(screen.getByRole('button', { name: '原形：aller' })).toBeTruthy();
    await page.screenshot({ element: panel, path: '../../../../../lirelia/qa/panel.png' });
    document.documentElement.setAttribute('data-eink', 'true');
    expect(getComputedStyle(panel).borderTopWidth).toBe('1px');
    await page.screenshot({ element: panel, path: '../../../../../lirelia/qa/panel-eink.png' });
  });
});
