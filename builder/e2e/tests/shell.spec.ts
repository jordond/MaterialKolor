import { expect, test, type Page } from '@playwright/test';
import { site } from './builder';
import { BOOT_TIMEOUT_MS, seedField } from '../fixtures/workspace';

// The static shell around the app. boot.js picks the engine and colors the splash before any app
// code loads. index.html carries the splash, the unsupported page and the error overlay.

type Engine = 'wasm' | 'js';

/** What `#mk-assets` lists, each engine's glue and wasm files and the fonts both load. */
interface Assets {
  wasm: { glue: string; binaries: string[] };
  js: { glue: string; binaries: string[] };
  fonts: string[];
}

/** The glue, the script boot.js adds last. Holding it keeps the page on the splash. */
const GLUE = /\/assets\/builder\.[0-9a-f]{16}\.js$/;

/** A share code with the default seed, #D9653B. */
const DEFAULT_LINK = '/t/AdllOwAAAAAT';

/** The start of a share code for seed #1A73E8. The splash only reads the seed, bytes 1 to 3. */
const BLUE_LINK = '/t/ARpz6A';

const LIGHT = 0xff102030 | 0;
const DARK = 0xff405060 | 0;
const BLUE = 0xff1a73e8 | 0;
const DEFAULT_SEED = 0xffd9653b | 0;

test.describe('splash', () => {
  // An mk:splash from before the seed and the appearance were stored still paints.
  test('paints an older mk:splash, chrome only, for each scheme with a neutral poster', async ({ page }) => {
    await storeSplash(page, { light: LIGHT, dark: DARK });
    await holdGlue(page);
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });

    const light = await splash(page);
    expect(light.chrome).toBe(rgb(LIGHT));
    expect(light.poster).toBe(light.canvas);
    expect(light.hex).not.toContain('#');

    await page.emulateMedia({ colorScheme: 'dark' });
    expect((await splash(page)).chrome).toBe(rgb(DARK));
  });

  test('paints a stored seed on the poster, and a link seed over it', async ({ page }) => {
    await storeSplash(page, { light: LIGHT, dark: DARK, seed: BLUE });
    await holdGlue(page);
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
    expect(await splash(page)).toMatchObject({ poster: rgb(BLUE), hex: '"#1A73E8"' });

    await page.goto(site(DEFAULT_LINK), { waitUntil: 'domcontentloaded' });
    expect(await splash(page)).toMatchObject({ chrome: rgb(LIGHT), poster: 'rgb(217, 101, 59)', hex: '"#D9653B"' });
  });

  test('paints a forced appearance whatever the page scheme, with the stored seed', async ({ page }) => {
    await storeSplash(page, { light: LIGHT, dark: DARK, seed: BLUE, appearance: 'dark' });
    await holdGlue(page);
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
    expect(await splash(page)).toMatchObject({ chrome: rgb(DARK), poster: rgb(BLUE), hex: '"#1A73E8"' });
    expect(await themeColors(page)).toEqual([hex(DARK), hex(DARK)]);

    await storeSplash(page, { light: LIGHT, dark: DARK, seed: BLUE, appearance: 'light' });
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.reload({ waitUntil: 'domcontentloaded' });
    expect((await splash(page)).chrome).toBe(rgb(LIGHT));
    expect(await themeColors(page)).toEqual([hex(LIGHT), hex(LIGHT)]);
  });

  test('follows the page scheme when the appearance is system or anything else', async ({ page }) => {
    await holdGlue(page);
    // Each stored value is added after the last, so it is the one the next load reads.
    for (const appearance of ['system', 'sepia', 42, null]) {
      await storeSplash(page, { light: LIGHT, dark: DARK, seed: BLUE, appearance });
      await page.emulateMedia({ colorScheme: 'light' });
      await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
      expect((await splash(page)).chrome, String(appearance)).toBe(rgb(LIGHT));
      expect(await themeColors(page)).toEqual([hex(LIGHT), hex(DARK)]);

      await page.emulateMedia({ colorScheme: 'dark' });
      expect((await splash(page)).chrome, String(appearance)).toBe(rgb(DARK));
    }
  });

  test('paints the default seed on a first visit', async ({ page }) => {
    await holdGlue(page);
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
    expect(await splash(page)).toMatchObject({ poster: 'rgb(217, 101, 59)', hex: '"#D9653B"' });
  });

  test('paints the seed of a theme link with no storage', async ({ page }) => {
    await holdGlue(page);
    await page.goto(site(DEFAULT_LINK), { waitUntil: 'domcontentloaded' });
    expect(await splash(page)).toMatchObject({ poster: 'rgb(217, 101, 59)', hex: '"#D9653B"' });
  });

  test('with no storage paints the colors the app then writes, and leaves with the summary', async ({ page }) => {
    const release = await holdGlue(page);
    await page.emulateMedia({ colorScheme: 'light' });
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
    const light = (await splash(page)).chrome;
    await page.emulateMedia({ colorScheme: 'dark' });
    const dark = (await splash(page)).chrome;
    // The Compose mirror lives in a shadow root, so the page's own h1 is the summary.
    expect(await page.evaluate(() => document.querySelector('h1')?.textContent)).toBe('MaterialKolor Builder');

    release();
    await expect.poll(() => page.evaluate(() => localStorage.getItem('mk:splash')), { timeout: 30_000 }).not.toBeNull();
    const written = JSON.parse((await page.evaluate(() => localStorage.getItem('mk:splash')))!);
    expect({ light, dark }).toEqual({ light: rgb(written.light), dark: rgb(written.dark) });
    expect({ seed: written.seed, appearance: written.appearance }).toEqual({ seed: DEFAULT_SEED, appearance: 'system' });

    await expect(page.locator('#splash')).toHaveCount(0, { timeout: 30_000 });
    expect(await page.evaluate(() => document.querySelector('h1'))).toBeNull();
    await expect
      .poll(() =>
        page.evaluate(() => {
          const host = document.activeElement;
          return (host?.shadowRoot?.activeElement ?? host)?.tagName;
        }),
      )
      .toBe('CANVAS');
  });
});

