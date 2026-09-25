import { expect, test, type Locator, type Page } from '@playwright/test';
import { clickMiddle, openBuilder, pressBareCanvas, SETTLE_MS, wantHooks } from './builder';

// The page's keyboard map on the real builder, read from the page's accessibility tree. Keys
// only reach the page once it has focus, so each test first presses the bare canvas, which also
// hands focus to the page's focus holder.
//
// The page writes its shortcuts with Cmd or Ctrl by its user agent, which the Desktop Chrome device
// fixes to Windows. `ControlOrMeta` follows the machine that runs the test instead, so the primary
// key here comes from the page's own user agent.

const A11Y = '#cmp_a11y_root';

/** The cheat sheet's line for screen reader users, `command_screen_reader_note`. */
const SCREEN_READER_NOTE =
  'With a screen reader, single-key shortcuts reach the page in focus mode, also called forms mode, and not in browse mode.';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('after a press on the bare canvas, Space shuffles the seed', async ({ page }) => {
  await openBuilder(page);
  const before = await seedText(page);

  await pressBareCanvas(page);
  await page.keyboard.press('Space');

  await expect.poll(() => seedText(page), { timeout: 10_000 }).not.toBe(before);
});

test('l typed in the seed field sets no lock', async ({ page }) => {
  await openBuilder(page);
  const field = await seedField(page);
  const unlocked = await lockState(page);

  await clickMiddle(page, field);
  await page.keyboard.type('l');
  // The l showing in the field says the key has been handled, the page's shortcuts included.
  await expect.poll(() => seedText(page), { timeout: 10_000 }).toContain('l');
  expect(await lockState(page)).toBe(unlocked);

  // The same key on the page does set it, so the check above can tell the two apart.
  await pressBareCanvas(page);
  await page.keyboard.press('l');
  await expect.poll(() => lockState(page), { timeout: 10_000 }).not.toBe(unlocked);
});

test('? opens the cheat sheet, and while it is open single keys and Space stay in it', async ({ page }) => {
  await openBuilder(page);
  const seed = await seedText(page);
  const unlocked = await lockState(page);
  await pressBareCanvas(page);

  await page.keyboard.press('?');
  const note = page.locator(A11Y).getByText(SCREEN_READER_NOTE, { exact: true });
  await expect(note).toHaveCount(1, { timeout: 10_000 });

  await page.keyboard.press('l');
  await page.keyboard.press('Space');
  // Focus stayed in the dialog, so it is still open and Esc still reaches it. Esc closing it also
  // says the two keys before it have been handled.
  await expect(note).toHaveCount(1);
  await page.keyboard.press('Escape');
  await expect(note).toHaveCount(0, { timeout: 10_000 });

  expect(await seedText(page)).toBe(seed);
  expect(await lockState(page)).toBe(unlocked);
});

test('Cmd or Ctrl with K, S and O belong to the page, and K opens the palette panel', async ({ page }) => {
  await openBuilder(page);
  await seedField(page);
  await pressBareCanvas(page);
  const primary = await primaryKey(page);
  // Added after the page's own listeners, so it hears each key once the page has handled it.
  await listenForKeys(page);

  await page.keyboard.press(`${primary}+s`);
  await page.keyboard.press(`${primary}+k`);
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBe('Palette');
  // The router names the palette on the key, before the palette has drawn, so its search field
  // showing is what says it is open. The palette owns the keyboard then, so Esc closes it first, and
  // its pane leaving the page's tree says focus is back on the page. Until then a key goes to the
  // search field, where Compose on the web hears it a frame late, too late to keep it from the browser.
  await expect(searchField(page)).toBeAttached({ timeout: 10_000 });
  await page.keyboard.press('Escape');
  await expect.poll(() => openOverlay(page), { timeout: 10_000 }).toBeNull();
  await expect(page.locator(A11Y).getByText(/^Command palette, dialog/)).toHaveCount(0, { timeout: 10_000 });
  await page.keyboard.press(`${primary}+o`);
  await expect(page.locator(A11Y).getByText(/^Projects, dialog/)).toHaveCount(1, { timeout: 10_000 });

  expect(await seenKeys(page)).toEqual(['s true', 'k true', 'o true']);
});

