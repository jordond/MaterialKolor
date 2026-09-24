import { expect, test } from '@playwright/test';
import { reloadBuilder, wantHooks } from './builder';
import {
  button,
  focusCanvas,
  LAND_TIMEOUT_MS,
  labelled,
  onPage,
  openWorkspace,
  press,
  pressFor,
  seedField,
  seedText,
  storedDocument,
  storedProjects,
  typeInto,
  typeSeed,
} from '../fixtures/workspace';

// b-503
// Save and return (flow 5.5). Autosave keeps the edit and its undo history through a reload, and the
// Projects panel renames, duplicates and deletes, with the delete undone from its toast.

/** The Projects panel's title in the mirror, the dialog fold (D40). */
const PROJECTS_DIALOG = /^Projects, dialog/;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('an edit is saved, and a reload brings back the seed and the undo that reverts it', async ({ page }) => {
  await openWorkspace(page);
  const first = await seedText(page);

  await typeSeed(page, '#0B6E4F');
  await expect.poll(async () => (await storedDocument(page))?.seed).toBe('#0B6E4F');
  await reloadBuilder(page);

  await expect(seedField(page)).toBeAttached({ timeout: LAND_TIMEOUT_MS });
  await expect.poll(() => seedText(page)).toBe('#0B6E4F');
  const undo = button(page, /^Undo\b/).and(page.locator(':not([aria-label$="disabled"])'));
  await press(page, undo);
  await expect.poll(() => seedText(page), { timeout: LAND_TIMEOUT_MS }).toBe(first);
});

test('Projects renames, duplicates, switches and deletes, and the toast undoes the delete', async ({ page }) => {
  await openWorkspace(page);
  await expect.poll(async () => (await storedProjects(page)).length).toBe(1);
  const [original] = await storedProjects(page);
  await focusCanvas(page);
  await page.keyboard.press('p');
  await expect(onPage(page, PROJECTS_DIALOG)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });

  await pressFor(page, button(page, `More for ${original.name}`), button(page, 'Rename'));
  await press(page, button(page, 'Rename'));
  const name = page.locator('#cmp_a11y_root').getByRole('textbox', { name: /^Project name/ });
  await typeInto(page, name, 'Plant shop');
  await page.keyboard.press('Enter');
  await expect.poll(async () => (await storedProjects(page)).map((project) => project.name)).toEqual(['Plant shop']);

  await pressFor(page, button(page, 'More for Plant shop'), button(page, 'Duplicate'));
  await press(page, button(page, 'Duplicate'));
  await expect.poll(async () => (await storedProjects(page)).length).toBe(2);
  const copy = (await storedProjects(page)).find((project) => project.id !== original.id)!;

  await pressFor(page, labelled(page, `${copy.name}, radio, not selected`), labelled(page, `Projects, ${copy.name}`));

  await pressFor(page, button(page, `More for ${copy.name}`), button(page, 'Delete'));
  await press(page, button(page, 'Delete'));
  await expect(onPage(page, `Deleted ${copy.name}`)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect.poll(async () => (await storedProjects(page)).length).toBe(1);

  await press(page, button(page, 'Undo'));
  await expect.poll(async () => (await storedProjects(page)).map((project) => project.name).sort()).toEqual(
    [copy.name, 'Plant shop'].sort(),
  );
});