// A browser without WasmGC runs the JS engine rather than getting the unsupported page (D61).
test('a browser without WasmGC boots the JS glue and reaches the workspace', async ({ page }) => {
  // Turns down a module whose first type is a struct, as boot.js's WasmGC gate declares, and hands
  // every other module, the exception handling gate included, to the real validate.
  await page.addInitScript(() => {
    const validate = WebAssembly.validate.bind(WebAssembly);
    WebAssembly.validate = (source: BufferSource) => {
      const bytes = ArrayBuffer.isView(source)
        ? new Uint8Array(source.buffer, source.byteOffset, source.byteLength)
        : new Uint8Array(source);
      const typeSection = bytes[8] === 0x01;
      if (typeSection && bytes[11] === 0x5f) return false;
      return validate(source);
    };
  });
  const requests = collectRequests(page);
  await page.goto(site('/'));

  await expect(seedField(page)).toBeAttached({ timeout: BOOT_TIMEOUT_MS });
  await expect(page.locator('#unsupported')).toBeHidden();
  const assets = await readAssets(page);
  expect(requests.filter((pathname) => pathname === assets.js.glue)).toHaveLength(1);
  expect(requests.filter((pathname) => onlyIn(assets, 'wasm').includes(pathname))).toEqual([]);
});

test.describe('unsupported browsers', () => {
  const stubs: [string, () => void][] = [
    [
      'without WebAssembly',
      () => {
        delete (window as { WebAssembly?: unknown }).WebAssembly;
      },
    ],
    [
      // Neither gate module validates, as in a browser whose WebAssembly has no exception handling.
      'without exception handling',
      () => {
        WebAssembly.validate = () => false;
      },
    ],
    [
      'without WebGL 2',
      () => {
        const getContext = HTMLCanvasElement.prototype.getContext;
        HTMLCanvasElement.prototype.getContext = function (this: HTMLCanvasElement, type: string, ...rest: unknown[]) {
          return type === 'webgl2' ? null : (getContext as (...args: unknown[]) => unknown).call(this, type, ...rest);
        } as never;
      },
    ],
  ];

  for (const [name, stub] of stubs) {
    test(`a browser ${name} gets the floors and the link seed and loads no app`, async ({ page }) => {
      await page.addInitScript(stub);
      const requests = collectRequests(page);
      await page.goto(site(BLUE_LINK));

      const unsupported = page.locator('#unsupported');
      await expect(unsupported).toBeVisible();
      for (const floor of ['Chrome or Edge 95', 'Firefox 100', 'Safari 15.2', '#1A73E8']) {
        await expect(unsupported).toContainText(floor);
      }
      await expect(unsupported.getByRole('link', { name: 'MaterialKolor on GitHub' })).toBeVisible();
      await expect(page.locator('.mk-skeleton')).toBeHidden();
      await page.waitForLoadState('networkidle');
      expect(requests.filter((pathname) => pathname.startsWith('/assets/') || pathname.endsWith('.wasm'))).toEqual([]);
      expect(await page.locator('link[rel="preload"]').count()).toBe(0);
    });
  }
});

