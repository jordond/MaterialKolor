import { expect, test, type Locator, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import { button, LAND_TIMEOUT_MS, openWorkspace, press, pressKeyUntil, typeSeed } from '../fixtures/workspace';

// The History list. Three library switches make three steps, and a click on a row goes straight to
// its step, across skins, with the list still open and the mirror keeping up with each skin it
// redraws in. Undo names the step the theme is at, the rows after it read undone, and the next real
// edit drops them.

/** A seed nothing else in the flow lands on, so its step is the only one that names it. */
const NEW_SEED = '#2E7D32';

test.use({ viewport: { width: 1280, height: 800 } });

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('a row travels to its step, and the next edit drops the undone steps', async ({ page }) => {
  await openWorkspace(page);
  await pressKeyUntil(page, '3', () => undoNames(page, 'Unstyled'));
  await pressKeyUntil(page, '4', () => undoNames(page, 'Fluent'));
  await pressKeyUntil(page, '1', () => undoNames(page, 'M3'));

  await openHistory(page);
  await press(page, row(page, 'Library change to Unstyled'));

  await expect(button(page, 'Undo library change to Unstyled')).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect(row(page, 'Library change to Fluent', /undone/)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect(row(page, 'Library change to M3', /undone/)).toHaveCount(1);
  await press(page, row(page, 'Library change to M3'));
  await expect(button(page, /^Redo, disabled$/)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await press(page, row(page, 'Library change to Unstyled'));
  await expect(button(page, 'Undo library change to Unstyled')).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await page.keyboard.press('Escape');
  await expect(row(page, 'Start')).toHaveCount(0, { timeout: LAND_TIMEOUT_MS });
  await typeSeed(page, NEW_SEED);
  await openHistory(page);

  await expect(row(page, `Seed change to ${NEW_SEED}`)).toHaveCount(1);
  await expect(row(page, 'Library change to Unstyled')).toHaveCount(1);
  await expect(row(page, 'Library change to Fluent')).toHaveCount(0);
  await expect(row(page, 'Library change to M3')).toHaveCount(0);
});

/** Whether the top bar's Undo names a switch to [name], read without waiting. */
async function undoNames(page: Page, name: string): Promise<boolean> {
  return (await button(page, `Undo library change to ${name}`).count()) > 0;
}

/** Opens the History list from its top bar button and waits for its Start row. */
async function openHistory(page: Page): Promise<void> {
  await press(page, button(page, 'History'));
  await expect(row(page, 'Start')).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
}

/**
 * The History row called [headline]. The web folds its second line and its state into its name,
 * "Library change to Fluent, Just now, undone, not selected", which [rest] can look into.
 */
function row(page: Page, headline: string, rest: RegExp = /.*/): Locator {
  const escaped = headline.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return button(page, new RegExp(`^${escaped}, (?=.*${rest.source})`, 'i'));
}
