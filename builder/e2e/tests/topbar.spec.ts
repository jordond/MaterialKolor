import { expect, test, type Locator, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-231
// The top bar gives its end-edge actions their full size first, so More options stays reachable on
// a 1280 wide desktop window, where the library switcher used to squeeze it to nothing.

const A11Y = '#cmp_a11y_root';

/** The top bar's height, `ShellMetrics.topBarHeight`, in CSS pixels at a device scale of one. */
const TOP_BAR_HEIGHT = 64;

/** The More options menu's first item, which shows once a click has opened the menu. */
const FIRST_MORE_ITEM = 'Use the system appearance';

test.use({ viewport: { width: 1280, height: 800 } });

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('top bar', () => {
  test('at 1280 wide, a click on More options opens Help and About', async ({ page }) => {
    await openBuilder(page);
    await expect(item(page, 'Help')).toHaveCount(0);

    await clickTopBarButton(page, 'More options', FIRST_MORE_ITEM);

    await expect(item(page, 'Help').first()).toBeAttached();
    await expect(item(page, 'About').first()).toBeAttached();
  });
});

/** Whatever in the page's accessibility tree reads exactly [text], as its text or its label. */
function item(page: Page, text: string) {
  const root = page.locator(A11Y);
  return root.getByText(text, { exact: true }).or(root.getByLabel(text, { exact: true }));
}

/**
 * Clicks the middle of the button called [name] in the top bar, not the one in the preview's sample
 * app, once Compose has given it a size. Then waits for [opens], the first item of what it opens.
 */
async function clickTopBarButton(page: Page, name: string, opens: string): Promise<void> {
  const buttons = page.locator(A11Y).getByRole('button', { name, exact: true });
  await expect
    .poll(async () => {
      const box = await inTopBar(buttons.all());
      return box ? Math.min(box.width, box.height) : 0;
    }, { timeout: 30_000 })
    .toBeGreaterThan(0);
  const box = (await inTopBar(buttons.all()))!;
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await expect(item(page, opens).first()).toBeAttached({ timeout: 15_000 });
}

type Box = { x: number; y: number; width: number; height: number };

/** The box of the one button among [all] whose middle sits in the top bar, if there is one. */
async function inTopBar(all: Promise<Locator[]>): Promise<Box | null> {
  const boxes = await Promise.all((await all).map((button) => button.boundingBox()));
  return boxes.find((box): box is Box => box !== null && box.y + box.height / 2 < TOP_BAR_HEIGHT) ?? null;
}
