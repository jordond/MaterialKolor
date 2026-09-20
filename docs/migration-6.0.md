# Migrating to the upstream Kotlin engine in 6.0

This is an intentional source and binary API migration on `next`. Artifact coordinates remain
unchanged. The low-level engine now comes from pinned upstream Kotlin, including the 2026 color
specification and CMF scheme. Compose theme, conversion, remember, image and animation conveniences
remain in their existing modules.

## Package and role changes

Update imports for `DynamicScheme` and `Variant`:

```kotlin
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.Variant
```

They no longer belong to `com.materialkolor.scheme`. Concrete schemes, such as `SchemeTonalSpot` and
`SchemeCmf`, remain under `com.materialkolor.scheme`. There is no old-package ABI facade.

Use Kotlin role properties in place of Java-style role functions. For example, `scheme.primary`
gives the primary ARGB value and `MaterialDynamicColors().primary` gives its dynamic provider. Use
`getArgb(scheme)` or `getHct(scheme)` on a non-null provider to resolve it.

Dim roles and some provider callbacks are nullable. Decide the appropriate fallback at the call
site, such as falling back to the corresponding ordinary role. A missing role is `null`, not ARGB
zero or another sentinel. Callback signatures for background, contrast curve, tone-delta pair and
opacity also reflect upstream nullability.

### Legacy Android roles moved to `ktx` extensions

Upstream Kotlin no longer defines the eight Java-era Android roles `controlActivated`,
`controlNormal`, `controlHighlight`, `textPrimaryInverse`, `textSecondaryAndTertiaryInverse`,
`textPrimaryInverseDisableOnly`, `textSecondaryAndTertiaryInverseDisabled` and `textHintInverse`.
They are gone from `ColorSpec`, `MaterialDynamicColors` and `DynamicScheme` in
`material-color-utilities`.

MaterialKolor keeps them as extension properties on `MaterialDynamicColors` in `material-kolor`, so
they now need an explicit import:

```kotlin
import com.materialkolor.ktx.controlActivated

val role = MaterialDynamicColors().controlActivated
```

The equivalent `MaterialKolors` methods, such as `MaterialKolors.controlActivated()`, are unchanged.
These are MaterialKolor conveniences, not upstream conformance roles.

## Source lists, custom palettes and spec fallback

`DynamicScheme` supports both a nonempty `sourceColorHctList` and a single-`sourceColorHct`
constructor. `sourceColorHct`/`sourceColorArgb` still refer to the first source. Keep using direct
custom-palette injection and the supported copy/factory conveniences when an application supplies
its own primary, secondary, tertiary, neutral, neutral-variant or error palette.

The general defaults remain `ColorSpec.SpecVersion.SPEC_2021` and `DynamicScheme.Platform.PHONE`. A
requested specification may fall back according to the upstream variant support rules. Read
`scheme.specVersion` when the effective specification matters; do not assume the requested value was
honored. In this pin, expressive, vibrant, tonal-spot and neutral fall back from 2026 to 2025, while
other non-CMF variants use 2021.

`SchemeCmf` is available directly through the low-level API, accepts one or two source colors and
requires `SPEC_2026`. It has its own 2026 default; this does not change the general default. A
second source supplies the tertiary hue/chroma, while a one-source scheme follows upstream's
single-source behavior. Empty lists are unsupported. There is no new high-level multi-seed or
`PaletteStyle` abstraction in this migration.

## Constructors replace the nested Builder

`DynamicColor` and `ToneDeltaPair` use the upstream data-class constructors, named arguments,
defaults and `copy`. For example:

```kotlin
val customRole = DynamicColor(
    name = "custom",
    palette = { it.primaryPalette },
    tone = { if (it.isDark) 80.0 else 40.0 },
)
val backgroundRole = customRole.copy(isBackground = true)
```

