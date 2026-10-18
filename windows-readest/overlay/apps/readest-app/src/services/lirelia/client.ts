import { isTauriAppPlatform } from '@/services/environment';
import type { Child } from '@tauri-apps/plugin-shell';
import type { LireliaSelection, ReadingLanguage } from './types';

const pending = new Map<
  number,
  {
    resolve: (result: unknown) => void;
    reject: (error: Error) => void;
    timer: ReturnType<typeof setTimeout>;
  }
>();
let sequence = 0;
let starting: Promise<Child> | undefined;

function rejectPending(message: string) {
  for (const request of pending.values()) {
    clearTimeout(request.timer);
    request.reject(new Error(message));
  }
  pending.clear();
  starting = undefined;
}

async function start(): Promise<Child> {
  if (!isTauriAppPlatform()) throw new Error('Lirelia 学习模块请在 Windows 桌面版中使用。');
  if (!starting) {
    starting = (async () => {
      const { Command } = await import('@tauri-apps/plugin-shell');
      const command = Command.sidecar('binaries/lirelia-core', [], { encoding: 'utf-8' });
      command.stdout.on('data', (line) => {
        // The shell plugin emits one complete line per data event.
        try {
          const response = JSON.parse(line) as {
            id: number;
            ok: boolean;
            result?: unknown;
            error?: string;
          };
          const request = pending.get(response.id);
          if (!request) return;
          clearTimeout(request.timer);
          pending.delete(response.id);
          if (response.ok) request.resolve(response.result);
          else request.reject(new Error(response.error || 'Lirelia 操作失败。'));
        } catch {
          rejectPending('Lirelia 后台返回了无法识别的数据，请重新打开学习面板。');
        }
      });
      command.on('error', () => rejectPending('Lirelia 后台启动失败，请检查安装是否完整。'));
      command.on('close', () => rejectPending('Lirelia 后台已结束，请重试。'));
      const child = await command.spawn();
      window.addEventListener('pagehide', () => void child.kill(), { once: true });
      return child;
    })().catch(() => {
      starting = undefined;
      throw new Error('无法启动 Lirelia 学习模块，请使用附带后台模块的桌面安装包。');
    });
  }
  return starting;
}

export async function lireliaCall<T>(method: string, params: object = {}): Promise<T> {
  const child = await start();
  const id = ++sequence;
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => {
      pending.delete(id);
      reject(new Error('请求超时，请检查网络后重试。'));
    }, 180_000);
    pending.set(id, { resolve: (value) => resolve(value as T), reject, timer });
    void child.write(`${JSON.stringify({ id, method, params })}\n`).catch(() => {
      const request = pending.get(id);
      if (!request) return;
      clearTimeout(request.timer);
      pending.delete(id);
      request.reject(new Error('无法连接 Lirelia 后台，请重试。'));
    });
  });
}

export function openLirelia(selection?: LireliaSelection) {
  window.dispatchEvent(new CustomEvent('lirelia-open', { detail: selection }));
}

export async function chooseBookLanguage(
  bookId: string,
  title: string,
  force = false,
): Promise<boolean> {
  if (!isTauriAppPlatform()) return true;
  try {
    if (!force && (await lireliaCall<ReadingLanguage | null>('book_language', { bookId })))
      return true;
    return await new Promise<boolean>((resolve) => {
      window.dispatchEvent(
        new CustomEvent('lirelia-language', { detail: { bookId, title, resolve } }),
      );
    });
  } catch {
    // A missing optional sidecar must not stop the original reader opening books.
    return true;
  }
}
