import type { Page } from '@playwright/test';
import { expect, hook, openBuilder, test } from './builder';

// The page around the builder, driven through the shell's test hooks. Media queries, the tab, the
// splash and the signals that the page is going away.

test.describe('environment', () => {
  test('media queries follow the system live', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light', reducedMotion: 'no-preference' });
    await openBuilder(page);
    expect(await hook(page, 'media')).toContain('dark=false reducedMotion=false');

    await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
    await expect.poll(() => hook(page, 'media')).toContain('dark=true reducedMotion=true');
  });

  test('the tab keeps its project through a reload and gets a fresh id', async ({ page }) => {
    await openBuilder(page);
    const firstId = await hook(page, 'tabId');
    // The session writes down the project it opens at boot, so a fresh tab already has one.
    await expect.poll(() => hook(page, 'readTabProject')).not.toBe('');
    const booted = await hook(page, 'readTabProject');

    await openBuilder(page);
    await expect.poll(() => hook(page, 'readTabProject')).toBe(booted);
    expect(await hook(page, 'tabId')).not.toBe(firstId);
  });

  test('theme color, splash colors, splash hand off and page hides', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'setThemeColor', '#1a73e8');
    const themeColor = await page.evaluate(() =>
      document.querySelector('meta[name="theme-color"]')?.getAttribute('content'),
    );
    expect(themeColor?.toLowerCase()).toBe('#1a73e8');

    await hook(page, 'writeSplash', '#000000,#ffffff,#d9653b,Dark');
    expect(await page.evaluate(() => localStorage.getItem('mk:splash'))).toBe(
      '{"light":-16777216,"dark":-1,"seed":-2529989,"appearance":"dark"}',
    );

    // The boot splash goes after the first frame. Once it has, there is nothing to do.
    await expect.poll(() => page.evaluate(() => document.getElementById('splash') === null)).toBe(true);
    await hook(page, 'hideSplash');
    await page.evaluate(() => {
      const splash = document.createElement('div');
      splash.id = 'splash';
      document.body.appendChild(splash);
    });
    // Read the opacity in the same turn as the hook. The fade takes 200 ms, and a second round trip
    // can take longer than that on a busy machine, by which time the splash is gone.
    const opacity = await page.evaluate(() => {
      window.__mk!.hideSplash('');
      return document.getElementById('splash')?.style.opacity;
    });
    expect(opacity).toBe('0');
    await expect.poll(() => page.evaluate(() => document.getElementById('splash') === null)).toBe(true);
    expect(
      await page.evaluate(() => {
        const host = document.activeElement;
        return (host?.shadowRoot?.activeElement ?? host)?.tagName;
      }),
    ).toBe('CANVAS');

    await page.evaluate(() => window.dispatchEvent(new Event('pagehide')));
    expect(await hook(page, 'pageHides')).toBe('1');
    expect(await hook(page, 'eyeDropperAvailable')).toBe(
      String(await page.evaluate(() => typeof (window as any).EyeDropper === 'function')),
    );
  });

  test('a tab going to the background counts as a page hide and coming back does not', async ({ page }) => {
    await openBuilder(page);
    await setVisibility(page, 'visible');
    expect(await hook(page, 'pageHides')).toBe('0');

    await setVisibility(page, 'hidden');
    expect(await hook(page, 'pageHides')).toBe('1');
  });

  test.describe('on a touch screen', () => {
    test.use({ hasTouch: true });

    test('the pointer is coarse', async ({ page, browserName }) => {
      test.skip(browserName !== 'chromium', 'Chromium is the engine that turns the pointer coarse for touch emulation');
      await openBuilder(page);
      expect(await hook(page, 'media')).toContain('coarse=true');
    });
  });

  test('a mouse pointer is fine', async ({ page }) => {
    await openBuilder(page);
    expect(await hook(page, 'media')).toContain('coarse=false');
  });
});

/** Make the page report [state] as its visibility and tell it so, the way the browser does on a tab switch. */
async function setVisibility(page: Page, state: DocumentVisibilityState): Promise<void> {
  await page.evaluate((visibility) => {
    Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => visibility });
    document.dispatchEvent(new Event('visibilitychange'));
  }, state);
}
