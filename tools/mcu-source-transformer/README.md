# Upstream source adapter

This non-published tool compiles with the repository's Kotlin compiler and runs PSI with the
independently pinned `mcu-psi` compiler-embeddable dependency from `gradle/libs.versions.toml`.
Compiler classes are loaded only in the separate generator JVM.

`com.materialkolor.transformer.MainKt` takes six positional arguments.

```
<inputDir> <outputDir> <reportFile> <lockFile> <library|reference> <parserVersion>
```

The input directory is the pinned submodule's `kotlin/` directory. The reviewed
lock records schema/policy versions, revision, every relative Kotlin path and
SHA-256, and the SHA-256 of the adjacent `LICENSE`. The Gradle provenance task
independently verifies Git identity and cleanliness before any cache reuse.
Ordinary generation never updates a lock or performs Git/network operations.

The passes are deliberately small.

- `PsiSession` holds one disposable compiler environment per adaptation batch.
  Syntax errors and comment tokens are checked before and after transformation.
- `NamespaceRules` relocates package and import nodes. This is the **only** pass
  enabled for the raw Kotlin reference, preserving all original JVM dependencies
  and mutable/value behavior.
- `PortabilityRules` covers audited collection imports, sorting, Java-compatible
  random calls, angle helpers, RGB text, locale-independent enum names, and the
  specified finite contrast formatter. Unknown JVM references, annotations,
  shadowing, unsupported operations, and overlapping edits fail with a source
  location and rule name. Compiler/parity checks remain required because PSI is
  not symbol resolution.
- `SemanticRules` adds guarded members and visibility/value contracts. These
  preserve Hct immutability, cache-independent value equality, Float delegates,
  nullable Score fallback, small factories, and source-list/custom-palette scheme
  copying. Color calculations are still upstream implementations.
- `SourceLock` and `Generator` validate the full inventory before writing, stage
  the complete candidate tree, replace owned output and report with rollback, and
  emit relative paths, hashes and sorted rule counts without timestamps.

`rules/Mappings.kt` is the single table of JVM symbols the library passes
recognize. Review it alongside the rule policy.

`src/main/resources/library-policy-v1.tsv` independently records the eligible
library files and exact counts of each rule. Updating upstream requires reviewing
both the input lock and this rule policy. A newly locked source cannot silently
introduce a new adaptation operation. Reference fixtures may use smaller reviewed
inventories because only namespace relocation runs in reference mode.

Untouched source text and original Apache comments remain unchanged. CRLF inputs
retain CRLF. Mixed newline conventions and comment-bearing rewrites that would
lose trivia are refused for review.

Run `:mcu-source-transformer:test` for unit and isolated Gradle lifecycle checks.
Run `:mcu-source-transformer:testAlternateParser` for the same syntax/contract tests
with the catalogue's Kotlin as the parser and the original compiled transformer binary. The latter omits
TestKit lifecycle cases, whose child process is intentionally configured with the
production parser. Neither task changes the production parser pin.

Run `:material-color-utilities:verifyMcuParserAgreement` to regenerate the shipped
library tree with the alternate parser and require byte-identical output. The
`verifyMcuJvm` aggregate runs it in CI, so a divergence between the two front
ends fails the build rather than going unnoticed.
