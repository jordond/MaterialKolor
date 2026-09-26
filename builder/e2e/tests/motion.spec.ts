import { existsSync } from 'node:fs';
import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import { button, focusCanvas, LAND_TIMEOUT_MS, openWorkspace } from '../fixtures/workspace';

// Motion on a library switch. The new skin reveals from the switcher in a circle,
// and under reduced motion it crossfades instead. A small copy of every frame the page draws tells
// the two apart. Mid reveal some cells of the canvas are already the new skin while others are
// still the old one, and a crossfade moves every cell together.

/** The canvas copy is this many cells across and down. */
const CELLS = [32, 18] as const;

/** How far a channel may be from the old or new frame and still count as it. */
const SAME = 10;

/** How long the canvas has to stay the same for the switch to count as done, longer than any reveal. */
const STILL_MS = 1_000;

/** How far a cell has to move between the old and new frame to count as changed. */
const CHANGED = 32;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
  await context.addInitScript(copyFrames, CELLS);
});

test('under reduced motion a library switch crossfades with no reveal', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  const frames = await switchLibrary(page);

  expect(frames.changed).toBeGreaterThan(0);
  expect(frames.split).toEqual([]);
});

test('with motion a library switch reveals from the switcher', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  const frames = await switchLibrary(page);

  expect(frames.changed).toBeGreaterThan(0);
  expect(frames.split.length).toBeGreaterThan(0);
});

test.describe('frozen motion', () => {
  test('a frozen switch to each library looks the same every time', async ({ page }, testInfo) => {
    // A screenshot differs by engine and system, so the baselines are kept for the ones they were
    // made on, Chromium on macOS so far. `--update-snapshots` on another makes its own.
    test.skip(
      !existsSync(testInfo.snapshotPath('material3.png', { kind: 'screenshot' })),
      'Follow-up: no frozen motion baselines for this engine and system yet',
    );
    await openWorkspace(page, '/?motion=frozen');
    await expect(page).toHaveScreenshot('material3.png', { maxDiffPixelRatio: 0.02 });
    for (const [key, name] of [['2', 'unstyled'], ['3', 'fluent'], ['4', 'custom']]) {
      await focusCanvas(page);
      await page.keyboard.press(key);
      await expect(button(page, /^Undo library change/)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
      await expect(page).toHaveScreenshot(`${name}.png`, { maxDiffPixelRatio: 0.02 });
    }
  });
});

interface Frames {
  /** How many cells differ between the frame before the switch and the frame after it. */
  changed: number;
  /** The frames in between where some changed cells are already new and others still old. */
  split: string[];
}

/** Presses 2 for Unstyled and reads the frames the page drew from just before until it is still again. */
async function switchLibrary(page: Page): Promise<Frames> {
  await openWorkspace(page);
  await focusCanvas(page);
  await page.evaluate(() => (window as unknown as { mkCopy: { start: () => void } }).mkCopy.start());

  await page.keyboard.press('2');
  await expect(button(page, /^Undo library change/)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect.poll(() => stillFor(page), { timeout: LAND_TIMEOUT_MS }).toBeGreaterThan(STILL_MS);

  const frames = await page.evaluate(() => (window as unknown as { mkCopy: { stop: () => number[][] } }).mkCopy.stop());
  return compare(frames);
}

/** How long the canvas has drawn nothing new, in milliseconds, or zero before it has drawn twice. */
async function stillFor(page: Page): Promise<number> {
  return page.evaluate(() => {
    const copy = (window as unknown as { mkCopy: { frames: number[][]; changedAt: number } }).mkCopy;
    return copy.frames.length < 2 ? 0 : performance.now() - copy.changedAt;
  });
}

function compare(frames: number[][]): Frames {
  const [first, last] = [frames[0], frames.at(-1)!];
  const cells = first.length / 4;
  const distance = (one: number[], two: number[], cell: number) =>
    Math.max(...[0, 1, 2].map((channel) => Math.abs(one[cell * 4 + channel] - two[cell * 4 + channel])));
  const changed = [...Array(cells).keys()].filter((cell) => distance(first, last, cell) > CHANGED);
  const split: string[] = [];
  frames.forEach((frame, index) => {
    const fresh = changed.filter((cell) => distance(frame, last, cell) <= SAME).length;
    const stale = changed.filter((cell) => distance(frame, first, cell) <= SAME).length;
    if (fresh >= changed.length / 10 && stale >= changed.length / 10) split.push(`frame ${index}, ${fresh} new and ${stale} old`);
  });
  return { changed: changed.length, split };
}

/**
 * Runs in the page before it loads. Wraps `requestAnimationFrame` so, once `mkCopy.start()` has run,
 * each frame the page draws is scaled down to [cells] and kept, read straight after the page's own
 * callback while the canvas still holds what it drew. The first frame after the key is the old skin,
 * since the reveal and the crossfade both start from a copy of it.
 */
function copyFrames([across, down]: readonly [number, number]): void {
  const frames: number[][] = [];
  let copying = false;
  let lastTime = -1;
  const small = document.createElement('canvas');
  small.width = across;
  small.height = down;
  const findCanvas = (root: Document | ShadowRoot): HTMLCanvasElement | null => {
    const found = root.querySelector('canvas');
    if (found) return found;
    for (const element of Array.from(root.querySelectorAll('*'))) {
      const inner = element.shadowRoot ? findCanvas(element.shadowRoot) : null;
      if (inner) return inner;
    }
    return null;
  };
  const state = { frames, changedAt: 0, start: () => {}, stop: () => frames };
  const copy = (time: number) => {
    if (!copying || time === lastTime) return;
    lastTime = time;
    const canvas = findCanvas(document);
    const context = small.getContext('2d', { willReadFrequently: true });
    if (!canvas || !context || canvas.width === 0) return;
    context.drawImage(canvas, 0, 0, canvas.width, canvas.height, 0, 0, across, down);
    const frame = Array.from(context.getImageData(0, 0, across, down).data);
    const last = frames.at(-1);
    if (!last || frame.some((value, at) => Math.abs(value - last[at]) > 2)) state.changedAt = performance.now();
    frames.push(frame);
  };
  const original = window.requestAnimationFrame.bind(window);
  window.requestAnimationFrame = (callback) =>
    original((time) => {
      callback(time);
      copy(time);
    });
  state.start = () => {
    copying = true;
    state.changedAt = performance.now();
  };
  state.stop = () => {
    copying = false;
    return frames;
  };
  (window as unknown as { mkCopy: unknown }).mkCopy = state;
}
