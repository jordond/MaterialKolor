import { expect, test, type Page } from '@playwright/test';
import { hook, openBuilder, wantHooks } from './builder';

// The browser services behind the builder, driven through the shell's test hooks while the app is
// still a placeholder. Storage first, then the router and the page around it.

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('storage', () => {
  test("another tab's write arrives as one change and the store reads it", async ({ context }, testInfo) => {
    const writer = await context.newPage();
    const reader = await context.newPage();
    await openBuilder(writer);
    await openBuilder(reader);
    await reader.evaluate(() => {
      window.addEventListener('storage', (event) => {
        if (event.key === 'mk:prefs') (window as any).__storageAt = performance.timeOrigin + performance.now();
      });
    });

    const [sentAt, outcome] = await writer.evaluate(() => {
      const at = performance.timeOrigin + performance.now();
      return [at, window.__mk!.addHint('from-writer')] as const;
    });
    expect(outcome).toBe('Done');

    await expect.poll(() => hook(reader, 'externalChanges')).toContain('Prefs');
    await expect.poll(() => hook(reader, 'seenHints')).toBe('from-writer');
    expect(await hook(reader, 'readHints')).toBe('from-writer');
    const arrivedAt = await reader.evaluate(() => (window as any).__storageAt as number);
    testInfo.annotations.push({ type: 'storage event ms', description: (arrivedAt - sentAt).toFixed(1) });
    console.log(`[S8] ${testInfo.project.name} storage event after ${(arrivedAt - sentAt).toFixed(1)} ms`);

    // One write is one event, however long the reader waits.
    await reader.waitForTimeout(250);
    const changes = (await hook(reader, 'externalChanges')).split(',');
    expect(changes.filter((key) => key === 'Prefs')).toHaveLength(1);
  });

  test('a write to full storage comes back as QuotaExceeded', async ({ page }) => {
    await openBuilder(page);
    expect(await hook(page, 'storageAvailable')).toBe('true');
    const filled = await fillStorage(page);
    expect(await hook(page, 'addHint', 'no-room')).toBe('QuotaExceeded');
    expect(await hook(page, 'readHints')).toBe('');

    await page.evaluate((count) => {
      for (let index = 0; index < count; index++) localStorage.removeItem(`fill:${index}`);
    }, filled);
    expect(await hook(page, 'addHint', 'room')).toBe('Done');
  });

  test('an unreadable record is set aside and reported once', async ({ page }) => {
    await openBuilder(page);
    await page.evaluate(() => localStorage.setItem('mk:prefs', 'not json'));

    expect(await hook(page, 'readHints')).toBe('');
    expect(await hook(page, 'readHints')).toBe('');
    await expect.poll(() => hook(page, 'quarantined')).toBe('mk:prefs Unreadable');
    const holders = await page.evaluate(() =>
      Object.keys(localStorage).filter((key) => localStorage.getItem(key) === 'not json'),
    );
    expect(holders).toHaveLength(1);
    expect(holders[0]).not.toBe('mk:prefs');
  });
});

test.describe('router', () => {
  test('a theme link is read at boot and home goes back in the bar without an entry', async ({ page }) => {
    await openBuilder(page, '/t/abc123');
    expect(await hook(page, 'route')).toBe('Theme(code=abc123)');

    const entries = await historyLength(page);
    await hook(page, 'replaceHome');
    expect(await page.evaluate(() => location.pathname + location.search)).toBe('/');
    expect(await historyLength(page)).toBe(entries);

    // Saving is an edit, and edits never touch history.
    expect(await hook(page, 'addHint', 'edit')).toBe('Done');
    expect(await historyLength(page)).toBe(entries);
  });

  test('back after opening an overlay reports exactly one pop', async ({ page }) => {
    await openBuilder(page);
    const entries = await historyLength(page);
    await hook(page, 'pushOverlay', 'export');
    expect(await historyLength(page)).toBe(entries + 1);
    expect(await page.evaluate(() => location.pathname)).toBe('/');
    expect(await historyState(page)).toBe('{"mkOverlay":"export","mkDepth":1}');

    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');
    await page.waitForTimeout(250);
    expect(await hook(page, 'overlayPops')).toBe('1');
  });

  test('closing an overlay from the UI pops its entry quietly', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'pushOverlay', 'export');
    await hook(page, 'popOverlay');
    await expect.poll(() => historyState(page)).toBe('null');

    // Reopening before the browser finished going back still gets its own entry.
    await page.evaluate(() => {
      window.__mk!.pushOverlay('export');
      window.__mk!.popOverlay();
      window.__mk!.pushOverlay('projects');
    });
    await expect.poll(() => historyState(page)).toBe('{"mkOverlay":"projects","mkDepth":1}');
    expect(await hook(page, 'overlayPops')).toBe('0');

    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');
  });
});

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
    expect(await hook(page, 'readTabProject')).toBe('');
    await hook(page, 'writeTabProject', 'p1');

    await openBuilder(page);
    expect(await hook(page, 'readTabProject')).toBe('p1');
    expect(await hook(page, 'tabId')).not.toBe(firstId);
  });

  test('theme color, splash colors, splash hand off and page hides', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'setThemeColor', '#1a73e8');
    const themeColor = await page.evaluate(() =>
      document.querySelector('meta[name="theme-color"]')?.getAttribute('content'),
    );
    expect(themeColor?.toLowerCase()).toBe('#1a73e8');

    await hook(page, 'writeSplashColors', '#000000,#ffffff');
    expect(await page.evaluate(() => localStorage.getItem('mk:splash'))).toBe('{"light":-16777216,"dark":-1}');

    // No splash on the page yet, so there is nothing to do.
    await hook(page, 'hideSplash');
    await page.evaluate(() => {
      const splash = document.createElement('div');
      splash.id = 'splash';
      document.body.appendChild(splash);
    });
    await hook(page, 'hideSplash');
    expect(await page.evaluate(() => document.getElementById('splash')?.style.opacity)).toBe('0');
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
});

/** Fill localStorage until not even a few characters fit, and return how many filler keys it took. */
async function fillStorage(page: Page): Promise<number> {
  return page.evaluate(() => {
    let count = 0;
    let size = 1024 * 1024;
    while (size >= 4) {
      try {
        localStorage.setItem(`fill:${count}`, 'x'.repeat(size));
        count++;
      } catch {
        size = Math.floor(size / 2);
      }
    }
    return count;
  });
}

async function historyLength(page: Page): Promise<number> {
  return page.evaluate(() => history.length);
}

async function historyState(page: Page): Promise<string> {
  return page.evaluate(() => JSON.stringify(history.state));
}
