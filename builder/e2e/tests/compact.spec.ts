import { expect, test, type CDPSession, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import { settledBox, tap, type Box, type Point } from './touch';

// b-406
// Flow 5.7 on a phone held upright, with a finger. The preview comes first, the library chips sit
// under the top bar and the poster peeks from the bottom. Pick opens the picker as a sheet, where a
// drag on the hue repaints the seed as the finger moves. Dragging the poster sheet to half reaches
// the fine tune rows, and Export opens over the whole screen.

const A11Y = '#cmp_a11y_root';

/** The top bar's height, in CSS pixels at a device scale of one. */
const TOP_BAR_HEIGHT = 64;

const VIEWPORT = { width: 390, height: 844 };

test.use({ viewport: VIEWPORT, hasTouch: true });

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('at 390 wide with a finger, the chips, the peek, the picker, half height and Export all work', async ({
  page,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'The finger goes through a Chromium CDP session');
  await openBuilder(page);
  const cdp = await page.context().newCDPSession(page);

  // The chips sit in their own row under the top bar.
  const chip = page.locator(A11Y).getByRole('radio', { name: /^Fluent/ }).first();
  const chipBox = await settledBox(chip, 30_000);
  expect(chipBox.y).toBeGreaterThanOrEqual(TOP_BAR_HEIGHT - 1);
  expect(chipBox.y).toBeLessThan(TOP_BAR_HEIGHT * 2);

  // The poster peeks from the bottom with the seed row, under the preview.
  const shuffle = button(page, 'Shuffle');
  const peek = await settledBox(shuffle);
  expect(peek.y).toBeGreaterThan(VIEWPORT.height / 2);

  // Pick opens the picker, and the seed follows a finger on the hue while it is still down.
  const before = await seedHex(page);
  await tap(cdp, middle(await settledBox(button(page, 'Pick'))));
  const hue = page.locator(A11Y).getByRole('slider', { name: /^Hue/ }).first();
  const track = await settledBox(hue);
  const from = middle(track);
  await touch(cdp, 'touchStart', from);
  await touch(cdp, 'touchMove', { x: from.x + track.width / 4, y: from.y });
  await expect.poll(() => seedHex(page), { timeout: 10_000 }).not.toBe(before);
  await touch(cdp, 'touchEnd');
  await tap(cdp, middle(await settledBox(button(page, 'Done'))));
  await expect(button(page, 'Done')).toHaveCount(0, { timeout: 10_000 });

  // A drag up from the sheet's top edge brings the fine tune rows into view.
  const keyColors = page.locator(A11Y).getByText('Key colors', { exact: true }).first();
  const edge = { x: VIEWPORT.width / 2, y: (await settledBox(shuffle)).y - 16 };
  await drag(cdp, edge, { x: edge.x, y: VIEWPORT.height / 2 });
  await expect
    .poll(async () => {
      const box = await keyColors.boundingBox();
      return box !== null && box.y > 0 && box.y + box.height <= VIEWPORT.height;
    }, { timeout: 15_000 })
    .toBe(true);

  // Export opens over the whole screen.
  await tap(cdp, middle(await settledBox(button(page, 'Export code'))));
  const exportSheet = page.locator(A11Y).getByText(/^Copy/).first();
  await expect(exportSheet).toBeAttached({ timeout: 15_000 });
  const panel = await settledBox(page.locator(A11Y).getByRole('dialog').first());
  expect(panel.width).toBeGreaterThanOrEqual(VIEWPORT.width - 1);
});

/** The mirror's button called [name], the whole name. */
function button(page: Page, name: string): Locator {
  return page.locator(A11Y).getByRole('button', { name, exact: true }).first();
}

/** The seed's hex as the poster's seed field shows it. */
async function seedHex(page: Page): Promise<string> {
  const field = page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ }).first();
  return (await field.textContent()) ?? '';
}

function middle(box: Box): Point {
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 };
}

/** One touch event, the finger at [at], or lifted when there is no point. */
async function touch(cdp: CDPSession, type: 'touchStart' | 'touchMove' | 'touchEnd', at?: Point): Promise<void> {
  await cdp.send('Input.dispatchTouchEvent', { type, touchPoints: at ? [at] : [] });
  await new Promise((resolve) => setTimeout(resolve, 50));
}

/** Drags a finger from [from] to [to] in small steps, the way a thumb moves. */
async function drag(cdp: CDPSession, from: Point, to: Point): Promise<void> {
  const steps = 12;
  await touch(cdp, 'touchStart', from);
  for (let step = 1; step <= steps; step++) {
    const at = { x: from.x + ((to.x - from.x) * step) / steps, y: from.y + ((to.y - from.y) * step) / steps };
    await touch(cdp, 'touchMove', at);
  }
  await touch(cdp, 'touchEnd');
}