test('inside the palette, Cmd or Ctrl with S and O never reach the browser, and S saves', async ({ page }) => {
  await openBuilder(page);
  await seedField(page);
  await pressBareCanvas(page);
  const primary = await primaryKey(page);
  await page.keyboard.press(`${primary}+k`);
  await expect(searchField(page)).toBeAttached({ timeout: 10_000 });
  // A key pressed in the search field reaches Compose a frame late on the web, too late to keep it
  // from the browser, so focus moves down into the rows first.
  await page.keyboard.press('ArrowDown');
  await page.waitForTimeout(SETTLE_MS);
  await listenForKeys(page);
  // The poster keeps the save state in the Projects button's name, so the word itself is only the
  // toast that S raises.
  const projects = page.locator(`${A11Y} [aria-label^="Projects, "]`).first();
  const toast = page.locator(A11Y).getByText('Saved', { exact: true });
  const toastsBefore = await toast.count();

  await page.keyboard.press(`${primary}+s`);
  await page.keyboard.press(`${primary}+o`);

  await expect.poll(() => toast.count(), { timeout: 10_000 }).toBeGreaterThan(toastsBefore);
  await expect(projects).toHaveAttribute('aria-label', /, saved$/, { timeout: 10_000 });
  expect(await openOverlay(page)).toBe('Palette');
  await expect(page.locator(A11Y).getByText(/^Projects, dialog/)).toHaveCount(0);
  expect(await seenKeys(page)).toEqual(['s true', 'o true']);
});

test('after a number key switches the library, Space and V work with no click', async ({ page }) => {
  await openBuilder(page);
  const before = await seedText(page);
  await pressBareCanvas(page);
  const visionRow = page.locator(A11Y).getByText('Deuteranopia', { exact: true });
  await expect(visionRow).toHaveCount(0);

  await page.keyboard.press('3');
  // The top bar's Undo names the switch once it has landed, and the page moves into the new skin then.
  // The keys reach the page again once the canvas holds focus with that label still in place.
  await expect.poll(() => switchLanded(page), { timeout: 10_000 }).toBe(true);
  await page.keyboard.press('Space');
  await expect.poll(() => seedText(page), { timeout: 10_000 }).not.toBe(before);
  await page.keyboard.press('v');

  await expect(visionRow.first()).toBeAttached({ timeout: 10_000 });
});

/**
 * Whether Undo names the library change and the canvas holds focus, and both still hold two frames
 * later, so the new skin has settled around them.
 */
async function switchLanded(page: Page): Promise<boolean> {
  const undo = page.locator(A11Y).getByRole('button', { name: /^Undo library change/ });
  const holds = async () =>
    (await undo.count()) === 1 &&
    (await page.evaluate(() => {
      const host = document.activeElement;
      return (host?.shadowRoot?.activeElement ?? host)?.tagName;
    })) === 'CANVAS';
  if (!(await holds())) return false;
  await page.evaluate(
    () => new Promise((settled) => requestAnimationFrame(() => requestAnimationFrame(() => settled(null)))),
  );
  return holds();
}

/** The palette's search field, once it shows. */
function searchField(page: Page): Locator {
  return page.locator(A11Y).getByRole('textbox', { name: /^Search commands/ });
}

/** Notes each key down and whether the page kept it from the browser, once the page has handled it. */
async function listenForKeys(page: Page): Promise<void> {
  await page.evaluate(() => {
    const seen: string[] = [];
    (window as unknown as { __mkKeys: string[] }).__mkKeys = seen;
    window.addEventListener('keydown', (event) => seen.push(`${event.key.toLowerCase()} ${event.defaultPrevented}`));
  });
}

/** The keys [listenForKeys] noted, K, S and O only. */
async function seenKeys(page: Page): Promise<string[]> {
  return page.evaluate(() =>
    (window as unknown as { __mkKeys: string[] }).__mkKeys.filter((entry) => /^[kso] /.test(entry)),
  );
}

/** The seed field, once the page has drawn it. */
async function seedField(page: Page): Promise<Locator> {
  const field = page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ });
  await expect(field).toBeAttached({ timeout: 30_000 });
  await expect.poll(async () => (await field.boundingBox())?.height ?? 0, { timeout: 10_000 }).toBeGreaterThan(0);
  return field;
}

/** What the seed field shows, the seed's hex. */
async function seedText(page: Page): Promise<string> {
  return (await (await seedField(page)).textContent()) ?? '';
}

/** Every ARIA attribute of the poster's hue lock, which change when it is turned on. */
async function lockState(page: Page): Promise<string> {
  const name = /^Lock hue/;
  const scope = page.locator(A11Y);
  const lock = scope
    .getByRole('checkbox', { name })
    .or(scope.getByRole('switch', { name }))
    .or(scope.getByRole('button', { name }))
    .first();
  return lock.evaluate((element) =>
    Array.from(element.attributes)
      .filter((attribute) => attribute.name.startsWith('aria-'))
      .map((attribute) => `${attribute.name}=${attribute.value}`)
      .sort()
      .join(' '),
  );
}

/** `Meta` when the page's user agent names an Apple system, where it takes Cmd, else `Control`. */
async function primaryKey(page: Page): Promise<'Meta' | 'Control'> {
  const apple = await page.evaluate(() => /Macintosh|Mac OS|iPhone|iPad|iPod|Darwin/.test(navigator.userAgent));
  return apple ? 'Meta' : 'Control';
}

/** The overlay the page's router last put in the history, the open panel's name. */
async function openOverlay(page: Page): Promise<string | null> {
  return page.evaluate(() => (history.state as { mkOverlay?: string } | null)?.mkOverlay ?? null);
}
