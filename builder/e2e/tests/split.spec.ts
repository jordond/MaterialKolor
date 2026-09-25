import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import { dragSplit, LAND_TIMEOUT_MS, onPage, openWorkspace, SPLIT_HANDLE } from '../fixtures/workspace';

// Dragging the preview's split handle. The split follows the pointer, and on Chromium,
// the one engine with the Long Animation Frames API, no frame of the drag runs long.

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('the split follows a drag with no long animation frame', async ({ page, browserName }) => {
  await openWorkspace(page);
  const handle = onPage(page, SPLIT_HANDLE);
  const before = (await handle.first().textContent()) ?? '';
  const loaf = browserName === 'chromium';
  if (loaf) await observeLongFrames(page);

  await dragSplit(page, -240);

  await expect.poll(async () => (await handle.first().textContent()) ?? '', { timeout: LAND_TIMEOUT_MS }).not.toBe(before);
  if (loaf) expect(await longFrames(page)).toEqual([]);
});

/** Starts noting every long animation frame from here on. */
async function observeLongFrames(page: Page): Promise<void> {
  await page.evaluate(() => {
    const frames: string[] = [];
    (window as unknown as { mkLongFrames: string[] }).mkLongFrames = frames;
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) frames.push(`${Math.round(entry.duration)} ms at ${Math.round(entry.startTime)}`);
    }).observe({ type: 'long-animation-frame' });
  });
}

/** The long animation frames noted so far, once the frame in flight has been reported. */
async function longFrames(page: Page): Promise<string[]> {
  return page.evaluate(
    () =>
      new Promise<string[]>((resolve) =>
        requestAnimationFrame(() =>
          requestAnimationFrame(() => resolve((window as unknown as { mkLongFrames: string[] }).mkLongFrames)),
        ),
      ),
  );
}
