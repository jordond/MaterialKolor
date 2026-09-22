# Maintaining the upstream Kotlin engine

## Source and toolchain pins

Initialize the exact recorded submodule revision before building:

```sh
git submodule update --init --recursive
./gradlew verifyMcuUpstream
./gradlew :material-color-utilities:generateMcuSources --configuration-cache
```

## What the adapter may change

Untouched text, copyright headers and comments are preserved. Unsupported shapes fail with the file,
location and rule identity.

The adapter makes three kinds of change, each reviewable on its own:

1. Namespace relocation changes upstream root packages to `com.materialkolor.*`.
2. Portability rules replace audited JVM operations with common Kotlin operations or narrow internal
   helpers.
3. MaterialKolor semantic rules preserve deliberate conveniences and immutable/value behavior.

| Upstream operation                             | Common replacement                                                              |
|------------------------------------------------|---------------------------------------------------------------------------------|
| `ArrayList`, `HashMap`, `LinkedHashMap`        | Kotlin collections with their required ordering                                 |
| `Arrays.sort`, `Collections.sort`              | In-place array/list sorting with the original comparator                        |
| `Collections.unmodifiableList`                 | A copy behind the audited private cache boundary                                |
| `Math.toRadians`, `Math.toDegrees`, `Math.max` | Common math/helpers                                                             |
| RGB `String.format`                            | Locale-independent lowercase hexadecimal with two digits per channel            |
| `Locale.ENGLISH` enum lowercasing              | Locale-independent Kotlin lowercasing                                           |
| Seeded `java.util.Random`                      | Internal Java-compatible 48-bit generator, including bounded rejection sampling |
| `DecimalFormat("0.0")` for scheme contrast     | The narrow diagnostic formatting policy below                                   |

Import aliases are permitted only when their target and call shape are audited. Wildcard JVM
imports, unknown JVM APIs/annotations, ambiguous symbol shadowing, new public mutation paths,
overlapping edits and source inventory drift require review.

Generated library sources live under `material-color-utilities/build/generated/mcu/commonMain/`. A
failed transform must leave the previous complete output intact. Review the deterministic report at
`material-color-utilities/build/reports/mcu-sources.tsv` alongside adapted source diffs.

The original Java compiled by `:mcu-upstream` and the raw Kotlin it carries as test fixtures are
independent JVM references. The latter performs namespace relocation only, into `upstream.kotlin.*`;
it must not receive portability or MaterialKolor semantic rules. Its report is
`tools/mcu-upstream/build/reports/mcu-reference.tsv`. Neither reference nor the transformer is a
public library dependency.

### Semantic rule budget

The semantic pass is the most expensive part of this adapter to maintain: every upstream refactor of
a declaration a rule selects becomes rule-repair work. Each rule is budgeted against one test:

> A semantic transformation is justified ONLY if the behavior requires in-class access:
> private/internal member access, equality/hashCode/toString overrides, constructor/visibility
> changes, removing a public mutation surface, or altering supertype/annotation shape. Anything
> expressible as a handwritten extension, top-level factory, or facade in ordinary source must be
> handwritten.

Rules fall into two groups. **Group A** passes the test: handwritten source cannot express it at
all. **Group B** does not: the injected declaration only calls public API and would compile as a
handwritten extension. Group B is retained under one exception: an unimported extension does not
replace a member where receiver-call ergonomics require one. Each group B declaration is already a
reviewed entry in `material-color-utilities/api/`, moving it would change that dump and force an
import at call sites where the neighboring upstream members need none, and
`material-color-utilities` has no handwritten public source file to receive it: the only handwritten
file in the module's production tree,
`src/commonMain/kotlin/com/materialkolor/compat/PlatformCompat.kt`, is `internal` portability
support. Group B is the part to revisit first if that policy changes, and it must not grow without
an explicit API-dump review.

Every rule ID below is recorded per file in `build/reports/mcu-sources.tsv`. A new rule must add a
row.

