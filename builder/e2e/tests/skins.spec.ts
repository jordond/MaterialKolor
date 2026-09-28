import { expect, test } from '@playwright/test';
import { openBuilder, pressBareCanvas, SETTLE_MS, wantHooks } from './builder';
import {
  button,
  LAND_TIMEOUT_MS,
  onPage,
  openWorkspace,
  press,
  pressKeyUntil,
  storedDocument,
} from '../fixtures/workspace';

// A library switch changes the preview and never the shell, which stays Material 3 Expressive. The
// number keys switch the library, 4 to Fluent and 1 back to Material 3, and the page keeps running
// with no error and the shell's tabs keep their folded names.

const A11Y = '#cmp_a11y_root';

/** The selected canvas tab, by the name the web folds its role and state into. */
const SELECTED_TAB = `${A11Y} [aria-label$=", tab, selected"]`;

/** The top bar's Undo, which names a library switch once it has landed. */
const UNDO_SWITCH = /^Undo library change/;

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('4 switches the library to Fluent and 1 back, with no page error and the shell tab names kept', async ({ page }) => {
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

// Switch library. 3, 4 and 1 move the library to Unstyled, Fluent and back to M3, and 2 then picks
// M3 Expressive, which takes the Expressive style along and moves a 2021 spec to 2025 in the same
// undo step, with no dialog. 1 leaves it and puts back the style and spec it came from. A new theme
// starts on 2026, which the pick keeps, so this opens a v1 link, which keeps its 2021 spec.

/** A v1 link to the default seed on TonalSpot, with no color_spec, so it opens on 2021. */
const LEGACY_2021 = '/?color_seed=FFD9653B';

test('3, 4 and 1 switch the library, 2 picks M3 Expressive with its style, and 1 puts the style back', async ({
  page,
}) => {
  await openWorkspace(page, LEGACY_2021);
  const undo = page.locator('#cmp_a11y_root').getByRole('button', { name: /^Undo library change to / });
  // Read without waiting, since there is no such Undo before the first switch.
  const undoNames = async (name: string) =>
    (await undo.evaluateAll((buttons) => buttons.map((button) => button.getAttribute('aria-label') ?? ''))).some((label) =>
      label.endsWith(name),
    );
  await pressKeyUntil(page, '3', () => undoNames('Unstyled'));
  await pressKeyUntil(page, '4', () => undoNames('Fluent'));
  await pressKeyUntil(page, '1', () => undoNames('M3'));
  await pressKeyUntil(page, '2', () => undoNames('M3 Expressive'));

  await expect.poll(async () => pick(await storedDocument(page)), { timeout: LAND_TIMEOUT_MS }).toEqual({
    style: 'Expressive',
    spec: 'Spec2025',
    expressive: true,
  });
  await expect(onPage(page, /^Use the Expressive style\?/)).toHaveCount(0);

  await pressKeyUntil(page, '1', () => undoNames('M3'));
  await expect.poll(async () => pick(await storedDocument(page)), { timeout: LAND_TIMEOUT_MS }).toEqual({
    style: 'TonalSpot',
    spec: 'Spec2021',
    expressive: false,
  });

  await press(page, button(page, /^Undo /));
  await expect.poll(async () => pick(await storedDocument(page)), { timeout: LAND_TIMEOUT_MS }).toEqual({
    style: 'Expressive',
    spec: 'Spec2025',
    expressive: true,
  });
});

/** The parts of a stored document an M3 Expressive pick moves. */
function pick(document: Record<string, unknown> | null) {
  return { style: document?.style, spec: document?.spec, expressive: document?.expressive };
}
