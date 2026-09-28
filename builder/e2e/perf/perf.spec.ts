import { expect, test, type Browser, type BrowserContext, type Locator, type Page } from '@playwright/test';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { clickMiddle, pressBareCanvas } from '../tests/builder';
import { servePerfSite, type PerfSite } from './serve-compressed';

// The perf run against the budgets in `budgets.json`.
// It only reports. Each number lands in `report/perf-report.json` and `report/summary.md` with its
// budget, and a number it cannot take is null with the reason. Nothing here fails over a slow number,
// only over a run that could not start. `first-visit-bytes` is gated by checkBudget in builder/apps/web.
//
// Timings come from the page's own clock. The app leaves `mk:` marks through `Environment.mark`, a
// wrapper around `requestAnimationFrame` times the work Compose does in each frame, and the Event
// Timing API gives each interaction's time to the next paint. Playwright's Chromium draws WebGL in
// software, so frame numbers here are upper bounds for a machine with a GPU.

const A11Y = '#cmp_a11y_root';
const REPORT_DIR = path.join(__dirname, 'report');
const PHOTO = path.join(__dirname, '../fixtures/photo-12mp.jpg');
const SEED_CHANGES = 30;
const DRAG_STEPS = 60;
const REVEAL_MS = 900;

interface Budget {
  id: string;
  what: string;
  unit: string;
  max?: number;
  min?: number;
}

interface Metric extends Budget {
  value: number | null;
  within: boolean | null;
  note?: string;
}

const config = JSON.parse(readFileSync(path.join(__dirname, 'budgets.json'), 'utf8')) as {
  network: { downloadMbps: number; rttMs: number };
  budgets: Budget[];
};
const taken = new Map<string, { value: number | null; note?: string }>();
let site: PerfSite;

test.describe.configure({ mode: 'serial' });

test.beforeAll(async () => {
  test.setTimeout(600_000);
  const root = process.env.MK_E2E_SITE_DIR;
  if (!root) throw new Error('MK_E2E_SITE_DIR is not set, the global setup did not run');
  site = await servePerfSite(root);
});

test.afterAll(async ({ browser }) => {
  await site?.close();
  writeReport(browser);
});

test('a cold and a repeat visit at 9 Mbps and 60 ms RTT', async ({ browser }) => {
  const context = await newContext(browser);
  const page = await context.newPage();
  const cdp = await context.newCDPSession(page);
  await cdp.send('Network.enable');
  await cdp.send('Network.emulateNetworkConditions', {
    offline: false,
    latency: config.network.rttMs,
    downloadThroughput: (config.network.downloadMbps * 1_000_000) / 8,
    uploadThroughput: (config.network.downloadMbps * 1_000_000) / 8,
  });

  await measure(['splash-paint', 'first-frame-throttled', 'other-origin-requests'], async () => {
    const cold = await visit(page);
    take('splash-paint', cold.fcp, 'The splash is plain HTML and CSS, so its first paint is the page first contentful paint.');
    take('first-frame-throttled', cold.firstFrame);
    take('other-origin-requests', cold.otherOrigins);
  });
  await measure(['repeat-visit-frame'], async () => {
    take('repeat-visit-frame', (await visit(page)).firstFrame, 'The same tab opens the page again, so the hashed assets come from the cache.');
  });
  await context.close();
});

