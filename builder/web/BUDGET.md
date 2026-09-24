# Size budget

`budget.json` holds the limits. `./gradlew :builder:web:checkBudget` measures the site, prints the
sizes and fails the build on an overrun. This file says how the numbers are measured, so two people
measuring the same change get the same number.

The limits are loose backstops, not targets. They sit well above today's sizes and only catch an
accident, such as a debug binary or a runtime shipped twice. Size is not something a change has to
argue for.

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

- By role. Each role in `budget.json` names its files with a glob. A role with `maxBytes` holds
  every file it names to that limit on its own: skiko, app wasm, glue and `index.html`. A role
  with `totalBytes` holds the sum of all its files: initial fonts, which is every font under a
  `com.materialkolor.*` resource folder, the same fonts `index.html` lists for boot. A font added
  there adds its whole size to that one total.
- First visit, in total. The sum of every file in `firstVisit.files` minus `firstVisit.exclude`,
  which is everything `index.html` loads before the first complete screen. That is the page, the
  glue, both wasm files, the builder's fonts and its string resources. Library fallback fonts the
  page never asks for at boot are left out. The Playwright smoke holds the set to what the browser
  really fetches both ways: it fails if the page loads a file at boot that the set does not count,
  and if the set counts a file the page does not load at boot. So the change that makes a file
  lazy also moves it into `firstVisit.exclude` and, for a font, out of the initial fonts role and
  the boot list. Until it does, the file counts toward first visit and the smoke fails. String
  resource files (`values/*.cvr`, one or two KB each) are the one exception to the second check:
  Compose loads each when a screen first reads it, so some arrive after boot. They stay counted,
  which only makes the total stricter.
- Files outside the roles count toward first visit only.
- Anything under `assets/` that no role names fails, so a new chunk cannot slip in unbudgeted.
- Raw size, per file. No file in the site may be over `rawFile.maxBytes`, 26,214,400 raw bytes,
  which is Cloudflare's 25 MiB cap on one asset. This one counts every file, skipped and
  unmeasured ones too, since the host is given all of them. The script prints the largest file
  and how far under the cap it is. Today that is the platform theme's `NotoColorEmoji.ttf`.

## What a skin costs

A skin has no line of its own. All skins ship in the one app wasm (D1, architecture 2), so a skin is
paid for out of the app wasm limit, the initial fonts total for any font it brings, and the first
visit total. Its cost is the brotli size at the head of the change that adds it minus the brotli
size at that change's merge base, per file and for first visit, both measured as above on full
production builds of the whole site. It is not a delta against a stand-in, a build with other
parts left out, or any partial build. Such a delta depends on what the stand-in already pulls in,
and spike S1 measured the same Fluent code at 446 KB against one stand-in and 220 KB against
another.

## Changing the numbers

A change that hits a limit raises it in `budget.json` in the same change, with room to spare. CI
never edits the file.
