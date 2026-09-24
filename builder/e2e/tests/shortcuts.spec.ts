import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-315
// The page's keyboard map on the real builder (F-34), read from the page's accessibility tree. Keys
// only reach the page once it has focus, so each test first presses the bare canvas, which also
// hands focus to the page's focus holder.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows. `ControlOrMeta` follows the machine that runs the test instead, so the primary
// key here comes from the page's own user agent.

const A11Y = '#cmp_a11y_root';

/** The cheat sheet's line for screen reader users, `command_screen_reader_note`. */
const SCREEN_READER_NOTE =
  'With a screen reader, single-key shortcuts reach the page in focus mode, also called forms mode, and not in browse mode.';

/** Long enough for a key or a press to reach Compose and settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('after a press on the bare canvas, Space shuffles the seed', async ({ page }) => {
  await openBuilder(page);
  const before = await seedText(page);

  await pressBareCanvas(page);
  await page.keyboard.press('Space');

  await expect.poll(() => seedText(page), { timeout: 10_000 }).not.toBe(before);
});

test('l typed in the seed field sets no lock', async ({ page }) => {
  await openBuilder(page);
  const field = await seedField(page);
  const unlocked = await lockState(page);

  await clickMiddle(page, field);
  await page.keyboard.type('l');
  // b-315a
  // The l showing in the field says the key has been handled, the page's shortcuts included.
  await expect.poll(() => seedText(page), { timeout: 10_000 }).toContain('l');
  expect(await lockState(page)).toBe(unlocked);

  // The same key on the page does set it, so the check above can tell the two apart.
  await pressBareCanvas(page);
  await page.keyboard.press('l');
  await expect.poll(() => lockState(page), { timeout: 10_000 }).not.toBe(unlocked);
});

test('? opens the cheat sheet, and while it is open single keys and Space stay in it', async ({ page }) => {
  await openBuilder(page);
  const seed = await seedText(page);
  const unlocked = await lockState(page);
  await pressBareCanvas(page);

  await page.keyboard.press('?');
  const note = page.locator(A11Y).getByText(SCREEN_READER_NOTE, { exact: true });
  await expect(note).toHaveCount(1, { timeout: 10_000 });

  await page.keyboard.press('l');
  await page.keyboard.press('Space');
  // b-315a
  // Focus stayed in the dialog, so it is still open and Esc still reaches it. Esc closing it also
  // says the two keys before it have been handled.
  await expect(note).toHaveCount(1);
  await page.keyboard.press('Escape');
  await expect(note).toHaveCount(0, { timeout: 10_000 });

  expect(await seedText(page)).toBe(seed);
  expect(await lockState(page)).toBe(unlocked);
});

test('Cmd or Ctrl with K, S and O belong to the page, and K opens the palette panel', async ({ page }) => {
  await openBuilder(page);
  await seedField(page);
  await pressBareCanvas(page);
  const primary = await primaryKey(page);
  // Added after the page's own listeners, so it hears each key once the page has handled it.
  await page.evaluate(() => {
    const seen: string[] = [];
    (window as unknown as { __mkKeys: string[] }).__mkKeys = seen;
    window.addEventListener('keydown', (event) => seen.push(`${event.key.toLowerCase()} ${event.defaultPrevented}`));
  });

  await page.keyboard.press(`${primary}+s`);
  await page.keyboard.press(`${primary}+k`);
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBe('Palette');
  // b-315a
  // The palette owns the keyboard while it is open, so Esc closes it first, and its search field
  // leaving says focus is on its way back to the page.
  await page.keyboard.press('Escape');
  await expect(page.locator(A11Y).getByRole('textbox', { name: /^Search commands/ })).toHaveCount(0, {
    timeout: 10_000,
  });
  await page.keyboard.press(`${primary}+o`);
  await expect(page.locator(A11Y).getByText(/^Projects, dialog/)).toHaveCount(1, { timeout: 10_000 });

  const seen = await page.evaluate(() =>
    (window as unknown as { __mkKeys: string[] }).__mkKeys.filter((entry) => /^[kso] /.test(entry)),
  );
  expect(seen).toEqual(['s true', 'k true', 'o true']);
});

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

/** Every ARIA attribute of the poster's hue lock, which change when it is turned on. */
async function lockState(page: Page): Promise<string> {
  const name = /^Lock hue/;
  const scope = page.locator(A11Y);
  const lock = scope
    .getByRole('checkbox', { name })
    .or(scope.getByRole('switch', { name }))
    .or(scope.getByRole('button', { name }))
    .first();
  return lock.evaluate((element) =>
    Array.from(element.attributes)
      .filter((attribute) => attribute.name.startsWith('aria-'))
      .map((attribute) => `${attribute.name}=${attribute.value}`)
      .sort()
      .join(' '),
  );
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

/** The overlay the page's router last put in the history, the open panel's name. */
async function openOverlay(page: Page): Promise<string | null> {
  return page.evaluate(() => (history.state as { mkOverlay?: string } | null)?.mkOverlay ?? null);
}
