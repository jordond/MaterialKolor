#!/usr/bin/env node
// Compresses every file of the assembled site with brotli and holds the result against budget.json.
// BUDGET.md beside budget.json says what is measured and why.
//
//   node scripts/check-budget.mjs [--site dir] [--budget file] [--baseline file] [--write-baseline]
//
// Exits 1 when a file is over its role's limit, first visit is over its total, a budgeted file grew
// more than the growth rule allows against the baseline, or an asset belongs to no role.

import { readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { brotliCompressSync, constants } from 'node:zlib';

const moduleDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = parseArgs(process.argv.slice(2));
const siteDir = path.resolve(args.site ?? path.join(moduleDir, 'build/site'));
const budgetFile = path.resolve(args.budget ?? path.join(moduleDir, 'budget.json'));
const budget = JSON.parse(readFileSync(budgetFile, 'utf8'));
const baselineFile = path.resolve(args.baseline ?? path.join(path.dirname(budgetFile), budget.growth.baseline));

const roles = budget.roles.map((role) => ({ ...role, pattern: globToRegExp(role.files) }));
const skipped = budget.skip.map(globToRegExp);
const firstVisit = budget.firstVisit.files.map(globToRegExp);
const lazy = budget.firstVisit.exclude.map(globToRegExp);

// Files that are neither budgeted nor loaded on first visit are not compressed at all. Quality 11
// is slow, and the platform theme's fallback fonts alone are 75 MB.
const files = listFiles(siteDir)
  .filter((file) => !skipped.some((pattern) => pattern.test(file)))
  .map((file) => ({
    file,
    key: stableKey(file),
    role: roles.find((role) => role.pattern.test(file)),
    firstVisit: firstVisit.some((pattern) => pattern.test(file)) && !lazy.some((pattern) => pattern.test(file)),
  }))
  .filter((entry) => entry.role || entry.firstVisit || entry.file.startsWith('assets/'))
  .map((entry) => {
    const bytes = readFileSync(path.join(siteDir, entry.file));
    const measured = entry.role || entry.firstVisit;
    return { ...entry, raw: bytes.length, brotli: measured ? compress(bytes).length : 0 };
  });

if (args['write-baseline']) {
  const sizes = Object.fromEntries(files.filter((entry) => entry.role).map((entry) => [entry.key, entry.brotli]));
  writeFileSync(baselineFile, JSON.stringify({ files: sortKeys(sizes) }, null, 2) + '\n');
  console.log(`Wrote ${path.relative(process.cwd(), baselineFile)}. Commit it with the change that moved the numbers.`);
  process.exit(0);
}

const baseline = JSON.parse(readFileSync(baselineFile, 'utf8')).files;
const failures = [];
const rows = [];

for (const entry of files) {
  const { role } = entry;
  if (!role) {
    if (entry.file.startsWith('assets/')) failures.push(`${entry.file} belongs to no role in budget.json`);
    if (entry.firstVisit) rows.push([entry.file, '-', entry.raw, entry.brotli, '', '', '']);
    continue;
  }
  const allowed = baseline[entry.key];
  let growth = '';
  if (allowed === undefined) {
    failures.push(`${entry.file} has no baseline size, add ${entry.key} to ${path.basename(baselineFile)}`);
    growth = 'new';
  } else {
    const percent = ((entry.brotli - allowed) / allowed) * 100;
    growth = `${percent >= 0 ? '+' : ''}${percent.toFixed(1)}%`;
    if (percent > budget.growth.maxPercent) {
      failures.push(`${entry.file} grew ${growth} against its baseline of ${allowed} bytes, the limit is +${budget.growth.maxPercent}%`);
    }
  }
  if (entry.brotli > role.maxBytes) {
    failures.push(`${entry.file} is ${entry.brotli} bytes, over the ${role.name} limit of ${role.maxBytes}`);
  }
  rows.push([entry.file, role.name, entry.raw, entry.brotli, role.maxBytes, allowed ?? '', growth]);
}

const total = files.filter((entry) => entry.firstVisit).reduce((sum, entry) => sum + entry.brotli, 0);
if (total > budget.firstVisit.maxBytes) {
  failures.push(`first visit is ${total} bytes, over the limit of ${budget.firstVisit.maxBytes}`);
}
rows.push(['first visit', '', '', total, budget.firstVisit.maxBytes, '', '']);

console.log(`brotli ${process.versions.brotli} (node ${process.versions.node}), quality ${budget.compression.quality}\n`);
printTable(['file', 'role', 'raw', 'brotli', 'limit', 'baseline', 'growth'], rows);
if (failures.length > 0) {
  console.error(`\nOver budget:\n${failures.map((failure) => `  ${failure}`).join('\n')}`);
  console.error('\nRaising a limit or the baseline is a reviewed edit of budget.json or budget-baseline.json.');
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

/** The site-relative path of every file under [root], with forward slashes. */
function listFiles(root, prefix = '') {
  return readdirSync(path.join(root, prefix))
    .sort()
    .flatMap((name) => {
      const relative = prefix ? `${prefix}/${name}` : name;
      return statSync(path.join(root, relative)).isDirectory() ? listFiles(root, relative) : [relative];
    });
}

/** [file] without its content hash, so a file keeps its baseline from one build to the next. */
function stableKey(file) {
  return file.replace(/\.[0-9a-f]{16}(?=\.)/, '');
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

function sortKeys(object) {
  return Object.fromEntries(Object.entries(object).sort(([left], [right]) => left.localeCompare(right)));
}

function parseArgs(argv) {
  const parsed = {};
  for (let index = 0; index < argv.length; index++) {
    const name = argv[index].replace(/^--/, '');
    if (name === 'write-baseline') parsed[name] = true;
    else parsed[name] = argv[++index];
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
