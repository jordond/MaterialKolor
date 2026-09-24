import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-217c
// Tab inside the canvas (R-B-210). Spike S11 saw WebKit bounce Tab between the canvas's first two
// stops. Compose focus never reaches the page (D40, P5), so the specs type after each move and read
// through the accessibility mirror where the text went. Keys go in one at a time with a pause, so
// each lands after the focus move before it.

/** The gallery's text fields. Their labels are their text in the mirror, so they have no `aria-label`. */
const FIELDS = '#cmp_a11y_root [contenteditable]:not([aria-label])';

// b-227
// The Filled and Outlined cards each hold a "Destination" field and a disabled "Origin" one, in
// that order in `FIELDS`. Both "Destination" fields show the gallery's one text.
const FILLED = 0;
const OUTLINED = 2;

/**
 * The text input Compose keeps in its shadow root while a text field has focus, laid over that field.
 * It goes away when focus leaves every text field.
 */
const BACKING_FIELD = '.compose-backing-field';

/** Long enough for a key to reach Compose and the focus move it makes to settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('Tab twice from a gallery text field moves past it and does not bounce back', async ({ page }) => {
  const start = await typeInField(page, FILLED);

  // The first Tab reaches the Outlined field and the second the Slider card.
  await press(page, 'Tab');
  await press(page, 'Tab');
  await typeSettled(page, 'zq');
  await press(page, 'Tab');
  await typeSettled(page, 'w');

  await expect(start).not.toContainText('zq');
  await expect(start).not.toContainText('w');
});

test('one Tab leaves a gallery text field', async ({ page }) => {
  // The Outlined field has no twin after it, so one Tab goes on to the Slider card. Compose drops its
  // backing input once no text field has focus, and a key typed then lands in neither field.
  const start = await typeInField(page, OUTLINED);

  await press(page, 'Tab');
  await expect(page.locator(BACKING_FIELD)).toHaveCount(0);
  await typeSettled(page, 'y');

  await expect(start).toHaveText('start');
  await expect(page.locator(FIELDS).nth(FILLED)).toHaveText('start');
});

test('one Tab from the Filled field lands in the Outlined one', async ({ page }) => {
  // This is what looked like the first Tab staying put (B-217c). Focus does move, but into the
  // Outlined "Destination" field, which shows the same text. So a key typed next shows up in both,
  // at the end in Chromium, which moves the caret there when the text is set, and at the start in
  // WebKit, where a field that never had focus keeps its caret at 0.
  await typeInField(page, FILLED);
  const outlined = (await page.locator(FIELDS).nth(OUTLINED).boundingBox())!;

  await press(page, 'Tab');

  await expect
    .poll(async () => {
      const boxes = await page.locator(BACKING_FIELD).evaluateAll((fields) =>
        fields.map((field) => {
          const { x, y, width, height } = field.getBoundingClientRect();
          return { x, y, width, height };
        }),
      );
      return boxes.length === 1 && holdsCentreOf(outlined, boxes[0]);
    })
    .toBe(true);
});

// b-227
// The Trips note is the one multi-line field in the sample apps. On its own it would type Tab as a
// character, so the preview moves focus on Tab and Shift+Tab instead.
for (const key of ['Tab', 'Shift+Tab']) {
  test(`${key} leaves the Trips note without typing into it`, async ({ page }) => {
    const note = await typeInTripsNote(page);

    await press(page, key);
    await expect(page.locator(BACKING_FIELD)).toHaveCount(0);
    await typeSettled(page, 'x');

    await expect(note).toHaveText('note');
  });
}

/** Opens the gallery, clicks into its text field [nth] of `FIELDS` and types `start` there. */
async function typeInField(page: Page, nth: number): Promise<Locator> {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  const tab = (await page.locator('#cmp_a11y_root [aria-label^="Components, tab"]').boundingBox())!;
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  const start = page.locator(FIELDS).nth(nth);
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

/** Opens the App tab, scrolls the open trip to its note, clicks into the note and types `note` there. */
async function typeInTripsNote(page: Page): Promise<Locator> {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  const tab = (await page.locator('#cmp_a11y_root [aria-label^="App, tab"]').boundingBox())!;
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  // The note closes the open trip's pane, so scrolling that pane well past it only stops at its end.
  const checkIn = page.locator('#cmp_a11y_root [role="button"]').filter({ hasText: /^Check in$/ });
  const pane = (await checkIn.boundingBox())!;
  await page.mouse.move(pane.x + pane.width / 2, pane.y + pane.height / 2);
  for (const _ of [1, 2, 3]) {
    await page.mouse.wheel(0, 400);
    await page.waitForTimeout(SETTLE_MS);
  }
  // The note is the only text field in the app, and like the gallery's its label is its text.
  const note = page.locator(FIELDS).first();
  await expect.poll(async () => (await note.boundingBox())?.height ?? 0, { timeout: 15_000 }).toBeGreaterThan(0);
  const box = (await note.boundingBox())!;
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await typeSettled(page, 'note');
  await expect(note).toHaveText('note');
  return note;
}

/** Whether the centre of [inner] lies inside [outer]. */
function holdsCentreOf(
  outer: { x: number; y: number; width: number; height: number },
  inner: { x: number; y: number; width: number; height: number },
): boolean {
  const x = inner.x + inner.width / 2;
  const y = inner.y + inner.height / 2;
  return x >= outer.x && x <= outer.x + outer.width && y >= outer.y && y <= outer.y + outer.height;
}

async function press(page: Page, key: string): Promise<void> {
  await page.keyboard.press(key);
  await page.waitForTimeout(SETTLE_MS);
}

async function typeSettled(page: Page, text: string): Promise<void> {
  await page.keyboard.type(text);
  await page.waitForTimeout(SETTLE_MS);
}
