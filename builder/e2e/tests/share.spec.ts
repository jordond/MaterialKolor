import { expect, test, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import {
  A11Y,
  button,
  focusCanvas,
  LAND_TIMEOUT_MS,
  labelled,
  onPage,
  openWorkspace,
  seedText,
} from '../fixtures/workspace';

// b-503
// Opening a shared link (flow 5.6, F-32). A link opens a project of its own, the same link again
// opens that project rather than a copy, and a link from the old builder keeps its dark preview.

/** The typed seed share code, #6750A4 on TonalSpot. */
const LINK = '/t/AWdQpAAAAABw';

/** An old builder link, as the spec writes it. */
const LEGACY = '/?color_seed=FF6750A4&dark_mode=true&style=Vibrant';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('the same link twice opens one project', async ({ page }) => {
  await openWorkspace(page, LINK);
  await expect.poll(() => seedText(page)).toBe('#6750A4');
  const first = await projectRows(page);

  await openWorkspace(page, LINK);
  await expect.poll(() => seedText(page)).toBe('#6750A4');

  expect(await projectRows(page)).toEqual(first);
  expect(await page.evaluate(() => location.pathname)).toBe('/');
});

test('an old builder link opens its seed and style and keeps the dark preview', async ({ page }) => {
  await openBuilder(page, LEGACY);

  await expect.poll(() => seedText(page), { timeout: 30_000 }).toBe('#6750A4');
  await expect(labelled(page, 'Vibrant, ').and(page.locator('[aria-label$=", radio, selected"]'))).toHaveCount(1);
  await expect(labelled(page, 'Dark, radio, selected')).toHaveCount(1);
  expect(await page.evaluate(() => location.pathname + location.search)).toBe('/');
});

/** The names of the projects the Projects panel lists, opened with P and closed again with Esc. */
async function projectRows(page: Page): Promise<string[]> {
  await focusCanvas(page);
  await page.keyboard.press('p');
  await expect(onPage(page, /^Projects, dialog/)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  const more = page.locator(A11Y).getByRole('button', { name: /^More for / });
  await expect(more.first()).toBeAttached();
  const names = (await more.evaluateAll((rows) => rows.map((row) => row.getAttribute('aria-label') ?? ''))).map((label) =>
    label.replace(/^More for /, ''),
  );
  await page.keyboard.press('Escape');
  await expect(button(page, 'New project')).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  return names;
}
