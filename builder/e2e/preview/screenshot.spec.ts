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

test('the builder at this commit', async ({ page }) => {
  // A staging build may carry an analytics token. A screenshot is not a visit.
  await page.route('https://static.cloudflareinsights.com/**', (route) => route.abort());

  await page.goto(site('/'));
  await expect(page.locator(NAMED_PROJECTS).first()).toBeAttached({ timeout: 60_000 });
  // Let the first frames settle, the splash fade and the fonts among them.
  await page.waitForTimeout(1_500);
  await page.screenshot({ path: OUT });
});
