import { readFileSync } from 'node:fs';
import path from 'node:path';
import { expect, type Locator, type Page } from '@playwright/test';
import { hook, openBuilder } from '../tests/builder';

// What the flow specs share. Compose draws on a canvas, so every control is found in the page's
// accessibility mirror and pressed where the canvas draws it. Every wait here waits on something the
// page shows or stores, never on a fixed time.

export const A11Y = '#cmp_a11y_root';

/** Long enough for wasm to boot on a busy machine. */
export const BOOT_TIMEOUT_MS = 30_000;

/** Whatever in the mirror holds [text], word for word when it is a string. */
export function onPage(page: Page, text: string | RegExp): Locator {
  return page.locator(A11Y).getByText(text, { exact: typeof text === 'string' });
}

/** The buttons in the mirror called [name], word for word when it is a string. */
export function button(page: Page, name: string | RegExp): Locator {
  return page.locator(A11Y).getByRole('button', { name, exact: typeof name === 'string' });
}

/** Whatever in the mirror has an `aria-label` that starts with [prefix]. */
export function labelled(page: Page, prefix: string): Locator {
  return page.locator(`${A11Y} [aria-label^="${prefix.replace(/"/g, '\\"')}"]`);
}

/** The seed field, whose text is the seed's hex. */
export function seedField(page: Page): Locator {
  return page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ });
}

/** What the seed field shows, upper case. */
export async function seedText(page: Page): Promise<string> {
  return ((await seedField(page).textContent()) ?? '').trim().toUpperCase();
}

/** Opens [route] and waits until the poster has drawn its seed field. */
export async function openWorkspace(page: Page, route = '/'): Promise<void> {
  await openBuilder(page, route);
  await boxOf(seedField(page), BOOT_TIMEOUT_MS);
}

export interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Where the canvas draws [target], once it has given it a size, within [timeout] or the expect default. */
export async function boxOf(target: Locator, timeout?: number): Promise<Box> {
  const first = target.first();
  let box: Box | null = null;
  await expect
    .poll(
      async () => {
        box = await first.boundingBox();
        return box?.height ?? 0;
      },
      { timeout },
    )
    .toBeGreaterThan(0);
  // The box the poll saw, since a second read can miss it while the canvas lays out again.
  return box!;
}

/**
 * How many animation frames in a row a box has to hold still to count as laid out, about 1.2 s at
 * 60 fps, longer than the mirror's longest wait before it syncs. A page starved of frames takes
 * longer, and a tab in the background gets none.
 */
const STILL_FRAMES = 72;

/**
 * The box of [target] once it is laid out, when it has a height and has held still for
 * `STILL_FRAMES` of the browser's animation frames, which tick whether or not Compose drew. A
 * scroll, a docked panel or a dialog that is still moving moves the mirror's box with it, and a
 * press read off the box before then lands beside it. Pass a longer [timeout] where frames come
 * slowly.
 */
export async function settledBox(target: Locator, timeout?: number): Promise<Box> {
  const page = target.page();
  let last: Box | null = null;
  let since = 0;
  await expect
    .poll(
      async () => {
        const frame = await animationFrames(page);
        const box = await target.boundingBox();
        if (box === null || box.height === 0 || frame < since) {
          last = null;
          return false;
        }
        if (last === null || !sameBox(box, last)) {
          last = box;
          since = frame;
          return false;
        }
        return frame - since >= STILL_FRAMES;
      },
      { timeout, intervals: [100], message: 'The box never held still' },
    )
    .toBe(true);
  return last!;
}

/**
 * How many animation frames the browser has run since the count started. The first call starts it,
 * and it stops once nothing has asked for five seconds, so the next call starts again from nothing.
 */
async function animationFrames(page: Page): Promise<number> {
  return page.evaluate(() => {
    const host = window as unknown as { __mkFrames?: { count: number; askedAt: number } };
    let frames = host.__mkFrames;
    if (!frames) {
      const counting = { count: 0, askedAt: 0 };
      const tick = () => {
        counting.count++;
        if (performance.now() - counting.askedAt < 5_000) requestAnimationFrame(tick);
        else delete host.__mkFrames;
      };
      host.__mkFrames = counting;
      frames = counting;
      requestAnimationFrame(tick);
    }
    frames.askedAt = performance.now();
    return frames.count;
  });
}

