import type { Page } from '@playwright/test';
import { expect, reloadBuilder, test } from './builder';
import {
  button,
  LAND_TIMEOUT_MS,
  labelled,
  onPage,
  openWorkspace,
  press,
  pressFor,
  pressKeyFor,
  seedField,
  seedText,
  storedDocument,
  storedProjects,
  typeInto,
  typeSeed,
} from '../fixtures/workspace';

// Save and return. Autosave keeps the edit and its undo history through a reload, and the
// Projects panel renames, duplicates and deletes, with the delete undone from its toast.

/** The Projects panel's title in the mirror, the dialog fold. */
const PROJECTS_DIALOG = /^Projects, dialog/;

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
  await pressKeyFor(page, 'p', onPage(page, PROJECTS_DIALOG));

  await pressFor(page, button(page, `More for ${original.name}`), menuItem(page, 'Rename'));
  await press(page, menuItem(page, 'Rename'));
  const name = page.locator('#cmp_a11y_root').getByRole('textbox', { name: /^Project name/ });
  await typeInto(page, name, 'Plant shop');
  await page.keyboard.press('Enter');
  await expect.poll(async () => (await storedProjects(page)).map((project) => project.name)).toEqual(['Plant shop']);

  await pressFor(page, button(page, 'More for Plant shop'), menuItem(page, 'Duplicate'));
  await press(page, menuItem(page, 'Duplicate'));
  await expect.poll(async () => (await storedProjects(page)).length).toBe(2);
  const copy = (await storedProjects(page)).find((project) => project.id !== original.id)!;

  await pressFor(page, labelled(page, `${copy.name}, radio, not selected`), labelled(page, `Projects, ${copy.name}`));
  // Opening a project closes the panel, so the delete opens it again.
  await expect(onPage(page, PROJECTS_DIALOG)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  await pressKeyFor(page, 'p', onPage(page, PROJECTS_DIALOG));

  await pressFor(page, button(page, `More for ${copy.name}`), menuItem(page, 'Delete'));
  await press(page, menuItem(page, 'Delete'));
  await expect(onPage(page, `Deleted ${copy.name}`)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect.poll(async () => (await storedProjects(page)).length).toBe(1);

  await press(page, button(page, 'Undo'));
  await expect.poll(async () => (await storedProjects(page)).map((project) => project.name).sort()).toEqual(
    [copy.name, 'Plant shop'].sort(),
  );
});

/** An item of an open menu, by the name the web folds its role into. */
function menuItem(page: Page, name: string) {
  return labelled(page, `${name}, `).or(button(page, name));
}
