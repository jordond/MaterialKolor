import { expect, test, type Locator, type Page } from '@playwright/test';
import { wantHooks } from './builder';
import {
  boxOf,
  button,
  LAND_TIMEOUT_MS,
  onPage,
  openWorkspace,
  press,
  scrollTo,
  seedField,
  seedText,
  storedProjects,
  typeSeed,
} from '../fixtures/workspace';

// Two tabs on one project. An edit in one tab puts a banner in the other, which offers Load
// latest and Keep mine. storage.spec.ts covers the storage event underneath through the hooks.
// The banner only comes up in a tab that edited in the last 2 s, so neither edit is lost. A tab that
// was only looking takes the other tab's edit as an undo step, the way ProjectSession says.

/** `projects_conflict`, word for word. */
const CONFLICT = 'This project changed in another tab';

/** The Fine-tune button at the poster's foot, which opens the sheet that holds the extra colors. */
const FINE_TUNE = /^Fine-tune, /;

/** The first extra color's light tone slider, which the mirror writes as text. */
const TONE_SLIDER = /light color tone, slider/;

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
  // The reader holds an extra color's tone slider and keeps it moving. Each step is an edit that
  // saves nothing until it lets go, so the writer's save lands in a tab that edited in the last 2 s
  // however busy the machine, and the writer never hears from the reader.
  // Contrast is four choices now. The seed picker's drag stops once the other tab takes the
  // keys, its save then comes in as an undo step, so the writer adds the extra color and the reader
  // takes it in before it holds the slider.
  // The Fine-tune sheet takes the poster under it out of the tree, so the writer closes it again
  // before it types the seed.
  await openFineTune(writer);
  await scrollTo(writer, button(writer, 'Add extra color'), poster(writer));
  await press(writer, button(writer, 'Add extra color'));
  await writer.keyboard.press('Escape');
  await scrollBackUp(writer);
  await openFineTune(reader);
  const slider = onPage(reader, TONE_SLIDER);
  await expect(slider.first()).toBeAttached({ timeout: LAND_TIMEOUT_MS });
  await scrollTo(reader, slider, poster(reader));
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
  await reader.keyboard.press('Escape');
  expect(await seedText(reader)).not.toBe('#0B6E4F');
  await expect(button(reader, 'Keep mine')).toHaveCount(1);
  await press(reader, button(reader, 'Load latest'));
  await expect.poll(() => seedText(reader), { timeout: LAND_TIMEOUT_MS }).toBe('#0B6E4F');
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);
});

/** The poster's scrolling pane, named by its group label. */
function poster(page: Page): Locator {
  return onPage(page, /^Seed and theme controls/);
}

/** Scrolls the poster down to the Fine-tune button and opens the sheet. The sheet then scrolls under the same wheel. */
async function openFineTune(page: Page): Promise<void> {
  await scrollTo(page, button(page, FINE_TUNE), poster(page));
  await press(page, button(page, FINE_TUNE));
}

/** Scrolls the poster back up until the seed field is on screen, for [typeSeed]. */
async function scrollBackUp(page: Page): Promise<void> {
  const area = await boxOf(poster(page));
  await page.mouse.move(area.x + area.width / 2, area.y + area.height / 2);
  await expect
    .poll(
      async () => {
        const box = await seedField(page).boundingBox();
        if (box !== null && box.height > 0 && box.y >= area.y) return true;
        await page.mouse.wheel(0, -240);
        return false;
      },
      { timeout: LAND_TIMEOUT_MS, intervals: [500] },
    )
    .toBe(true);
}

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
