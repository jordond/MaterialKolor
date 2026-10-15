import { expect, test, type Page } from '@playwright/test';
import { site } from './builder';

// The fan on the boot splash. Nine chips, tints of the seed through the seed to shades of it, fan
// open on a rivet, then a lift walks across them while the app loads. Holding the glue keeps the
// page on the splash.

/** The glue, the script boot.js adds last. */
const GLUE = /\/assets\/builder\.[0-9a-f]{16}\.js$/;

/** The start of a share code for seed #1A73E8. The splash only reads the seed, bytes 1 to 3. */
const BLUE_LINK = '/t/ARpz6A';

test.beforeEach(async ({ page }) => {
  // Never answered, so the app never starts and the splash stays up.
  await page.route(GLUE, () => {});
});

test('the nine chips run from a tint of the seed through the seed to a shade of it', async ({ page }) => {
  await page.goto(site(BLUE_LINK), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-face')).toHaveCount(9);

  const chips = await chipColors(page);
  expect(chips[4]).toEqual([0x1a, 0x73, 0xe8]);
  const lightness = chips.map(([red, green, blue]) => red + green + blue);
  for (let i = 1; i < lightness.length; i++) expect(lightness[i], `chip ${i + 1}`).toBeLessThan(lightness[i - 1]);
  // Every tint and shade keeps the seed's blue as its strongest channel.
  for (const [red, green, blue] of chips) expect(blue).toBeGreaterThan(Math.max(red, green));
});

test('the fan opens and the lift walks by default', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  const names = (selector: string) =>
    page.evaluate(
      (selector) =>
        Array.from(document.querySelectorAll(selector), (element) =>
          element.getAnimations().map((animation) => (animation as CSSAnimation).animationName),
        ),
      selector,
    );
  expect(await names('.mk-chip')).toEqual(Array(9).fill(['mk-open']));
  expect(await names('.mk-face')).toEqual(Array(9).fill(['mk-walk']));
  // The walk never ends, so it is still running whenever the page is looked at.
  expect(
    await page.evaluate(() =>
      Array.from(document.querySelectorAll('.mk-face'), (face) => face.getAnimations()[0].playState),
    ),
  ).toEqual(Array(9).fill('running'));
});

test('nothing moves under reduced motion or with motion frozen', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-face').first()).toBeVisible();
  expect(await page.evaluate(() => document.getAnimations().length)).toBe(0);

  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.goto(site('/?motion=frozen'), { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.mk-face').first()).toBeVisible();
  expect(await page.evaluate(() => document.getAnimations().length)).toBe(0);
});

test('the status line says where the seed came from', async ({ page }) => {
  const status = page.locator('.mk-status');
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  await expect(status).toHaveText('Loading the builder', { useInnerText: true });

  await page.goto(site(BLUE_LINK), { waitUntil: 'domcontentloaded' });
  await expect(status).toHaveText('Opening a shared theme', { useInnerText: true });

  await page.evaluate(() => localStorage.setItem('mk:splash', JSON.stringify({ seed: 0xff1a73e8 | 0 })));
  await page.goto(site('/'), { waitUntil: 'domcontentloaded' });
  await expect(status).toHaveText('Opening your last theme', { useInnerText: true });
});

/** Each chip's color as sRGB channels, drawn through a 2D canvas so a color-mix fallback reads the same. */
async function chipColors(page: Page): Promise<number[][]> {
  return page.evaluate(() => {
    const context = document.createElement('canvas').getContext('2d', { willReadFrequently: true })!;
    return Array.from(document.querySelectorAll('.mk-face'), (face) => {
      context.clearRect(0, 0, 1, 1);
      context.fillStyle = getComputedStyle(face).backgroundColor;
      context.fillRect(0, 0, 1, 1);
      return Array.from(context.getImageData(0, 0, 1, 1).data.slice(0, 3));
    });
  });
}