test('a broadband visit, then seed changes, drags, a library switch and a photo', async ({ browser }) => {
  const context = await newContext(browser);
  const page = await context.newPage();
  const cdp = await context.newCDPSession(page);

  await measure(['first-visit-bytes', 'first-frame-broadband'], async () => {
    const cold = await visit(page);
    take('first-visit-bytes', cold.bytes, `Encoded body bytes of every response before the first frame, sent as ${[...site.encodings].join(' and ')}.`);
    take('first-frame-broadband', cold.firstFrame, 'Served from this machine with no throttling.');
  });
  await expect(page.locator(A11Y).getByRole('textbox', { name: /^Seed color/ })).toHaveCount(1, { timeout: 30_000 });

  await measure(['theme-resolve'], async () => {
    await pressBareCanvas(page);
    const since = await now(page);
    for (let change = 0; change < SEED_CHANGES; change++) {
      await page.keyboard.press('Space');
      await page.waitForTimeout(200);
    }
    const durations = resolveDurations(await marks(page), since);
    if (durations.length === 0) throw new Error('No mk:resolve-start and mk:resolve pair came');
    take(
      'theme-resolve',
      p95(durations),
      `${durations.length} resolves over ${SEED_CHANGES} shuffles of the seed. The page clock ticks in 0.1 ms steps.`,
    );
  });

  await measure(['seed-drag-frame-work'], async () => {
    // Contrast is four choices now, so the drag that changes the whole theme every frame is
    // the seed picker's. Its hue moves the seed on each step, and Esc cancels the picker after.
    const pick = page.locator(A11Y).getByRole('button', { name: 'Pick', exact: true });
    await expect(pick).toHaveCount(1, { timeout: 10_000 });
    await clickMiddle(page, pick);
    const frames = await dragAcross(page, page.locator(A11Y).getByText(/^Hue, slider, /), 0.8);
    await page.keyboard.press('Escape');
    await expect(page.locator(A11Y).getByRole('button', { name: 'Done', exact: true })).toHaveCount(0, {
      timeout: 10_000,
    });
    take('seed-drag-frame-work', p95(frames.work), `The seed picker's hue track. ${framesNote(frames)}`);
  });

  await measure(['split-drag-frame-work', 'split-drag-recompositions'], async () => {
    const frames = await dragAcross(page, page.locator(A11Y).getByText(/^Split, \d+% Light$/, { exact: true }), 0);
    take('split-drag-frame-work', p95(frames.work), framesNote(frames));
    take(
      'split-drag-recompositions',
      null,
      'The page cannot see recompositions. SplitHandle reads the split only in placement and drawing, which its JVM tests hold.',
    );
  });

  await measure(['switch-reveal-fps'], async () => {
    // The first switch to Fluent also builds its preview panes, so it is timed on its own and the
    // reveal is timed on the second.
    const first = await switchLibrary(page, '4');
    await switchLibrary(page, '1');
    const frames = await switchLibrary(page, '4');
    await switchLibrary(page, '1');
    const firstWorst = Math.max(0, ...intervalsOf(first.frames)).toFixed(1);
    take(
      'switch-reveal-fps',
      fps(frames.busy),
      `The second switch to Fluent, key 4, frames with work until ${REVEAL_MS} ms after it landed. ` +
        `${framesNote(frames)} The first switch's worst frame took ${firstWorst} ms.`,
    );
  });

  await measure(['photo-candidates', 'photo-thumbnail'], async () => {
    await page.route('**/__perf/photo-12mp.jpg', (route) =>
      route.fulfill({ contentType: 'image/jpeg', body: readFileSync(PHOTO) }),
    );
    const since = await page.evaluate(async () => {
      const blob = await (await fetch('/__perf/photo-12mp.jpg')).blob();
      const file = new File([blob], 'photo-12mp.jpg', { type: 'image/jpeg' });
      const transfer = new DataTransfer();
      transfer.items.add(file);
      const enter = new Event('dragenter', { bubbles: true, cancelable: true });
      Object.defineProperty(enter, 'dataTransfer', { value: transfer });
      document.body.dispatchEvent(enter);
      const drop = new Event('drop', { bubbles: true, cancelable: true });
      Object.defineProperty(drop, 'dataTransfer', { value: transfer });
      const start = performance.now();
      document.body.dispatchEvent(drop);
      return start;
    });
    await expect
      .poll(async () => (await marks(page)).some((mark) => mark.name === 'mk:extract' && mark.time > since), {
        timeout: 30_000,
      })
      .toBe(true);
    const after = (await marks(page)).filter((mark) => mark.time > since);
    const first = (name: string) => after.find((mark) => mark.name === name)?.time ?? null;
    const extract = first('mk:extract');
    const thumbnail = first('mk:thumbnail');
    take('photo-candidates', extract === null ? null : extract - since, 'From the drop event to the candidates in the image model.');
    take('photo-thumbnail', thumbnail === null ? null : thumbnail - since, 'From the drop event to the thumbnail in the image model.');
  });

  await measure(['slowest-interaction'], async () => {
    const durations = await page.evaluate(() => window.__mkPerf!.interactions);
    if (durations.length === 0) throw new Error('The Event Timing API gave no interaction');
    take('slowest-interaction', Math.max(...durations), `The slowest of ${durations.length} interactions over 16 ms, clicks and keys.`);
  });

  await measure(['heap'], async () => {
    // A forced collection first, so the reading is what stays alive rather than how long ago the
    // last collection ran. Without it the same build read anywhere from 34 to 105 MB.
    await cdp.send('HeapProfiler.collectGarbage');
    const heap = (await cdp.send('Runtime.getHeapUsage')) as { usedSize: number; backingStorageSize?: number };
    take(
      'heap',
      heap.usedSize + (heap.backingStorageSize ?? 0),
      'Used JS heap after a forced collection, which holds the wasm GC objects, plus array buffer storage, which holds Skia memory. Desktop, not mobile.',
    );
  });
  await context.close();
});

