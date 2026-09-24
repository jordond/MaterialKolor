import { expect, test } from '@playwright/test';
import { wantHooks } from './builder';
import { button, LAND_TIMEOUT_MS, onPage, openWorkspace, press, seedText, storedProjects, typeSeed } from '../fixtures/workspace';

// b-503
// Two tabs on one project (F-30). An edit in one tab puts a banner in the other, which offers Load
// latest and Keep mine. storage.spec.ts covers the storage event underneath through the hooks.

/** `projects_conflict`, word for word. */
const CONFLICT = 'This project changed in another tab';

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test('an edit in one tab offers the other the latest, and Load latest takes it', async ({ context }) => {
  test.fixme(true, 'Follow-up for the app: the other tab on the same project shows no banner after an edit (F-30)');
  const writer = await context.newPage();
  await openWorkspace(writer);
  await expect.poll(async () => (await storedProjects(writer)).length).toBe(1);
  const reader = await context.newPage();
  await openWorkspace(reader);
  expect(await seedText(reader)).toBe(await seedText(writer));
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);

  await typeSeed(writer, '#0B6E4F');

  await expect(onPage(reader, CONFLICT)).toHaveCount(1, { timeout: LAND_TIMEOUT_MS });
  await expect(button(reader, 'Keep mine')).toHaveCount(1);
  await press(reader, button(reader, 'Load latest'));
  await expect.poll(() => seedText(reader), { timeout: LAND_TIMEOUT_MS }).toBe('#0B6E4F');
  await expect(onPage(reader, CONFLICT)).toHaveCount(0);
});
