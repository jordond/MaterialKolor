import { expect, test, type Page } from '@playwright/test';
import { hook, openBuilder, reloadBuilder, wantHooks } from './builder';
import {
  A11Y,
  boxOf,
  button,
  dragSplit,
  focusCanvas,
  LAND_TIMEOUT_MS,
  labelled,
  openWorkspace,
  press,
  pressFor,
  pressKeyFor,
  primaryKey,
  seedText,
  SPLIT_HANDLE,
  typeSeed,
} from '../fixtures/workspace';

// The one hint a first visit gets, read from the page's accessibility tree. A dismissal kept
// in storage has to hold through a reload.

/** The hint, word for word, `poster_first_run_hint`. */
const HINT = 'Paste a color or drop an image anywhere.';

/**
 * The poster's Projects button once boot has opened a project. It reads "Projects" and the project's
 * name, and only "Projects" before that. The hint shows in the same frame the name does.
 */
const NAMED_PROJECTS = '#cmp_a11y_root [aria-label^="Projects, "]';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('first run', () => {
  test('a fresh page shows the hint', async ({ page }) => {
    await openBuilder(page);

    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });
    // The boot signal the next test waits on is there beside the hint.
    await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached();
  });

  test('a dismissed hint is gone after a reload', async ({ page }) => {
    await openBuilder(page);
    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });

    expect(await hook(page, 'addHint', 'first-run')).toBe('Done');
    await reloadBuilder(page);
    await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached({ timeout: 30_000 });

    await expect(onPage(page, HINT)).toHaveCount(0);
  });
});

// The whole first run. A typed seed, a drag of the split, Inspect over the app's floating
// button, three shuffles and two undos, then Theme.kt copied from Export, which retires the hint.

test('the first run, from a typed seed to a copied Theme.kt, retires the hint for good', async ({
  page,
  context,
  browserName,
}) => {
  if (browserName === 'chromium') await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  await openWorkspace(page);
  await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });

  await typeSeed(page, '#0B6E4F');
  const handle = page.locator(A11Y).getByText(SPLIT_HANDLE);
  const split = await handle.first().textContent();
  await dragSplit(page, -200);
  await expect.poll(() => handle.first().textContent()).not.toBe(split);

  const readout = page.locator(A11Y).getByText(/onPrimaryContainer/);
  await expect(readout).toHaveCount(0);
  await pressKeyFor(page, 'i', labelled(page, 'Inspect, checked'));
  const fab = await boxOf(button(page, 'New trip'));
  await page.mouse.move(fab.x + fab.width / 2, fab.y + fab.height / 2);
  await expect(readout.first()).toBeAttached({ timeout: LAND_TIMEOUT_MS });
  await pressKeyFor(page, 'i', labelled(page, 'Inspect, not checked'));

  await focusCanvas(page);
  const seeds = [await seedText(page)];
  for (let shuffle = 0; shuffle < 3; shuffle++) {
    await page.keyboard.press('Space');
    await expect.poll(() => seedText(page), { timeout: LAND_TIMEOUT_MS }).not.toBe(seeds.at(-1));
    seeds.push(await seedText(page));
  }
  const primary = await primaryKey(page);
  await page.keyboard.press(`${primary}+z`);
  await expect.poll(() => seedText(page), { timeout: LAND_TIMEOUT_MS }).toBe(seeds[2]);
  await page.keyboard.press(`${primary}+z`);
  await expect.poll(() => seedText(page), { timeout: LAND_TIMEOUT_MS }).toBe(seeds[1]);

  await pressKeyFor(page, 'e', labelled(page, 'Theme.kt, tab, '));
  await pressFor(page, labelled(page, 'Theme.kt, tab, not selected'), labelled(page, 'Theme.kt, tab, selected'));
  await pressFor(page, button(page, 'Copy file'), button(page, 'Copied'));
  if (browserName === 'chromium') {
    expect(await page.evaluate(() => navigator.clipboard.readText())).toContain('package com.example.theme');
  }
  await press(page, button(page, 'Close'));
  await expect(page.locator(A11Y).getByText(/^Export code, dialog/)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });

  await expect(onPage(page, HINT)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  await reloadBuilder(page);
  await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached({ timeout: 30_000 });
  await expect(onPage(page, HINT)).toHaveCount(0);
});

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string) {
  return page.locator('#cmp_a11y_root').getByText(text);
}
