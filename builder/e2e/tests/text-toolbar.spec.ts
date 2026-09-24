import { expect, test, type CDPSession, type Locator, type Page } from '@playwright/test';
import { openBuilder, reloadBuilder, wantHooks } from './builder';

// b-224
// The text toolbar the kit draws in the page for a touch selection (D40). A long press on a field
// shows it, and Paste reads the clipboard inside the tap's own user activation. Chromium only, since
// Playwright drives the long press through a CDP session.
//
// Foundation shows the toolbar on touch input, not at a phone width, so the tests keep the desktop
// viewport, where the seed field sits docked on the first screen, and only turn touch on.
//
// The row is found by its pixels, the part of the page that changed above or below the field, and
// each button by the gaps between the labels. That dates from when a long press put up foundation's
// selection handles, popups that took the mirror over for good (D40), and it still works now that
// the handles stay off on the web (D45).

/** The digits the Paste test puts over the long pressed word, the hex's own digits. */
const PASTED = '0B6E4F';

test.use({ hasTouch: true });

test.beforeEach(async ({ context, browserName }) => {
  test.skip(browserName !== 'chromium', 'The long press goes through a Chromium CDP session');
  await wantHooks(context);
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
});

test('a long press on a field shows the toolbar and Paste puts the clipboard text in', async ({ page, context }) => {
  await openBuilder(page);
  await page.evaluate((text) => navigator.clipboard.writeText(text), PASTED);
  const cdp = await context.newCDPSession(page);
  const field = await seedField(page);
  const box = await boxOf(field);
  const tint = await themeColor(page);

  const row = await longPressForRow(page, cdp, box);
  expect(row.labels).toEqual(['Cut', 'Copy', 'Paste', 'Select all']);
  await tap(cdp, row.button('Paste'));

  // The new seed tints the page, and once saved it is what the field shows after a reload.
  await expect.poll(() => themeColor(page), { timeout: 10_000 }).not.toBe(tint);
  await page.waitForTimeout(1_500);
  await reloadBuilder(page);
  await expect.poll(async () => (await seedField(page)).textContent(), { timeout: 10_000 }).toContain(PASTED);
});

test('Copy from the toolbar puts the selection on the clipboard', async ({ page, context }) => {
  await openBuilder(page);
  await page.evaluate(() => navigator.clipboard.writeText(''));
  const cdp = await context.newCDPSession(page);
  const field = await seedField(page);
  const text = (await field.textContent()) ?? '';
  const box = await boxOf(field);

  const row = await longPressForRow(page, cdp, box);
  await tap(cdp, row.button('Copy'));

  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText()), { timeout: 10_000 }).not.toBe('');
  const copied = await page.evaluate(() => navigator.clipboard.readText());
  expect(text).toContain(copied);
});

// b-228
// Spike S13. A long press used to put up foundation's selection handles, popups that took the
// mirror over for good (D40). With the handles kept out of a field on the web, the mirror keeps the
// page through the long press and still hears the seed the Paste puts in.
test('the mirror keeps the page through a long press and hears the paste after it', async ({ page, context }) => {
  await openBuilder(page);
  await page.evaluate((text) => navigator.clipboard.writeText(text), PASTED);
  const cdp = await context.newCDPSession(page);
  const field = await seedField(page);
  const box = await boxOf(field);
  const before = await settledMirror(page);

  const row = await longPressForRow(page, cdp, box);
  const during = await settledMirror(page);
  console.log(`b-228 mirror nodes: before ${before}, after the long press ${during}`);
  expect(during).toBeGreaterThan(before * 0.8);

  await tap(cdp, row.button('Paste'));
  await expect
    .poll(async () => (await seedField(page)).textContent(), { timeout: 10_000 })
    .toContain(PASTED);
  console.log(`b-228 mirror nodes after the paste: ${await settledMirror(page)}`);
});

// b-228a
// D45 reaches the text the builder shows for copying by hand too. A share link and a refused copy's
// hex sit in read only fields, where a long press brings up Copy with no handles, and the export
// code takes no selection from a finger at all. Each keeps the mirror through a long press.

test('a long press on the share link keeps the mirror and shows Copy', async ({ page, context }) => {
  await openBuilder(page);
  const cdp = await context.newCDPSession(page);
  await openBy(page, mirrorButton(page, 'Share'), mirrorButton(page, 'Copy link'));
  const link = await copyField(page);
  const before = await settledMirror(page);

  const row = await longPressForRow(page, cdp, link, { readOnly: true });
  expect(row.labels).toEqual(['Copy', 'Select all']);
  const after = await settledMirror(page);
  console.log(`b-228a mirror nodes in the share dialog: before ${before}, after the long press ${after}`);
  expect(after).toBeGreaterThan(before * 0.8);
});

