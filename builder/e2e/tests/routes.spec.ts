import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import { LAND_TIMEOUT_MS, onPage, openOverlay, openWorkspace, pressKeyFor, typeSeed } from '../fixtures/workspace';

// The address bar through real edits and panels. An edit never moves the address or
// adds a history entry, and a panel pushes exactly one entry at the same address, which Back pops.
// router.spec.ts drives the router's own hooks; here the keys and the seed field do it.

/** The Export sheet's title in the mirror, the dialog fold. */
const EXPORT_DIALOG = /^Export code, dialog/;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('an edit keeps the address at / and adds no history entry', async ({ page }) => {
  await openWorkspace(page);
  const entries = await historyLength(page);

  await typeSeed(page, '#0B6E4F');

  expect(await address(page)).toBe('/');
  expect(await historyLength(page)).toBe(entries);
});

test('E pushes one entry at the same address and Back closes the sheet', async ({ page }) => {
  await openWorkspace(page);
  const entries = await historyLength(page);

  await pressKeyFor(page, 'e', onPage(page, EXPORT_DIALOG));
  expect(await historyLength(page)).toBe(entries + 1);
  expect(await address(page)).toBe('/');
  expect(await openOverlay(page)).toBe('Export');

  await page.evaluate(() => history.back());
  await expect(onPage(page, EXPORT_DIALOG)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  expect(await openOverlay(page)).toBeNull();
  expect(await address(page)).toBe('/');
});

async function address(page: Page): Promise<string> {
  return page.evaluate(() => location.pathname + location.search + location.hash);
}

async function historyLength(page: Page): Promise<number> {
  return page.evaluate(() => history.length);
}
