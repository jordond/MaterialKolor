import { expect, test, type Page } from '@playwright/test';
import { hook, openBuilder, reloadBuilder, site, wantHooks } from './builder';

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
    await hook(page, 'watchQuarantine');
    await page.evaluate(() => localStorage.setItem('mk:prefs', 'not json'));

    expect(await hook(page, 'readHints')).toBe('');
    expect(await hook(page, 'readHints')).toBe('');
    await expect.poll(() => hook(page, 'quarantined')).toBe('mk:prefs Unreadable');
    const holders = await page.evaluate(() =>
      Object.keys(localStorage).filter((key) => localStorage.getItem(key) === 'not json'),
    );
    expect(holders).toHaveLength(1);
    expect(holders[0]).toMatch(/^mk:quarantine:/);
  });

  test("a newer build's index from another tab stays as it is and refuses writes", async ({ context }) => {
    const newer = '{"schema":999,"data":{"projects":[],"from":"a newer build"}}';
    const older = await context.newPage();
    const other = await context.newPage();
    await openBuilder(older);
    await openBuilder(other);
    await hook(older, 'watchQuarantine');

    await other.evaluate((text) => localStorage.setItem('mk:index', text), newer);
    await expect.poll(() => hook(older, 'externalChanges')).toContain('Index');

    expect(await hook(older, 'readIndex')).toBe('0');
    expect(await hook(older, 'touchIndex')).toBe('Unavailable');
    expect(await hook(older, 'readIndex')).toBe('0');
    expect(await hook(older, 'quarantined')).toBe('mk:index NewerSchema');
    expect(await older.evaluate(() => localStorage.getItem('mk:index'))).toBe(newer);
    expect(await older.evaluate(() => Object.keys(localStorage).filter((key) => key.startsWith('mk:quarantine:')))).toEqual(
      [],
    );
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

  test('backs the browser folds into one move are all settled', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'pushOverlay', 'export');
    await hook(page, 'pushOverlay', 'projects');

    // Both closes land as one two-entry move.
    await closeWithBacksHeld(page, 2);
    await page.evaluate(() => history.go(-2));
    await expect.poll(() => historyState(page)).toBe('null');
    expect(await hook(page, 'overlayPops')).toBe('0');

    // One close and one user back land as one move, and only the user's back is reported.
    await hook(page, 'pushOverlay', 'export');
    expect(await historyState(page)).toBe('{"mkOverlay":"export","mkDepth":1}');
    await hook(page, 'pushOverlay', 'projects');
    await closeWithBacksHeld(page, 1);
    await page.evaluate(() => history.go(-2));
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');
    expect(await historyState(page)).toBe('null');

    await hook(page, 'pushOverlay', 'export');
    expect(await historyState(page)).toBe('{"mkOverlay":"export","mkDepth":1}');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('2');
  });

  test('a back the browser refuses leaves nothing pending', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'pushOverlay', 'export');
    await page.evaluate(() => {
      history.go = () => {
        throw new DOMException('Too many calls to the history API', 'SecurityError');
      };
      window.__mk!.popOverlay();
      delete (history as { go?: unknown }).go;
    });
    expect(await historyState(page)).toBe('{"mkOverlay":"export","mkDepth":1}');

    // The next overlay goes on at once, and the next back closes it.
    await hook(page, 'pushOverlay', 'projects');
    expect(await historyState(page)).toBe('{"mkOverlay":"projects","mkDepth":2}');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');

    // The entry the refused back left behind is skipped quietly after that.
    await expect.poll(() => historyState(page)).toBe('null');
    expect(await hook(page, 'overlayPops')).toBe('1');
  });

  test('a reload on an overlay entry goes back quietly', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'pushOverlay', 'export');
    await hook(page, 'pushOverlay', 'projects');

    await reloadBuilder(page);
    await expect.poll(() => historyState(page)).toBe('null');
    await page.waitForTimeout(250);
    expect(await hook(page, 'overlayPops')).toBe('0');

    await hook(page, 'pushOverlay', 'export');
    expect(await historyState(page)).toBe('{"mkOverlay":"export","mkDepth":1}');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');
  });

  test('a forward into an overlay entry goes back quietly', async ({ page }) => {
    await openBuilder(page);
    await hook(page, 'pushOverlay', 'export');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('1');

    await page.evaluate(
      () =>
        new Promise<void>((resolve) => {
          window.addEventListener('popstate', () => resolve(), { once: true });
          history.forward();
        }),
    );
    await expect.poll(() => historyState(page)).toBe('null');
    await page.waitForTimeout(250);
    expect(await hook(page, 'overlayPops')).toBe('1');

    await hook(page, 'pushOverlay', 'projects');
    expect(await historyState(page)).toBe('{"mkOverlay":"projects","mkDepth":1}');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('2');
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

test('the site server answers a malformed path with 400 and keeps serving', async ({ request }) => {
  expect((await request.get(site('/%E0%A4%A'))).status()).toBe(400);
  expect((await request.get(site('/'))).status()).toBe(200);
});

/** Close [count] overlays from the UI while the browser's back does nothing, so the router's backs stay pending. */
async function closeWithBacksHeld(page: Page, count: number): Promise<void> {
  await page.evaluate((closes) => {
    history.go = () => {};
    try {
      for (let close = 0; close < closes; close++) window.__mk!.popOverlay();
    } finally {
      delete (history as { go?: unknown }).go;
    }
  }, count);
}

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
