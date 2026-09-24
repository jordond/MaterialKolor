#!/usr/bin/env node
// Compresses every file of the assembled site with brotli and holds the result against budget.json.
// BUDGET.md beside budget.json says what is measured and why.
//
//   node scripts/check-budget.mjs [--site dir] [--budget file]
//
// Exits 1 when a file is over its role's limit, a role's files together are over its total, first
// visit is over its total, an asset belongs to no role, or any site file is over the host's raw size
// cap.

import { readFileSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { brotliCompressSync, constants } from 'node:zlib';

const moduleDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = parseArgs(process.argv.slice(2));
const siteDir = path.resolve(args.site ?? path.join(moduleDir, 'build/site'));
const budgetFile = path.resolve(args.budget ?? path.join(moduleDir, 'budget.json'));
const budget = JSON.parse(readFileSync(budgetFile, 'utf8'));

const roles = budget.roles.map((role) => {
  if ((role.maxBytes === undefined) === (role.totalBytes === undefined)) {
    throw new Error(`Role ${role.name} in budget.json needs one of maxBytes (each file) or totalBytes (all files together)`);
  }
  return { ...role, pattern: globToRegExp(role.files) };
});
const skipped = budget.skip.map(globToRegExp);
const firstVisit = budget.firstVisit.files.map(globToRegExp);
const lazy = budget.firstVisit.exclude.map(globToRegExp);

const site = listFiles(siteDir);

// Files that are neither budgeted nor loaded on first visit are not compressed at all. Quality 11
// is slow, and the platform theme's fallback fonts alone are 75 MB.
const files = site
  .filter(({ file }) => !skipped.some((pattern) => pattern.test(file)))
  .map(({ file, raw }) => ({
    file,
    raw,
    role: roles.find((role) => role.pattern.test(file)),
    firstVisit: firstVisit.some((pattern) => pattern.test(file)) && !lazy.some((pattern) => pattern.test(file)),
  }))
  .filter((entry) => entry.role || entry.firstVisit || entry.file.startsWith('assets/'))
  .map((entry) => {
    const measured = entry.role || entry.firstVisit;
    return { ...entry, brotli: measured ? compress(readFileSync(path.join(siteDir, entry.file))).length : 0 };
  });

const failures = [];
const rows = [];

for (const entry of files) {
  const { role } = entry;
  if (!role) {
    if (entry.file.startsWith('assets/')) failures.push(`${entry.file} belongs to no role in budget.json`);
    if (entry.firstVisit) rows.push([entry.file, '-', entry.raw, entry.brotli, '']);
    continue;
  }
  if (role.maxBytes !== undefined && entry.brotli > role.maxBytes) {
    failures.push(`${entry.file} is ${entry.brotli} bytes, over the ${role.name} limit of ${role.maxBytes}`);
  }
  rows.push([entry.file, role.name, entry.raw, entry.brotli, role.maxBytes ?? '']);
}

// A role with a total holds its files together, whatever their number.
for (const role of roles.filter((role) => role.totalBytes !== undefined)) {
  const sum = files.filter((entry) => entry.role === role).reduce((total, entry) => total + entry.brotli, 0);
  if (sum > role.totalBytes) {
    failures.push(`${role.name} are ${sum} bytes together, over the limit of ${role.totalBytes}`);
  }
  rows.push([`${role.name}, all files`, role.name, '', sum, role.totalBytes]);
}

const total = files.filter((entry) => entry.firstVisit).reduce((sum, entry) => sum + entry.brotli, 0);
if (total > budget.firstVisit.maxBytes) {
  failures.push(`first visit is ${total} bytes, over the limit of ${budget.firstVisit.maxBytes}`);
}
rows.push(['first visit', '', '', total, budget.firstVisit.maxBytes]);

// The host's cap is on raw bytes and holds every file it is given, measured or skipped here.
const rawLimit = budget.rawFile.maxBytes;
for (const { file, raw } of site.filter((entry) => entry.raw > rawLimit)) {
  failures.push(`${file} is ${raw} raw bytes, over the host's per-file cap of ${rawLimit}`);
}
const largest = site.reduce((max, entry) => (entry.raw > max.raw ? entry : max));

console.log(`brotli ${process.versions.brotli} (node ${process.versions.node}), quality ${budget.compression.quality}\n`);
printTable(['file', 'role', 'raw', 'brotli', 'limit'], rows);
console.log(`\nLargest file: ${largest.file}, ${largest.raw} raw bytes, ${rawLimit - largest.raw} under the per-file cap of ${rawLimit}.`);
if (failures.length > 0) {
  console.error(`\nOver budget:\n${failures.map((failure) => `  ${failure}`).join('\n')}`);
  process.exit(1);
}
console.log('\nWithin budget.');

function compress(bytes) {
  const { quality, window } = budget.compression;
  return brotliCompressSync(bytes, {
    params: {
      [constants.BROTLI_PARAM_MODE]: constants.BROTLI_MODE_GENERIC,
      [constants.BROTLI_PARAM_QUALITY]: quality,
      [constants.BROTLI_PARAM_LGWIN]: window,
    },
  });
}

/** Every file under [root], its site-relative path with forward slashes and its raw size. */
function listFiles(root, prefix = '') {
  return readdirSync(path.join(root, prefix))
    .sort()
    .flatMap((name) => {
      const relative = prefix ? `${prefix}/${name}` : name;
      const stats = statSync(path.join(root, relative));
      return stats.isDirectory() ? listFiles(root, relative) : [{ file: relative, raw: stats.size }];
    });
}

/** `*` matches within one path segment and `**` across segments. */
function globToRegExp(glob) {
  const source = glob
    .split(/(\*\*\/|\*\*|\*)/)
    .map((part) => {
      if (part === '**/') return '(?:.*/)?';
      if (part === '**') return '.*';
      if (part === '*') return '[^/]*';
      return part.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
    })
    .join('');
  return new RegExp(`^${source}$`);
}

function parseArgs(argv) {
  const parsed = {};
  for (let index = 0; index < argv.length; index++) {
    const name = argv[index].replace(/^--/, '');
    parsed[name] = argv[++index];
  }
  return parsed;
}

function printTable(header, body) {
  const table = [header, ...body].map((row) => row.map(String));
  const widths = header.map((_, column) => Math.max(...table.map((row) => row[column].length)));
  for (const row of table) {
    console.log(row.map((cell, column) => (column < 2 ? cell.padEnd(widths[column]) : cell.padStart(widths[column]))).join('  '));
  }
}
