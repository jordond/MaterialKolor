import type { Locator, Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { boxOf, nextFrames, settledBox } from '../fixtures/workspace';

// Tab inside the canvas. Compose focus never reaches the page, so the specs type after each move and
// read through the accessibility mirror where the text went. Each key waits for the page to draw
// after it, so it lands after the focus move before it.

/** The gallery's text fields. Their labels are their text in the mirror, so they have no `aria-label`. */
const FIELDS = '#cmp_a11y_root [contenteditable]:not([aria-label])';

// The Filled and Outlined cards each hold a "Destination" field and a disabled "Origin" one, in
// that order in `FIELDS`. Both "Destination" fields show the gallery's one text.
const FILLED = 0;
const OUTLINED = 2;

/**
 * The text input Compose keeps in its shadow root while a text field has focus, laid over that field.
 * It goes away when focus leaves every text field.
 */
const BACKING_FIELD = '.compose-backing-field';

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
  const tab = await boxOf(page.locator('#cmp_a11y_root [aria-label^="Components, tab"]'));
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  await expect(page.locator('#cmp_a11y_root [aria-label^="Components, tab, selected"]')).toBeAttached();
  const start = page.locator(FIELDS).nth(nth);
  // The Inputs cards sit 600 px down the gallery. A fixed scroll lands there however slowly the
  // mirror follows, where scrolling until the field shows could carry it past on a busy machine.
  await page.mouse.move(tab.x + tab.width / 2, tab.y + 240);
  for (const _ of [1, 2]) {
    await page.mouse.wheel(0, 300);
    await nextFrames(page);
  }
  // The scroll runs on for a few frames, and a click read off a moving box lands beside the field.
  const box = await settledBox(start);
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await typeSettled(page, 'start');
  await expect(start).toHaveText('start');
  return start;
}

/** Opens the App tab, scrolls the open trip to its note, clicks into the note and types `note` there. */
async function typeInTripsNote(page: Page): Promise<Locator> {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  const tab = await boxOf(page.locator('#cmp_a11y_root [aria-label^="App, tab"]'));
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  // The note closes the open trip's pane, so scrolling that pane well past it only stops at its end.
  const checkIn = page.locator('#cmp_a11y_root [role="button"]').filter({ hasText: /^Check in$/ });
  const pane = await settledBox(checkIn);
  await page.mouse.move(pane.x + pane.width / 2, pane.y + pane.height / 2);
  for (const _ of [1, 2, 3]) {
    await page.mouse.wheel(0, 400);
    await nextFrames(page);
  }
  // The note is the only text field in the app, and like the gallery's its label is its text.
  const note = page.locator(FIELDS).first();
  const box = await settledBox(note);
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await typeSettled(page, 'note');
  await expect(note).toHaveText('note');
  return note;
}

/** Presses [key], then waits for the page to draw the focus move it makes. */
async function press(page: Page, key: string): Promise<void> {
  await page.keyboard.press(key);
  await nextFrames(page, 3);
}

/** Types [text], then waits for the page to draw what it did with it. */
async function typeSettled(page: Page, text: string): Promise<void> {
  await page.keyboard.type(text);
  await nextFrames(page, 3);
}
