import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import { LAND_TIMEOUT_MS, button, onPage, press } from '../fixtures/workspace';

// A Medium window, where the top bar holds the library dropdown. The dropdown measures every
// library's trigger off screen, and those measures once read the outlined field's frame lines, which
// left Compose's rect list without their parents. The next overlay to open then crashed the page on
// "LayoutNode not found in RectList". Export, Share and More options open here and the page stays up.

test.use({ viewport: { width: 1024, height: 768 } });

/** The More options menu's first item, which shows once the menu is open. */
const FIRST_MORE_ITEM = 'Use the system appearance';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('at 1024 wide, Export, Share and More options open without an error', async ({ page, browserName }) => {
  test.skip(browserName !== 'chromium', 'The crash is in Compose layout, which runs the same in every browser');
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await openBuilder(page);

  await press(page, button(page, 'Export code'));
  await opens(page, onPage(page, /^Export code, dialog/), errors);

  await press(page, button(page, 'Close'));
  await expect(onPage(page, /^Export code, dialog/)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });

  await press(page, button(page, 'Share'));
  await opens(page, onPage(page, /^Share this theme, dialog/), errors);

  await page.keyboard.press('Escape');
  await expect(onPage(page, /^Share this theme, dialog/)).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });

  await press(page, button(page, 'More options'));
  await opens(page, onPage(page, FIRST_MORE_ITEM), errors);
});

/**
 * Waits for [shown] to reach the mirror, then a few frames more, and fails with the page's own
 * errors if any came first, since a crash takes the overlay down with it.
 */
async function opens(page: Page, shown: Locator, errors: string[]): Promise<void> {
  const state = async () => (errors.length > 0 ? errors.join(' | ') : (await shown.count()) > 0 ? 'open' : 'waiting');
  await expect.poll(state, { timeout: LAND_TIMEOUT_MS }).toBe('open');
  await settle(page);
  expect(errors).toEqual([]);
}

/** Waits a few frames, long enough for a crash in the frames after an overlay opens to reach the page. */
async function settle(page: Page): Promise<void> {
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        let frames = 10;
        const next = () => (--frames > 0 ? requestAnimationFrame(next) : resolve());
        requestAnimationFrame(next);
      }),
  );
}