function sameBox(one: Box, two: Box): boolean {
  return (
    Math.abs(one.x - two.x) < 0.5 &&
    Math.abs(one.y - two.y) < 0.5 &&
    Math.abs(one.width - two.width) < 0.5 &&
    Math.abs(one.height - two.height) < 0.5
  );
}

/** Waits for the page to draw [count] more frames, so Compose has taken whatever came in before. */
export async function nextFrames(page: Page, count = 2): Promise<void> {
  await page.evaluate(
    (frames) =>
      new Promise<void>((resolve) => {
        let left = frames;
        const step = () => (--left <= 0 ? resolve() : requestAnimationFrame(step));
        requestAnimationFrame(step);
      }),
    count,
  );
}

/** Clicks the middle of [target] where the canvas draws it. */
export async function press(page: Page, target: Locator): Promise<void> {
  const box = await boxOf(target);
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
}

/** Clicks the middle of [target] once it holds still, for a control that slides or fades in. */
export async function pressSettled(page: Page, target: Locator): Promise<void> {
  const box = await settledBox(target.first());
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
}

/** Clicks [target], then waits for [shows] to be in the mirror. */
export async function pressFor(page: Page, target: Locator, shows: Locator): Promise<void> {
  await press(page, target);
  await expect(shows.first()).toBeAttached();
}

/**
 * Types [hex] into the seed field and commits it with Enter, then waits for the field to show it.
 * The press puts Compose's backing field in the page, and the keys wait for that to have focus.
 */
export async function typeSeed(page: Page, hex: string): Promise<void> {
  await typeInto(page, seedField(page), hex);
  await page.keyboard.press('Enter');
  await expect.poll(() => seedText(page)).toContain(hex.replace('#', '').toUpperCase());
}

/**
 * Presses the canvas where nothing is, halfway between the top bar's last button and the window's
 * edge on the tab row, and waits for the canvas to hold the page's focus, so the next key reaches
 * the page's own shortcuts.
 */
export async function focusCanvas(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (!viewport) throw new Error('The page has no viewport');
  const tab = await boxOf(labelled(page, 'Contrast, tab, '), BOOT_TIMEOUT_MS);
  await page.mouse.click((tab.x + tab.width + viewport.width) / 2, tab.y + tab.height / 2);
  await canvasHoldsFocus(page);
}

/**
 * Presses the canvas where nothing is, in the preview's header row halfway from its last tab to the
 * window's edge, and waits for the canvas to hold the page's focus.
 */
export async function pressBareCanvas(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (!viewport) throw new Error('The page has no viewport');
  const tabs = page.locator(A11Y).getByText('Contrast', { exact: true });
  await expect(tabs.first()).toBeAttached({ timeout: BOOT_TIMEOUT_MS });
  const boxes = (await Promise.all((await tabs.all()).map((tab) => tab.boundingBox()))).filter(
    (box): box is Box => box !== null && box.x > viewport.width / 3,
  );
  if (boxes.length === 0) throw new Error('The preview has no Contrast tab on screen');
  const tab = boxes.reduce((top, box) => (box.y < top.y ? box : top));
  await page.mouse.click((tab.x + tab.width + viewport.width) / 2, tab.y + tab.height / 2);
  await canvasHoldsFocus(page);
}

/**
 * Waits for the page's focus holder to have Compose focus with no text field under it. The holder
 * claims a bare press a couple of frames after the release, and a key sent before then lands nowhere.
 */
async function canvasHoldsFocus(page: Page): Promise<void> {
  await expect(page.locator('.compose-backing-field')).toHaveCount(0);
  await expect.poll(() => hook(page, 'holderFocused')).toBe('true');
}

/** Focuses the canvas, presses [key] once and waits for [landed] to say it has. */
export async function pressKey(page: Page, key: string, landed: () => Promise<boolean>): Promise<void> {
  await focusCanvas(page);
  await page.keyboard.press(key);
  await expect.poll(landed).toBe(true);
}

/** Focuses the canvas, presses [key] once and waits for [shows] to be in the mirror. */
export async function pressKeyFor(page: Page, key: string, shows: Locator): Promise<void> {
  await focusCanvas(page);
  await page.keyboard.press(key);
  await expect(shows.first()).toBeAttached();
}

/**
 * Clicks the middle of text field [field] once it holds still, since a panel that just closed can
 * still be sliding it into place, and waits for Compose's backing field to take the keys.
 */
