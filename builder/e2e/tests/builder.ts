import { expect, test as base, type BrowserContext, type Page } from '@playwright/test';

declare global {
  interface Window {
    __mkE2e?: boolean;
    __mk?: Record<string, (argument?: string) => string>;
  }
}

/**
 * The spec runner, with the shell's test hooks wanted on every page of the test's own context. A
 * context the test opens itself calls [wantHooks] on its own.
 */
export const test = base.extend<{ shellHooks: void }>({
  shellHooks: [
    async ({ context }, use) => {
      await wantHooks(context);
      await use();
    },
    { auto: true },
  ],
});

export { expect };

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

/** Open [path] and wait until the shell has hung its hooks and drawn its first frame. */
export async function openBuilder(page: Page, path = '/'): Promise<void> {
  await page.goto(site(path));
  await waitForReady(page);
}

/** Reload [page] and wait until the shell has hung its hooks and drawn its first frame again. */
export async function reloadBuilder(page: Page): Promise<void> {
  await page.reload();
  await waitForReady(page);
}

/**
 * Waits for the shell's hooks and the `mk:first-frame` mark, which the app sets once it has drawn,
 * so a press or a key after this reaches a page that is there to take it.
 */
async function waitForReady(page: Page): Promise<void> {
  await page.waitForFunction(
    () =>
      ['route', 'addHint', 'media'].every((name) => typeof window.__mk?.[name] === 'function') &&
      performance.getEntriesByName('mk:first-frame').length > 0,
  );
}

/** Call the shell's test hook [name] with [argument] and return what it says. */
export async function hook(page: Page, name: string, argument = ''): Promise<string> {
  return page.evaluate(([hookName, hookArgument]) => window.__mk![hookName](hookArgument), [name, argument]);
}

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