test('a long press on the text of a refused copy keeps the mirror and shows Copy', async ({ page, context }) => {
  await context.addInitScript(() => {
    const refuse = () => Promise.reject(new DOMException('Refused for the test', 'NotAllowedError'));
    Object.defineProperty(Clipboard.prototype, 'writeText', { configurable: true, value: refuse });
    Object.defineProperty(Clipboard.prototype, 'write', { configurable: true, value: refuse });
  });
  await openBuilder(page);
  const cdp = await context.newCDPSession(page);
  await openBy(page, mirrorButton(page, /^Copy hex/), mirrorButton(page, 'Done'));
  const hex = await copyField(page);
  const before = await settledMirror(page);

  const at = { x: hex.x + 16, y: hex.y + hex.height / 2 };
  const row = await longPressForRow(page, cdp, hex, { readOnly: true, at });
  expect(row.labels).toEqual(['Copy', 'Select all']);
  const after = await settledMirror(page);
  console.log(`b-228a mirror nodes in the manual copy dialog: before ${before}, after the long press ${after}`);
  expect(after).toBeGreaterThan(before * 0.8);
});

test('a long press on the export code keeps the mirror', async ({ page, context }) => {
  await openBuilder(page);
  const cdp = await context.newCDPSession(page);
  const code = page.locator('#cmp_a11y_root').getByRole('list', { name: 'Color.kt' });
  await openBy(page, mirrorButton(page, 'Export code'), code);
  const box = await boxOf(code);
  const before = await settledMirror(page);

  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: box.x + 120, y: box.y + 44 }] });
  await page.waitForTimeout(800);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  const after = await settledMirror(page);
  console.log(`b-228a mirror nodes on the export sheet: before ${before}, after the long press ${after}`);
  expect(after).toBeGreaterThan(before * 0.8);
});

/** The button named [name] in the mirror, the whole name when it is a string. */
function mirrorButton(page: Page, name: string | RegExp): Locator {
  return page.locator('#cmp_a11y_root').getByRole('button', { name, exact: true }).first();
}

/**
 * Clicks [button] and waits for [opened] to show in the mirror, and clicks again if it does not,
 * since a click that lands while the page is still settling can go unheard. It never clicks while
 * a click may still be opening, since a second click would land on the scrim and close it.
 */
