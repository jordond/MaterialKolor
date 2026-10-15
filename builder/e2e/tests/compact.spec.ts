import type { CDPSession, Locator, Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { holdBetweenTouches } from '../fixtures/timing';
import { settledBox, tap, type Box, type Point } from './touch';

// The builder on a phone held upright, with a finger. The preview comes first, the library chips sit
// under the top bar and the poster peeks from the bottom. Pick opens the picker as a sheet, where a
// drag on the hue repaints the seed as the finger moves. Dragging the poster sheet to half reaches
// the Fine-tune button, and Export opens over the whole screen.

const A11Y = '#cmp_a11y_root';

/** The top bar's height, in CSS pixels at a device scale of one. */
const TOP_BAR_HEIGHT = 64;

const VIEWPORT = { width: 390, height: 844 };

test.use({ viewport: VIEWPORT, hasTouch: true });

test('at 390 wide with a finger, the chips sit under the bar, the poster peeks and Pick follows the finger', async ({
  page,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'The finger goes through a Chromium CDP session');
  await openBuilder(page);
  const cdp = await page.context().newCDPSession(page);

  // The chips sit in their own row under the top bar.
  // The web mirror reads every kit control as a button with its role and state folded into the name.
  const chip = page.locator(A11Y).getByRole('button', { name: /^Fluent, radio/ }).first();
  const chipBox = await settledBox(chip, 30_000);
  expect(chipBox.y).toBeGreaterThanOrEqual(TOP_BAR_HEIGHT - 1);
  expect(chipBox.y).toBeLessThan(TOP_BAR_HEIGHT * 2);

  // The poster peeks from the bottom with the seed row, under the preview.
  const peek = await settledBox(button(page, 'Shuffle'));
  expect(peek.y).toBeGreaterThan(VIEWPORT.height / 2);

  // Pick opens the picker, and the color follows a finger on the hue while it is still down.
  await tap(cdp, middle(await settledBox(button(page, 'Pick'))));
  const hue = page.locator(A11Y).getByText(/^Hue, slider/).first();
  const track = await settledBox(hue);
  const before = await pickedHex(page);
  const from = middle(track);
  await touch(cdp, 'touchStart', from);
  await touch(cdp, 'touchMove', { x: from.x + track.width / 4, y: from.y });
  await expect.poll(() => pickedHex(page)).not.toBe(before);
  await touch(cdp, 'touchEnd');
  await tap(cdp, middle(await settledBox(button(page, 'Done'))));
  await expect(button(page, 'Done')).toHaveCount(0);
});

test('at 390 wide with a finger, a drag on the edge of the poster raises it to half height', async ({ page, browserName }) => {
  test.skip(browserName !== 'chromium', 'The finger goes through a Chromium CDP session');
  await openBuilder(page);
  const cdp = await page.context().newCDPSession(page);

  const edge = { x: VIEWPORT.width / 2, y: (await settledBox(button(page, 'Shuffle'), 30_000)).y - 16 };
  await drag(cdp, edge, { x: edge.x, y: VIEWPORT.height / 2 });

  await expect(button(page, 'Seed and theme controls, Half')).toBeAttached();
});

test('at 390 wide with a finger, a drag on the half height poster reaches the Fine-tune button', async ({
  page,
  browserName,
}) => {
  test.skip(browserName !== 'chromium', 'The finger goes through a Chromium CDP session');
  await openBuilder(page);
  const cdp = await page.context().newCDPSession(page);
  const edge = { x: VIEWPORT.width / 2, y: (await settledBox(button(page, 'Shuffle'), 30_000)).y - 16 };
  await drag(cdp, edge, { x: edge.x, y: VIEWPORT.height / 2 });
  await expect(button(page, 'Seed and theme controls, Half')).toBeAttached();

  // A drag on the content raises the sheet to full before the content scrolls. The browser takes
  // over a drag the page reports as scrolled nowhere, so the sheet reports its own rise as scrolled.
  const inside = { x: VIEWPORT.width / 2, y: VIEWPORT.height - 80 };
  await drag(cdp, inside, { x: inside.x, y: VIEWPORT.height / 3 });
  await expect(button(page, 'Seed and theme controls, Full')).toBeAttached();

  // The next drag scrolls the content up to the Fine-tune button.
  const fineTune = page.locator(A11Y).getByRole('button', { name: /^Fine-tune, / }).first();
  await drag(cdp, inside, { x: inside.x, y: VIEWPORT.height / 3 });
  await expect.poll(() => inView(fineTune)).toBe(true);
});

test('at 390 wide with a finger, Export opens over the whole screen', async ({ page, browserName }) => {
  test.skip(browserName !== 'chromium', 'The finger goes through a Chromium CDP session');
  await openBuilder(page);
  const cdp = await page.context().newCDPSession(page);

  await tap(cdp, middle(await settledBox(button(page, 'Export code'), 30_000)));

  // The mirror folds the dialog role into the sheet's own text, so its box is the sheet's.
  const sheet = page.locator(A11Y).getByText(/^Export code, dialog/).first();
  const panel = await settledBox(sheet);
  expect(panel.x).toBeLessThanOrEqual(1);
  expect(panel.width).toBeGreaterThanOrEqual(VIEWPORT.width - 1);
  await expect(button(page, 'Copy all')).toBeAttached();
});

/** The mirror's button called [name], the whole name. */
function button(page: Page, name: string): Locator {
  return page.locator(A11Y).getByRole('button', { name, exact: true }).first();
}

/** What the open picker's color field shows. The poster leaves the mirror while the picker is open. */
async function pickedHex(page: Page): Promise<string> {
  const field = page.locator(A11Y).getByRole('textbox', { name: 'Color', exact: true }).first();
  return (await field.textContent()) ?? '';
}

/** Whether [target] sits wholly inside the viewport's height. */
async function inView(target: Locator): Promise<boolean> {
  const box = await target.boundingBox();
  return box !== null && box.y > 0 && box.y + box.height <= VIEWPORT.height;
}

function middle(box: Box): Point {
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 };
}

/** One touch event, the finger at [at], or lifted when there is no point. */
async function touch(cdp: CDPSession, type: 'touchStart' | 'touchMove' | 'touchEnd', at?: Point): Promise<void> {
  await cdp.send('Input.dispatchTouchEvent', { type, touchPoints: at ? [at] : [] });
  await holdBetweenTouches();
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