| Rule ID                     | Verdict        | Justification                                                                                                                                                                                                                                                                                                                             |
|-----------------------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `analogous-property`        | KEEP (A)       | Replaces `TemperatureCache.getAnalogousColors()` with `val analogousColors`. Both compile to `getAnalogousColors()Ljava/util/List;`, so the property can exist only if the no-argument member is deleted, and deleting a member needs in-class access.                                                                                    |
| `cam16-visibility`          | KEEP (A)       | Widens five `internal` `Cam16` members to `public`. Visibility change.                                                                                                                                                                                                                                                                    |
| `contrast-float`            | KEEP (B)       | Float overloads of existing `Contrast` members. As extensions they would split the overload set: an unimported `Contrast.lighter(tone, ratioFloat)` would fail to resolve rather than fall back to the `Double` member.                                                                                                                   |
| `dynamic-color-factory`     | KEEP (B)       | `DynamicColor.Companion.fromPalette`. A companion extension preserves the call shape but requires an import; the member and its `fromPalette$default` synthetic are reviewed dump entries.                                                                                                                                                |
| `enum-default`              | KEEP (A)       | Adds `companion object { val Default }` to `SpecVersion` and `Platform`. A companion object cannot be attached to a class from outside it.                                                                                                                                                                                                |
| `harmonize-hct`             | KEEP (B)       | `Hct` overload of `Blend.harmonize`. Same overload-set split as `contrast-float`; `material-kolor-core`'s own `Blend.harmonize(Color, Color, Boolean)` extension resolves it through the implicit receiver.                                                                                                                                    |
| `hct-copy-body`             | KEEP (A)       | Rewrites the in-place `setInternalState` call into `return Hct(...)`. Reads a private member and a private constructor.                                                                                                                                                                                                                   |
| `hct-copy-method`           | KEEP (A)       | Renames `setHue`/`setChroma`/`setTone` to `withHue`/`withChroma`/`withTone`, removing the public mutation surface.                                                                                                                                                                                                                        |
| `hct-copy-type`             | KEEP (A)       | Adds the `Hct` return type that rename requires. Signature change.                                                                                                                                                                                                                                                                        |
| `hct-value-contract`        | KEEP (A and B) | `equals`/`hashCode` over `argb` can only be members. The bundled `Float` `withHue`/`withChroma`/`withTone` and `isBlue`/`isYellow`/`isCyan` are group B, kept as members because `docs/upstream-api-inventory.md` records them as preserved delegating members; they share this rule ID rather than splitting it and changing the report. |
| `hide-palette-cache`        | KEEP (A)       | Makes `TonalPalette.cache` private. Visibility change, and what keeps the cache outside `value-equality`.                                                                                                                                                                                                                                 |
| `implementation-visibility` | KEEP (A)       | Narrows nine quantizer, spec and math types to `internal`. Visibility change.                                                                                                                                                                                                                                                             |
| `initial-tone-default`      | KEEP (A)       | Adds `= null` to the `getInitialToneFromBackground` parameter. Signature change.                                                                                                                                                                                                                                                          |
| `luminance-member`          | KEEP (B)       | `ColorUtils.calculateLuminance`. `docs/upstream-api-inventory.md` records "Preserve member" for it and `docs/migration-6.0.md` states it preserves receiver-call ergonomics; both are reviewed decisions.                                                                                                                                 |
| `scheme-copy`               | KEEP (B)       | `DynamicScheme.copy` and the four nullable dim roles. An extension named `copy` would be silently shadowed if upstream ever made the class a data class, and the dim roles would need an import while the adjacent role members do not.                                                                                                   |
| `scheme-error-default`      | KEEP (A)       | Adds the `errorPalette` default to the primary and secondary constructors. Constructor change.                                                                                                                                                                                                                                            |
| `score-default`             | KEEP (A)       | Adds `desired`, `fallbackColorArgb` and `filter` defaults to the full `score` overload. Signature change.                                                                                                                                                                                                                                 |
| `score-nullable-fallback`   | KEEP (A)       | Widens `fallbackColorArgb` to `Int?` on the three- and four-argument overloads. Signature change.                                                                                                                                                                                                                                         |
| `score-optional-fallback`   | KEEP (A)       | Guards the fallback append on the now-nullable parameter. Body change that only exists behind that signature change.                                                                                                                                                                                                                      |
| `value-equality`            | KEEP (A)       | Injects `equals`/`hashCode`/`toString` into `TonalPalette`, `TemperatureCache` and `ContrastCurve`. Named by the test.                                                                                                                                                                                                                    |
| `value-input-visibility`    | KEEP (A)       | Makes the `TemperatureCache` and `ContrastCurve` constructor inputs private. Visibility change.                                                                                                                                                                                                                                           |
| `viewing-visibility`        | KEEP (A)       | Widens five `ViewingConditions` constructor inputs from `internal` to `public`. Visibility change.                                                                                                                                                                                                                                        |

