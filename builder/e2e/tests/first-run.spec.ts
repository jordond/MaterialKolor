import { expect, test, type Page } from '@playwright/test';
import { hook, openBuilder, reloadBuilder, wantHooks } from './builder';

// b-314
// The one hint a first visit gets (F-35), read from the page's accessibility tree. A dismissal kept
// in storage has to hold through a reload.

/** The hint, word for word, `about_first_run_hint`. */
const HINT = 'Paste a color, drop an image, or press Space to shuffle. Press ? for shortcuts.';

/** The poster's Shuffle button, there once the workspace has drawn. */
const SHUFFLE = 'Shuffle';

/** Long enough for boot to open the project once the workspace has drawn. */
const BOOT_SETTLE_MS = 1_500;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('first run', () => {
  test('a fresh page shows the hint', async ({ page }) => {
    await openBuilder(page);

    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });
  });

  test('a dismissed hint is gone after a reload', async ({ page }) => {
    await openBuilder(page);
    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });

    expect(await hook(page, 'addHint', 'first-run')).toBe('Done');
    await reloadBuilder(page);
    await expect(onPage(page, SHUFFLE).first()).toBeAttached({ timeout: 30_000 });
    await page.waitForTimeout(BOOT_SETTLE_MS);

    await expect(onPage(page, HINT)).toHaveCount(0);
  });
});

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string) {
  return page.locator('#cmp_a11y_root').getByText(text);
}
