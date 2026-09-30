import type { Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { button, onPage, pressSettled } from '../fixtures/workspace';

// The boot notices, read from the page's accessibility tree and dismissed by clicking their buttons
// where Compose draws them. Boot has already opened the builder under each one, and the address bar
// says `/` by then.

/** `banners_unknown_path`, word for word. */
const UNKNOWN_PATH = 'There’s no page at that address, so here’s the builder.';

/** `banners_newer_version`, word for word. */
const NEWER_VERSION = 'Made with a newer version. Reload to update.';

/** `banners_invalid_link_defaults`, word for word. A fresh browser has no last theme to open. */
const INVALID_LINK_DEFAULTS = 'That link didn’t work, so you’re starting from the defaults.';

/** One byte, a share code version past this build's, so it reads as a newer build's code. */
const NEWER_LINK = '/t/Ag';

/** No build wrote this, so the code reads as corrupt. */
const CORRUPT_LINK = '/t/abc123';

test.describe('errors', () => {
  test('an unknown path says so, puts / back and goes on Dismiss', async ({ page }) => {
    await openBuilder(page, '/nope');
    await expect(onPage(page, UNKNOWN_PATH)).toHaveCount(1, { timeout: 30_000 });
    expect(await page.evaluate(() => location.pathname)).toBe('/');

    await click(page, 'Dismiss');

    await expect(onPage(page, UNKNOWN_PATH)).toHaveCount(0);
  });

  test('a link from a newer build asks for a reload that goes back to the link', async ({ page }) => {
    await openBuilder(page, NEWER_LINK);
    await expect(onPage(page, NEWER_VERSION)).toHaveCount(1, { timeout: 30_000 });
    expect(await page.evaluate(() => location.pathname)).toBe('/');

    const reload = page.waitForRequest(
      (request) => request.isNavigationRequest() && request.url().endsWith(NEWER_LINK),
    );
    await click(page, 'Reload');
    await reload;

    await expect(onPage(page, NEWER_VERSION)).toHaveCount(1, { timeout: 30_000 });
  });

  test('a corrupt link on a first visit says the defaults are open and offers no second start', async ({ page }) => {
    await openBuilder(page, CORRUPT_LINK);
    await expect(onPage(page, INVALID_LINK_DEFAULTS)).toHaveCount(1, { timeout: 30_000 });
    await expect(button(page, 'Start from defaults')).toHaveCount(0);

    await click(page, 'Dismiss');

    await expect(onPage(page, INVALID_LINK_DEFAULTS)).toHaveCount(0);
  });
});

/** Clicks the first button called [name] once the banner that holds it has come in. */
async function click(page: Page, name: string): Promise<void> {
  await pressSettled(page, button(page, name));
}
