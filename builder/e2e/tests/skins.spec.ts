import { expect, test } from '@playwright/test';
import { openBuilder, pressBareCanvas, SETTLE_MS, wantHooks } from './builder';
import {
  button,
  focusCanvas,
  LAND_TIMEOUT_MS,
  onPage,
  openWorkspace,
  press,
  pressKeyUntil,
  storedDocument,
} from '../fixtures/workspace';

// The shell in the Fluent skin, whose tabs, segmented rows, switches, checkboxes and disclosures are
// Fluent's own components. The number keys switch the library, 4 to Fluent and 1 back to Material 3,
// and the page keeps running with no error. On the web the mirror loses a tab's role and state, so the
// Fluent tab carries both in its name the way every skin's tab does.

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

// Switch library. 3, 4 and 2 move the library to Unstyled, Fluent and Expressive, and 2
// on TonalSpot 2021 suggests the Expressive style on the 2025 spec, which Apply sets as one undo.

test('3, 4 and 2 switch the library, and Apply takes the Expressive suggestion as one undo', async ({ page }) => {
  await openWorkspace(page);
  const undo = page.locator('#cmp_a11y_root').getByRole('button', { name: /^Undo library change to / });
  // Read without waiting, since there is no such Undo before the first switch.
  const undoNames = async (name: string) =>
    (await undo.evaluateAll((buttons) => buttons.map((button) => button.getAttribute('aria-label') ?? ''))).some((label) =>
      label.endsWith(name),
    );
  // The web folds the dialog's role into its title, `Use the Expressive style?, dialog`.
  const suggestion = onPage(page, /^Use the Expressive style\?/);
  await pressKeyUntil(page, '3', () => undoNames('Unstyled'));
  await pressKeyUntil(page, '4', () => undoNames('Fluent'));
  // The 2 opens the suggestion with the switch, and the suggestion is modal, so the mirror hides the
  // top bar's Undo and the tab row the canvas is focused by until it closes. The suggestion showing
  // is what says the 2 landed, and each try waits for it as long as a switch may take to land, so
  // no try goes looking for the tab row once the suggestion is up.
  for (let tries = 0; tries < 3 && (await suggestion.count()) === 0; tries += 1) {
    await focusCanvas(page);
    await page.keyboard.press('2');
    await suggestion.first().waitFor({ state: 'attached', timeout: LAND_TIMEOUT_MS }).catch(() => undefined);
  }

  await expect(suggestion).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await press(page, button(page, 'Apply'));
  await expect.poll(async () => pick(await storedDocument(page)), { timeout: LAND_TIMEOUT_MS }).toEqual({
    style: 'Expressive',
    spec: 'Spec2025',
    expressive: true,
  });

  await press(page, button(page, /^Undo /));
  await expect.poll(async () => pick(await storedDocument(page)), { timeout: LAND_TIMEOUT_MS }).toEqual({
    style: 'TonalSpot',
    spec: 'Spec2021',
    expressive: true,
  });
});

/** The parts of a stored document the Expressive suggestion changes. */
function pick(document: Record<string, unknown> | null) {
  return { style: document?.style, spec: document?.spec, expressive: document?.expressive };
}
