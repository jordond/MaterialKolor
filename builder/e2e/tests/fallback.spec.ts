import { readFileSync } from 'node:fs';
import { expect, test, type Browser } from '@playwright/test';
import { wantHooks } from './builder';
import { readZip } from '../fixtures/zip';
import { button, onPage, openWorkspace, press, pressKeyFor, seedText, shareVectors } from '../fixtures/workspace';

// The JS engine, the fallback for browsers without WasmGC (D61). ?engine=js boots it where wasm
// would run, and the same share code exports the same code on both engines. The codegen's own
// tests hold the text itself on each engine, this holds the two sites to each other.

/** The Export sheet's title in the mirror, the dialog fold. */
const EXPORT_DIALOG = /^Export code, dialog/;

test('a share code boots on the JS engine with its seed and exports what wasm exports', async ({ browser, browserName }) => {
  test.skip(browserName !== 'chromium', 'One browser is enough to hold the engines to each other');
  // Two boots and two exports, the JS one the slower.
  test.slow();
  const vector = shareVectors().find((candidate) => candidate.label === 'every section at once')!;
  const route = `/t/${vector.code}`;

  const js = await exportFrom(browser, `${route}?engine=js`, 'js', vector.seedHex);
  const wasm = await exportFrom(browser, route, 'wasm', vector.seedHex);

  expect([...js.keys()].sort()).toEqual([...wasm.keys()].sort());
  expect(js.size).toBeGreaterThan(0);
  for (const [file, text] of wasm) expect(js.get(file), file).toBe(text);
});

/**
 * Boots [route] in a fresh context, checks it ran [engine] and opened with [seedHex], and returns
 * the text of every file in the export zip by its path.
 */
async function exportFrom(
  browser: Browser,
  route: string,
  engine: 'wasm' | 'js',
  seedHex: string,
): Promise<Map<string, string>> {
  const context = await browser.newContext();
  try {
    await wantHooks(context);
    const page = await context.newPage();
    const glues: string[] = [];
    page.on('request', (request) => {
      const match = /\/assets\/(builder(?:-js)?)\.[0-9a-f]{16}\.js$/.exec(new URL(request.url()).pathname);
      if (match) glues.push(match[1]);
    });

    await openWorkspace(page, route);
    expect(glues).toEqual([engine === 'js' ? 'builder-js' : 'builder']);
    await expect.poll(() => seedText(page)).toBe(seedHex);

    await pressKeyFor(page, 'e', onPage(page, EXPORT_DIALOG));
    const download = page.waitForEvent('download');
    await press(page, button(page, 'Download zip'));
    const zip = readZip(readFileSync((await (await download).path())!));
    return new Map([...zip].map(([name, bytes]) => [name, bytes.toString('utf8')]));
  } finally {
    await context.close();
  }
}
