# Size budget

`budget.json` holds the limits and `budget-baseline.json` the sizes the growth rule compares with.
`./gradlew :builder:web:checkBudget` measures the site and fails the build on an overrun. This file
says how the numbers are measured, so two people measuring the same change get the same number.

## What is measured

- The site as `./gradlew :builder:web:assembleSite` lays it out in `builder/web/build/site`. That is
  the production `wasmJsBrowserDistribution`, with webpack in production mode and the Kotlin/Wasm
  production binary, and no extra `-P` flags. `-Psite.env=staging` only changes `_headers` and
  `robots.txt`, which are not measured.
- Every skin and every font the build ships. The builder is one bundle, so there is no build to
  measure with parts left out.
- Source maps (`*.map`) never count, nor the extracted license comments (`*.LICENSE.txt`) or the host
  files (`_headers`, `robots.txt`). The page never loads them.

## How a file is sized

Each file is compressed on its own with Node's built-in brotli (`node:zlib`), quality 11, window 22,
generic mode, and the size is the compressed byte count. Limits are in those bytes, and 1 MB is
1,000,000 bytes. The number of record is the one CI measures with the Node the `site` job sets up.
The script prints the brotli version it ran with, since another version can differ by a few bytes.

## The limits

- Per file by role. Each role in `budget.json` names its files with a glob, and every file it names
  must be at or under the role's limit. The roles are skiko, app wasm, glue and initial fonts, the
  last one per font file.
- First visit, in total. The sum of every file in `firstVisit.files` minus `firstVisit.exclude`,
  which is everything `index.html` loads before the first complete screen. That is the page, the
  glue, both wasm files, the builder's fonts and its string resources. Library fallback fonts the page never
  asks for at boot are left out. The Playwright smoke fails if the page loads a file at boot that
  this set does not count, so the set cannot drift from what the browser really fetches.
- Growth, per file. A file a role names may not grow more than `growth.maxPercent` (5%) over its
  size in `budget-baseline.json`. Files are keyed without their content hash, so
  `assets/skiko.<hash>.wasm` is `assets/skiko.wasm`. A role file with no baseline fails too. Files
  outside the roles count toward first visit only, so a string added to a small resource file does
  not trip the rule.
- Anything under `assets/` that no role names fails, so a new chunk cannot slip in unbudgeted.

## What a skin costs

A skin has no line of its own. All skins ship in the one app wasm (D1, architecture 2), so a skin is
paid for out of the app wasm limit, the font limit for any font it brings, and the first visit
total. Its cost is its share of that one bundle, the growth the full build shows in the change that
adds it. It is not a delta against a build without it. That delta depends on what the stand-in
build already pulls in, and spike S1 measured the same Fluent code at 446 KB against one stand-in
and 220 KB against another.

## Changing the numbers

Raising a limit in `budget.json` or a size in `budget-baseline.json` is a reviewed edit, made in the
change that needs it and never by CI. To take new baseline sizes from a fresh `assembleSite`, run
`node builder/web/scripts/check-budget.mjs --write-baseline` and commit the result.
