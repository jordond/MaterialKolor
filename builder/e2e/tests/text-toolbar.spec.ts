import { expect, test, type CDPSession, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-224
// The text toolbar the kit draws in the page for a touch selection (D40). A long press on a field
// shows it, and Paste reads the clipboard inside the tap's own user activation. Chromium only, since
// Playwright drives the long press through a CDP session.
//
// Both tests are fixme for now. At a phone viewport the only editable field on the first screen is
// the seed field in the controls sheet, which only peeks. A CDP tap on the sheet's peek button does
// not open it, so the field's box in the mirror stays 0 by 0 and the long press has nowhere to land.
// They can run once a route or a test hook opens the sheet, or a field sits on the first screen.

const PASTED = '#0B6E4F';

test.use({ hasTouch: true, isMobile: true, viewport: { width: 390, height: 844 } });

test.beforeEach(async ({ context, browserName }) => {
  test.skip(browserName !== 'chromium', 'The long press goes through a Chromium CDP session');
  await wantHooks(context);
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
});

test.fixme('a long press on a field shows the toolbar and Paste puts the clipboard text in', async ({ page, context }) => {
  await openBuilder(page);
  await page.evaluate((text) => navigator.clipboard.writeText(text), PASTED);
  const cdp = await context.newCDPSession(page);
  const field = await seedField(page, cdp);
  await longPress(cdp, field);

  const paste = page.locator('#cmp_a11y_root').getByRole('button', { name: 'Paste', exact: true });
  await expect(paste).toBeAttached({ timeout: 10_000 });
  await tap(cdp, paste);

  await expect.poll(() => field.textContent(), { timeout: 10_000 }).toContain(PASTED.slice(1));
  await expect(paste).toHaveCount(0);
});

test.fixme('Copy from the toolbar puts the selection on the clipboard', async ({ page, context }) => {
  await openBuilder(page);
  await page.evaluate(() => navigator.clipboard.writeText(''));
  const cdp = await context.newCDPSession(page);
  const field = await seedField(page, cdp);
  const before = (await field.textContent()) ?? '';
  await longPress(cdp, field);

  const selectAll = page.locator('#cmp_a11y_root').getByRole('button', { name: 'Select all', exact: true });
  if (await selectAll.count()) await tap(cdp, selectAll);
  const copy = page.locator('#cmp_a11y_root').getByRole('button', { name: 'Copy', exact: true });
  await expect(copy).toBeAttached({ timeout: 10_000 });
  await tap(cdp, copy);

  await expect.poll(() => page.evaluate(() => navigator.clipboard.readText())).not.toBe('');
  expect(before).toContain(await page.evaluate(() => navigator.clipboard.readText()));
});

/**
 * The seed field, found by its box in the mirror. On a phone it sits in the controls sheet, so the
 * sheet opens first when it only peeks.
 */
async function seedField(page: Page, cdp: CDPSession): Promise<Locator> {
  const mirror = page.locator('#cmp_a11y_root');
  const field = mirror.getByRole('textbox', { name: /^Seed color/ });
  await expect(field).toBeAttached({ timeout: 30_000 });
  const peek = mirror.getByRole('button', { name: /^Seed and theme controls, Peek/ });
  if (await peek.count()) await tap(cdp, peek.first());
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

/** Holds a finger on the start of [target] for longer than a long press takes. */
async function longPress(cdp: CDPSession, target: Locator): Promise<void> {
  const box = await target.boundingBox();
  if (!box) throw new Error('The field has no box');
  const point = { x: box.x + Math.min(24, box.width / 4), y: box.y + box.height / 2 };
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await new Promise((resolve) => setTimeout(resolve, 700));
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}

/** Taps the middle of [target] with a finger. */
async function tap(cdp: CDPSession, target: Locator): Promise<void> {
  const box = await target.boundingBox();
  if (!box) throw new Error('The button has no box');
  const point = { x: box.x + box.width / 2, y: box.y + box.height / 2 };
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}