Four identifiers in the same pass record no edit. They exist to fail, with file and location, when
upstream moves something a rule depends on. They add no API surface and are outside the budget, but
they are still maintenance and a new one needs the same review as a rule:

| Guard ID               | What it refuses                                                                                                        |
|------------------------|------------------------------------------------------------------------------------------------------------------------|
| `semantic-inventory`   | A file whose path and package no longer agree, which would otherwise silently skip every rule for it.                  |
| `palette-cache`        | `TonalPalette.cache` losing the mutable non-private shape that `hide-palette-cache` hides.                             |
| `hct-mutation-surface` | Any change to `Hct`'s constructor, method and property inventory, writable properties, or internal mutation structure. |
| `score-fallback`       | A change to the one-, two-, three- and four-argument `Score.score` overload inventory.                                 |

No rule has been moved to handwritten source so far. A move must replace its row above with "moved
to handwritten `<file>` on `<date>`" and record the resulting API dump diff as a reviewed change.

### Diagnostic formatting

The common contrast formatter is only for the finite, one-decimal diagnostic value in
`DynamicScheme.toString()`. It uses common `round(abs(value) * 10)` with ties to even on that scaled
binary floating-point value, writes exactly one decimal digit and preserves a negative sign
including negative zero. Finite magnitudes of at least `2^52` (`4503599627370496.0`) are integral at
the available precision and receive a `.0` suffix. Non-finite values are rejected. It is locale
independent and has tie/sign regression tests. This policy can differ from Java decimal formatting
at binary/decimal rounding boundaries. It does not implement general `DecimalFormat` patterns,
locale selection, grouping or arbitrary precision, and its output is a diagnostic rather than a
stable serialization format. Any rounding-policy change needs a regression case and review alongside
the helper's tests.

## Verification gates

Run every gate from the repository root:

```sh
./gradlew verifyMcuUpstream
rm -rf material-color-utilities/build/generated/mcu
./gradlew :material-color-utilities:generateMcuSources --no-build-cache --rerun-tasks
./gradlew :mcu-source-transformer:test :mcu-source-transformer:testAlternateParser
./gradlew checkKotlinAbi spotlessCheck
./gradlew verifyMcuJvm
./gradlew verifyMcuWeb
./gradlew verifyMcuAndroid
./gradlew verifyMcuApple
./gradlew verifyMcuPublication
./gradlew -Pmaterialkolor.useLocal=true :builder:shared:jvmTest :builder:shared:testAndroidHostTest
./gradlew -Pmaterialkolor.useLocal=true :builder:shared:composeCompatibilityBrowserDistribution
python3 -B -m unittest discover -s .github/tests -v
```

