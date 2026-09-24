import { expect, test } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import { button, labelled, openWorkspace, press, seedText, storedProjects } from '../fixtures/workspace';

// b-503
// Opening a shared link (flow 5.6, F-32). A link opens as a theme that is not in the projects until
// it is saved, the same link again then opens that project rather than a copy, and a link from the
// old builder keeps its dark preview.

/** The typed seed share code, #6750A4 on TonalSpot. */
const LINK = '/t/AWdQpAAAAABw';

/** The banner button that keeps a theme opened from a link, `share_transient_save`. */
const SAVE = 'Save to my projects';

/** An old builder link, as the spec writes it. */
const LEGACY = '/?color_seed=FF6750A4&dark_mode=true&style=Vibrant';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('a saved link opened again opens that project, not a copy', async ({ page }) => {
  await openWorkspace(page, LINK);
  await expect.poll(() => seedText(page)).toBe('#6750A4');
  const before = await storedProjects(page);
  await press(page, button(page, SAVE));
  await expect.poll(async () => (await storedProjects(page)).length).toBe(before.length + 1);
  const saved = await storedProjects(page);

  await openWorkspace(page, LINK);
  await expect.poll(() => seedText(page)).toBe('#6750A4');

  await expect(button(page, SAVE)).toHaveCount(0);
  expect((await storedProjects(page)).map((project) => project.id).sort()).toEqual(saved.map((project) => project.id).sort());
  expect(await page.evaluate(() => location.pathname)).toBe('/');
});

test('an old builder link opens its seed and style and keeps the dark preview', async ({ page }) => {
  await openBuilder(page, LEGACY);

  await expect.poll(() => seedText(page), { timeout: 30_000 }).toBe('#6750A4');
  await expect(labelled(page, 'Vibrant, ').and(page.locator('[aria-label$=", radio, selected"]'))).toHaveCount(1);
  await expect(labelled(page, 'Dark, radio, selected')).toHaveCount(1);
  expect(await page.evaluate(() => location.pathname + location.search)).toBe('/');
});