declare global {
  interface Window {
    __mkPerf?: {
      marks: { name: string; time: number }[];
      interactions: number[];
      start: () => void;
      stop: () => Frames;
    };
  }
}

interface Frames {
  frames: number[];
  work: number[];
  /** When each frame with work ran. */
  busy: number[];
}

interface Visit {
  fcp: number | null;
  firstFrame: number;
  bytes: number;
  otherOrigins: number;
}

async function newContext(browser: Browser): Promise<BrowserContext> {
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });
  await context.addInitScript(instrument);
  return context;
}

/** Open the site in [page] and read the load's numbers once the first frame is up. */
async function visit(page: Page): Promise<Visit> {
  await page.goto(site.url);
  await page.waitForFunction(() => window.__mkPerf?.marks.some((mark) => mark.name === 'mk:first-frame'), null, {
    timeout: 120_000,
  });
  return page.evaluate(() => {
    const firstFrame = window.__mkPerf!.marks.find((mark) => mark.name === 'mk:first-frame')!.time;
    const fcp = performance.getEntriesByName('first-contentful-paint')[0]?.startTime ?? null;
    const resources = (performance.getEntriesByType('resource') as PerformanceResourceTiming[]).filter(
      (entry) => entry.startTime < firstFrame,
    );
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    const bytes = [navigation, ...resources].reduce((sum, entry) => sum + entry.encodedBodySize, 0);
    const otherOrigins = resources.filter((entry) => new URL(entry.name).origin !== location.origin).length;
    return { fcp, firstFrame, bytes, otherOrigins };
  });
}

/** Drag the middle of [target] sideways across [share] of the window, or 200 px when that is 0. */
async function dragAcross(page: Page, target: Locator, share: number): Promise<Frames> {
  await expect.poll(async () => (await target.first().boundingBox())?.height ?? 0, { timeout: 15_000 }).toBeGreaterThan(0);
  const box = (await target.first().boundingBox())!;
  const distance = share > 0 ? box.width * share : 200;
  const x = box.x + (share > 0 ? box.width * 0.1 : box.width / 2);
  const y = box.y + box.height / 2;
  await page.mouse.move(x, y);
  await page.mouse.down();
  await page.evaluate(() => window.__mkPerf!.start());
  for (let step = 1; step <= DRAG_STEPS; step++) {
    await page.mouse.move(x + (distance * step) / DRAG_STEPS, y);
    await page.waitForTimeout(16);
  }
  const frames = await stopFrames(page);
  await page.mouse.up();
  return frames;
}

/** Press [key] on the bare canvas and record frames until [REVEAL_MS] after Undo names a library switch. */
async function switchLibrary(page: Page, key: string): Promise<Frames> {
  await pressBareCanvas(page);
  await page.evaluate(() => window.__mkPerf!.start());
  await page.keyboard.press(key);
  const undo = page.locator(A11Y).getByRole('button', { name: /^Undo library change/ });
  await expect(undo).toHaveCount(1, { timeout: 10_000 });
  await page.waitForTimeout(REVEAL_MS);
  return stopFrames(page);
}

function intervalsOf(times: number[]): number[] {
  return times.slice(1).map((time, index) => time - times[index]);
}

async function stopFrames(page: Page): Promise<Frames> {
  return page.evaluate(() => window.__mkPerf!.stop());
}

async function marks(page: Page): Promise<{ name: string; time: number }[]> {
  return page.evaluate(() => window.__mkPerf!.marks);
}

async function now(page: Page): Promise<number> {
  return page.evaluate(() => performance.now());
}

/** How long each resolve after [since] took, from its `mk:resolve-start` to the `mk:resolve` after it. */
function resolveDurations(all: { name: string; time: number }[], since: number): number[] {
  const durations: number[] = [];
  let start: number | null = null;
  for (const mark of all.filter((entry) => entry.time >= since)) {
    if (mark.name === 'mk:resolve-start') start = mark.time;
    if (mark.name === 'mk:resolve' && start !== null) {
      durations.push(mark.time - start);
      start = null;
    }
  }
  return durations;
}

function p95(values: number[]): number | null {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.ceil(sorted.length * 0.95) - 1)];
}

