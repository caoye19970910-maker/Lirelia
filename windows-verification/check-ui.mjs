import { chromium } from 'playwright';
import { writeFile, mkdir } from 'node:fs/promises';

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
  await writeFile('verification-results/report.json', JSON.stringify({
    passed: true, url: page.url(), checks: ['installed application startup',
      'Lirelia panel', 'native sidecar FSRS settings', 'settings write and read',
      'empty review queue'], pageErrors: errors,
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
