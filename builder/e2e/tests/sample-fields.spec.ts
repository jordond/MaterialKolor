import type { CDPSession, Locator, Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { nextFrames } from '../fixtures/workspace';
import { boxOf, longPressAt, mirrorButton, settledBox, settledMirror, tap, type Point } from './touch';

// The preview's own sample fields keep the mirror through a long press too, here the Trips note. The
// field takes a word before the long press, since a long press on an empty field selects nothing, and
// another after it, which only replaces the first if the long press selected it, and only reaches the
// mirror while the mirror is alive.
// Chromium only, since Playwright drives the long press through a CDP session.

/** The preview's text fields. Their labels are their text in the mirror, so they have no `aria-label`. */
const SAMPLE_FIELDS = '#cmp_a11y_root [contenteditable]:not([aria-label])';

/** The word each sample field gets before the long press. */
const SAMPLE_WORD = 'Lisbon';

/** The word typed over it after the long press. */
const SAMPLE_REPLACEMENT = 'Porto';

/**
 * The text input Compose keeps in its shadow root while a text field has focus, laid over that field.
 * It takes the touches meant for the field under it.
 */
const BACKING_FIELD = '.compose-backing-field';

test.use({ hasTouch: true });

test.beforeEach(async ({ browserName }) => {
  test.skip(browserName !== 'chromium', 'The long press goes through a Chromium CDP session');
});

test('a long press on the Trips note keeps the mirror', async ({ page, context }) => {
  // Tall enough that the note sits clear of the preview's bar at the foot of the pane.
  await page.setViewportSize({ width: 1280, height: 1400 });
  await openBuilder(page);
  const cdp = await context.newCDPSession(page);
  const note = await typeInTripsNote(page);
  await longPressKeepsMirror(page, cdp, note, 'the Trips note');
});

/**
 * Long presses the word in [field] and checks the mirror kept the page through it, then that the
 * word was selected and the mirror hears what takes its place.
 */
async function longPressKeepsMirror(page: Page, cdp: CDPSession, field: Locator, what: string): Promise<void> {
  // Focus leaves the text fields first, since the backing input would take the touch.
  await leaveFields(page);
  // The first touch after the mouse goes unheard in Chromium through CDP, so a tap goes first.
  await tap(cdp, await wordIn(field));
  await nextFrames(page, 3);
  // Under load the tap is sometimes heard after all, and the field it focused has to be left again.
  await leaveFields(page);
  const word = await wordIn(field);
  const before = await settledMirror(page);

  await longPressAt(page, cdp, word);
  const after = await settledMirror(page);
  console.log(`Mirror nodes on ${what}: before ${before}, after the long press ${after}`);
  expect(after).toBeGreaterThan(before * 0.8);

  await page.keyboard.type(SAMPLE_REPLACEMENT);
  await expect(field).toHaveText(SAMPLE_REPLACEMENT);
}

/** Tabs until no text field holds focus, so no backing input lies over a field. */
async function leaveFields(page: Page): Promise<void> {
  for (let tab = 0; tab < 3 && (await page.locator(BACKING_FIELD).count()) > 0; tab++) {
    await page.keyboard.press('Tab');
    await nextFrames(page, 3);
  }
  await expect(page.locator(BACKING_FIELD)).toHaveCount(0);
}

/**
 * A point on the word in [field], read once the field holds still, since a Tab can scroll the pane
 * to the next stop.
 */
async function wordIn(field: Locator): Promise<Point> {
  const box = await settledBox(field);
  // The word starts 16 px in, and its line sits 36 px down, under a filled field's label and under
  // the half line an outlined field's label takes above its outline.
  return { x: box.x + 36, y: box.y + 36 };
}

/**
 * Shows the preview in light only, one copy of the screen, the one the mirror reads. Split, the dark
 * copy past the handle takes the touches on its side.
 */
async function showOneCopy(page: Page): Promise<void> {
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  const light = await boxOf(mirrorButton(page, /^Light/));
  await page.mouse.click(light.x + light.width / 2, light.y + light.height / 2);
  await expect(mirrorButton(page, 'Light, radio, selected')).toBeAttached();
}

/** Opens the App tab, scrolls the open trip to its note, clicks into the note and types the word there. */
async function typeInTripsNote(page: Page): Promise<Locator> {
  await showOneCopy(page);
  const tab = await boxOf(page.locator('#cmp_a11y_root [aria-label^="App, tab"]'));
  await page.mouse.click(tab.x + tab.width / 2, tab.y + tab.height / 2);
  // The note closes the open trip's pane, so scrolling that pane well past it only stops at its end.
  const checkIn = page.locator('#cmp_a11y_root [role="button"]').filter({ hasText: /^Check in$/ });
  await expect(checkIn).toBeAttached();
  const pane = await settledBox(checkIn);
  await page.mouse.move(pane.x + pane.width / 2, pane.y + pane.height / 2);
  for (const _ of [1, 2, 3]) {
    await page.mouse.wheel(0, 400);
    await nextFrames(page);
  }
  // The note is the only text field in the app.
  return typeInto(page, page.locator(SAMPLE_FIELDS).first());
}

/** Waits for [field] to be laid out, clicks into it with the mouse and types the word. */
async function typeInto(page: Page, field: Locator): Promise<Locator> {
  // A scroll that is still running moves the field after its box is read.
  const box = await settledBox(field);
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.keyboard.type(SAMPLE_WORD);
  await expect(field).toHaveText(SAMPLE_WORD);
  return field;
}
