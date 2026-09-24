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
// The mirror cannot find the row. A long press puts up foundation's selection handles, which are
// popups, and a popup takes the mirror over for good (D40), leaving it three nodes deep. So the row
// is found by its pixels, the part of the page that changed above or below the field, and each
// button by the gaps between the labels.

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
 * Holds a finger on the middle of the field's hex for longer than a long press takes, then finds the
 * row that came up, in the pixels that changed outside the field.
 */
async function longPressForRow(page: Page, cdp: CDPSession, field: Box): Promise<Row> {
  const before = await page.screenshot();
  const point = { x: field.x + field.width / 2, y: field.y + field.height * 0.4 };
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await page.waitForTimeout(800);
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  let row: Row | null = null;
  await expect
    .poll(
      async () => {
        row = await findRow(page, before, field);
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
async function findRow(page: Page, before: Buffer, field: Box): Promise<Row | null> {
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
    2: ['Paste', 'Select all'],
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
