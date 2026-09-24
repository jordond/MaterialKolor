import { expect, test, type Page } from '@playwright/test';
import { hook, openBuilder, reloadBuilder, wantHooks } from './builder';

// b-314
// The one hint a first visit gets (F-35), read from the page's accessibility tree. A dismissal kept
// in storage has to hold through a reload.

/** The hint, word for word, `about_first_run_hint`. */
const HINT = 'Paste a color, drop an image, or press Space to shuffle. Press ? for shortcuts.';

// b-314a
/**
 * The poster's Projects button once boot has opened a project. It reads "Projects" and the project's
 * name, and only "Projects" before that. The hint shows in the same frame the name does.
 */
const NAMED_PROJECTS = '#cmp_a11y_root [aria-label^="Projects, "]';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('first run', () => {
  test('a fresh page shows the hint', async ({ page }) => {
    await openBuilder(page);

    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });
    // b-314a
    // The boot signal the next test waits on is there beside the hint.
    await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached();
  });

  test('a dismissed hint is gone after a reload', async ({ page }) => {
    await openBuilder(page);
    await expect(onPage(page, HINT)).toHaveCount(1, { timeout: 30_000 });

    expect(await hook(page, 'addHint', 'first-run')).toBe('Done');
    await reloadBuilder(page);
    await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached({ timeout: 30_000 });

    await expect(onPage(page, HINT)).toHaveCount(0);
  });
});

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string) {
  return page.locator('#cmp_a11y_root').getByText(text);
}
