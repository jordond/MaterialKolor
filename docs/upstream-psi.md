# Maintaining the upstream engine

`material-color-utilities` isn't written by hand. It is generated from Google's upstream Kotlin
source, which is checked out as a submodule at `tools/mcu-upstream/src/main` and pinned by
`gradle/mcu-upstream.lock.json`. `tools/mcu-source-transformer` parses that source with the Kotlin
PSI and rewrites it for Kotlin Multiplatform and for MaterialKolor's API.

## Setup

```sh
git submodule update --init --recursive
./gradlew verifyMcuUpstream
./gradlew :material-color-utilities:generateMcuSources --configuration-cache
```

The generated sources go to `material-color-utilities/build/generated/mcu/commonMain/`. A report of
every rule applied to each file goes to `material-color-utilities/build/reports/mcu-sources.tsv`.
Review the report and the generated diff together. If a transform fails, the previous output is
left untouched.

## What the transformer changes

The transformer makes three kinds of change, each in its own pass:

1. Namespace: it moves upstream's packages under `com.materialkolor.*`.
2. Portability: it replaces JVM-only calls with common Kotlin.
3. Semantics: it adds the MaterialKolor API changes, listed under [Semantic rules](#semantic-rules).

Everything else in a file, including comments and copyright headers, is kept as is. If a rule hits
a shape it doesn't support, generation fails and reports the file, the location and the rule ID.

| Upstream | Replacement |
|---|---|
| `ArrayList`, `HashMap`, `LinkedHashMap` | Kotlin collections with the same ordering |
| `Arrays.sort`, `Collections.sort` | In-place sort with the same comparator |
| `Collections.unmodifiableList` | A private copy |
| `Math.toRadians`, `Math.toDegrees`, `Math.max` | Common math |
| RGB `String.format` | Locale-independent lowercase hex, two digits per channel |
| `Locale.ENGLISH` enum lowercasing | Locale-independent lowercasing |
| Seeded `java.util.Random` | An internal Java-compatible 48-bit generator, including bounded draws |
| `DecimalFormat("0.0")` in `DynamicScheme.toString` | See [Diagnostic formatting](#diagnostic-formatting) |

The following changes in upstream fail the build until someone reviews them: wildcard JVM imports,
unknown JVM APIs or annotations, ambiguous symbol shadowing, new public mutation, overlapping
edits, and files being added or removed. An import alias is only allowed when its target and call
shape have been reviewed.

There are two reference builds, and neither is published. `:mcu-upstream` compiles upstream's Java,
and it also compiles upstream's Kotlin with only the namespace moved, into `upstream.kotlin.*`. The
parity tests compare the generated library against both. The namespace-only copy must never get
portability or semantic rules. Its report is `tools/mcu-upstream/build/reports/mcu-reference.tsv`.

### Semantic rules

Semantic rules are the expensive part to maintain, because every upstream refactor of a
declaration a rule targets means repairing that rule. A rule is only justified if it needs access
from inside the class:

- private or internal members
- `equals`, `hashCode` or `toString`
- constructor or visibility changes
- removing public mutation
- supertype or annotation changes

Anything that could be a handwritten extension, factory or facade should be handwritten.

Group A rules meet that test. Group B rules don't. They only call public API, but they are kept as
members because they sit next to upstream members that need no import, and each one is already a
reviewed entry in the API dump. Group B is the first thing to reconsider, and it doesn't grow
without an API dump review.

The module has two handwritten production files, and conveniences don't belong in either one.
`InternalMaterialKolorApi.kt` is the opt-in marker, and `compat/PlatformCompat.kt` holds internal
portability helpers.

| Rule | Group | What it does |
|---|---|---|
| `analogous-property` | A | `TemperatureCache.getAnalogousColors()` becomes `val analogousColors`. Both compile to the same JVM signature, so the function has to be removed. |
| `cam16-visibility` | A | Makes five internal `Cam16` members public. They were public in 5.x. |
| `dynamic-color-factory` | B | `DynamicColor.Companion.fromPalette`. As an extension it would need an import. |
| `enum-default` | A | Adds `Default` companions to `SpecVersion` (`SPEC_2025`) and `Platform` (`PHONE`). |
| `harmonize-hct` | B | A static `Hct` overload of `Blend.harmonize`, next to the `Int` one. |
| `hct-copy-body` | A | Returns a new `Hct` instead of calling the private `setInternalState`. |
| `hct-copy-method` | A | Renames `setHue`, `setChroma` and `setTone` to `withHue`, `withChroma` and `withTone`. |
| `hct-copy-type` | A | Adds the `Hct` return type that the rename needs. |
| `hct-value-contract` | A, B | `equals` and `hashCode` over `argb` (A), plus `isBlue`, `isYellow` and `isCyan` (B). |
| `hide-palette-cache` | A | Makes `TonalPalette.cache` private, which keeps it out of equality. |
| `implementation-visibility` | A | Makes thirteen quantizer, spec, solver and math types internal, including `ColorSpec2021` to `2026` and `HctSolver`. |
| `initial-tone-default` | A | Defaults the `getInitialToneFromBackground` parameter to `null`. |
| `luminance-member` | B | `ColorUtils.calculateLuminance`, which core's `Color.isLight` uses. |
| `member-visibility` | A | Makes named members internal: the raw `ColorUtils` math, some `DynamicScheme` and `DynamicColor` companion helpers, and `ViewingConditions.rgbD`. Hidden members drop `@JvmStatic`. Fails if a named member disappears, is overloaded or is no longer public. |
| `poko-class` | A | Turns `DynamicColor`, `ToneDeltaPair`, `CorePalettes`, `Cam16` and `ViewingConditions` into `@Poko` classes, so a new upstream property can't break `copy` or `componentN` callers. `DynamicColor` keeps an internal `copy` for upstream's own use. Fails if any other public data class appears. |
| `scheme-copy` | B | `DynamicScheme.copy` and the four nullable dim roles. An extension `copy` would be shadowed if upstream made the class a data class. |
| `scheme-error-default` | A | Defaults `errorPalette` in the primary and secondary constructors. |
| `score-default` | A | Adds defaults to the full `score` function and deletes the shorter overloads. `@JvmOverloads` keeps them for Java. |
| `score-nullable-fallback` | A | Makes `fallbackColorArgb` an `Int?`. |
| `score-optional-fallback` | A | Only appends the fallback when it isn't null. |
| `spec-default` | A | Points `DynamicScheme`'s default spec version at `SpecVersion.Default`. Fails if upstream's default changes. |
| `subclass-opt-in` | A | Adds `@OptIn(InternalMaterialKolorApi::class)` to the in-module subclasses of opt-in types. |
| `subclass-opt-in-required` | A | Requires `InternalMaterialKolorApi` to subclass `ColorSpec` or `DynamicScheme`, because their open members change with upstream. |
| `value-equality` | A | Adds `equals`, `hashCode` and `toString` to `TonalPalette`, `TemperatureCache` and `ContrastCurve`. |
| `value-input-visibility` | A | Makes the `TemperatureCache` and `ContrastCurve` constructor inputs private. |
| `white-point-copy` | A | `ColorUtils.whitePointD65` returns a copy, so callers can't mutate the shared array. |

Four guards make no edits. They exist to fail, with a file and location, when upstream moves
something a rule depends on. Adding a guard needs the same review as adding a rule.

| Guard | Fails when |
|---|---|
| `semantic-inventory` | A file's path and package no longer agree. Otherwise every rule for that file would be skipped without any error. |
| `palette-cache` | `TonalPalette.cache` is no longer the mutable, non-private field that `hide-palette-cache` expects. |
| `hct-mutation-surface` | `Hct`'s constructor, methods, properties or internal mutation change. |
| `score-fallback` | The `Score.score` overloads change, or a short overload stops delegating with the defaults. |

A new rule needs a row in the rules table. No rule has been moved to handwritten source yet. If one
is, replace its row with "moved to handwritten `<file>` on `<date>`" and review the resulting API
dump diff.

### Diagnostic formatting

`DynamicScheme.toString()` prints the contrast level with one decimal, which upstream does with
`DecimalFormat`. The common replacement handles only that case. It is `round(abs(value) * 10)` with
ties to even, keeps the sign including on negative zero, adds `.0` to values of at least 2^52, and
rejects non-finite values. It can differ from Java at binary rounding boundaries. This is
diagnostic output, not a serialization format, so don't rely on it anywhere else. Any change to the
rounding needs a regression test.

## Verification gates

Run these from the repository root:

```sh
./gradlew verifyMcuUpstream
rm -rf material-color-utilities/build/generated/mcu
./gradlew :material-color-utilities:generateMcuSources --no-build-cache --rerun-tasks
./gradlew :mcu-source-transformer:test :mcu-source-transformer:testAlternateParser
./gradlew checkKotlinAbi spotlessCheck
./gradlew verifyMcuJvm verifyMcuWeb verifyMcuAndroid verifyMcuApple verifyMcuPublication
python3 -B -m unittest discover -s .github/tests -v
```

| Gate | Checks |
|---|---|
| `verifyMcuUpstream` | The submodule, its Kotlin file list, the source hashes and the license hash match the lock. |
| `:mcu-source-transformer:test` | Rules match what they claim to, edits don't overlap, untouched text survives, and unsupported shapes fail with a location. |
| `testAlternateParser` | The same sources transform identically with a newer PSI runtime. |
| Determinism | Regenerating from the same inputs gives byte-identical files and the same report. Compare them against the previous run. |
| `checkKotlinAbi` | Every module's public API matches its reviewed dump in `<module>/api/`, for JVM, Android and klib targets. |
| `spotlessCheck` | Handwritten code is formatted. Generated and upstream code is excluded. |
| `verifyMcuJvm` | Generated output matches both the Java and the namespace-only Kotlin reference, ARGB for ARGB, and the MaterialKolor characterization tests pass. |
| `verifyMcuWeb` | Common tests pass in Node and headless Chrome for `js` and `wasmJs`. |
| `verifyMcuAndroid` | Every library module assembles for Android, and its host tests and lint pass. |
| `verifyMcuApple` | Common tests pass on the iOS simulator, the device framework links, and macOS compiles. |
| `verifyMcuPublication` | Artifacts publish to a temporary repository under `build/`, and an artifact-only consumer compiles against them. |
| Builder | The Builder app compiles and tests against the local modules instead of published ones. |
| `.github/tests` | The upstream scripts behave correctly against throwaway Git repositories, without network access. |

`verifyMcuPublication` never publishes to Maven Central or deploys docs. The source jars include the
generated code, the handwritten helpers and the upstream license, each once. The PSI, reference and
transformer dependencies must never show up in published metadata.

`dokkaGenerate` warns about one unresolved link, `[DynamicScheme]` in `palettes/CorePalettes.kt`.
It comes from upstream's Javadoc, so leave it.

Tests that exercise failure cases use throwaway fixture checkouts and never touch the real
submodule. Neither a build nor a test may regenerate the API dumps or golden fixtures on its own.

## API stability

The generated API is frozen for 6.x. Every pin bump is checked against
`material-color-utilities/api/`.

- New entries in `Variant`, `SpecVersion`, `Platform`, `TonePolarity`, `DeltaConstraint` or
  `PaletteStyle` don't count as breaking changes. Consumers shouldn't write exhaustive `when`
  expressions over them.
- `SpecVersion.Default` stays `SPEC_2025` for all of 6.x.
- If a bump changes a public signature, `checkKotlinAbi` fails. Nothing keeps the old signature
  automatically. Before updating the dump, either add a `@Deprecated(level = HIDDEN)` overload by
  hand or decide explicitly to accept the break.

## Updating the upstream pin

The [upstream monitor](#ci-and-the-upstream-monitor) normally opens this change as a pull request.
To update by hand, first save the current output so you can diff it afterwards:

```sh
./gradlew :material-color-utilities:generateMcuSources
mkdir -p build/mcu-update
cp -R material-color-utilities/build/generated/mcu/commonMain build/mcu-update/previous
cp material-color-utilities/build/reports/mcu-sources.tsv build/mcu-update/previous-report.tsv
PREVIOUS=$(git -C tools/mcu-upstream/src/main rev-parse HEAD)
```

Then move the submodule to the candidate revision and review what changed upstream:

```sh
git -C tools/mcu-upstream/src/main fetch origin
CANDIDATE=<full commit sha>
git -C tools/mcu-upstream/src/main checkout --detach "$CANDIDATE"
git -C tools/mcu-upstream/src/main diff "$PREVIOUS" "$CANDIDATE" -- kotlin java LICENSE
./gradlew candidateMcuUpstreamLock
diff -u gradle/mcu-upstream.lock.json build/mcu-upstream.lock.candidate.json
```

`candidateMcuUpstreamLock` writes a proposed lock without touching the real one. Check added and
removed files and any license change. If upstream adds a new API, a new mutation path or a new JVM
call, decide how to handle it.

> [!IMPORTANT]
> Don't loosen a drift check to make the build pass.

Adopt the lock, regenerate, and review the generated diff:

```sh
cp build/mcu-upstream.lock.candidate.json gradle/mcu-upstream.lock.json
git add gradle/mcu-upstream.lock.json tools/mcu-upstream/src/main
./gradlew verifyMcuUpstream :material-color-utilities:generateMcuSources
git diff --no-index build/mcu-update/previous material-color-utilities/build/generated/mcu/commonMain
diff -u build/mcu-update/previous-report.tsv material-color-utilities/build/reports/mcu-sources.tsv
```

Run every gate. Review the API differences before running `updateKotlinAbi`, which needs macOS.
After that, `checkKotlinAbi` should pass without further changes. Commit the submodule, the lock,
any rule changes, the reviewed dumps and fixtures, and any migration notes together, for example as
`fix(mcu): update upstream pin to <sha>`, and target `next`.

### Golden fixtures

Golden fixtures are only regenerated as part of a reviewed pin update. The generators read the
namespace-only reference, never the generated library, and each output file records the generator,
the upstream revision and the command that produced it.

```sh
# Compile first, because a cold build prints the transformer summary even with -q.
./gradlew :mcu-upstream:testClasses
./gradlew -q :mcu-upstream:printMcuRoleGoldens \
  > material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamRoleGoldenData.kt
./gradlew -q :mcu-upstream:printMcuQuantizerGoldens \
  > material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamQuantizerGoldenData.kt
./gradlew :material-color-utilities:spotlessApply
```

> [!CAUTION]
> Never edit fixture data by hand.

If only the pin moved, the diff should be the header and the
reviewed ARGB changes. A fixture change in a commit that also changes rules or algorithm behavior
needs an explanation.

### Upgrading the PSI parser

The `mcu-psi` version is bumped separately from the project's Kotlin version. After a bump, run
the transformer tests, the alternate parser test, the determinism check and the full gates, and
review the generated diff. A parser update alone shouldn't change any algorithm output.

#### Parser succession

`PsiSession` creates its parser with `KotlinCoreEnvironment.createForProduction`, behind the
`K1Deprecation` opt-in. JetBrains is removing that entry point along with K1. The pinned `mcu-psi`
2.4.20 still ships it. Its replacement, `buildStandaloneAnalysisAPISession`, is still experimental
and not on Maven Central ([KT-56203](https://youtrack.jetbrains.com/issue/KT-56203)).
`testAlternateParser` runs a newer parser on the same sources, so it fails as soon as a candidate
version drops `createForProduction`. Porting `PsiSession` to the standalone session then becomes
part of that upgrade.

## CI and the upstream monitor

The scripts are in `.github/scripts/`, and each has tests in `.github/tests/` that run against
throwaway Git repositories with no network access. `test_workflow_wiring.py` checks that the
workflows and scripts agree on every output and environment variable name. When you change them,
run these tests rather than the workflow.

`check-upstream` looks for upstream commits that touch `kotlin/` or the license and notice files.
It skips Java-only commits, because the engine is generated from Kotlin alone. It flags commits
that change build scaffolding under `kotlin/`, which could mean upstream is starting its own
multiplatform publication
([material-color-utilities#76](https://github.com/material-foundation/material-color-utilities/pull/76)).
It fetches over HTTPS, because `.gitmodules` uses an SSH remote that runners can't use. The script
is read-only. It outputs the revisions, and a commit list for the pull request body capped at 50
commits, with a compare link for the rest. Upstream issue references and mentions are rewritten so
they don't ping anyone in this repository.

`.github/workflows/upstream.yml` keeps one rolling pull request from `upstream/mcu` into `next`:

1. `preflight` fails fast if the token secret is missing.
2. `prepare` runs upstream code through Gradle, so it has no write token. `plan-upstream` decides
   what to do. `bump-upstream` moves the submodule, writes the lock, checks the transform and
   regenerates the golden fixtures in a separate commit. The commits are passed on as a Git bundle.
3. `publish` starts from a fresh checkout of the base commit. `publish-upstream` rejects a bundle
   that doesn't build on that base, touches anything other than the submodule, the lock and the two
   fixture files, or pins a different revision than planned. It then pushes with a lease and
   updates the pull request.

The bot can't refresh the API dumps, because `updateKotlinAbi` needs macOS. A red `checkKotlinAbi`
on the pull request means you need to review the API change and push the dumps yourself. Merging
always goes through the review described in [Updating the upstream pin](#updating-the-upstream-pin).

`plan-upstream` behaves as follows:

- With no open pull request, it opens one, unless a pull request for the same upstream revision
  was closed without merging. Closing it by hand snoozes the bump until upstream moves again.
- An open pull request is rewritten when upstream moves or when the pin on `next` changes.
  Commits from a maintainer stop the rewrite, and the bot comments once instead. "Update branch"
  merges and commits that only touch `<module>/api/**/*.api` don't count as maintainer commits,
  so a rewrite drops them and the dumps have to be pushed again.
- If `next` already pins every relevant change, the pull request is closed. The branch is deleted
  unless it has maintainer commits.
- A branch with maintainer commits and no open pull request is left alone. Delete the branch to
  resume.
- Pull requests from forks that use the same branch name are ignored.
