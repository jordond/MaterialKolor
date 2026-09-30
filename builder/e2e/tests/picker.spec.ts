import type { Page } from '@playwright/test';
import { expect, openBuilder, test } from './builder';
import { A11Y, button, pressSettled } from '../fixtures/workspace';

// The seed's color picker with the browser's eyedropper. The eyedropper is a stand-in
// the test resolves with a color or closes with Esc, the way browser-apis.spec.ts drives it, so the
// spec runs in every browser. Compose draws on a canvas, so buttons are found in the accessibility
// mirror and clicked where they sit.

test.beforeEach(async ({ page }) => {
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
  await expect(page.locator(A11Y)).toContainText('#1A73E8');

  await click(page, 'Pick');
  await click(page, 'Pick from screen');
  await pickOffScreen(page, '#34a853');
  await expect(page.locator(A11Y)).toContainText('#34A853');
  // The Esc that closes the browser's eyedropper leaves the picker open.
  await click(page, 'Pick from screen');
  await page.waitForFunction(() => typeof (window as any).__pick === 'function');
  await page.keyboard.press('Escape');
  // The stand-in lets go of its pick once the Esc has reached it.
  await page.waitForFunction(() => (window as any).__pick === undefined);
  await expect(button(page, 'Done')).toHaveCount(1);
  await page.keyboard.press('Escape');

  await expect(button(page, 'Done')).toHaveCount(0);
  await expect(page.locator(A11Y)).toContainText('#1A73E8');
  await expect(page.locator(A11Y)).not.toContainText('#34A853');
});

/** Clicks the first button called [name] once the sheet that holds it has come in. */
async function click(page: Page, name: string): Promise<void> {
  await pressSettled(page, button(page, name));
}

/** Resolves the stand-in eyedropper with [hex] once the page has opened it. */
async function pickOffScreen(page: Page, hex: string): Promise<void> {
  await page.waitForFunction(() => typeof (window as any).__pick === 'function');
  await page.evaluate((color) => (window as any).__pick(color), hex);
}
