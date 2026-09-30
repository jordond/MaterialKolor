import type { Locator, Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { A11Y, boxOf, button, nextFrames, onPage, press, settledBox, type Box } from '../fixtures/workspace';

// The accessibility smoke, role and name for the top bar, the poster's sections, the dock, the
// canvas tabs, the export sheet and the split handle, plus one dialog that holds focus.
// Everything is read through Compose's accessibility mirror. The mirror writes every clickable as a
// button, so a control's role and state go in its name, "App, tab, selected". A group label or
// a slider has no role there, so its name is a text node. The mirror has no slider role at all, so
// the split handle's role is left to the JVM semantics tests and only its name and value are read here.
// The top bar also gives its end-edge actions their full size first, so More options stays reachable
// at 1280 wide, where the library switcher used to squeeze it to nothing.

/** The text input Compose lays over the text field that has focus, gone when none has. */
const BACKING_FIELD = '.compose-backing-field';

/** More Tab presses than the export sheet has stops, so a lap brings focus back to where it began. */
const TAB_PRESSES = 40;

test.use({ viewport: { width: 1280, height: 800 } });

test('the top bar names every action', async ({ page }) => {
  await openReady(page);

  for (const name of [
    /^Projects, .+/,
    /^Library, pop-up button, M3$/,
    'Command palette',
    /^Undo(, disabled)?$/,
    /^Redo(, disabled)?$/,
    /^History$/,
    'Share',
    'Export code',
    'More options',
  ]) {
    await expect(button(page, name), String(name)).toHaveCount(1);
  }
});

test('at 1280 wide, a click on More options opens Help and About', async ({ page }) => {
  await openReady(page);
  await expect(item(page, 'Help')).toHaveCount(0);

  // The top bar holds the one More options in the mirror, as the test above says.
  expect((await boxOf(button(page, 'More options'))).width).toBeGreaterThan(0);
  await press(page, button(page, 'More options'));

  await expect(item(page, 'Use the system appearance').first()).toBeAttached();
  await expect(item(page, 'Help').first()).toBeAttached();
  await expect(item(page, 'About').first()).toBeAttached();
});

test('the poster names its sections and their controls', async ({ page }) => {
  await openReady(page);

  await expect(label(page, 'Seed and theme controls')).toHaveCount(1);
  // Seed
  await expect(field(page, /^Seed color/)).toHaveCount(1);
  await expect(button(page, /^What is the seed\?, (collapsed|expanded)$/)).toHaveCount(1);
  for (const name of ['Copy hex', 'Copy Kotlin', 'Shuffle', 'Pick', 'Image']) {
    await expect(button(page, name), name).toHaveCount(1);
  }
  // Style
  await expect(label(page, 'Palette style')).toHaveCount(1);
  await expect(button(page, /^TonalSpot, .+, radio, selected$/)).toHaveCount(1);
  await expect(button(page, /, radio, not selected$/).first()).toBeAttached();
  await expect(button(page, /^Keep the style when shuffling, (checked|not checked)$/)).toHaveCount(1);
  // Contrast, one choice of the four named levels
  await expect(label(page, 'Contrast level')).toHaveCount(1);
  await expect(button(page, 'Standard, radio, selected')).toHaveCount(1);
  for (const level of ['Reduced', 'Medium', 'High']) {
    await expect(button(page, `${level}, radio, not selected`), level).toHaveCount(1);
  }
  // Fine-tune, one button that sums up the sheet it opens
  await expect(button(page, /^Fine-tune, .+/)).toHaveCount(1);
});

test('the dock names the preview mode, the device, Inspect, color vision and Fullscreen', async ({ page }) => {
  await openReady(page);

  await expect(label(page, 'Preview mode')).toHaveCount(1);
  await expect(button(page, 'Light, radio, not selected')).toHaveCount(1);
  await expect(button(page, 'Split, radio, selected')).toHaveCount(1);
  await expect(button(page, 'Dark, radio, not selected')).toHaveCount(1);
  await expect(button(page, /^Device width, .+/)).toHaveCount(1);
  await expect(button(page, /^Inspect, (checked|not checked)$/)).toHaveCount(1);
  await expect(button(page, /^Color vision, .+/)).toHaveCount(1);
  await expect(button(page, 'Fullscreen')).toHaveCount(1);
});

test('the canvas tabs say which one is selected, and follow a click', async ({ page }) => {
  await openReady(page);
  const others = ['Components', 'Roles', 'Palettes', 'Contrast'];

  await expect(button(page, 'App, tab, selected')).toHaveCount(1);
  for (const name of others) await expect(button(page, `${name}, tab, not selected`), name).toHaveCount(1);

  await press(page, button(page, 'Components, tab, not selected'));

  await expect(button(page, 'Components, tab, selected')).toHaveCount(1, { timeout: 10_000 });
  await expect(button(page, 'App, tab, not selected')).toHaveCount(1);
});

test('the export sheet is a named dialog with its buttons, and the page behind leaves the mirror', async ({
  page,
}) => {
  await openExport(page);

  await expect(label(page, 'Export code, dialog')).toHaveCount(1);
  for (const name of ['Close', 'Copy file', 'Copy all', 'Download zip']) {
    await expect(button(page, name), name).toHaveCount(1);
  }
  await expect(button(page, /^Color\.kt, tab, selected$/)).toHaveCount(1);
  await expect(field(page, 'Package name')).toHaveCount(1);
  await expect(field(page, 'Theme name')).toHaveCount(1);
  await expect(field(page, /^Seed color/)).toHaveCount(0);

  await page.keyboard.press('Escape');

  await expect(label(page, 'Export code, dialog')).toHaveCount(0, { timeout: 10_000 });
  await expect(field(page, /^Seed color/)).toHaveCount(1, { timeout: 10_000 });
});

test('Tab stays inside the export sheet and comes back around to its first field', async ({ page }) => {
  await openExport(page);
  const fields = [field(page, 'Package name'), field(page, 'Theme name')];
  const boxes = await Promise.all(fields.map((field) => settledBox(field)));

  // Compose focus never reaches the page. A text field that has focus is the one thing
  // that shows, as the backing input laid over it, so each stop is read by where that input sits.
  const stops: string[] = [];
  for (let press = 0; press < TAB_PRESSES; press++) {
    await page.keyboard.press('Tab');
    // Compose moves focus on the key and brings in the backing field on a frame after it.
    await nextFrames(page, 3);
    stops.push(await focusedField(page, boxes));
  }

  expect(stops.filter((stop) => stop === 'elsewhere')).toEqual([]);
  expect(stops.filter((stop) => stop === 'package').length).toBeGreaterThanOrEqual(2);
  await expect(label(page, 'Export code, dialog')).toHaveCount(1);
});

test('the split handle says how much light shows, follows a drag, and the hidden copy is silent', async ({
  page,
}) => {
  await openReady(page);
  const handle = text(page, /^Split, \d+% Light$/);

  await expect(text(page, 'Split, 50% Light')).toHaveCount(1);
  // The two copies of a split draw the same app, and only one is in the mirror.
  await expect(button(page, /^Notifications, /)).toHaveCount(1);

  const box = await boxOf(handle);
  const y = box.y + box.height / 2;
  await page.mouse.move(box.x + box.width / 2, y);
  await page.mouse.down();
  for (let step = 1; step <= 10; step++) await page.mouse.move(box.x + box.width / 2 + step * 15, y);
  await page.mouse.up();

  await expect(handle).toHaveCount(1);
  await expect(text(page, 'Split, 50% Light')).toHaveCount(0, { timeout: 10_000 });
});

/** Open the builder and wait until the mirror holds the workspace. */
async function openReady(page: Page): Promise<void> {
  await openBuilder(page);
  await expect(field(page, /^Seed color/)).toHaveCount(1, { timeout: 30_000 });
  await expect(text(page, /^Split, \d+% Light$/)).toHaveCount(1, { timeout: 30_000 });
}

/** Open the export sheet from the top bar and wait until its fields are laid out. */
async function openExport(page: Page): Promise<void> {
  await openReady(page);
  await press(page, button(page, 'Export code'));
  await expect(label(page, 'Export code, dialog')).toHaveCount(1, { timeout: 10_000 });
  await boxOf(field(page, 'Package name'));
}

/** Whatever in the mirror reads exactly [text], as its text or its label. */
function item(page: Page, text: string): Locator {
  const root = page.locator(A11Y);
  return root.getByText(text, { exact: true }).or(root.getByLabel(text, { exact: true }));
}

/** Which of the sheet's fields the backing input sits over after a Tab, none, or one elsewhere. */
async function focusedField(page: Page, [pkg, theme]: Box[]): Promise<string> {
  const inputs = await page.locator(BACKING_FIELD).evaluateAll((all) =>
    all.map((input) => {
      const { x, y, width, height } = input.getBoundingClientRect();
      return { x: x + width / 2, y: y + height / 2 };
    }),
  );
  if (inputs.length === 0) return 'none';
  if (inputs.length > 1) return 'elsewhere';
  const [point] = inputs;
  if (holds(pkg, point)) return 'package';
  if (holds(theme, point)) return 'theme';
  return 'elsewhere';
}

function holds(box: Box, point: { x: number; y: number }): boolean {
  return point.x >= box.x && point.x <= box.x + box.width && point.y >= box.y && point.y <= box.y + box.height;
}

function field(page: Page, name: string | RegExp): Locator {
  return page.locator(A11Y).getByRole('textbox', { name, exact: true });
}

function text(page: Page, value: string | RegExp): Locator {
  return onPage(page, value);
}

/**
 * The group or pane named [name]. The mirror writes the name as the first text of the element that
 * holds the group's controls, so its whole text only starts with it.
 */
function label(page: Page, name: string): Locator {
  return page.locator(A11Y).getByText(new RegExp(`^${name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`));
}
