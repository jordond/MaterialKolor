import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-315a
// The command palette on the real builder (F-33), opened with Cmd or Ctrl+K and read from the page's
// accessibility tree. Keys only reach the page once it has focus, so each test first presses the bare
// canvas, which hands focus to the page's focus holder.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows, so the primary key here comes from the page's own user agent, as in
// shortcuts.spec.ts.

const A11Y = '#cmp_a11y_root';

/** Long enough for a press to reach Compose and settle. */
const SETTLE_MS = 300;

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

/**
 * Presses the canvas where nothing is, in the preview's header row halfway from its last tab to the
 * window's edge, and waits for the page's focus holder to take focus.
 */
async function pressBareCanvas(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (!viewport) throw new Error('The page has no viewport');
  const tabs = page.locator(A11Y).getByText('Contrast', { exact: true });
  await expect(tabs.first()).toBeAttached({ timeout: 30_000 });
  const boxes = (await Promise.all((await tabs.all()).map((tab) => tab.boundingBox()))).filter(
    (box): box is NonNullable<typeof box> => box !== null && box.x > viewport.width / 3,
  );
  if (boxes.length === 0) throw new Error('The preview has no Contrast tab on screen');
  const tab = boxes.reduce((top, box) => (box.y < top.y ? box : top));
  await page.mouse.click((tab.x + tab.width + viewport.width) / 2, tab.y + tab.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}

async function clickMiddle(page: Page, locator: Locator): Promise<void> {
  const box = await locator.boundingBox();
  if (!box) throw new Error('Nothing to click');
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}

/** `Meta` when the page's user agent names an Apple system, where it takes Cmd, else `Control`. */
async function primaryKey(page: Page): Promise<'Meta' | 'Control'> {
  const apple = await page.evaluate(() => /Macintosh|Mac OS|iPhone|iPad|iPod|Darwin/.test(navigator.userAgent));
  return apple ? 'Meta' : 'Control';
}
