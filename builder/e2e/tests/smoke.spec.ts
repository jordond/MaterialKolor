import { readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { expect, test, type Page } from '@playwright/test';
import { site } from './builder';
import { networkQuietFor } from '../fixtures/timing';

// The production site as the host serves it. It boots from the root and from a theme link, takes
// its scripts and wasm from /assets/ whatever path it was opened on, and logs no errors doing it.

/**
 * Console errors that come from upstream code and say nothing about the site. Skia's buffer copy in
 * Compose 1.12.1 still reads `wasmExports.memory`, which Kotlin 2.4 reports as deprecated.
 */
const UPSTREAM_ERRORS = [/^Accessing `memory` via `wasmExports` is deprecated/];

for (const route of ['/', '/t/AdllOwAAAAAT']) {
  test(`the site boots from ${route}`, async ({ page }) => {
    const errors = collectErrors(page);
    const responses = collectResponses(page);

    await page.goto(site(route));
    await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
    await page.waitForLoadState('networkidle');

    const origin = new URL(site('/')).origin;
    const own = responses.filter((response) => new URL(response.url).origin === origin);
    expect(own.filter((response) => response.status !== 200)).toEqual([]);
    const loaded = own.map((response) => new URL(response.url).pathname);
    expect(loaded[0]).toBe(route);
    // boot.js is the one script at the root. It is not hashed, so the host serves it no-cache.
    const outside = loaded.filter((pathname) => /\.(js|wasm)$/.test(pathname) && !pathname.startsWith('/assets/'));
    expect(outside).toEqual(['/boot.js']);
    for (const asset of [/^builder\.[0-9a-f]{16}\.js$/, /^skiko\.[0-9a-f]{16}\.wasm$/, /^MaterialKolor-builder-apps-web\.[0-9a-f]{16}\.wasm$/]) {
      expect(loaded.some((pathname) => pathname.startsWith('/assets/') && asset.test(pathname.slice('/assets/'.length)))).toBe(true);
    }
    expect(errors.filter((error) => !UPSTREAM_ERRORS.some((known) => known.test(error)))).toEqual([]);
  });
}

// Both ways, so each engine's first visit total means what the site fetches at boot on that engine
// and nothing else. A file the page stops loading at boot has to move to `firstVisit.exclude` in
// the same change.
for (const [engine, route] of [
  ['wasm', '/'],
  ['js', '/?engine=js'],
] as const) {
  test(`the budget counts as first visit on ${engine} exactly what the site loads at boot there`, async ({ page }) => {
    const responses = collectResponses(page);
    await page.goto(site(route));
    await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
    await page.waitForLoadState('networkidle');
    // Fonts are asked for after the first frame, which can be after networkidle.
    await networkQuietFor(responses);

    const counted = firstVisitFiles(engine);
    const origin = new URL(site('/')).origin;
    const fetched = responses
      .map((response) => new URL(response.url))
      // `/api/` answers come from the Worker, not the site, so the budget has no file for them.
      .filter((url) => url.origin === origin && !url.pathname.startsWith('/api/'))
      .map((url) => (url.pathname === '/' ? 'index.html' : url.pathname.slice(1)));
    expect(fetched.filter((file) => !counted(file)), 'loaded at boot but not counted').toEqual([]);
    const root = process.env.MK_E2E_SITE_DIR;
    if (!root) throw new Error('MK_E2E_SITE_DIR is not set, the global setup did not run');
    const unfetched = siteFiles(root).filter((file) => counted(file) && !fetched.includes(file));
    expect(unfetched, 'counted but not loaded at boot').toEqual([]);
  });
}

test('the page lists the hashed assets each engine boots with', async ({ page, request }) => {
  await page.goto(site('/'));
  const assets = JSON.parse((await page.locator('#mk-assets').textContent()) ?? '{}');
  expect(assets.wasm.glue).toMatch(/^\/assets\/builder\.[0-9a-f]{16}\.js$/);
  expect(assets.wasm.binaries).toHaveLength(2);
  expect(assets.js.glue).toMatch(/^\/assets\/builder-js\.[0-9a-f]{16}\.js$/);
  expect(assets.js.binaries).toEqual([expect.stringMatching(/^\/assets\/skiko\.[0-9a-f]{16}\.wasm$/)]);
  expect(assets.fonts.length).toBeGreaterThan(0);
  const listed = [assets.wasm.glue, ...assets.wasm.binaries, assets.js.glue, ...assets.js.binaries, ...assets.fonts];
  for (const pathname of listed) {
    expect((await request.get(site(pathname))).status(), pathname).toBe(200);
  }
});

test('the server answers the way the host does', async ({ request }) => {
  expect((await request.get(site('/no-such-file.txt'))).status()).toBe(404);

  const root = process.env.MK_E2E_SITE_DIR;
  if (!root) throw new Error('MK_E2E_SITE_DIR is not set, the global setup did not run');
  const wasm = readdirSync(path.join(root, 'assets')).find((file) => file.endsWith('.wasm'));
  if (!wasm) throw new Error(`No wasm in ${root}/assets`);
  const response = await request.get(site(`/assets/${wasm}`), { headers: { 'Accept-Encoding': 'gzip' } });
  expect(response.status()).toBe(200);
  expect(response.headers()['content-encoding']).toBe('gzip');
  expect(response.headers()['vary']).toBe('Accept-Encoding');
  expect(response.headers()['cache-control']).toBe('public, max-age=31536000, immutable');
});

function collectErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('requestfailed', (request) => errors.push(`${request.url()} ${request.failure()?.errorText}`));
  return errors;
}

function collectResponses(page: Page): { url: string; status: number }[] {
  const responses: { url: string; status: number }[] = [];
  page.on('response', (response) => responses.push({ url: response.url(), status: response.status() }));
  return responses;
}

/**
 * Whether `budget.json` counts a site file as part of [engine]'s first visit, the shared files and
 * the engine's own, read the way `check-budget.mjs` reads it.
 */
function firstVisitFiles(engine: 'wasm' | 'js'): (file: string) => boolean {
  const budget = JSON.parse(readFileSync(path.resolve(__dirname, '../../apps/web/budget.json'), 'utf8'));
  const skipped = (budget.skip as string[]).map(globToRegExp);
  const counted = [...budget.firstVisit.files, ...budget.firstVisit.engines[engine].files].map(globToRegExp);
  const lazy = (budget.firstVisit.exclude as string[]).map(globToRegExp);
  return (file) =>
    !skipped.some((pattern) => pattern.test(file)) &&
    counted.some((pattern) => pattern.test(file)) &&
    !lazy.some((pattern) => pattern.test(file));
}

/** The site-relative path of every file under [root], with forward slashes. */
function siteFiles(root: string, prefix = ''): string[] {
  return readdirSync(path.join(root, prefix), { withFileTypes: true }).flatMap((entry) => {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
    return entry.isDirectory() ? siteFiles(root, relative) : [relative];
  });
}

/** `*` matches within one path segment and `**` across segments, as in `check-budget.mjs`. */
function globToRegExp(glob: string): RegExp {
  const source = glob
    .split(/(\*\*\/|\*\*|\*)/)
    .map((part) => {
      if (part === '**/') return '(?:.*/)?';
      if (part === '**') return '.*';
      if (part === '*') return '[^/]*';
      return part.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
    })
    .join('');
  return new RegExp(`^${source}$`);
}