export async function focusField(page: Page, field: Locator): Promise<void> {
  await pressSettled(page, field);
  await expect(page.locator('.compose-backing-field')).toBeFocused();
}

/** `Meta` when the page's user agent names an Apple system, where it takes Cmd, else `Control`. */
export async function primaryKey(page: Page): Promise<'Meta' | 'Control'> {
  const apple = await page.evaluate(() => /Macintosh|Mac OS|iPhone|iPad|iPod|Darwin/.test(navigator.userAgent));
  return apple ? 'Meta' : 'Control';
}

/** The overlay the page's router last put in the history, the open panel's name. */
export async function openOverlay(page: Page): Promise<string | null> {
  return page.evaluate(() => (history.state as { mkOverlay?: string } | null)?.mkOverlay ?? null);
}

/** One project in the stored index. */
export interface StoredProject {
  id: string;
  name: string;
  library: string;
}

/** The projects the stored index lists, most recent first as the page keeps them. */
export async function storedProjects(page: Page): Promise<StoredProject[]> {
  return page.evaluate(() => {
    const text = localStorage.getItem('mk:index');
    return text === null ? [] : (JSON.parse(text).data.projects as StoredProject[]);
  });
}

/** The stored preferences, as the page wrote them. */
export async function storedPrefs(page: Page): Promise<Record<string, unknown>> {
  return page.evaluate(() => JSON.parse(localStorage.getItem('mk:prefs') ?? '{"data":{}}').data);
}

/** One case of `builder/fixtures/share-codes.json`, the share codes every part of the builder agrees on. */
export interface ShareVector {
  label: string;
  code: string;
  seedHex: string;
  library: string;
  style: string;
  projectName: string | null;
}

export function shareVectors(): ShareVector[] {
  return JSON.parse(readFileSync(path.resolve(__dirname, '../../fixtures/share-codes.json'), 'utf8'));
}

/** The document of the project the page last opened, as autosave stored it. */
export async function storedDocument(page: Page): Promise<Record<string, unknown> | null> {
  return page.evaluate(() => {
    const id = JSON.parse(localStorage.getItem('mk:prefs') ?? '{"data":{}}').data.lastProjectId;
    const text = id ? localStorage.getItem(`mk:project:${id}`) : null;
    return text === null ? null : JSON.parse(text).data.document;
  });
}

/**
 * Types [text] into the text field [field] in place of what it holds. The press puts Compose's
 * backing field in the page, and the keys wait for that to have focus.
 */
export async function typeInto(page: Page, field: Locator, text: string): Promise<void> {
  await focusField(page, field);
  // Select all is Cmd or Ctrl by the host system in Compose, not by the page's user agent, so the
  // old text goes a key at a time from wherever the press left the caret.
  const old = ((await field.first().textContent()) ?? '').length;
  for (let key = 0; key < old; key++) {
    await page.keyboard.press('Delete');
    await page.keyboard.press('Backspace');
  }
  await expect.poll(async () => (await field.first().textContent())?.trim()).toBe('');
  await page.keyboard.type(text);
  await expect.poll(async () => (await field.first().textContent())?.trim()).toBe(text);
}

/** Turns the mouse wheel over [over] until the canvas draws [target] inside it. */
export async function scrollTo(page: Page, target: Locator, over: Locator): Promise<void> {
  const area = await boxOf(over);
  await page.mouse.move(area.x + area.width / 2, area.y + area.height / 2);
  await expect
    .poll(
      async () => {
        const box = await target.first().boundingBox();
        if (box !== null && box.height > 0 && box.y >= area.y && box.y + box.height <= area.y + area.height) return true;
        await page.mouse.wheel(0, 120);
        return false;
      },
      // WebKit moves the mirror only once a scroll settles, so each turn waits for that.
      { intervals: [500] },
    )
    .toBe(true);
}

/** The preview's split handle, whose text names the split and the side it is on, `Split, 50% Light` at first. */
export const SPLIT_HANDLE = /^Split, \d+%/;

/** Drags the split handle [dx] CSS pixels across, in small steps as a hand would. */
export async function dragSplit(page: Page, dx: number): Promise<void> {
  const box = await boxOf(onPage(page, SPLIT_HANDLE));
  const x = box.x + box.width / 2;
  const y = box.y + box.height / 2;
  await page.mouse.move(x, y);
  await page.mouse.down();
  await page.mouse.move(x + dx, y, { steps: 30 });
  await page.mouse.up();
}