The Java-era `DynamicColor.Builder` is retired. `fromPalette` conveniences remain thin factories.
Use `constraint`, the upstream spelling, for tone-delta constraints. `TonePolarity` is now nested
under `ToneDeltaPair`; update its import to
`com.materialkolor.dynamiccolor.ToneDeltaPair.TonePolarity`. Spec extensions apply to the selected
spec and later specs (`>=`).

Data-class equality includes constructor values. Function-valued constructor arguments use their
ordinary function equality; separately created but behaviorally identical lambdas do not become
equal. Private calculation caches are not part of equality or hashing.

## Nullable contrast results

`Contrast.lighter` and `Contrast.darker` return `Double?`; Float overloads return `Float?`. An
unattainable contrast request returns `null`. Replace checks for a negative failure sentinel with
explicit nullable handling:

```kotlin
val adjustedTone = Contrast.lighter(originalTone, desiredRatio) ?: originalTone
```

Compose `Color.lighten` and `Color.darken` keep their fallback to the original color when no
solution exists. Existing unsafe contrast helpers retain their documented clamping behavior; use
them only when that behavior is intended.

## Retired CorePalette factories

The deprecated Java-only `CorePalette` factory API is removed. Select an appropriate concrete
dynamic scheme, then use its palettes (`primaryPalette`, `secondaryPalette`, `tertiaryPalette`,
`neutralPalette`, `neutralVariantPalette`, `errorPalette`). Use direct palette injection when you
already have custom palettes.

`CorePalettes` is not a rename of `CorePalette`. It is a container with a different shape, has no
legacy factory algorithms and lacks the legacy error-palette field. Replacing the type name alone
does not preserve behavior.

## Preserved MaterialKolor behavior

- `Hct` remains publicly immutable, compares and hashes by ARGB value, and provides non-mutating
  `withHue`, `withChroma`, `withTone` plus instance hue classifiers.
- `TonalPalette`, `TemperatureCache` and `ContrastCurve` retain their deliberate value-equality
  inputs. Implementation caches stay private and do not affect equality or hashing.
- Retained Float/member overloads and local helpers, including `ColorUtils.calculateLuminance`,
  preserve receiver-call ergonomics.
- Custom color overrides, theme state, AMOLED rules, Compose conversion/remember helpers, image
  fallbacks and animation behavior remain supported.

The checked-in public API baselines and [local-delta inventory](upstream-api-inventory.md) document
the exact public API. This release does not promise binary compatibility with 5.x; recompile
downstream code after updating imports, constructors and nullable results.

## Image quantization can produce different colors

The old Kotlin port seeded Kotlin's `Random` in `QuantizerWsmeans`. The generated implementation
reproduces upstream Java's seeded random sequence, including bounded draws. Identical image inputs
can therefore choose different initial clusters and produce different palette populations or
selected seed colors. This is an intentional upstream-conformance correction, not nondeterminism.

Review image-derived golden outputs separately from unrelated API migration. Use independently
established upstream expected values; do not regenerate expectations because the adapted
implementation disagrees. Empty/transparent image fallbacks and population/tie ordering remain
explicit test cases.

For an RNG regression case, the 96-pixel fixture in the common conformance tests gives first cluster
`0xff495e42` with population 12 in the old port and `0xff486048` with population 13 in upstream. The
direct ordered fixture records all eight clusters and the score result; both references
independently establish its values.

Scheme diagnostic text uses a locale-independent one-decimal contrast formatter. It is not a general
`DecimalFormat` replacement or a persistence format. See [the maintainer guide](upstream-psi.md) for
the source, transformation and verification contracts.

## Invalid non-finite palette inputs

Pinned upstream Kotlin rejects `TonalPalette.fromHueAndChroma(Double.NaN, 0.0)` with
`IllegalArgumentException`. The old port and raw Java accepted it because their integer conversion
handled NaN differently. Keep palette inputs finite. The generated implementation preserves this
upstream behavior; value equality for constructible palettes and NaN/signed-zero semantics in
`ContrastCurve` are separately tested.