function fps(frames: number[]): number | null {
  if (frames.length < 2) return null;
  return ((frames.length - 1) * 1000) / (frames[frames.length - 1] - frames[0]);
}

function framesNote({ frames, work }: Frames): string {
  const intervals = intervalsOf(frames);
  const worst = intervals.length > 0 ? Math.max(...intervals).toFixed(1) : 'none';
  return `${work.length} frames with work, frame interval p95 ${p95(intervals)?.toFixed(1) ?? 'none'} ms, worst ${worst} ms.`;
}

function take(id: string, value: number | null, note?: string): void {
  taken.set(id, { value: value === null ? null : Math.round(value * 100) / 100, note });
}

/** Run [block] and, when it throws, take each of [ids] as null with the error as the reason. */
async function measure(ids: string[], block: () => Promise<void>): Promise<void> {
  try {
    await block();
  } catch (error) {
    const message = String((error as Error).message ?? error).replace(/\u001b\[[0-9;]*m/g, '');
    const reason = `Not measured, ${message.split('\n')[0]}`;
    for (const id of ids) if (!taken.has(id)) take(id, null, reason);
  }
}

function writeReport(browser: Browser): void {
  const metrics: Metric[] = config.budgets.map((budget) => {
    const { value, note } = taken.get(budget.id) ?? { value: null, note: 'Not measured, the run stopped before it.' };
    const within =
      value === null ? null : budget.max !== undefined ? value <= budget.max : budget.min !== undefined ? value >= budget.min : null;
    return { ...budget, value, within, ...(note ? { note } : {}) };
  });
  const report = {
    generatedAt: new Date().toISOString(),
    ref: process.env.GITHUB_REF_NAME ?? null,
    commit: process.env.GITHUB_SHA ?? null,
    browser: `Chromium ${browser.version()}`,
    network: config.network,
    encodings: [...(site?.encodings ?? [])],
    metrics,
  };
  mkdirSync(REPORT_DIR, { recursive: true });
  writeFileSync(path.join(REPORT_DIR, 'perf-report.json'), `${JSON.stringify(report, null, 2)}\n`);
  const rows = metrics.map((metric) => {
    const limit = metric.max !== undefined ? `at most ${metric.max}` : `at least ${metric.min}`;
    const verdict = metric.within === null ? 'not measured' : metric.within ? 'within' : 'over';
    return `| ${metric.id} | ${metric.what} | ${metric.value ?? 'null'} ${metric.unit} | ${limit} | ${verdict} | ${metric.note ?? ''} |`;
  });
  const summary = [
    `## Builder perf run, ${report.browser}`,
    '',
    'Report only. Nothing here fails the run.',
    '',
    '| Budget | What | Value | Limit | Verdict | Note |',
    '| --- | --- | --- | --- | --- | --- |',
    ...rows,
    '',
  ].join('\n');
  writeFileSync(path.join(REPORT_DIR, 'summary.md'), summary);
}

/**
 * Runs in the page before any of its own scripts. It collects the `mk:` marks and the slow
 * interactions, and wraps `requestAnimationFrame` so a recording can sum the work each frame does.
 */
function instrument(): void {
  const marks: { name: string; time: number }[] = [];
  const interactions: number[] = [];
  let recording = false;
  let frames: number[] = [];
  let work = new Map<number, number>();
  new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
      if (entry.name.startsWith('mk:')) marks.push({ name: entry.name, time: entry.startTime });
    }
  }).observe({ type: 'mark', buffered: true });
  try {
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries() as (PerformanceEntry & { interactionId?: number })[]) {
        if (entry.interactionId) interactions.push(entry.duration);
      }
    }).observe({ type: 'event', buffered: true, durationThreshold: 16 } as PerformanceObserverInit);
  } catch {
    // No Event Timing here, so INP stays unmeasured.
  }
  const raf = window.requestAnimationFrame.bind(window);
  window.requestAnimationFrame = (callback) =>
    raf((time) => {
      const start = performance.now();
      try {
        callback(time);
      } finally {
        if (recording) work.set(time, (work.get(time) ?? 0) + performance.now() - start);
      }
    });
  window.__mkPerf = {
    marks,
    interactions,
    start: () => {
      recording = true;
      frames = [];
      work = new Map();
      const loop = (time: number) => {
        if (!recording) return;
        frames.push(time);
        raf(loop);
      };
      raf(loop);
    },
    stop: () => {
      recording = false;
      return { frames, work: [...work.values()], busy: [...work.keys()] };
    },
  };
}
