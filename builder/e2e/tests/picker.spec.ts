import { expect, test, type Page } from '@playwright/test';
import { openBuilder, wantHooks } from './builder';

// b-307
// The seed's color picker with the browser's eyedropper (F-06, F-07). The eyedropper is a stand-in
// the test resolves with a color or closes with Esc, the way browser-apis.spec.ts drives it, so the
// spec runs in every browser. Compose draws on a canvas, so buttons are found in the accessibility
// mirror and clicked where they sit.

/** Long enough for a key or a click to reach Compose and what it opens to settle. */
const SETTLE_MS = 300;

test.beforeEach(async ({ context, page }) => {
  await wantHooks(context);
  await page.addInitScript(() => {
    class TestEyeDropper {
      open() {
        return new Promise((resolve, reject) => {
          const onKey = (event: KeyboardEvent) => {
            if (event.key !== 'Escape') return;
            document.removeEventListener('keydown', onKey, true);
            delete (window as any).__pick;
            reject(new DOMException('Closed by the test', 'AbortError'));
          };
          document.addEventListener('keydown', onKey, true);
          (window as any).__pick = (hex: string) => {
            document.removeEventListener('keydown', onKey, true);
            delete (window as any).__pick;
            resolve({ sRGBHex: hex });
          };
        });
      }
    }
    Object.defineProperty(window, 'EyeDropper', { configurable: true, writable: true, value: TestEyeDropper });
  });
});

test('an eyedropper pick lands on Done, and Esc puts the seed back', async ({ page }) => {
  await openBuilder(page);
  await expect(page.locator('#cmp_a11y_root > *').first()).toBeAttached({ timeout: 30_000 });

  await click(page, 'Pick');
  await click(page, 'Pick from screen');
  await pickOffScreen(page, '#1a73e8');
  await click(page, 'Done');
  await expect(button(page, 'Done')).toHaveCount(0);
  await expect(page.locator('#cmp_a11y_root')).toContainText('#1A73E8');

  await click(page, 'Pick');
  await click(page, 'Pick from screen');
  await pickOffScreen(page, '#34a853');
  await expect(page.locator('#cmp_a11y_root')).toContainText('#34A853');
  // The Esc that closes the browser's eyedropper leaves the picker open.
  await click(page, 'Pick from screen');
  await page.waitForFunction(() => typeof (window as any).__pick === 'function');
  await press(page, 'Escape');
  await expect(button(page, 'Done')).toHaveCount(1);
  await press(page, 'Escape');

  await expect(button(page, 'Done')).toHaveCount(0);
  await expect(page.locator('#cmp_a11y_root')).toContainText('#1A73E8');
  await expect(page.locator('#cmp_a11y_root')).not.toContainText('#34A853');
});

function button(page: Page, name: string) {
  return page.locator('#cmp_a11y_root').getByRole('button', { name, exact: true });
}

/** Clicks the first button called [name] where Compose draws it. */
async function click(page: Page, name: string): Promise<void> {
  const target = button(page, name).first();
  await expect.poll(async () => (await target.boundingBox())?.height ?? 0, { timeout: 15_000 }).toBeGreaterThan(0);
  const box = (await target.boundingBox())!;
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}

async function pickOffScreen(page: Page, hex: string): Promise<void> {
  await page.waitForFunction(() => typeof (window as any).__pick === 'function');
  await page.evaluate((color) => (window as any).__pick(color), hex);
  await page.waitForTimeout(SETTLE_MS);
}

async function press(page: Page, key: string): Promise<void> {
  await page.keyboard.press(key);
  await page.waitForTimeout(SETTLE_MS);
}
