import { beforeEach, describe, expect, it, vi } from 'vitest';

const { stdout, events, write, spawn, sidecar } = vi.hoisted(() => {
  const stdout = new Map<string, (data: string) => void>();
  const events = new Map<string, () => void>();
  const write = vi.fn(async (_data: string) => {});
  const kill = vi.fn(async () => {});
  const spawn = vi.fn(async () => ({ write, kill }));
  const command = {
    stdout: { on: (name: string, fn: (data: string) => void) => stdout.set(name, fn) },
    on: (name: string, fn: () => void) => events.set(name, fn),
    spawn,
  };
  return { stdout, events, write, kill, spawn, sidecar: vi.fn(() => command) };
});
vi.mock('@/services/environment', () => ({ isTauriAppPlatform: () => true }));
vi.mock('@tauri-apps/plugin-shell', () => ({ Command: { sidecar } }));

describe('Lirelia sidecar transport', () => {
  beforeEach(() => {
    vi.resetModules();
    stdout.clear();
    events.clear();
    write.mockClear();
    spawn.mockClear();
  });

  it('starts one child and matches out-of-order responses by request id', async () => {
    const { lireliaCall } = await import('@/services/lirelia/client');
    const first = lireliaCall<string>('query', { text: 'a' });
    const second = lireliaCall<string>('query', { text: 'b' });
    await vi.waitFor(() => expect(write).toHaveBeenCalledTimes(2));
    const requests = write.mock.calls.map((args) => JSON.parse(String(args[0])) as { id: number });
    stdout.get('data')!(JSON.stringify({ id: requests[1]!.id, ok: true, result: 'second' }));
    stdout.get('data')!(JSON.stringify({ id: requests[0]!.id, ok: true, result: 'first' }));
    expect(await first).toBe('first');
    expect(await second).toBe('second');
    expect(spawn).toHaveBeenCalledTimes(1);
    expect(sidecar).toHaveBeenCalledWith('binaries/lirelia-core', [], { encoding: 'utf-8' });
  });

  it('rejects outstanding work on exit and restarts on the next request', async () => {
    const { lireliaCall } = await import('@/services/lirelia/client');
    const first = lireliaCall('settings');
    const rejection = expect(first).rejects.toThrow('Lirelia 后台已结束');
    await vi.waitFor(() => expect(write).toHaveBeenCalledTimes(1));
    events.get('close')!();
    await rejection;
    const second = lireliaCall('settings');
    await vi.waitFor(() => expect(write).toHaveBeenCalledTimes(2));
    const request = JSON.parse(String(write.mock.calls[1]![0])) as { id: number };
    stdout.get('data')!(JSON.stringify({ id: request.id, ok: true, result: {} }));
    await second;
    expect(spawn).toHaveBeenCalledTimes(2);
  });
});
