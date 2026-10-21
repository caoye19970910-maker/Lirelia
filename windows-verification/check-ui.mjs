import { chromium } from 'playwright';
import { writeFile, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

await mkdir('verification-results', { recursive: true });
let browser;
const deadline = Date.now() + 90000;
while (!browser && Date.now() < deadline) {
  try { browser = await chromium.connectOverCDP('http://127.0.0.1:9222'); }
  catch { await new Promise(resolve => setTimeout(resolve, 1000)); }
}
if (!browser) throw new Error('Installed Windows application did not expose its test WebView.');
let page;
try {
  while (!page && Date.now() < deadline) {
    page = browser.contexts().flatMap(context => context.pages())
      .find(candidate => /tauri\.localhost|localhost/.test(candidate.url()));
    if (!page) await new Promise(resolve => setTimeout(resolve, 500));
  }
  if (!page) throw new Error('The installed reader did not open its application page.');
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const decline = page.getByRole('button', { name: 'Not now', exact: true });
  if (await decline.isVisible().catch(() => false)) await decline.click();
  await page.getByRole('button', { name: '打开 Lirelia 学习面板', exact: true }).click({ timeout: 60000 });
  const panel = page.getByRole('region', { name: 'Lirelia 学习面板', exact: true });
  await panel.getByRole('button', { name: '设置', exact: true }).click();
  await panel.getByText('复习调度：FSRS', { exact: false }).waitFor({ timeout: 60000 });
  const daily = panel.getByRole('spinbutton', { name: '每日新词', exact: true });
  await daily.fill('17');
  await daily.blur();
  await page.screenshot({ path: 'verification-results/installed-settings.png', fullPage: true });
  await panel.getByRole('button', { name: '今日复习', exact: true }).click();
  await panel.getByText('当前没有待复习单词。', { exact: false }).waitFor({ timeout: 30000 });
  await page.screenshot({ path: 'verification-results/installed-review.png', fullPage: true });
  await panel.getByRole('button', { name: '设置', exact: true }).click();
  if (await daily.inputValue() !== '17') throw new Error('Native settings persistence failed.');
  await page.getByRole('button', { name: '关闭 Lirelia', exact: true }).click();
  // Only substitute the native file chooser result. Import, document rendering,
  // DOM double-click and the local learning backend remain real.
  await page.evaluate((fixture) => {
    const native = window.__TAURI_INTERNALS__;
    const invoke = native.invoke.bind(native);
    native.invoke = (command, args, options) => command === 'plugin:dialog|open'
      ? Promise.resolve([fixture]) : invoke(command, args, options);
  }, resolve('verification-results/lirelia-test.epub'));
  await page.getByRole('button', { name: 'Import Books', exact: true }).first().click();
  await page.getByRole('menuitem', { name: 'From Local File', exact: true }).click();
  const book = page.locator('[aria-label="Bookshelf"] div[role="button"]').filter({ hasText: 'Lirelia Test' }).first();
  await book.waitFor({ timeout: 60000 });
  await book.click();
  await page.getByRole('dialog', { name: '选择这本书的阅读语言' })
    .getByRole('button', { name: '法语', exact: true }).click();
  let reader;
  const readerDeadline = Date.now() + 60000;
  while (!reader && Date.now() < readerDeadline) {
    reader = browser.contexts().flatMap(context => context.pages())
      .find(candidate => /\/reader(?:\/|\?|$)/.test(candidate.url()));
    if (!reader) await new Promise(resolve => setTimeout(resolve, 500));
  }
  if (!reader) throw new Error('Opening the test EPUB did not create the reader.');
  await reader.evaluate(() => {
    window.__lireliaTestQueries = [];
    const native = window.__TAURI_INTERNALS__;
    const invoke = native.invoke.bind(native);
    native.invoke = (command, args, options) => {
      if (command === 'plugin:shell|stdin_write') {
        try {
          const request = JSON.parse(args.buffer);
          if (request.method === 'query') window.__lireliaTestQueries.push(request.params);
        } catch {}
      }
      return invoke(command, args, options);
    };
  });
  let word;
  while (!word && Date.now() < readerDeadline) {
    for (const frame of reader.frames()) {
      const candidate = frame.locator('em').filter({ hasText: 'disait' });
      if (await candidate.isVisible().catch(() => false)) { word = candidate; break; }
    }
    if (!word) await new Promise(resolve => setTimeout(resolve, 500));
  }
  if (!word) throw new Error('The EPUB document did not render its test word.');
  await word.dblclick();
  const lookup = reader.getByRole('region', { name: 'Lirelia 学习面板', exact: true });
  await lookup.getByRole('heading', { name: '整句翻译', exact: true }).waitFor();
  if (await lookup.getByRole('textbox', { name: '查询内容', exact: true }).inputValue() !== 'disait')
    throw new Error('A real DOM double-click did not open the selected word.');
  await lookup.getByText('Il disait toujours la vérité.', { exact: true }).waitFor();
  await reader.waitForFunction(() => window.__lireliaTestQueries?.length >= 2, { timeout: 30000 });
  const requests = await reader.evaluate(() => window.__lireliaTestQueries);
  if (!requests.some(p => p.text === 'disait' && p.language === 'fr') ||
      !requests.some(p => p.text === 'Il disait toujours la vérité.' && p.kind === 'sentence' && p.language === 'fr'))
    throw new Error('Double-click did not dispatch both the word and the exact sentence to the real sidecar.');
  await reader.screenshot({ path: 'verification-results/installed-double-click.png', fullPage: true });
  await writeFile('verification-results/report.json', JSON.stringify({
    passed: true, url: page.url(), checks: ['installed application startup',
      'Lirelia panel', 'native sidecar FSRS settings', 'settings write and read',
      'empty review queue', 'EPUB native import and rendering',
      'native DOM double-click lookup', 'exact full-sentence extraction',
      'word and sentence requests to native sidecar'], pageErrors: errors,
  }, null, 2));
} catch (error) {
  if (page) {
    await page.screenshot({ path: 'verification-results/failure.png', fullPage: true }).catch(() => {});
    await writeFile('verification-results/failure.txt', `${error.stack}\n\n${await page.locator('body').innerText().catch(() => '')}`);
  }
  throw error;
} finally {
  await browser.close();
}
