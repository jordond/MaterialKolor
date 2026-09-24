import { expect, test } from '@playwright/test';
import { openBuilder, pressBareCanvas, SETTLE_MS, wantHooks } from './builder';

// b-403b
// The shell in the Fluent skin, whose tabs, segmented rows, switches, checkboxes and disclosures are
// Fluent's own components. The number keys switch the library, 4 to Fluent and 1 back to Material 3,
// and the page keeps running with no error. On the web the mirror loses a tab's role and state, so the
// Fluent tab carries both in its name the way every skin's tab does (D37).

const A11Y = '#cmp_a11y_root';

/** The selected canvas tab, by the name the web folds its role and state into. */
const SELECTED_TAB = `${A11Y} [aria-label$=", tab, selected"]`;

/** The top bar's Undo, which names a library switch once it has landed. */
const UNDO_SWITCH = /^Undo library change/;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('4 switches the shell to Fluent and 1 back, with no page error and the tab name folded', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await openBuilder(page);
  await pressBareCanvas(page);
  const undo = page.locator(A11Y).getByRole('button', { name: UNDO_SWITCH });

  await page.keyboard.press('4');
  await expect(undo).toHaveCount(1, { timeout: 10_000 });
  await page.waitForTimeout(SETTLE_MS);
  await expect(page.locator(SELECTED_TAB).first()).toBeAttached({ timeout: 10_000 });
  await expect(page.locator(`${A11Y} [aria-label^="Contrast, tab, "]`).first()).toBeAttached();

  await pressBareCanvas(page);
  await page.keyboard.press('1');
  await page.waitForTimeout(SETTLE_MS);
  await expect(page.locator(SELECTED_TAB).first()).toBeAttached({ timeout: 10_000 });

  expect(errors).toEqual([]);
});
