import { expect, test, type Page, type Request } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-504
// Cloudflare Web Analytics loads only after the first frame and sets no cookie (D13, PB-10). The
// site the e2e run builds has no token, so it never asks for the beacon. The token test adds the
// `#mk-config` tag a site built with `builder.analyticsToken` carries, and stands in for Cloudflare.

const BEACON = 'https://static.cloudflareinsights.com/beacon.min.js';
const TOKEN = '0123456789abcdef0123456789abcdef';

/** Where `boot.js` is loaded, which the site build puts `#mk-config` in front of. */
const BOOT_TAG = '<script src="/boot.js"></script>';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('a site built with no token never asks for the beacon', async ({ page }) => {
  const insights = watchInsights(page);

  await openBuilder(page);
  await waitForFirstFrame(page);
  await page.waitForTimeout(1_000);

  expect(insights).toEqual([]);
});

test('with a token the beacon starts after the first frame, and no cookie is set', async ({ page, context }) => {
  const insights = watchInsights(page);
  await page.route(
    (url) => url.pathname === '/',
    async (route) => {
      const response = await route.fetch();
      const html = (await response.text()).replace(
        BOOT_TAG,
        `<script type="application/json" id="mk-config">{"analyticsToken":"${TOKEN}"}</script>${BOOT_TAG}`,
      );
      await route.fulfill({ response, body: html });
    },
  );
  await page.route(BEACON, (route) =>
    route.fulfill({ contentType: 'text/javascript', body: 'window.__mkBeaconRan = true;' }),
  );

  await openBuilder(page);
  await expect.poll(() => page.evaluate(() => (window as any).__mkBeaconRan === true), { timeout: 30_000 }).toBe(true);

  // Both times on the page's own clock, the mark's and the moment the browser began to fetch.
  const timing = await page.evaluate((beacon) => {
    const [frame] = performance.getEntriesByName('mk:first-frame');
    const [fetch] = performance.getEntriesByName(beacon);
    return { frame: frame?.startTime ?? null, fetch: fetch?.startTime ?? null };
  }, BEACON);
  expect(timing.frame).not.toBeNull();
  expect(timing.fetch).not.toBeNull();
  expect(timing.fetch!).toBeGreaterThan(timing.frame!);
  expect(insights).toEqual([BEACON]);

  const beacon = await page.evaluate(
    (url) => document.querySelector(`script[src="${url}"]`)?.getAttribute('data-cf-beacon') ?? null,
    BEACON,
  );
  expect(JSON.parse(beacon!)).toEqual({ token: TOKEN, spa: false });
  expect(await context.cookies()).toEqual([]);
  expect(await page.evaluate(() => document.cookie)).toBe('');
});

/** Every request the page makes to Cloudflare Insights, in order. */
function watchInsights(page: Page): string[] {
  const seen: string[] = [];
  page.on('request', (request: Request) => {
    if (new URL(request.url()).hostname.endsWith('cloudflareinsights.com')) seen.push(request.url());
  });
  return seen;
}

async function waitForFirstFrame(page: Page): Promise<void> {
  await expect
    .poll(() => page.evaluate(() => performance.getEntriesByName('mk:first-frame').length), { timeout: 30_000 })
    .toBe(1);
}
