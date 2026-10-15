# Migrating to 6.0

6.0 splits the library into several artifacts and replaces the hand-ported color engine with one
generated from upstream's Kotlin source. Most apps only need new imports and one dependency change.
6.0 is not binary compatible with 5.x, so recompile anything that depends on it.

> [!IMPORTANT]
> Some defaults changed, so your colors can shift even when everything compiles. See
> [Colors that change without a code change](#colors-that-change-without-a-code-change).

<details>
<summary>Migrating with an AI assistant</summary>

Paste this prompt into your coding assistant:

```text
Migrate this project from MaterialKolor 5.x to 6.0 by following the guide at
https://raw.githubusercontent.com/jordond/MaterialKolor/main/docs/migration-6.0.md

1. Replace the com.materialkolor:material-kolor dependency with material-kolor-material3 if the
   project uses Compose Material 3, otherwise with material-kolor-core.
2. Update imports for every symbol the guide lists as moved, and fix the compile errors the guide
   describes. Don't rename or restructure anything else.
3. Replace calls to deprecated APIs as the guide describes, keeping the colors the same.
4. Find every place that generates a scheme or theme without passing specVersion, and every
   DynamicMaterialTheme, DynamicMaterialExpressiveTheme or rememberDynamicMaterialThemeState
   call that passes both seedColor and primary. The default colors changed for these. List them
   and ask me whether to keep the 5.x colors before changing them.
5. Build the project and fix what's left. Summarize what you changed and anything you weren't
   sure about.
```

</details>

## Dependencies

`material-kolor` is gone. Pick the artifact that matches your UI toolkit:

| You use                         | Depend on                                    |
| ------------------------------- | -------------------------------------------- |
| Material3                       | `com.materialkolor:material-kolor-material3` |
| Something else, or no UI at all | `com.materialkolor:material-kolor-core`      |

`material-kolor-material3` has `material-kolor-core` as an `api` dependency, so you don't need to
declare both. There are also new adapters for Compose Unstyled (`material-kolor-unstyled`) and
Compose Fluent (`material-kolor-fluent`), plus a kmpalette integration (`material-kolor-palette`).
See the [README](../README.md#install) for those.

## Colors that change without a code change

### Default spec is SPEC_2025

It was `SPEC_2021` in 5.x, and the default applies
everywhere: theme functions, `dynamicColorScheme`, `DynamicScheme` and the `Scheme*` constructors.
To keep the 5.x colors, pass the old spec:

```kotlin
DynamicMaterialTheme(
    seedColor = seed,
    specVersion = ColorSpec.SpecVersion.SPEC_2021,
    content = content,
)
```

### A primary override no longer replaces the seed

In 5.x, `DynamicMaterialTheme`,
`DynamicMaterialExpressiveTheme` and `rememberDynamicMaterialThemeState` treated a `primary`
override as the new seed, so it drove every palette. Now it sets only the primary palette, and
`seedColor` still drives secondary, tertiary, the neutrals and error. `dynamicColorScheme` already
worked this way. For the old result, pass the same color as both:

```kotlin
DynamicMaterialTheme(seedColor = brand, primary = brand, content = content)
```

### Image seeds

The image helpers now sample the bitmap down to 128x128 pixels before
quantizing. Pass `sampleArea = 0` to read every pixel. The quantizer also uses upstream's random
sequence now, so a few images pick a different seed than they did in 5.x.

## Moved to the material3 package

Everything that depends on Material3 moved from `com.materialkolor` to
`com.materialkolor.material3`. Nothing was renamed, so updating the imports is enough.

| Symbol                                                                            | New package                       |
| --------------------------------------------------------------------------------- | --------------------------------- |
| `DynamicMaterialTheme`, `DynamicMaterialExpressiveTheme`                          | `com.materialkolor.material3`     |
| `DynamicMaterialThemeState`, `rememberDynamicMaterialThemeState`                  | `com.materialkolor.material3`     |
| `LocalDynamicMaterialThemeSeed`                                                   | `com.materialkolor.material3`     |
| `dynamicColorScheme`, `rememberDynamicColorScheme`, `DynamicScheme.toColorScheme` | `com.materialkolor.material3`     |
| `animateColorScheme`, `harmonizeWithPrimary`, `colors`, `m3Colors`                | `com.materialkolor.material3.ktx` |

`PaletteStyle`, `MaterialKolors`, `Contrast` and the rest of `com.materialkolor.ktx` stay in core.

## Deprecations

These still work in 6.x and will be removed in 7.0.

### Primary-first overloads

The overloads that take `primary` in place of `seedColor` are deprecated. The IDE quick fix
passes the color as both `seedColor` and `primary`, which gives the same colors as before. If you
don't need the primary palette taken from the exact color, drop `primary` and let the style derive
it:

```kotlin
// Before
dynamicColorScheme(primary = brand, isDark = isDark)

// After
dynamicColorScheme(seedColor = brand, isDark = isDark)
```

### Image helpers in core

`themeColors`, `themeColor`, `themeColorOrNull`, `rememberThemeColors`, `rememberThemeColor` and
`QuantizerCelebi.quantize(ImageBitmap)` are replaced by `material-kolor-palette`. Its
[README](../material-kolor-palette/README.md) shows the replacements. If you keep using the
old helpers for now, `fallback` is required, and it comes first in `themeColors`:

```kotlin
// Before
val colors = bitmap.themeColors()
val seed = rememberThemeColor(image = bitmap)

// After
val colors = bitmap.themeColors(fallback = Color(0xFF4285F4))
val seed = rememberThemeColor(image = bitmap, fallback = MaterialTheme.colorScheme.primary)
```

## PaletteStyle is no longer an enum

`PaletteStyle` is a sealed interface now, and the existing styles are `data object`s.
`PaletteStyle.TonalSpot` and the others still work as values and in `when` branches. The enum
members are gone:

| 5.x                                | 6.0                                                     |
| ---------------------------------- | ------------------------------------------------------- |
| `PaletteStyle.entries`, `values()` | `PaletteStyle.KnownStyles`                              |
| `PaletteStyle.valueOf(name)`       | `PaletteStyle.fromName(name)` or `fromNameOrNull(name)` |
| `style.ordinal`                    | none, store `style.toString()`                          |

Names didn't change, so stored values still read back. To persist a style, write `toString()` and
read it back with `PaletteStyle.parse` or `parseOrNull`.

`Cmf` is a class rather than an object, so an exhaustive `when` needs an `is PaletteStyle.Cmf`
branch.

## Engine API

These changes only affect code that uses the low-level `material-color-utilities` types directly.

### Packages

`DynamicScheme` and `Variant` moved from `com.materialkolor.scheme` to
`com.materialkolor.dynamiccolor`. `TonePolarity` is now nested as `ToneDeltaPair.TonePolarity`.
The concrete schemes (`SchemeTonalSpot` and the rest) stay in `com.materialkolor.scheme`.

### Roles are properties

On `MaterialDynamicColors` and `ColorSpec`, `primary()` is now `primary`,
and the same goes for every other role and for `allDynamicColors`.

```kotlin
// Before
val argb = MaterialDynamicColors().primary().getArgb(scheme)

// After
val argb = MaterialDynamicColors().primary.getArgb(scheme)
```

### Legacy Android roles

`controlActivated`, `controlNormal`, `controlHighlight`,
`textPrimaryInverse`, `textSecondaryAndTertiaryInverse`, `textPrimaryInverseDisableOnly`,
`textSecondaryAndTertiaryInverseDisabled` and `textHintInverse` are no longer part of upstream.
Core keeps them as extensions on `MaterialDynamicColors`, which now need an import from
`com.materialkolor.ktx`. They are not available on `DynamicScheme`, so use
`scheme.getArgb(MaterialDynamicColors().controlActivated)` in place of `scheme.controlActivated`.
The `MaterialKolors` methods for these roles are unchanged.

### DynamicColor and ToneDeltaPair

- `DynamicColor.Builder` is gone. Use the constructor with named arguments, because the parameter
  order changed. There is no `copy`.
- `ToneDeltaPair.deltaConstraint` is now `constraint`.
- `TonePolarity.NEARER` and `FARTHER` were removed. `DeltaConstraint` still has both.

### Contrast

`Contrast.lighter` and `Contrast.darker` return `null` when no tone meets the
requested ratio. In 5.x they returned `-1.0`. The `Float` overloads were removed.

```kotlin
val tone = Contrast.lighter(baseTone, 4.5) ?: baseTone
```

`Color.lighten` and `Color.darken` behave as before.

### CorePalette

`CorePalette` was removed, together with the `ktx` helpers built on it. You can get the same
palettes from a `DynamicScheme`: `primaryPalette`, `secondaryPalette`, `tertiaryPalette`,
`neutralPalette`, `neutralVariantPalette` and `errorPalette`. The new `CorePalettes` is a plain
container with no factory methods, so it doesn't replace `CorePalette`.

### ContrastThreshold

The `ContrastThreshold` entries are now PascalCase. For example, `WCAG_AA_NORMAL_TEXT` became
`WcagAaNormalText`.

### Narrowed or removed

- `ColorSpec2021`, `ColorSpec2025` and `HctSolver` are now internal.
- The raw `ColorUtils` math (`linearized`, `delinearized`, `labF`, `labInvf`, `argbFromLinrgb`) is
  now internal.
- `ViewingConditions` keeps `n`, `aw`, `nbb` and `flRoot` public. Its other getters are now
  internal.
- `DynamicScheme.getPiecewiseValue` and `getRotatedHue` were removed.
- The `DynamicScheme` constructor that took palettes before the platform was removed. Use named
  arguments.
- Subclassing `DynamicScheme` or `ColorSpec` now requires `@OptIn(InternalMaterialKolorApi::class)`.
