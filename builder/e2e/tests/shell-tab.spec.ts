import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-217c
// Tab inside the canvas (R-B-210). Spike S11 saw WebKit bounce Tab between the canvas's first two
// stops. Compose focus never reaches the page (D40, P5), so the specs type after each move and read
// through the accessibility mirror where the text went. Keys go in one at a time with a pause, so
// each lands after the focus move before it.

/** The gallery's text fields. Their labels are their text in the mirror, so they have no `aria-label`. */
const FIELDS = '#cmp_a11y_root [contenteditable]:not([aria-label])';

/** Long enough for a key to reach Compose and the focus move it makes to settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('Tab twice from a gallery text field moves past it and does not bounce back', async ({ page }) => {
  const start = await typeInFirstField(page);

  await press(page, 'Tab');
  await press(page, 'Tab');
  await typeSettled(page, 'zq');
  await press(page, 'Tab');
  await typeSettled(page, 'w');

  await expect(start).not.toContainText('zq');
  await expect(start).not.toContainText('w');
});

test('one Tab leaves a gallery text field', async ({ page }) => {
  // Both browsers keep focus in the field for the first Tab and move on at the second, and WebKit also
  // puts the caret back at the start, so focus leaves and comes straight back there. The field and the
  // page's focus repair are app and web code, outside this slice (B-217c).
  test.fixme(true, 'The first Tab out of a canvas text field stays in it on Chromium and WebKit');
  const start = await typeInFirstField(page);

  await press(page, 'Tab');
  await typeSettled(page, 'y');

  await expect(start).toHaveText('start');
});

/** Opens the gallery, clicks into its first text field and types `start` there. */
async function typeInFirstField(page: Page): Promise<Locator> {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  const tab = (await page.locator('#cmp_a11y_root [aria-label^="Components, tab"]').boundingBox())!;
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  const start = page.locator(FIELDS).first();
  // The Inputs cards sit 600 px down the gallery. A fixed scroll lands there however slowly the
  // mirror follows, where scrolling until the field shows could carry it past on a busy machine.
  await page.mouse.move(tab.x + tab.width / 2, tab.y + 240);
  for (const _ of [1, 2]) {
    await page.mouse.wheel(0, 300);
    await page.waitForTimeout(SETTLE_MS);
  }
  await expect.poll(async () => (await start.boundingBox())?.height ?? 0, { timeout: 15_000 }).toBeGreaterThan(0);
  const box = (await start.boundingBox())!;
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await typeSettled(page, 'start');
  await expect(start).toHaveText('start');
  return start;
}

async function press(page: Page, key: string): Promise<void> {
  await page.keyboard.press(key);
  await page.waitForTimeout(SETTLE_MS);
}

async function typeSettled(page: Page, text: string): Promise<void> {
  await page.keyboard.type(text);
  await page.waitForTimeout(SETTLE_MS);
}
