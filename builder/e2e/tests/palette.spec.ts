import { expect, test, type Locator, type Page } from '@playwright/test';
import { clickMiddle, openBuilder, pressBareCanvas, wantHooks } from './builder';

// b-315a
// The command palette on the real builder (F-33), opened with Cmd or Ctrl+K and read from the page's
// accessibility tree. Keys only reach the page once it has focus, so each test first presses the bare
// canvas, which hands focus to the page's focus holder.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows, so the primary key here comes from the page's own user agent, as in
// shortcuts.spec.ts.

const A11Y = '#cmp_a11y_root';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('a search with one match, a style, runs it on Enter', async ({ page }) => {
  await openBuilder(page);
  const field = await openPalette(page);

  await clickMiddle(page, field);
  await page.keyboard.type('Monochrome');
  await expect(paletteRow(page, /^Use style Monochrome/)).toHaveCount(1, { timeout: 10_000 });
  await page.keyboard.press('Enter');

  // The line under the style chips names the chosen style.
  await expect(page.locator(A11Y).getByText(/^Monochrome\. /)).toHaveCount(1, { timeout: 10_000 });
});

test('a typed hex leads with setting the seed to it', async ({ page }) => {
  await openBuilder(page);
  const field = await openPalette(page);

  await clickMiddle(page, field);
  await page.keyboard.type('#0B6E4F');
  await expect(paletteRow(page, /^Set seed to #0B6E4F/)).toHaveCount(1, { timeout: 10_000 });
  // Enter runs the top row, so the seed changing says the row came first.
  await page.keyboard.press('Enter');

  await expect.poll(() => seedText(page), { timeout: 10_000 }).toMatch(/0B6E4F/i);
});

// b-315d
test('Esc then Cmd or Ctrl+O opens Projects in the history, and Back closes it', async ({ page }) => {
  await openBuilder(page);
  await openPalette(page);
  const primary = await primaryKey(page);

  await page.keyboard.press('Escape');
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBeNull();
  // Straight away, while the palette may still be on its way out with focus in it.
  await page.keyboard.press(`${primary}+o`);

  const projects = page.locator(A11Y).getByText(/^Projects, dialog/);
  await expect(projects).toHaveCount(1, { timeout: 10_000 });
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBe('Projects');
  await page.evaluate(() => history.back());
  await expect(projects).toHaveCount(0, { timeout: 10_000 });
  expect(await openOverlay(page)).toBeNull();
});

/** The overlay the page's router last put in the history, the open panel's name. */
async function openOverlay(page: Page): Promise<string | null> {
  return page.evaluate(() => (history.state as { mkOverlay?: string } | null)?.mkOverlay ?? null);
}

/** Opens the palette with Cmd or Ctrl+K and returns its search field. */
async function openPalette(page: Page): Promise<Locator> {
  await seedField(page);
  await pressBareCanvas(page);
  await page.keyboard.press(`${await primaryKey(page)}+k`);
  const field = page.locator(A11Y).getByRole('textbox', { name: /^Search commands/ });
  await expect(field).toBeAttached({ timeout: 10_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

/** A row of the palette, a button named [name]. */
function paletteRow(page: Page, name: RegExp): Locator {
  return page.locator(A11Y).getByRole('button', { name });
}

/** The seed field, once the page has drawn it. */
async function seedField(page: Page): Promise<Locator> {
  const field = page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ });
  await expect(field).toBeAttached({ timeout: 30_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

/** What the seed field shows, the seed's hex. */
async function seedText(page: Page): Promise<string> {
  return (await (await seedField(page)).textContent()) ?? '';
}

/** `Meta` when the page's user agent names an Apple system, where it takes Cmd, else `Control`. */
async function primaryKey(page: Page): Promise<'Meta' | 'Control'> {
  const apple = await page.evaluate(() => /Macintosh|Mac OS|iPhone|iPad|iPod|Darwin/.test(navigator.userAgent));
  return apple ? 'Meta' : 'Control';
}