for (const [engine, route, binaries] of [
  ['wasm', '/', 2],
  ['js', '/?engine=js', 1],
] as const) {
  test(`boot on ${engine} adds no preload and fetches its glue, each of its wasm files and each font once`, async ({
    page,
  }) => {
    const requests = collectRequests(page);
    await page.goto(site(route));
    await expect(page.locator('#splash')).toHaveCount(0, { timeout: BOOT_TIMEOUT_MS });
    await settle(requests);

    expect(await page.locator('link[rel="preload"]').count()).toBe(0);
    const assets = await readAssets(page);
    expect(assets[engine].binaries).toHaveLength(binaries);
    expect(assets.fonts.length).toBeGreaterThan(0);
    const expected: string[] = [assets[engine].glue, ...assets[engine].binaries, ...assets.fonts];
    for (const pathname of expected) {
      expect(requests.filter((request) => request === pathname), pathname).toHaveLength(1);
    }
    const other = engine === 'wasm' ? 'js' : 'wasm';
    expect(requests.filter((pathname) => onlyIn(assets, other).includes(pathname))).toEqual([]);
  });
}

test.describe('error overlay', () => {
  test('shows on an uncaught error, holds focus and copies the details', async ({ page, context, browserName }) => {
    if (browserName === 'chromium') await context.grantPermissions(['clipboard-read', 'clipboard-write']);
    await page.goto(site('/'));
    await expect(page.locator('#splash')).toHaveCount(0, { timeout: 30_000 });
    await page.evaluate(() => {
      setTimeout(() => {
        throw new Error('Thrown by the shell spec');
      });
    });

    const dialog = page.getByRole('alertdialog', { name: 'Something went wrong' });
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText('Check the console for logs, or try reloading.');
    await expect(page.locator('#error-reload')).toBeFocused();
    expect(await page.evaluate(() => document.getElementById('app')?.inert)).toBe(true);

    await page.locator('#error-copy').click();
    await expect(page.locator('#error-status')).toHaveText('Details copied.');
    if (browserName === 'chromium') {
      const details = await page.evaluate(() => navigator.clipboard.readText());
      expect(details).toContain('Error: Thrown by the shell spec');
      expect(details).toMatch(/^Build: builder\.[0-9a-f]{16}\.js$/m);
      expect(details).toMatch(/^Engine: wasm$/m);
      expect(details).toContain(await page.evaluate(() => navigator.userAgent));
    }
  });

  test('shows on an unhandled rejection and not on the notices it ignores', async ({ page }) => {
    await holdGlue(page);
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
    await page.evaluate(() => {
      window.dispatchEvent(new ErrorEvent('error', { message: 'ResizeObserver loop completed with undelivered notifications.' }));
      window.dispatchEvent(new ErrorEvent('error', { message: 'Script error.' }));
    });
    await expect(page.locator('#error-overlay')).toBeHidden();

    await page.evaluate(() => {
      void Promise.reject(new Error('Rejected by the shell spec'));
    });
    await expect(page.getByRole('alertdialog')).toBeVisible();
  });

  // A dev server that has no such file answers with the page, which the browser refuses to run.
  test('shows when the glue fails to load', async ({ page }) => {
    await page.route(GLUE, (route) =>
      route.fulfill({
        contentType: 'text/html',
        headers: { 'X-Content-Type-Options': 'nosniff' },
        body: '<!doctype html>',
      }),
    );
    await page.goto(site('/'), { waitUntil: 'domcontentloaded' });

    await expect(page.getByRole('alertdialog', { name: 'Something went wrong' })).toBeVisible();
  });
});

