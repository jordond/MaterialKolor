import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import { dispatchDrag, dispatchPaste, installFileMakers } from './image-files';
import path from 'node:path';
import { button, press, pressFor, scrollTo, seedText, storedDocument } from '../fixtures/workspace';

// b-311
// Seeding from an image on the real builder (F-08). A drop or a paste goes through the browser's
// decode and the extractor, lands as the seed with an undo toast, and the poster offers the other
// candidates as chips. A file that is not an image only says so. The page reads through the
// accessibility mirror, where the chip group's name goes in as text.

/** The chip group's name, `image_candidates`. */
const CANDIDATES = 'Colors from the image';

/** The toast once the quadrants set the seed, `image_seeded_named`. */
const SEEDED = 'Seed taken from quadrants.png';

/** The toast for a file that is not an image, `image_unsupported`. */
const UNSUPPORTED = 'couldn’t be read as an image';

/** Decode, extraction and the reveal, with room for a busy machine. */
const SEED_TIMEOUT_MS = 15_000;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
  await context.addInitScript(installFileMakers);
});

test('a dropped PNG seeds the theme and offers its colors as chips', async ({ page }) => {
  await openReady(page);
  await expect(onPage(page, CANDIDATES)).toHaveCount(0);

  await dispatchDrag(page, 'dragenter', []);
  expect(await dispatchDrag(page, 'drop', ['quadrants.png'])).toBe(true);

  await expect(onPage(page, SEEDED)).toHaveCount(1, { timeout: SEED_TIMEOUT_MS });
  await expect(onPage(page, CANDIDATES)).toHaveCount(1);
  await expect(onPage(page, 'From quadrants.png')).not.toHaveCount(0);
});

test('a dropped file that is not an image says so and leaves the theme alone', async ({ page }) => {
  await openReady(page);

  await dispatchDrag(page, 'drop', ['notes.txt']);

  await expect(onPage(page, UNSUPPORTED)).toHaveCount(1, { timeout: SEED_TIMEOUT_MS });
  await expect(onPage(page, CANDIDATES)).toHaveCount(0);
  await expect(onPage(page, 'From notes.txt')).toHaveCount(0);
});

test('a pasted image seeds the theme', async ({ page }) => {
  await openReady(page);

  expect(await dispatchPaste(page, ['quadrants.png'])).toBe(true);

  await expect(onPage(page, SEEDED)).toHaveCount(1, { timeout: SEED_TIMEOUT_MS });
  await expect(onPage(page, CANDIDATES)).toHaveCount(1);
});

// b-503
// Image to theme (flow 5.2) with the 12 MP fixture photo. The drag says what a drop does, the drop
// seeds the theme, another chip swaps the seed, and Match exactly in the explainer pins primary.

/** The fixture photo, served to the page from this address. */
const PHOTO_ROUTE = '/__e2e/photo-12mp.jpg';

test('a dropped photo seeds the theme, another chip swaps it, and Match exactly pins primary', async ({ page }) => {
  await page.route(`**${PHOTO_ROUTE}`, (route) =>
    route.fulfill({ path: path.resolve(__dirname, '../fixtures/photo-12mp.jpg'), contentType: 'image/jpeg' }),
  );
  await openReady(page);

  expect(await dropPhoto(page, 'dragenter')).toBe(true);
  await expect(onPage(page, 'Drop to pull colors from this image')).not.toHaveCount(0, { timeout: SEED_TIMEOUT_MS });
  expect(await dropPhoto(page, 'drop')).toBe(true);
  await expect(onPage(page, 'Seed taken from photo-12mp.jpg')).toHaveCount(1, { timeout: SEED_TIMEOUT_MS });

  const chips = page.locator('#cmp_a11y_root').getByRole('button', { name: /^#[0-9A-F]{6}, .*not selected$/ });
  await expect.poll(() => chips.count(), { timeout: SEED_TIMEOUT_MS }).toBeGreaterThan(0);
  const chosen = ((await chips.first().getAttribute('aria-label')) ?? '').slice(0, 7);
  await press(page, chips.first());
  await expect.poll(() => seedText(page), { timeout: SEED_TIMEOUT_MS }).toBe(chosen);

  const why = button(page, 'Why?');
  await scrollTo(page, why, onPage(page, 'Seed and theme controls'));
  await pressFor(page, why, onPage(page, 'Why primary differs from your seed'));
  await press(page, button(page, 'Match exactly'));
  await expect
    .poll(async () => Object.keys(((await storedDocument(page))?.pins ?? {}) as object).length, { timeout: SEED_TIMEOUT_MS })
    .toBeGreaterThan(0);
});

/** Fires [type] on the page with the fixture photo, fetched through [PHOTO_ROUTE]. True when the page took it. */
async function dropPhoto(page: Page, type: string): Promise<boolean> {
  return page.evaluate(
    async ({ type, route }) => {
      const blob = await (await fetch(route)).blob();
      const file = new File([blob], 'photo-12mp.jpg', { type: 'image/jpeg' });
      const event = new Event(type, { bubbles: true, cancelable: true });
      Object.defineProperty(event, 'dataTransfer', { value: (window as any).__makeTransfer([file]) });
      document.body.dispatchEvent(event);
      return event.defaultPrevented;
    },
    { type, route: PHOTO_ROUTE },
  );
}

/** Open the builder and wait until the accessibility mirror has the workspace in it. */
async function openReady(page: Page): Promise<void> {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });
  await expect(onPage(page, 'Shuffle')).not.toHaveCount(0, { timeout: 30_000 });
}

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string): Locator {
  return page.locator('#cmp_a11y_root').getByText(text);
}
