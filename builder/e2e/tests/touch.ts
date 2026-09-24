import { expect, type CDPSession, type Locator, type Page } from '@playwright/test';

// b-228c
// What the touch specs share, a finger through a Chromium CDP session, the mirror's buttons and the
// mirror's size. `text-toolbar.spec.ts` and `sample-fields.spec.ts` both import it.

export interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface Point {
  x: number;
  y: number;
}

/** How long a long press holds the finger down, well past the long press timeout under load. */
const LONG_PRESS_MS = 1_000;

/**
 * How long a box has to hold still to count as laid out, longer than the listener's longest wait
 * before it syncs the mirror.
 */
const STILL_MS = 1_200;

/** The box of [target] as it is now. */
export async function boxOf(target: Locator): Promise<Box> {
  const box = await target.boundingBox();
  if (!box) throw new Error('The field has no box');
  return box;
}

/**
 * The box of [target] once it is laid out, when it has a height and has held still for `STILL_MS`.
 * A scroll, a docked panel or a dialog that is still moving moves the mirror's box with it, and a
 * touch read off the box before then lands beside the field.
 */
export async function settledBox(target: Locator, timeout = 15_000): Promise<Box> {
  const page = target.page();
  const deadline = Date.now() + timeout;
  let last: Box | null = null;
  let since = Date.now();
  while (Date.now() < deadline) {
    const box = await target.boundingBox();
    const shown = box !== null && box.height > 0;
    if (shown && last && sameBox(box, last)) {
      if (Date.now() - since >= STILL_MS) return box;
    } else {
      last = shown ? box : null;
      since = Date.now();
    }
    await page.waitForTimeout(200);
  }
  throw new Error(`The field never held still, last at ${JSON.stringify(last)}`);
}

function sameBox(one: Box, two: Box): boolean {
  return (
    Math.abs(one.x - two.x) < 0.5 &&
    Math.abs(one.y - two.y) < 0.5 &&
    Math.abs(one.width - two.width) < 0.5 &&
    Math.abs(one.height - two.height) < 0.5
  );
}

/** Holds a finger at [at] past the long press timeout, then lifts it. */
export async function longPressAt(page: Page, cdp: CDPSession, at: Point): Promise<void> {
  checkPoint(at);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [at] });
  await page.waitForTimeout(LONG_PRESS_MS);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

/** Taps [point] with a finger. */
export async function tap(cdp: CDPSession, point: Point): Promise<void> {
  checkPoint(point);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await new Promise((resolve) => setTimeout(resolve, 60));
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
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
 * The number of elements in the mirror once it has held still for a second, which is longer than
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
  for (let round = 0; round < 20; round++) {
    await page.waitForTimeout(1_000);
    const now = await count();
    if (now === last) return now;
    last = now;
  }
  return last;
}
