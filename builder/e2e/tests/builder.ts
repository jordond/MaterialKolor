import { expect, type BrowserContext, type Locator, type Page } from '@playwright/test';

declare global {
  interface Window {
    __mkE2e?: boolean;
    __mk?: Record<string, (argument?: string) => string>;
  }
}

/** The address of [path] on the site the global setup serves. */
export function site(path: string): string {
  const base = process.env.MK_E2E_BASE_URL;
  if (!base) throw new Error('MK_E2E_BASE_URL is not set, the global setup did not run');
  return new URL(path, base).toString();
}

/** Ask every page in [context] to hang the shell's test hooks on `window.__mk`. */
export async function wantHooks(context: BrowserContext): Promise<void> {
  await context.addInitScript(() => {
    window.__mkE2e = true;
  });
}

/** Open [path] and wait until the shell has built its services and hung their hooks. */
export async function openBuilder(page: Page, path = '/'): Promise<void> {
  await page.goto(site(path));
  await waitForHooks(page);
}

/** Reload [page] and wait until the shell has hung its hooks again. */
export async function reloadBuilder(page: Page): Promise<void> {
  await page.reload();
  await waitForHooks(page);
}

async function waitForHooks(page: Page): Promise<void> {
  await page.waitForFunction(() =>
    ['route', 'addHint', 'media'].every((name) => typeof window.__mk?.[name] === 'function'),
  );
}

/** Call the shell's test hook [name] with [argument] and return what it says. */
export async function hook(page: Page, name: string, argument = ''): Promise<string> {
  return page.evaluate(([hookName, hookArgument]) => window.__mk![hookName](hookArgument), [name, argument]);
}

// b-302

/** Open [path] and wait until the browser API hooks are up too. */
export async function openWithBrowserApis(page: Page, path = '/'): Promise<void> {
  await openBuilder(page, path);
  await page.waitForFunction(() => typeof window.__mk?.gesture === 'function');
}

/**
 * Run the browser API hook [action] inside a real click, the only place the browser lets a page copy,
 * download, share, pick a file or open the eyedropper.
 */
export async function gesture(page: Page, action: string): Promise<void> {
  await hook(page, 'gesture', action);
  await page.click('#mk-e2e-gesture');
}

// b-315c

/**
 * Paste [text] and [files] on the page, into a text field, into a text field in a shadow root the
 * way Compose keeps its inputs, or into a stand-in for Compose's hidden clip target. True when the
 * page took it. The page needs `__makeFiles` and `__makeTransfer` hung on it first, as
 * `installFileMakers` in `image-files.ts` does.
 */
export async function dispatchPaste(
  page: Page,
  {
    text,
    files = [],
    into = 'page',
  }: { text?: string; files?: string[]; into?: 'page' | 'field' | 'shadowField' | 'clipTarget' },
): Promise<boolean> {
  return page.evaluate(
    async ({ text, names, into }) => {
      let target: HTMLElement = document.body;
      let added: HTMLElement | null = null;
      if (into !== 'page') {
        target = document.createElement('textarea');
        if (into === 'field') {
          added = target;
        } else {
          added = document.createElement('div');
          added.attachShadow({ mode: 'open' }).appendChild(target);
          if (into === 'clipTarget') target.setAttribute('aria-hidden', 'true');
        }
        document.body.appendChild(added);
        target.focus();
      }
      const transfer = (window as any).__makeTransfer(await (window as any).__makeFiles(names), text);
      const event = new Event('paste', { bubbles: true, cancelable: true, composed: true });
      Object.defineProperty(event, 'clipboardData', { value: transfer });
      target.dispatchEvent(event);
      added?.remove();
      return event.defaultPrevented;
    },
    { text, names: files, into },
  );
}

// b-315d

/** Long enough for a key or a press to reach Compose and settle. */
export const SETTLE_MS = 300;

/**
 * Presses the canvas where nothing is, in the preview's header row halfway from its last tab to the
 * window's edge, and waits for the page's focus holder to take focus.
 */
export async function pressBareCanvas(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (!viewport) throw new Error('The page has no viewport');
  const tabs = page.locator('#cmp_a11y_root').getByText('Contrast', { exact: true });
  await expect(tabs.first()).toBeAttached({ timeout: 30_000 });
  const boxes = (await Promise.all((await tabs.all()).map((tab) => tab.boundingBox()))).filter(
    (box): box is NonNullable<typeof box> => box !== null && box.x > viewport.width / 3,
  );
  if (boxes.length === 0) throw new Error('The preview has no Contrast tab on screen');
  const tab = boxes.reduce((top, box) => (box.y < top.y ? box : top));
  await page.mouse.click((tab.x + tab.width + viewport.width) / 2, tab.y + tab.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}

/** Clicks the middle of [locator] and lets the press settle. */
export async function clickMiddle(page: Page, locator: Locator): Promise<void> {
  const box = await locator.boundingBox();
  if (!box) throw new Error('Nothing to click');
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(SETTLE_MS);
}