async function openBy(page: Page, button: Locator, opened: Locator): Promise<void> {
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

/** The read only field of the open dialog, which holds the text to copy by hand. */
async function copyField(page: Page): Promise<Box> {
  const field = page.locator('#cmp_a11y_root').getByRole('textbox', { name: /^Text to copy/ });
  await expect(field).toBeAttached({ timeout: 10_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return boxOf(field);
}

/**
 * The number of elements in the mirror once it has held still for a second, which is longer than
 * the listener's longest wait before it syncs.
 */
async function settledMirror(page: Page): Promise<number> {
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

interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Point {
  x: number;
  y: number;
}

/** The toolbar row as it shows on screen, its labels in order and a point on each button. */
interface Row {
  labels: string[];
  button(label: string): Point;
}

/** The seed field, found by its box in the mirror, docked on the first screen at a desktop width. */
async function seedField(page: Page): Promise<Locator> {
  const field = page.locator('#cmp_a11y_root').getByRole('textbox', { name: /^Seed color/ });
  await expect(field).toBeAttached({ timeout: 30_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

async function boxOf(target: Locator): Promise<Box> {
  const box = await target.boundingBox();
  if (!box) throw new Error('The field has no box');
  return box;
}

/** The `theme-color` the shell tints the browser with, which follows the seed. */
async function themeColor(page: Page): Promise<string> {
  return page.evaluate(() => document.querySelector('meta[name="theme-color"]')?.getAttribute('content') ?? '');
}

/**
 * Holds a finger on the middle of the field's hex, or on [options.at], for longer than a long press
 * takes, then finds the row that came up, in the pixels that changed outside the field. A read only
 * field's row has no Cut or Paste.
 */
async function longPressForRow(
  page: Page,
  cdp: CDPSession,
  field: Box,
  options: { readOnly?: boolean; at?: Point } = {},
): Promise<Row> {
  const before = await page.screenshot();
  const point = options.at ?? { x: field.x + field.width / 2, y: field.y + field.height * 0.4 };
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await page.waitForTimeout(800);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  let row: Row | null = null;
  await expect
    .poll(
      async () => {
        row = await findRow(page, before, field, options.readOnly ?? false);
        return row?.labels.length ?? 0;
      },
      { timeout: 10_000 },
    )
    .toBeGreaterThan(0);
  return row!;
}

/** Taps [point] with a finger. */
async function tap(cdp: CDPSession, point: Point): Promise<void> {
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await new Promise((resolve) => setTimeout(resolve, 60));
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

/**
 * The row in the page's pixels, or null while none shows. It compares a screenshot from before the
 * long press with one now, in a band above and below the field that leaves out the field itself and
 * its handles. The rows of pixels with a long run of change are the toolbar, and along its middle
 * each run of label pixels, split at a gap wider than a space, is one button.
 */
async function findRow(page: Page, before: Buffer, field: Box, readOnly: boolean): Promise<Row | null> {
  const after = await page.screenshot();
  const found = await page.evaluate(
    async ({ beforePng, afterPng, box }) => {
      const pixels = async (png: string) => {
        // Decoded by hand, since the page's content policy turns a data URL fetch away.
        const bytes = Uint8Array.from(atob(png), (char) => char.charCodeAt(0));
        const bitmap = await createImageBitmap(new Blob([bytes], { type: 'image/png' }));
        const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
        const context = canvas.getContext('2d')!;
        context.drawImage(bitmap, 0, 0);
        return context.getImageData(0, 0, bitmap.width, bitmap.height);
      };
      const a = await pixels(beforePng);
      const b = await pixels(afterPng);
      const width = b.width;
      const at = (image: ImageData, x: number, y: number) => (y * width + x) * 4;
      const distance = (one: ImageData, i: number, two: ImageData, j: number) =>
        Math.abs(one.data[i] - two.data[j]) +
        Math.abs(one.data[i + 1] - two.data[j + 1]) +
        Math.abs(one.data[i + 2] - two.data[j + 2]);
      const margin = 16;
      const inField = (x: number, y: number) =>
        x > box.x - margin && x < box.x + box.width + margin && y > box.y - margin && y < box.y + box.height + margin;
      const top = Math.max(0, Math.floor(box.y - 160));
      const bottom = Math.min(b.height - 1, Math.ceil(box.y + box.height + 160));
      const rows: number[] = [];
      let left = width;
      let right = 0;
      for (let y = top; y <= bottom; y++) {
        let changed = 0;
        for (let x = 0; x < width; x++) {
          if (inField(x, y)) continue;
          if (distance(a, at(a, x, y), b, at(b, x, y)) > 30) {
            changed++;
            left = Math.min(left, x);
            right = Math.max(right, x);
          }
        }
        if (changed > 120) rows.push(y);
      }
      if (rows.length < 20) return null;
      const middle = Math.round((rows[0] + rows[rows.length - 1]) / 2);
      const counts = new Map<string, number>();
      for (let x = left; x <= right; x++) {
        const i = at(b, x, middle);
        const key = `${b.data[i]},${b.data[i + 1]},${b.data[i + 2]}`;
        counts.set(key, (counts.get(key) ?? 0) + 1);
      }
      const surface = [...counts.entries()].sort((one, two) => two[1] - one[1])[0][0].split(',').map(Number);
      const ink = (x: number) => {
        for (let y = middle - 6; y <= middle + 6; y++) {
          const i = at(b, x, y);
          const off =
            Math.abs(b.data[i] - surface[0]) + Math.abs(b.data[i + 1] - surface[1]) + Math.abs(b.data[i + 2] - surface[2]);
          if (off > 90) return true;
        }
        return false;
      };
      const labels: { start: number; end: number }[] = [];
      for (let x = left + 8; x <= right - 8; x++) {
        if (!ink(x)) continue;
        const last = labels[labels.length - 1];
        if (last && x - last.end <= 14) last.end = x;
        else labels.push({ start: x, end: x });
      }
      return { y: middle, centers: labels.map((label) => (label.start + label.end) / 2) };
    },
    { beforePng: before.toString('base64'), afterPng: after.toString('base64'), box: field },
  );
  if (!found || found.centers.length === 0) return null;
  const names: Record<number, string[]> = {
    2: readOnly ? ['Copy', 'Select all'] : ['Paste', 'Select all'],
    3: ['Cut', 'Copy', 'Paste'],
    4: ['Cut', 'Copy', 'Paste', 'Select all'],
  };
  const labels = names[found.centers.length];
  if (!labels) throw new Error(`The row shows ${found.centers.length} labels`);
  return {
    labels,
    button: (label) => ({ x: found.centers[labels.indexOf(label)], y: found.y }),
  };
}
