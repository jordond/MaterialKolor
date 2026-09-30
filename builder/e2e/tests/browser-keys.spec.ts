import type { Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { boxOf, focusField, primaryKey, seedField } from '../fixtures/workspace';

// Browser shortcuts typed into a text field. Compose hears a key typed in a field a frame late, so
// the boot script stops the browser's own Cmd or Ctrl with S, O and K before anything else hears
// them, and leaves every other key alone. A listener added after the page's own reads
// `defaultPrevented` on each key the way the browser will once the page is done with it.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows. `ControlOrMeta` follows the machine that runs the test instead, so the primary
// key here comes from the page's own user agent, as in `shortcuts.spec.ts`.

test('Cmd or Ctrl with C, V, X and A and a plain S typed in the seed field are left alone', async ({ page }) => {
  await openBuilder(page);
  const primary = await primaryKey(page);
  await boxOf(seedField(page));
  await listenLast(page);

  await focusField(page, seedField(page));
  for (const key of ['a', 'c', 'x', 'v']) await page.keyboard.press(`${primary}+${key}`);
  await page.keyboard.press('s');

  await expect.poll(() => seenKeys(page)).toEqual(['a false', 'c false', 'x false', 'v false', 's false']);
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
