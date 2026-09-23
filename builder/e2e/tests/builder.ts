import type { BrowserContext, Page } from '@playwright/test';

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
