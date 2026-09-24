import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-501c
// Browser shortcuts typed into a text field. Compose hears a key typed in a field a frame late, so
// the boot script stops the browser's own Cmd or Ctrl with S, O and K before anything else hears
// them, and leaves every other key alone. A listener added after the page's own reads
// `defaultPrevented` on each key the way the browser will once the page is done with it.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows. `ControlOrMeta` follows the machine that runs the test instead, so the primary
// key here comes from the page's own user agent, as in `shortcuts.spec.ts`.

const A11Y = '#cmp_a11y_root';

/** Long enough for a key or a press to reach Compose and settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('Cmd or Ctrl with S, O and K typed in the seed field are kept from the browser', async ({ page }) => {
  await openBuilder(page);
  const primary = await primaryKey(page);
  const field = await seedField(page);

  await clickMiddle(page, field);
  await page.keyboard.type('l');
  await expect.poll(() => seedText(page), { timeout: 10_000 }).toContain('l');
  await listenLast(page);

  await page.keyboard.press(`${primary}+s`);
  await page.waitForTimeout(SETTLE_MS);
  expect(await seedText(page)).toContain('l');

  // The app still gets the key, K opens the palette panel from the field.
  await page.keyboard.press(`${primary}+k`);
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBe('Palette');
  await page.keyboard.press('Escape');
  await expect(page.locator(A11Y).getByRole('textbox', { name: /^Search commands/ })).toHaveCount(0, {
    timeout: 10_000,
  });

  await clickMiddle(page, field);
  await page.keyboard.press(`${primary}+o`);

  expect(await seenKeys(page)).toEqual(['s true', 'k true', 'o true']);
});

test('Cmd or Ctrl with C, V, X and A and a plain S typed in the seed field are left alone', async ({ page }) => {
  await openBuilder(page);
  const primary = await primaryKey(page);
  const field = await seedField(page);
  await listenLast(page);

  await clickMiddle(page, field);
  for (const key of ['a', 'c', 'x', 'v']) await page.keyboard.press(`${primary}+${key}`);
  await page.keyboard.press('s');

  expect(await seenKeys(page)).toEqual(['a false', 'c false', 'x false', 'v false', 's false']);
});

/** Adds a `keydown` listener after the page's own, which notes each key and whether it was stopped. */
async function listenLast(page: Page): Promise<void> {
  await page.evaluate(() => {
    const seen: string[] = [];
    (window as unknown as { __mkKeys: string[] }).__mkKeys = seen;
    window.addEventListener('keydown', (event) => seen.push(`${event.key.toLowerCase()} ${event.defaultPrevented}`));
  });
}

/** The letter keys the listener from `listenLast` heard, each with whether it was stopped. */
async function seenKeys(page: Page): Promise<string[]> {
  return page.evaluate(() =>
    (window as unknown as { __mkKeys: string[] }).__mkKeys.filter((entry) => /^[a-z] /.test(entry)),
  );
}

/** The seed field, once the page has drawn it. */
async function seedField(page: Page): Promise<Locator> {
  const field = page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ });
  await expect(field).toBeAttached({ timeout: 30_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

/** What the seed field shows. */
async function seedText(page: Page): Promise<string> {
  return (await (await seedField(page)).textContent()) ?? '';
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