| Gate                                          | What it proves                                                                                                                                                                                                                                                            |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `verifyMcuUpstream`                           | The submodule checkout, Kotlin inventory, source hashes and license hash are the identity recorded in `gradle/mcu-upstream.lock.json`.                                                                                                                                    |
| `:mcu-source-transformer:test`                | Rules select the shapes they claim to, edits do not overlap, untouched text survives, and an unsupported shape fails with its file, location and rule identity.                                                                                                           |
| `:mcu-source-transformer:testAlternateParser` | The same corpus transforms under a second PSI runtime, so a parser change cannot alter the output or drop `createForProduction` unnoticed.                                                                                                                                |
| Determinism check                             | Regeneration from the same inputs is byte-identical. Compare the file inventory, a digest of the generated tree, and `build/reports/mcu-sources.tsv` against the previous run.                                                                                            |
| `checkKotlinAbi`                              | The public surface still matches the reviewed dumps in `material-color-utilities/api/`, `material-kolor-core/api/` and `material-kolor-material3/api/`, without regenerating them. It covers the JVM and Android class surfaces and, through each module's `.klib.api`, the native, `js` and `wasmJs` surfaces. |
| `spotlessCheck`                               | Handwritten code is formatted. Generated and raw upstream code is excluded from formatting.                                                                                                                                                                               |
| `verifyMcuJvm`                                | The generated implementation produces the same ARGB as both the exact-revision Java reference and the namespace-only Kotlin reference, and the characterization tests that protect MaterialKolor conveniences still pass.                                                 |
| `verifyMcuWeb`                                | The common fixtures pass in Node and in a headless browser for both `js` and `wasmJs`, including the Compose consumers on their Skiko runtime.                                                                                                                            |
| `verifyMcuAndroid`                            | Every library module assembles its Android variants, its host tests pass, and lint analysis runs.                                                                                                                                                                         |
| `verifyMcuApple`                              | The common fixtures pass on macOS and the iOS simulator, and the iOS device and simulator frameworks compile and link.                                                                                                                                                    |
| `verifyMcuPublication`                        | Artifacts, metadata, source archives and documentation build into a temporary repository under `build/`, and an artifact-only consumer compiles against them.                                                                                                             |
| Builder against the local engine              | A real consumer resolves `project(":material-kolor-core")`, `project(":material-kolor-material3")` and `project(":material-color-utilities")` instead of published coordinates, and still compiles and tests.                                                                                                          |
| `.github/tests`                               | The upstream monitor classifies commits correctly against disposable Git repositories, with no network access and no external issue or comment creation.                                                                                                                  |

The publication gate must not publish to Maven Central or deploy documentation. Source archives
include the generated Kotlin, handwritten helpers and upstream license material exactly once.
PSI/compiler, reference, transformer and scratchpad dependencies must never enter published
metadata.

`dokkaGenerate` reports one unresolved KDoc link, `[DynamicScheme]` in `palettes/CorePalettes.kt`.
That file is generated output and the reference is inherited from the upstream Javadoc. Leave the
warning alone, do not patch generated sources for it.

Lifecycle tests use disposable fixture checkouts for dirty/missing sources, cache restoration, stale
output, configuration-cache reuse, relocation and offline behavior. Do not mutate the production
submodule to test rejection paths. Neither a normal build nor a test may silently regenerate
reviewed API baselines or golden fixtures.

## Reviewing an upstream update

Keep the previous generated tree/report available before changing the pin so both raw and adapted
diffs can be reviewed. Fetching upstream is an explicit maintenance action:

```sh
./gradlew :material-color-utilities:generateMcuSources
mkdir -p build/mcu-update
cp -R material-color-utilities/build/generated/mcu/commonMain build/mcu-update/previous
cp gradle/mcu-upstream.lock.json build/mcu-update/previous-lock.json
cp material-color-utilities/build/reports/mcu-sources.tsv build/mcu-update/previous-report.tsv
PREVIOUS_REVISION=$(git -C tools/mcu-upstream/src/main rev-parse HEAD)

git -C tools/mcu-upstream/src/main fetch origin
# Replace this value with the full, reviewed candidate revision.
CANDIDATE_REVISION=the-full-reviewed-commit-sha
git -C tools/mcu-upstream/src/main checkout --detach "$CANDIDATE_REVISION"
git -C tools/mcu-upstream/src/main diff "$PREVIOUS_REVISION" "$CANDIDATE_REVISION" -- kotlin java LICENSE
./gradlew candidateMcuUpstreamLock
diff -u gradle/mcu-upstream.lock.json build/mcu-upstream.lock.candidate.json
```

`candidateMcuUpstreamLock` writes `build/mcu-upstream.lock.candidate.json` from the clean proposed
submodule checkout. Inspect added/removed files and their content changes, and review license
changes. The tracked lock is not rewritten by this task. Review rules and their shape expectations
before adopting the candidate. A new upstream API, mutation path or JVM operation is a policy
decision, not a reason to relax drift checks.

After reviewing the candidate and policy changes, adopt and stage the input identity together:

```sh
cp build/mcu-upstream.lock.candidate.json gradle/mcu-upstream.lock.json
git add gradle/mcu-upstream.lock.json tools/mcu-upstream/src/main
./gradlew verifyMcuUpstream :material-color-utilities:generateMcuSources
git diff --no-index build/mcu-update/previous material-color-utilities/build/generated/mcu/commonMain
diff -u build/mcu-update/previous-report.tsv material-color-utilities/build/reports/mcu-sources.tsv
```

The diff commands exit 1 when there are differences, review them before continuing. Run every
applicable gate above, including local Builder consumers. Review API differences before invoking
`updateKotlinAbi`; subsequent `checkKotlinAbi` must pass without updating expectations. Independent
golden fixtures need independently established expected values and an explained change, not
adapted-output snapshots.

Commit the Gitlink, lock, rule changes, reviewed API/fixture changes and migration notes together,
using the repository's Conventional Commits style, for example
`fix(mcu): update upstream pin to <sha>`. Record exact commands/results and host limitations in the
change description. Target `next`; maintenance verification does not authorize a remote release or
deployment.

### Regenerating golden fixtures

Golden fixtures are regenerated only as part of a reviewed upstream update.
`conformance.GenerateGoldenFixtures` and `conformance.GenerateQuantizerFixtures` read the
namespace-only Kotlin reference, never the adapted library, and each writes a header naming the
generator, the upstream revision read from `gradle/mcu-upstream.lock.json` at generation time, and
the command that produced the file. Regenerate from the repository root, then format the result:

```sh
./gradlew -q :mcu-upstream:printMcuRoleGoldens \
  > material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamRoleGoldenData.kt
./gradlew -q :mcu-upstream:printMcuQuantizerGoldens \
  > material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamQuantizerGoldenData.kt
./gradlew :material-color-utilities:spotlessApply
```

Never hand-edit fixture data, and never include a fixture change in a commit that also changes
transformer rules or algorithm behavior unless that commit explains the fixture diff. When only the
pin moved, the diff is the header plus reviewed ARGB changes.

### Upgrading the PSI parser

Change the `mcu-psi` version independently of the Kotlin compiler version. Run
`:mcu-source-transformer:test` and `:mcu-source-transformer:testAlternateParser` (the catalogue's
Kotlin), deterministic-generation checks and all compilation/parity gates with the actual project
compiler. Review the report and generated-source diff. A parser-only update should not silently
change the public algorithm output. Never assume a parser runtime test proves compatibility with a
different compiler used to build the tool.

#### Parser API succession

`PsiSession` in
`tools/mcu-source-transformer/src/main/kotlin/com/materialkolor/transformer/psi/PsiSession.kt` opts
into
`org.jetbrains.kotlin.K1Deprecation` and builds its parser with
`KotlinCoreEnvironment.createForProduction`, an entry point JetBrains is removing along with the K1
frontend. The pinned `mcu-psi` runtime (currently `2.4.20`) still ships that environment, so the
transformer uses it rather than an unstable replacement. The supported successor is the Analysis API
standalone session, `buildStandaloneAnalysisAPISession` in
`org.jetbrains.kotlin.analysis.api.standalone`; as of September 2026 that API is still experimental
and is not published to Maven Central, tracked by
[KT-56203](https://youtrack.jetbrains.com/issue/KT-56203).
`:mcu-source-transformer:testAlternateParser` runs a newer parser against the same corpus: when a
candidate pin drops `createForProduction`, that task fails before the pin is adopted, and porting
`PsiSession` to the standalone session becomes part of the parser upgrade.

## CI and the upstream monitor

`.github/check-upstream` reports Java, Kotlin and license/notice changes. It classifies each
reported commit: `upstream-source` for ordinary source changes, and `kotlin-build-scaffold` when the
commit touches build or publishing scaffolding under upstream's `kotlin/` tree, which may indicate
that upstream is starting its own Kotlin Multiplatform publication (see
[material-color-utilities#76](https://github.com/material-foundation/material-color-utilities/pull/76)).
Scaffold findings carry that note and a matching issue label. Without a token it prints findings,
with a token the scheduled workflow can create deduplicated upstream issues. Its tests use
disposable Git repositories plus stubbed `git fetch` and `curl`; they create no external issues or
comments. Run those tests, not the scheduled workflow, when validating monitor changes.
