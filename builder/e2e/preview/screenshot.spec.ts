import path from 'node:path';
import { expect, test } from '@playwright/test';
import { site } from '../tests/builder';

/**
 * Where the screenshot goes. By default it lands in the built site, so the preview version the
 * Worker uploads serves it beside the app and the pull request comment can show it.
 */
const OUT = process.env.MK_PR_SCREENSHOT ?? path.join(process.env.MK_E2E_SITE_DIR ?? '.', 'pr-screenshot.png');

/**
 * The poster's Projects button once boot has opened a project, the same boot signal the first run
 * test waits on.
 */
const NAMED_PROJECTS = '#cmp_a11y_root [aria-label^="Projects, "]';

/** The poster's contrast levels, the last strings on the poster to arrive. */
const CONTRAST_LEVELS = ['Reduced', 'Standard', 'Medium', 'High'];

test('the builder at this commit', async ({ page }) => {
  // A staging build may carry an analytics token. A screenshot is not a visit.
  await page.route('https://static.cloudflareinsights.com/**', (route) => route.abort());

  await page.goto(site('/'));
  await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached({ timeout: 60_000 });
  for (const level of CONTRAST_LEVELS) {
    await expect(page.locator(`#cmp_a11y_root [aria-label^="${level}, radio"]`), level).toHaveCount(1);
  }
  await page.waitForFunction(() => performance.getEntriesByName('mk:first-frame').length > 0);
  // The splash fades out and then leaves the page.
  await expect(page.locator('#splash')).toHaveCount(0);
  await page.evaluate(async () => {
    await document.fonts.ready;
    for (let frame = 0; frame < 3; frame++) await new Promise(requestAnimationFrame);
  });
  await page.screenshot({ path: OUT });
});