test.describe('head', () => {
  const origin = 'https://materialkolor.com';
  const description =
    'Build a Compose color theme from one seed color or a photo. Preview it in Material 3, Expressive, Unstyled and ' +
    'Fluent, then export code that compiles.';
  const imageAlt = 'MaterialKolor Builder, with the seed color #D9653B and six colors of the theme it makes';
  /** A selector for one tag, the attribute that carries its value, and the value. */
  const tags: [string, string, string][] = [
    ['meta[name="description"]', 'content', description],
    ['link[rel="canonical"]', 'href', `${origin}/`],
    ['meta[name="color-scheme"]', 'content', 'light dark'],
    ['meta[name="theme-color"][media="(prefers-color-scheme: light)"]', 'content', '#fff8f6'],
    ['meta[name="theme-color"][media="(prefers-color-scheme: dark)"]', 'content', '#1a110f'],
    ['meta[property="og:type"]', 'content', 'website'],
    ['meta[property="og:site_name"]', 'content', 'MaterialKolor'],
    ['meta[property="og:title"]', 'content', 'MaterialKolor Builder'],
    ['meta[property="og:description"]', 'content', description],
    ['meta[property="og:url"]', 'content', `${origin}/`],
    ['meta[property="og:image"]', 'content', `${origin}/og-default.png`],
    ['meta[property="og:image:width"]', 'content', '1200'],
    ['meta[property="og:image:height"]', 'content', '630'],
    ['meta[property="og:image:alt"]', 'content', imageAlt],
    ['meta[name="twitter:card"]', 'content', 'summary_large_image'],
    ['meta[name="twitter:image"]', 'content', `${origin}/og-default.png`],
    ['meta[name="twitter:image:alt"]', 'content', imageAlt],
    ['link[rel="manifest"]', 'href', '/manifest.webmanifest'],
    ['link[rel="icon"][sizes="32x32"]', 'href', '/favicon-32x32.png'],
    ['link[rel="icon"][sizes="16x16"]', 'href', '/favicon-16x16.png'],
    ['link[rel="icon"][type="image/svg+xml"]', 'href', '/favicon.svg'],
    ['link[rel="apple-touch-icon"]', 'href', '/apple-touch-icon.png'],
  ];

  test.describe('as a crawler reads it', () => {
    // No script runs, so the tags are as the host serves them and boot.js has tinted nothing.
    test.use({ javaScriptEnabled: false });

    test('carries each tag once with its value', async ({ page }) => {
      await page.goto(site('/'));
      for (const [selector, attribute, value] of tags) {
        await expect(page.locator(selector), selector).toHaveCount(1);
        expect(await page.locator(selector).getAttribute(attribute), selector).toBe(value);
      }
      for (const property of ['og:title', 'og:description', 'og:image', 'og:url']) {
        await expect(page.locator(`meta[property="${property}"]`), property).toHaveCount(1);
      }
      await expect(page.locator('meta[name="theme-color"]')).toHaveCount(2);
      await expect(page.locator('link[rel="canonical"]')).toHaveCount(1);

      const data = page.locator('script[type="application/ld+json"]');
      await expect(data).toHaveCount(1);
      expect(JSON.parse((await data.textContent()) ?? '{}')).toMatchObject({
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: 'MaterialKolor Builder',
        description,
        url: `${origin}/`,
        image: `${origin}/og-default.png`,
      });
    });
  });

  test('the card, the manifest and every icon resolve with their types', async ({ request }) => {
    const card = await request.get(site('/og-default.png'));
    expect(card.status()).toBe(200);
    expect(card.headers()['content-type']).toBe('image/png');
    expect(pngSize(await card.body())).toEqual({ width: 1200, height: 630 });

    const manifestResponse = await request.get(site('/manifest.webmanifest'));
    expect(manifestResponse.status()).toBe(200);
    expect(manifestResponse.headers()['content-type']).toBe('application/manifest+json');
    const manifest = await manifestResponse.json();
    expect(manifest).toMatchObject({
      name: 'MaterialKolor Builder',
      short_name: 'MaterialKolor',
      start_url: '/',
      scope: '/',
      display: 'standalone',
      background_color: '#fff8f6',
      theme_color: '#fff8f6',
    });
    expect(manifest.icons.map((icon: { sizes: string }) => icon.sizes)).toEqual(['192x192', '512x512']);
    for (const icon of manifest.icons as { src: string; sizes: string; type: string }[]) {
      const response = await request.get(site(icon.src));
      expect(response.status(), icon.src).toBe(200);
      expect(response.headers()['content-type'], icon.src).toBe(icon.type);
      const { width, height } = pngSize(await response.body());
      expect(`${width}x${height}`, icon.src).toBe(icon.sizes);
    }

    for (const [path, type] of [
      ['/favicon.ico', 'image/x-icon'],
      ['/favicon.svg', 'image/svg+xml'],
      ['/favicon-16x16.png', 'image/png'],
      ['/favicon-32x32.png', 'image/png'],
      ['/apple-touch-icon.png', 'image/png'],
    ]) {
      const response = await request.get(site(path));
      expect(response.status(), path).toBe(200);
      expect(response.headers()['content-type'], path).toBe(type);
    }
  });

  test('robots.txt lets every crawler in on production', async ({ request }) => {
    const response = await request.get(site('/robots.txt'));
    expect(response.status()).toBe(200);
    expect(await response.text()).toBe('User-agent: *\nAllow: /\n');
  });

  // One policy for both engines. Skiko is wasm on the JS engine too, so it needs 'wasm-unsafe-eval' as well.
  for (const [engine, route] of [
    ['wasm', '/'],
    ['js', '/?engine=js'],
  ] as const) {
    test(`the page boots on ${engine} with no content security policy violation`, async ({ page }) => {
      await page.addInitScript(() => {
        const violations: string[] = [];
        (window as unknown as { mkViolations: string[] }).mkViolations = violations;
        document.addEventListener('securitypolicyviolation', (event) => {
          violations.push(`${event.violatedDirective} ${event.blockedURI}`);
        });
      });
      const response = await page.goto(site(route));
      expect(response?.headers()['content-security-policy']).toContain("script-src 'self'");
      await expect(page.locator('#splash')).toHaveCount(0, { timeout: BOOT_TIMEOUT_MS });
      expect(await page.evaluate(() => (window as unknown as { mkViolations: string[] }).mkViolations)).toEqual([]);
    });
  }
});

