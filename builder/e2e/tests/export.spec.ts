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

// Export. The zip holds every file the sheet shows, its Kotlin byte for byte the codegen's
// golden for the default theme, and the package stays across a seed change, a reload and a new
// project. first-run.spec.ts copies a file to the clipboard.

/** The Export sheet's title in the mirror, the dialog fold. */
const EXPORT_DIALOG = /^Export code, dialog/;

/** The codegen's golden for the builder's first theme, on Material 3, Dynamic. */
const GOLDEN = path.resolve(__dirname, '../../codegen/src/jvmTest/resources/golden/material3-dynamic-first-theme');

/** The files of [GOLDEN], by their path under it. */
const GOLDEN_FILES = ['src/commonMain/kotlin/com/example/theme/Color.kt', 'src/commonMain/kotlin/com/example/theme/Theme.kt'];

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('Download zip holds every file the sheet shows, its Kotlin as the golden has it', async ({ page }) => {
  // WebKit raises the download too. The zip starts inside the press, as Safari asks.
  await openSheet(page);
  const tabs = await fileTabs(page);

  const download = page.waitForEvent('download');
  await press(page, button(page, 'Download zip'));
  const zip = readZip(readFileSync((await (await download).path())!));

  expect([...zip.keys()].map((name) => path.posix.basename(name)).sort()).toEqual([...tabs].sort());
  for (const file of GOLDEN_FILES) {
    const entry = [...zip.keys()].find((name) => name.endsWith(file));
    expect(entry, file).toBeDefined();
    const golden = readFileSync(path.join(GOLDEN, file), 'utf8');
    expect(asGolden(zip.get(entry!)!.toString('utf8'), golden), file).toBe(golden);
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

/** The link back to the builder on a file's second line, its origin and its share code. */
const LINK = /^(\/\/ Open this theme in the builder at )(https?:\/\/[^/]+)\/t\/([A-Za-z0-9_-]+)$/m;

/**
 * [code] as the golden [golden] would spell it. The page links back to its own origin, the test
 * server's, and its code carries the project's name, which the golden's first theme has none of. So
 * the origin becomes the golden's and the name leaves the code, and every other byte is compared as is.
 */
function asGolden(code: string, golden: string): string {
  const goldenLink = LINK.exec(golden);
  if (goldenLink === null) throw new Error('The golden has no link back');
  return code.replace(LINK, (_, lead: string, origin: string, shareCode: string) => {
    expect(origin).toBe(new URL(site('/')).origin);
    return `${lead}${goldenLink[2]}/t/${withoutProjectName(shareCode)}`;
  });
}

// The share code's layout, ShareCodec.kt in builder/domain.
const SECTIONS_BYTE = 7;
const SECTION_PROJECT_NAME = 0x10;

/**
 * [shareCode] with its project name taken out and the checksum written again. Only for a code whose
 * one section is the name, as the default theme's is, since the name then runs to the checksum.
 */
function withoutProjectName(shareCode: string): string {
  const bytes = Buffer.from(shareCode, 'base64url');
  expect(bytes[SECTIONS_BYTE], 'only a project name after the header').toBe(SECTION_PROJECT_NAME);
  const length = bytes[SECTIONS_BYTE + 1]!;
  expect(SECTIONS_BYTE + 2 + length, 'the name runs to the checksum').toBe(bytes.length - 1);
  const body = Buffer.from(bytes.subarray(0, SECTIONS_BYTE + 1));
  body[SECTIONS_BYTE] = 0;
  return Buffer.concat([body, Buffer.from([crc8(body)])]).toString('base64url');
}

/** The CRC-8 a share code ends with, polynomial 0x07, from zero, nothing reflected. */
function crc8(bytes: Uint8Array): number {
  let crc = 0;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = crc & 0x80 ? ((crc << 1) ^ 0x07) & 0xff : (crc << 1) & 0xff;
  }
  return crc;
}
