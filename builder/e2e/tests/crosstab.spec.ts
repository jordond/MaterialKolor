import { expect, test, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import {
  boxOf,
  button,
  LAND_TIMEOUT_MS,
  onPage,
  openWorkspace,
  press,
  scrollTo,
  seedText,
  storedProjects,
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

test('an edit in one tab offers the other the latest while it drags, and Load latest takes it', async ({ context }) => {
  const { writer, reader } = await twoTabs(context.newPage.bind(context));
  // The reader holds the contrast slider and keeps it moving. Each step is an edit that saves
  // nothing until it lets go, so the writer's save lands in a tab that edited in the last 2 s
  // however busy the machine, and the writer never hears from the reader.
  const slider = onPage(reader, /^Contrast level, slider/);
  await scrollTo(reader, slider, onPage(reader, /^Seed and theme controls/));
  const track = await boxOf(slider);
  const middle = { x: track.x + track.width / 2, y: track.y + track.height / 2 };
  await reader.mouse.move(middle.x, middle.y);
  await reader.mouse.down();
  let holding = true;
  const moving = (async () => {
    for (let step = 0; holding; step++) {
      await reader.mouse.move(middle.x + (step % 2 === 0 ? 12 : -12), middle.y, { steps: 3 });
      await reader.waitForTimeout(200);
    }
  })();

  try {
    await typeSeed(writer, '#0B6E4F');
    await expect(onPage(reader, CONFLICT)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  } finally {
    holding = false;
    await moving;
    await reader.mouse.up();
  }
  expect(await seedText(reader)).not.toBe('#0B6E4F');
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
