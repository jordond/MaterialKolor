import { expect, test, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

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

/** Long enough for a click to reach Compose and what it changes to settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

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

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string) {
  return page.locator('#cmp_a11y_root').getByText(text);
}

function button(page: Page, name: string) {
  return page.locator('#cmp_a11y_root').getByRole('button', { name, exact: true });
}

/** Clicks the first button called [name] where Compose draws it. */
async function click(page: Page, name: string): Promise<void> {
  const target = button(page, name).first();
  await expect.poll(async () => (await target.boundingBox())?.height ?? 0, { timeout: 15_000 }).toBeGreaterThan(0);
  const box = (await target.boundingBox())!;
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}
