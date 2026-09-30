import type { Page } from '@playwright/test';
import { expect, hook, openBuilder, test } from './builder';

// The storage behind the builder, driven through the shell's test hooks. What storage turns up is
// told to the user, so those checks read the page's accessibility tree. The router and the page
// around it have their own specs.

test.describe('storage', () => {
  test("another tab's write arrives as one change and the store reads it", async ({ context }, testInfo) => {
    const writer = await context.newPage();
    const reader = await context.newPage();
    await openBuilder(writer);
    await openBuilder(reader);
    // A fresh page pulses the library switcher once and stores that it did. Let both pages finish
    // that first and count only the changes after it.
    await settlePulse(writer);
    await settlePulse(reader);
    await syncTabs(writer, reader);
    const before = prefsChanges(await hook(reader, 'externalChanges'));
    await reader.evaluate(() => {
      window.addEventListener('storage', (event) => {
        if (event.key === 'mk:prefs') (window as any).__storageAt = performance.timeOrigin + performance.now();
      });
    });

    const [sentAt, outcome] = await writer.evaluate(() => {
      const at = performance.timeOrigin + performance.now();
      return [at, window.__mk!.addHint('from-writer')] as const;
    });
    expect(outcome).toBe('Done');

    await expect.poll(async () => prefsChanges(await hook(reader, 'externalChanges'))).toBe(before + 1);
    await expect.poll(async () => userHints(await hook(reader, 'seenHints'))).toBe('from-writer');
    expect(userHints(await hook(reader, 'readHints'))).toBe('from-writer');
    const arrivedAt = await reader.evaluate(() => (window as any).__storageAt as number);
    testInfo.annotations.push({ type: 'storage event ms', description: (arrivedAt - sentAt).toFixed(1) });
    console.log(`[storage] ${testInfo.project.name} storage event after ${(arrivedAt - sentAt).toFixed(1)} ms`);

    // One write is one event. A later write has reached the reader, so any second event for it would have too.
    await syncTabs(writer, reader);
    expect(prefsChanges(await hook(reader, 'externalChanges'))).toBe(before + 1);
  });

  test('a write to full storage comes back as QuotaExceeded', async ({ page }) => {
    await openBuilder(page);
    await settlePulse(page);
    expect(await hook(page, 'storageAvailable')).toBe('true');
    const filled = await fillStorage(page);
    expect(await hook(page, 'addHint', 'no-room')).toBe('QuotaExceeded');
    expect(userHints(await hook(page, 'readHints'))).toBe('');

    await page.evaluate((count) => {
      for (let index = 0; index < count; index++) localStorage.removeItem(`fill:${index}`);
    }, filled);
    expect(await hook(page, 'addHint', 'room')).toBe('Done');
  });

  test('an unreadable record is set aside and the user is told once', async ({ page }) => {
    await openBuilder(page);
    // The pulse stores its hint once, so it goes first and never writes over the broken record.
    await settlePulse(page);
    await page.evaluate(() => localStorage.setItem('mk:prefs', 'not json'));

    expect(userHints(await hook(page, 'readHints'))).toBe('');
    expect(userHints(await hook(page, 'readHints'))).toBe('');
    await expect(onPage(page, SET_ASIDE)).toHaveCount(1);
    const holders = await page.evaluate(() =>
      Object.keys(localStorage).filter((key) => localStorage.getItem(key) === 'not json'),
    );
    expect(holders).toHaveLength(1);
    expect(holders[0]).toMatch(/^mk:quarantine:/);
  });

  test("a newer build's index from another tab stays as it is, refuses writes and asks for a reload", async ({
    context,
  }) => {
    const newer = '{"schema":999,"data":{"projects":[],"from":"a newer build"}}';
    const older = await context.newPage();
    const other = await context.newPage();
    await openBuilder(older);
    await openBuilder(other);
    await expect(onPage(older, NEWER_DATA)).toHaveCount(0);

    await other.evaluate((text) => localStorage.setItem('mk:index', text), newer);
    await expect.poll(() => hook(older, 'externalChanges')).toContain('Index');

    expect(await hook(older, 'readIndex')).toBe('0');
    expect(await hook(older, 'touchIndex')).toBe('Unavailable');
    expect(await hook(older, 'readIndex')).toBe('0');
    await expect(onPage(older, NEWER_DATA)).toHaveCount(1);
    expect(await older.evaluate(() => localStorage.getItem('mk:index'))).toBe(newer);
    expect(await older.evaluate(() => Object.keys(localStorage).filter((key) => key.startsWith('mk:quarantine:')))).toEqual(
      [],
    );
  });
});

/** The toast that says unreadable data was moved aside, `projects_problem_set_aside`. */
const SET_ASIDE = 'couldn’t be read, so it was set aside';

/** The banner that asks for a reload once a newer build saved data here, `projects_newer_data`. */
const NEWER_DATA = 'A newer version of the builder saved some of your work here';

/** The hint the builder dismisses by itself once the library switcher has pulsed. */
const SWITCHER_PULSE = 'switcher-pulse';

/**
 * Wait until [page] has pulsed the library switcher, or seen another tab do it, and stored that.
 * Under reduced motion the pulse stores itself as soon as it is due, and once stored it never runs again.
 */
async function settlePulse(page: Page): Promise<void> {
  await expect.poll(() => hook(page, 'seenHints'), { timeout: 30_000 }).toContain(SWITCHER_PULSE);
}

/**
 * Writes a key the builder never reads in [from] and waits for [to] to hear it. Storage events
 * reach another tab in the order the writes were made, so every write [from] made before it has
 * reached [to] by then.
 */
async function syncTabs(from: Page, to: Page): Promise<void> {
  const mark = `${Date.now()}-${Math.random()}`;
  const heard = to.evaluate(
    (value) =>
      new Promise<void>((resolve) => {
        const listen = (event: StorageEvent) => {
          if (event.key !== 'e2e:sync' || event.newValue !== value) return;
          window.removeEventListener('storage', listen);
          resolve();
        };
        window.addEventListener('storage', listen);
      }),
    mark,
  );
  await from.evaluate((value) => localStorage.setItem('e2e:sync', value), mark);
  await heard;
}

/** The hints in [hints] a user or a spec dismissed, leaving out the one the builder keeps by itself. */
function userHints(hints: string): string {
  return hints
    .split(',')
    .filter((hint) => hint !== '' && hint !== SWITCHER_PULSE)
    .join(',');
}

/** How many of [changes] were to the preferences. */
function prefsChanges(changes: string): number {
  return changes.split(',').filter((key) => key === 'Prefs').length;
}

/** Whatever in the page's accessibility tree holds [text]. */
function onPage(page: Page, text: string) {
  return page.locator('#cmp_a11y_root').getByText(text);
}

/** Fill localStorage until not even a few characters fit, and return how many filler keys it took. */
async function fillStorage(page: Page): Promise<number> {
  return page.evaluate(() => {
    let count = 0;
    let size = 1024 * 1024;
    while (size >= 4) {
      try {
        localStorage.setItem(`fill:${count}`, 'x'.repeat(size));
        count++;
      } catch {
        size = Math.floor(size / 2);
      }
    }
    return count;
  });
}
