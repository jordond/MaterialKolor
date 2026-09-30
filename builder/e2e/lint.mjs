import { readdirSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Fails when a spec or fixture sleeps with `waitForTimeout`. A test waits on something the page
// shows, stores or draws, never on the clock. The perf and preview runs measure time and are left out.

const here = path.dirname(fileURLToPath(import.meta.url));
const CHECKED = ['tests', 'fixtures'];
const SLEEP = /\bwaitForTimeout\s*\(/;

const found = [];
for (const folder of CHECKED) {
  for (const file of sources(path.join(here, folder))) {
    readFileSync(file, 'utf8')
      .split('\n')
      .forEach((line, index) => {
        if (SLEEP.test(line)) found.push(`${path.relative(here, file)}:${index + 1}: ${line.trim()}`);
      });
  }
}

if (found.length > 0) {
  console.error(`waitForTimeout sleeps on the clock. Wait on the page's state instead.\n${found.join('\n')}`);
  process.exit(1);
}
console.log(`No waitForTimeout in ${CHECKED.join(' or ')}.`);

function sources(folder) {
  return readdirSync(folder, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(folder, entry.name);
    if (entry.isDirectory()) return sources(full);
    return /\.(ts|mjs|js)$/.test(entry.name) ? [full] : [];
  });
}