/** The width and height a PNG's header gives. */
function pngSize(bytes: Buffer): { width: number; height: number } {
  return { width: bytes.readUInt32BE(16), height: bytes.readUInt32BE(20) };
}

/** The content of each theme-color tag, in page order. */
async function themeColors(page: Page): Promise<string[]> {
  return page.evaluate(() =>
    Array.from(document.querySelectorAll('meta[name="theme-color"]'), (tag) => tag.getAttribute('content') ?? ''),
  );
}

/** The hex boot.js writes for a signed ARGB color. */
function hex(argb: number): string {
  return '#' + (argb & 0xffffff).toString(16).toUpperCase().padStart(6, '0');
}

/** What the splash shows, as computed colors and the poster's hex as a CSS string. */
async function splash(page: Page): Promise<{ chrome: string; poster: string; canvas: string; hex: string }> {
  return page.evaluate(() => {
    const style = (selector: string, pseudo?: string) => getComputedStyle(document.querySelector(selector)!, pseudo);
    return {
      chrome: style('#splash').backgroundColor,
      poster: style('.mk-poster').backgroundColor,
      canvas: style('.mk-canvas').backgroundColor,
      hex: style('.mk-poster', '::after').content,
    };
  });
}

/** Store [value] as mk:splash before any script on the page runs. */
async function storeSplash(page: Page, value: object): Promise<void> {
  await page.addInitScript((text) => {
    try {
      localStorage.setItem('mk:splash', text);
    } catch {
      // about:blank has no storage.
    }
  }, JSON.stringify(value));
}

/** Hold every request for the glue until the returned function is called. */
async function holdGlue(page: Page): Promise<() => void> {
  let release = (): void => {};
  const released = new Promise<void>((resolve) => {
    release = resolve;
  });
  await page.route(GLUE, async (route) => {
    await released;
    await route.continue();
  });
  return () => release();
}

function rgb(argb: number): string {
  return `rgb(${(argb >> 16) & 0xff}, ${(argb >> 8) & 0xff}, ${argb & 0xff})`;
}

async function readAssets(page: Page): Promise<Assets> {
  return JSON.parse((await page.locator('#mk-assets').textContent()) ?? '{}');
}

/** The glue and wasm files only [engine] loads, leaving out any the other engine shares. */
function onlyIn(assets: Assets, engine: Engine): string[] {
  const other = assets[engine === 'wasm' ? 'js' : 'wasm'];
  const shared = [other.glue, ...other.binaries];
  return [assets[engine].glue, ...assets[engine].binaries].filter((pathname) => !shared.includes(pathname));
}

/** The path of every request the page makes to the site. */
function collectRequests(page: Page): string[] {
  const origin = new URL(site('/')).origin;
  const requests: string[] = [];
  page.on('request', (request) => {
    const url = new URL(request.url());
    if (url.origin === origin) requests.push(url.pathname);
  });
  return requests;
}

/** Waits until no new request has gone out for two seconds, or fifteen seconds have passed. */
async function settle(requests: unknown[]): Promise<void> {
  const deadline = Date.now() + 15_000;
  let seen = requests.length;
  let quietSince = Date.now();
  while (Date.now() < deadline && Date.now() - quietSince < 2_000) {
    await new Promise((resolve) => setTimeout(resolve, 250));
    if (requests.length !== seen) {
      seen = requests.length;
      quietSince = Date.now();
    }
  }
}
