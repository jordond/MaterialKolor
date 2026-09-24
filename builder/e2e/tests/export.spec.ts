import { readFileSync } from 'node:fs';
import path from 'node:path';
import { expect, test, type Page } from '@playwright/test';
import { reloadBuilder, site, wantHooks } from './builder';
import { readZip } from '../fixtures/zip';
import {
  A11Y,
  button,
  LAND_TIMEOUT_MS,
  labelled,
  onPage,
  openWorkspace,
  press,
  pressFor,
  pressKeyFor,
  typeInto,
  typeSeed,
} from '../fixtures/workspace';

// b-503
// Export (flow 5.4). The zip holds every file the sheet shows, its Kotlin byte for byte the codegen's
// golden for the default theme, and the package stays across a seed change, a reload and a new
// project. first-run.spec.ts copies a file to the clipboard.

/** The Export sheet's title in the mirror, the dialog fold (D40). */
const EXPORT_DIALOG = /^Export code, dialog/;

/** The codegen's golden for the default seed on Material 3, Dynamic, the builder's first theme. */
const GOLDEN = path.resolve(__dirname, '../../codegen/src/jvmTest/resources/golden/material3-dynamic-default');

/** The files of [GOLDEN], by their path under it. */
const GOLDEN_FILES = ['src/commonMain/kotlin/com/example/theme/Color.kt', 'src/commonMain/kotlin/com/example/theme/Theme.kt'];

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('Download zip holds every file the sheet shows, its Kotlin as the golden has it', async ({ page }) => {
  await openSheet(page);
  const tabs = await fileTabs(page);

  const download = page.waitForEvent('download');
  await press(page, button(page, 'Download zip'));
  const zip = readZip(readFileSync((await (await download).path())!));

  expect([...zip.keys()].map((name) => path.posix.basename(name)).sort()).toEqual([...tabs].sort());
  for (const file of GOLDEN_FILES) {
    const entry = [...zip.keys()].find((name) => name.endsWith(file));
    expect(entry, file).toBeDefined();
    expect(withoutLink(zip.get(entry!)!.toString('utf8')), file).toBe(withoutLink(readFileSync(path.join(GOLDEN, file), 'utf8')));
    expect(linkLine(zip.get(entry!)!.toString('utf8')), file).toMatch(
      new RegExp(`^// Open this theme in the builder at ${escape(new URL(site('/')).origin)}/t/[A-Za-z0-9_-]+$`),
    );
  }
});

test('the package stays across a seed change, a reload and a new project', async ({ page }) => {
  await openSheet(page);
  await typeInto(page, textbox(page, 'Package name'), 'com.acme.app');
  await closeSheet(page);

  await typeSeed(page, '#0B6E4F');
  await openSheet(page);
  await expect.poll(() => textOf(page, 'Package name')).toBe('com.acme.app');
  await expect(page.locator(A11Y)).toContainText('0xFF0B6E4F');
  await closeSheet(page);

  await reloadBuilder(page);
  await openSheet(page);
  await expect.poll(() => textOf(page, 'Package name')).toBe('com.acme.app');
  await closeSheet(page);

  await pressKeyFor(page, 'p', button(page, 'New project'));
  await pressFor(page, button(page, 'New project'), labelled(page, 'Projects, '));
  await expect(onPage(page, /^Projects, dialog/)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  await openSheet(page);
  await expect.poll(() => textOf(page, 'Package name')).toBe('com.acme.app');
});

/** Opens the Export sheet with E. */
async function openSheet(page: Page): Promise<void> {
  const url = page.url();
  if (!url.startsWith('http')) await openWorkspace(page);
  await pressKeyFor(page, 'e', onPage(page, EXPORT_DIALOG));
}

async function closeSheet(page: Page): Promise<void> {
  await press(page, button(page, 'Close'));
  await expect(onPage(page, EXPORT_DIALOG)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
}

/** The file names on the sheet's tabs. */
async function fileTabs(page: Page): Promise<string[]> {
  const tabs = page.locator(`${A11Y} [aria-label*=", tab, "]`);
  const labels = await tabs.evaluateAll((all) => all.map((tab) => tab.getAttribute('aria-label') ?? ''));
  return labels.map((label) => label.replace(/, tab, .*$/, '')).filter((name) => /\.[a-z]+$/.test(name));
}

function textbox(page: Page, name: string) {
  return page.locator(A11Y).getByRole('textbox', { name, exact: true });
}

async function textOf(page: Page, name: string): Promise<string> {
  return ((await textbox(page, name).first().textContent()) ?? '').trim();
}

/** [code] without its second line, the link back, which carries the page's origin and the project's name. */
function withoutLink(code: string): string {
  return code.split('\n').filter((_, index) => index !== 1).join('\n');
}

function linkLine(code: string): string {
  return code.split('\n')[1];
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
