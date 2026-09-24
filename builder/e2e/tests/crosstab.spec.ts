import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import {
  button,
  LAND_TIMEOUT_MS,
  onPage,
  openWorkspace,
  press,
  seedField,
  seedText,
  storedProjects,
  typeInto,
  typeSeed,
} from '../fixtures/workspace';

// b-503
// Two tabs on one project (F-30). An edit in one tab puts a banner in the other, which offers Load
// latest and Keep mine. storage.spec.ts covers the storage event underneath through the hooks.
// b-503b
// The banner only comes up in a tab that edited in the last 2 s, so neither edit is lost. A tab that
// was only looking takes the other tab's edit as an undo step, the way ProjectSession says.

/** `projects_conflict`, word for word. */
const CONFLICT = 'This project changed in another tab';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('an edit in one tab lands in the other as a step its Undo takes back', async ({ context }) => {
  const { writer, reader } = await twoTabs(context.newPage.bind(context));
  const first = await seedText(reader);

  await typeSeed(writer, '#0B6E4F');

  await expect.poll(() => seedText(reader), { timeout: LAND_TIMEOUT_MS }).toBe('#0B6E4F');
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);
  await press(reader, button(reader, /^Undo\b/).and(reader.locator(':not([aria-label$="disabled"])')));
  await expect.poll(() => seedText(reader), { timeout: LAND_TIMEOUT_MS }).toBe(first);
});

test('an edit in one tab offers the other the latest while it edits too, and Load latest takes it', async ({
  context,
}) => {
  const { writer, reader } = await twoTabs(context.newPage.bind(context));
  // A field commits a color 400 ms after it reads as one, so both hold one digit short until the
  // writer commits and the reader straight after, well inside the writer's autosave delay. The
  // writer's save then reaches a tab that has just edited.
  await typeInto(writer, seedField(writer), '#0B6E4');
  await typeInto(reader, seedField(reader), '#8E24A');
  await writer.keyboard.press('F');
  await writer.keyboard.press('Enter');
  await reader.keyboard.press('A');
  await reader.keyboard.press('Enter');

  await expect(onPage(reader, CONFLICT)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  expect(await seedText(reader)).toBe('#8E24AA');
  await expect(button(reader, 'Keep mine')).toHaveCount(1);
  await press(reader, button(reader, 'Load latest'));
  await expect.poll(() => seedText(reader), { timeout: LAND_TIMEOUT_MS }).toBe('#0B6E4F');
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);
});

/** Opens the same project in two tabs, the writer first, once it is stored. */
async function twoTabs(newPage: () => Promise<Page>): Promise<{ writer: Page; reader: Page }> {
  const writer = await newPage();
  await openWorkspace(writer);
  await expect.poll(async () => (await storedProjects(writer)).length).toBe(1);
  const reader = await newPage();
  await openWorkspace(reader);
  expect(await seedText(reader)).toBe(await seedText(writer));
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);
  return { writer, reader };
}
