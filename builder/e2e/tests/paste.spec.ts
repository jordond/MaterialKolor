import type { Locator, Page } from '@playwright/test';
import { dispatchPaste, expect, openBuilder, test } from './builder';
import { installFileMakers } from './image-files';

// Text pasted with nothing editable focused on the real builder, read from the page's
// accessibility tree. A color sets the seed with an undo toast, and a share link asks to open.

const A11Y = '#cmp_a11y_root';

/** The toast once a pasted color sets the seed, `command_pasted_seed`. */
const SEEDED = 'Seed set to #6750A4';

/** The toast once a share link is pasted, `command_pasted_link`. */
const OFFER = 'Open the shared theme?';

/** A share code with the default seed, #D9653B, as `shell.spec.ts` has it. */
const DEFAULT_LINK = 'https://materialkolor.com/t/AdllOwAAAAAT';

test.beforeEach(async ({ context }) => {
  await context.addInitScript(installFileMakers);
});

test('a pasted color sets the seed and offers to undo it', async ({ page }) => {
  await openReady(page);

  expect(await dispatchPaste(page, { text: '#6750A4' })).toBe(true);

  await expect.poll(() => seedText(page), { timeout: 10_000 }).toContain('6750A4');
  await expect(onPage(page, SEEDED)).toHaveCount(1, { timeout: 10_000 });
  await expect(onPage(page, 'Undo')).not.toHaveCount(0);
});

test('a pasted share link offers to open the theme', async ({ page }) => {
  await openReady(page);

  expect(await dispatchPaste(page, { text: DEFAULT_LINK })).toBe(true);

  await expect(onPage(page, OFFER)).toHaveCount(1, { timeout: 10_000 });
  await expect(onPage(page, 'Open')).not.toHaveCount(0);
});

/** Open the builder and wait until the seed field is in the accessibility tree. */
async function openReady(page: Page): Promise<void> {
  await openBuilder(page);
  await expect(seedField(page)).toBeAttached({ timeout: 30_000 });
}

/** The seed field, whose text is the seed's hex. */
function seedField(page: Page): Locator {
  return page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ });
}

async function seedText(page: Page): Promise<string> {
  return (await seedField(page).textContent()) ?? '';
}

/** Whatever in the page's accessibility tree holds [text], word for word. */
function onPage(page: Page, text: string): Locator {
  return page.locator(A11Y).getByText(text, { exact: true });
}
