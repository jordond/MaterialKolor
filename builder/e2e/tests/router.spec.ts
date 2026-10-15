import type { Page } from '@playwright/test';
import { expect, hook, openBuilder, reloadBuilder, test } from './builder';
import { openWorkspace, typeSeed } from '../fixtures/workspace';

// The router behind the builder, driven through the shell's test hooks. The route read at boot and
// the history entries overlays push and pop. Then one real edit, which never moves the address or
// adds an entry. palette.spec.ts opens a real panel into the history and goes Back out of it.

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
    await backSettled(page, 1);
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
    await backSettled(page, 1);
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
    await backSettled(page, 3);
    expect(await hook(page, 'overlayPops')).toBe('1');

    await hook(page, 'pushOverlay', 'projects');
    expect(await historyState(page)).toBe('{"mkOverlay":"projects","mkDepth":1}');
    await page.evaluate(() => history.back());
    await expect.poll(() => hook(page, 'overlayPops')).toBe('2');
  });
});

test('an edit keeps the address at / and adds no history entry', async ({ page }) => {
  await openWorkspace(page);
  const entries = await historyLength(page);

  await typeSeed(page, '#0B6E4F');

  expect(await page.evaluate(() => location.pathname + location.search + location.hash)).toBe('/');
  expect(await historyLength(page)).toBe(entries);
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

/**
 * Waits until the router has handled [moves] `popstate` events since the page loaded and has no back
 * of its own still on the way, so any second pop would have been counted already.
 */
async function backSettled(page: Page, moves: number): Promise<void> {
  await expect.poll(() => hook(page, 'routerMoves')).toBe(String(moves));
  await expect.poll(() => hook(page, 'routerPendingBacks')).toBe('0');
}

async function historyLength(page: Page): Promise<number> {
  return page.evaluate(() => history.length);
}

async function historyState(page: Page): Promise<string> {
  return page.evaluate(() => JSON.stringify(history.state));
}
