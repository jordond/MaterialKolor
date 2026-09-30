import { readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Fails when a spec or fixture calls `waitForTimeout`, or `setTimeout` outside the named holds in
// fixtures/timing.ts. The perf and preview runs are not checked.

const here = path.dirname(fileURLToPath(import.meta.url));
const CHECKED = ['tests', 'fixtures'];
const WAIT_FOR_TIMEOUT = /\bwaitForTimeout\s*\(/;
const SLEEP = /\bsetTimeout\s*\(/;
const TIMING = 'fixtures/timing.ts';
const TIMED_HOLDS = new Set(['holdFingerDown', 'holdBetweenTouches', 'networkQuietFor', 'pauseBeforeServerCheck']);

const found = [];
for (const folder of CHECKED) {
  for (const file of sources(path.join(here, folder))) {
    const relative = path.relative(here, file).split(path.sep).join('/');
    let inside = '';
    readFileSync(file, 'utf8')
      .split('\n')
      .forEach((line, index) => {
        const declared = /^(?:export\s+)?(?:async\s+)?(?:function|const|let|var|class|type|interface)\s+(\w+)/.exec(line);
        if (declared) inside = declared[1];
        const allowed = relative === TIMING && TIMED_HOLDS.has(inside);
        if (WAIT_FOR_TIMEOUT.test(line) || (SLEEP.test(line) && !allowed)) {
          found.push(`${relative}:${index + 1}: ${line.trim()}`);
        }
      });
  }
}

if (found.length > 0) {
  console.error(
    'These sleep on the clock. Wait on the page state instead, or use a named hold from fixtures/timing.ts.\n' +
      found.join('\n'),
  );
  process.exit(1);
}
console.log(`No sleeps in ${CHECKED.join(' or ')}.`);

function sources(folder) {
  return readdirSync(folder, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(folder, entry.name);
    if (entry.isDirectory()) return sources(full);
    return /\.(ts|mjs|js)$/.test(entry.name) ? [full] : [];
  });
}
