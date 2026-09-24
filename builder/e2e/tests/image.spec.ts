import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';
import { dispatchDrag, dispatchPaste, installFileMakers } from './image-files';

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
