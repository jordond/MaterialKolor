import { expect, test, type Page } from '@playwright/test';
import { site } from './builder';

// The seed bloom on the boot splash. Five swatches, tints of the seed through the seed to shades of
// it, rise in one after the other while the app loads. Holding the glue keeps the page on the splash.

/** The glue, the script boot.js adds last. */
const GLUE = /\/assets\/builder\.[0-9a-f]{16}\.js$/;

/** The start of a share code for seed #1A73E8. The splash only reads the seed, bytes 1 to 3. */
const BLUE_LINK = '/t/ARpz6A';

test.beforeEach(async ({ page }) => {
  // Never answered, so the app never starts and the splash stays up.
  await page.route(GLUE, () => {});
});

test('the five swatches run from a tint of the seed through the seed to a shade of it', async ({ page }) => {
  await page.goto(site(BLUE_LINK), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-swatches span')).toHaveCount(5);

  const swatches = await swatchColors(page);
  expect(swatches[2]).toEqual([0x1a, 0x73, 0xe8]);
  const lightness = swatches.map(([red, green, blue]) => red + green + blue);
  for (let i = 1; i < lightness.length; i++) expect(lightness[i], `swatch ${i + 1}`).toBeLessThan(lightness[i - 1]);
  // Every tint and shade keeps the seed's blue as its strongest channel.
  for (const [red, green, blue] of swatches) expect(blue).toBeGreaterThan(Math.max(red, green));
});

test('the swatches bloom by default', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  expect(
    await page.evaluate(() =>
      Array.from(document.querySelectorAll('.mk-swatches span'), (swatch) =>
        swatch.getAnimations().map((animation) => animation.playState),
      ),
    ),
  ).toEqual([['running'], ['running'], ['running'], ['running'], ['running']]);
});

test('nothing moves under reduced motion or with motion frozen', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-swatches span').first()).toBeVisible();
  expect(await page.evaluate(() => document.getAnimations().length)).toBe(0);

  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.goto(site('/?motion=frozen'), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-swatches span').first()).toBeVisible();
  expect(await page.evaluate(() => document.getAnimations().length)).toBe(0);
});

/** Each swatch's color as sRGB channels, drawn through a 2D canvas since the tints compute to oklch. */
async function swatchColors(page: Page): Promise<number[][]> {
  return page.evaluate(() => {
    const context = document.createElement('canvas').getContext('2d', { willReadFrequently: true })!;
    return Array.from(document.querySelectorAll('.mk-swatches span'), (swatch) => {
      context.clearRect(0, 0, 1, 1);
      context.fillStyle = getComputedStyle(swatch).backgroundColor;
      context.fillRect(0, 0, 1, 1);
      return Array.from(context.getImageData(0, 0, 1, 1).data.slice(0, 3));
    });
  });
}
