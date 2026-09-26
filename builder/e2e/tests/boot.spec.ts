import { readFileSync } from 'node:fs';
import path from 'node:path';
import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import { startWorker, workerMissing, type Worker } from '../fixtures/worker';
import { A11Y, BOOT_TIMEOUT_MS, labelled, openWorkspace, seedField, seedText, shareVectors } from '../fixtures/workspace';

// Boot from a share code the builder's parts agree on (`builder/fixtures/share-codes.json`). It opens
// with the code's seed and style, and the address bar goes back to `/`. The Worker's own theme
// page is booted from `wrangler dev`, the one server that writes a link's meta.

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('share codes', () => {
  // One code with every section set stands for the lot. The codec's own tests read each one.
  for (const vector of shareVectors().filter((vector) => vector.label === 'every section at once')) {
    test(`boots with the seed and style of ${vector.label}`, async ({ page }) => {
      await openWorkspace(page, `/t/${vector.code}`);

      await expect.poll(() => seedText(page)).toBe(vector.seedHex);
      await expect(selectedStyle(page, vector.style)).toHaveCount(1);
      if (vector.projectName !== null) await expect(labelled(page, `Projects, ${vector.projectName}`).first()).toBeAttached();
      expect(await page.evaluate(() => location.pathname + location.search)).toBe('/');
    });
  }
});

test.describe('served by the Worker', () => {
  // One wrangler dev for the lot. The link meta is the server's, so one engine is enough.
  test.describe.configure({ mode: 'serial' });
  let worker: Worker | null = null;

  test.beforeAll(async ({ browserName }) => {
    if (browserName !== 'chromium' || workerMissing() !== null) return;
    worker = await startWorker();
  });

  test.afterAll(async () => {
    await worker?.stop();
  });

  test('a theme link carries its own meta and the policy, loads its assets and boots', async ({ page, browserName }) => {
    test.skip(browserName !== 'chromium', 'The Worker writes the same page for every engine');
    test.skip(worker === null, workerMissing() ?? 'no Worker');
    const code = 'AdllOwAAABAEQWNtZVA';
    const origin = worker!.url;
    const responses: { url: string; status: number }[] = [];
    page.on('response', (response) => responses.push({ url: response.url(), status: response.status() }));
    await page.addInitScript(() => {
      const violations: string[] = [];
      (window as unknown as { mkViolations: string[] }).mkViolations = violations;
      document.addEventListener('securitypolicyviolation', (event) => violations.push(event.violatedDirective));
    });

    const response = await page.goto(`${origin}/t/${code}`);

    expect(response?.status()).toBe(200);
    expect(response?.headers()['content-security-policy']).toBe(sitePolicy());
    // wrangler dev hands the Worker the production host from the route, so only the paths are checked.
    const link = await page.locator('meta[property="og:url"]').getAttribute('content');
    expect(link).toMatch(new RegExp(`^https?://[^/]+/t/${code}$`));
    expect(await page.locator('link[rel="canonical"]').getAttribute('href')).toBe(link);
    expect(await page.locator('meta[property="og:image"]').getAttribute('content')).toBe(
      `${new URL(link!).origin}/og/${code}.png`,
    );
    await expect(page.locator(`${A11Y} > *`).first()).toBeAttached({ timeout: BOOT_TIMEOUT_MS });
    await expect(seedField(page)).toBeAttached({ timeout: BOOT_TIMEOUT_MS });
    await expect.poll(() => seedText(page)).toBe('#D9653B');
    await expect(labelled(page, 'Projects, Acme').first()).toBeAttached();
    expect(await page.evaluate(() => location.pathname)).toBe('/');
    await page.waitForLoadState('networkidle');
    const own = responses.filter((entry) => new URL(entry.url).origin === origin);
    expect(own.filter((entry) => entry.status !== 200 && entry.status !== 304)).toEqual([]);
    expect(own.some((entry) => /\/assets\/MaterialKolor-builder-apps-web\.[0-9a-f]{16}\.wasm$/.test(entry.url))).toBe(true);
    expect(await page.evaluate(() => (window as unknown as { mkViolations: string[] }).mkViolations)).toEqual([]);

    const card = await page.request.get(`${origin}/og/${code}.png`);
    expect(card.status()).toBe(200);
    expect(card.headers()['content-type']).toBe('image/png');
  });
});

/** The style chip for [style] when it is the selected one. */
function selectedStyle(page: Page, style: string) {
  return page.locator(`${A11Y} [aria-label^="${style}, "][aria-label$=", radio, selected"]`);
}

/** The content security policy the site's `_headers` gives every page. */
function sitePolicy(): string {
  const root = process.env.MK_E2E_SITE_DIR;
  if (!root) throw new Error('MK_E2E_SITE_DIR is not set, the global setup did not run');
  const line = readFileSync(path.join(root, '_headers'), 'utf8')
    .split('\n')
    .find((entry) => entry.trim().startsWith('Content-Security-Policy:'));
  if (!line) throw new Error('The site has no Content-Security-Policy');
  return line.slice(line.indexOf(':') + 1).trim();
}
