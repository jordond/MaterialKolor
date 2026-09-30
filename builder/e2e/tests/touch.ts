import type { CDPSession, Locator, Page } from '@playwright/test';
import { expect } from './builder';
import { nextFrames, settledBox, type Box } from '../fixtures/workspace';

export { settledBox };

// What the touch specs share, a finger through a Chromium CDP session, the mirror's buttons and the
// mirror's size. `text-toolbar.spec.ts` and `sample-fields.spec.ts` both import it.

export type { Box };

export interface Point {
  x: number;
  y: number;
}

/** How long a long press holds the finger down, well past the long press timeout under load. */
const LONG_PRESS_MS = 1_000;

/** The box of [target] as it is now. */
export async function boxOf(target: Locator): Promise<Box> {
  const box = await target.boundingBox();
  if (!box) throw new Error('The field has no box');
  return box;
}

/** Holds a finger at [at] past the long press timeout, then lifts it. */
export async function longPressAt(page: Page, cdp: CDPSession, at: Point): Promise<void> {
  checkPoint(at);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [at] });
  await hold(LONG_PRESS_MS);
  // The page has drawn with the finger still down, so the press has reached Compose.
  await nextFrames(page);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

/** Taps [point] with a finger. */
export async function tap(cdp: CDPSession, point: Point): Promise<void> {
  checkPoint(point);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await hold(60);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

/** Keeps a finger down for [ms], how long the gesture lasts and not a wait on the page. */
function hold(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Fails with a readable message on a point with no place, which CDP only calls invalid parameters. */
function checkPoint(point: Point): void {
  if (!Number.isFinite(point?.x) || !Number.isFinite(point?.y)) {
    throw new Error(`No point to touch, got ${JSON.stringify(point)}`);
  }
}

/** The button named [name] in the mirror, the whole name when it is a string. */
export function mirrorButton(page: Page, name: string | RegExp): Locator {
  return page.locator('#cmp_a11y_root').getByRole('button', { name, exact: true }).first();
}

/**
 * Clicks [button] and waits for [opened] to show in the mirror, and clicks again if it does not,
 * since a click that lands while the page is still settling can go unheard. It never clicks while
 * a click may still be opening, since a second click would land on the scrim and close it.
 */
export async function openBy(page: Page, button: Locator, opened: Locator): Promise<void> {
  await expect(button).toBeAttached({ timeout: 30_000 });
  for (let attempt = 0; attempt < 3; attempt++) {
    const box = await button.boundingBox();
    if (box) await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    try {
      await opened.first().waitFor({ state: 'attached', timeout: 8_000 });
      return;
    } catch {
      // Not open yet, so the click went unheard.
    }
  }
  throw new Error('The click never opened it');
}

/**
 * The number of elements in the mirror once it has held the same for a second, which is longer than
 * the listener's longest wait before it syncs.
 */
export async function settledMirror(page: Page): Promise<number> {
  // The mirror sits in the viewport's shadow root, out of reach of a plain document query.
  const count = () =>
    page.evaluate(() => {
      const roots: (Document | ShadowRoot)[] = [document];
      for (let i = 0; i < roots.length; i++) {
        const mirror = roots[i].querySelector('#cmp_a11y_root');
        if (mirror) return mirror.querySelectorAll('*').length;
        roots[i].querySelectorAll('*').forEach((element) => {
          if (element.shadowRoot) roots.push(element.shadowRoot);
        });
      }
      return -1;
    });
  let last = -1;
  await expect
    .poll(
      async () => {
        const now = await count();
        const same = now === last;
        last = now;
        return same;
      },
      { timeout: 20_000, intervals: [1_000], message: 'The mirror never held still' },
    )
    .toBe(true);
  return last;
}
