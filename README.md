<picture>
  <source media="(prefers-color-scheme: dark)" srcset="art/materialkolor-logo-dark.svg">
  <img width="500" src="art/materialkolor-logo.svg" alt="MaterialKolor">
</picture>
<br />

![Maven Central](https://img.shields.io/maven-central/v/com.materialkolor/material-kolor-core)
[![Kotlin](https://img.shields.io/badge/kotlin-v2.4.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![MCU](https://img.shields.io/badge/mcu-5b3618b-blue)](https://github.com/material-foundation/material-color-utilities/tree/5b3618b16fdc3825e21d5679bafd144662088ea1)
[![Build](https://github.com/jordond/materialkolor/actions/workflows/ci.yml/badge.svg)](https://github.com/jordond/materialkolor/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/jordond/MaterialKolor)](https://opensource.org/license/mit/)

[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.12.1-blue)](https://github.com/JetBrains/compose-multiplatform)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-DB413D.svg?style=flat)
![badge-js](http://img.shields.io/badge/platform-js%2Fwasm-FDD835.svg?style=flat)

MaterialKolor generates Material color schemes from a single seed color in Compose Multiplatform.
It runs Google's [material-color-utilities](https://github.com/material-foundation/material-color-utilities)
on Android, iOS, macOS, desktop JVM, JS and Wasm. There are adapters for Material 3, Compose
Unstyled and Compose Fluent, and you can use the core module to build a theme of your own.

[MaterialKolor Builder](https://materialkolor.com) lets you try seeds and styles in the browser and
exports the theme code. API docs are at [docs.materialkolor.com](https://docs.materialkolor.com).

> [!NOTE]
> Upgrading from 5.x? Read the [6.0 migration guide](docs/migration-6.0.md).

## Install

Pick the artifact for your UI toolkit. Each adapter brings in `material-kolor-core` for you.

| Artifact | Use it for |
|---|---|
| `material-kolor-material3` | Material 3 apps |
| [`material-kolor-unstyled`](material-kolor-unstyled/README.md) | [Compose Unstyled](https://composeunstyled.com) apps |
| [`material-kolor-fluent`](material-kolor-fluent/README.md) | [Compose Fluent](https://github.com/Compose-Fluent/compose-fluent-ui) apps |
| `material-kolor-core` | Your own theme, or any app that only needs the colors |
| [`material-kolor-palette`](material-kolor-palette/README.md) | Seed colors from images, using [kmpalette](https://github.com/jordond/kmpalette) |
| `material-color-utilities` | The color engine on its own, without Compose |

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.materialkolor:material-kolor-material3:6.0.0")
        }
    }
}
```

Or with a version catalog:

```toml
[versions]
materialKolor = "6.0.0"

[libraries]
materialKolor-material3 = { module = "com.materialkolor:material-kolor-material3", version.ref = "materialKolor" }
```

The Unstyled and Fluent adapters also need the toolkit itself declared next to them. Their READMEs
cover the details.

## Material 3

`DynamicMaterialTheme` wraps `MaterialTheme` and generates the color scheme from a seed:

```kotlin
@Composable
fun AppTheme(seedColor: Color, content: @Composable () -> Unit) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme(),
        animate = true,
        content = content,
    )
}
```

If you'd rather call `MaterialTheme` yourself, `rememberDynamicColorScheme` returns the
`ColorScheme`, and `dynamicColorScheme` does the same outside of composition.

```kotlin
val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark)
MaterialTheme(colorScheme = colorScheme, content = content)
```

All of them accept the same options:

- `style` picks a [`PaletteStyle`](material-kolor-core/src/commonMain/kotlin/com/materialkolor/PaletteStyle.kt),
  such as `TonalSpot` (the default), `Vibrant`, `Expressive` or `Monochrome`.
- `primary`, `secondary`, `tertiary`, `neutral`, `neutralVariant` and `error` pin that palette to
  an exact color. The seed still drives the rest.
- `contrastLevel` runs from -1.0 to 1.0. `Contrast.Medium.value` and `Contrast.High.value` are
  the standard steps.
- `isAmoled` makes the dark background and surface pure black.
- `specVersion` defaults to `SPEC_2025`. Pass `ColorSpec.SpecVersion.SPEC_2021` for the colors
  MaterialKolor produced before 6.0.

For Material 3 Expressive, use `DynamicMaterialExpressiveTheme`. It defaults to
`PaletteStyle.Expressive` and accepts a `motionScheme`.

## Your own theme

`material-kolor-core` doesn't depend on Material 3. `rememberDynamicScheme` generates the scheme,
and `MaterialKolors` reads its roles as Compose colors:

```kotlin
val scheme = rememberDynamicScheme(seedColor = seedColor, isDark = isDark)
val kolors = remember(scheme) { MaterialKolors(scheme) }

val accent = kolors.primary()
val onAccent = kolors.onPrimary()
```

The roles are only part of what a scheme contains. Each scheme also carries the six tonal palettes
the roles come from, and you can read any tone from 0 to 100 off them. Use them for the colors
Material has no role for, such as a pressed state, a gradient stop or a severity scale. `onTone`
gives you a readable content color from the same palette, aiming for WCAG AA by default.

```kotlin
val pressed = scheme.primaryPalette.toneColor(30)
val badge = scheme.tertiaryPalette.toneColor(90)
val onBadge = scheme.tertiaryPalette.onTone(90)
```

When you need more accent colors than the scheme has, `rememberTonalPalette` builds a palette from
any color. You can also shift its hue towards your seed so it fits in with the rest of the theme:

```kotlin
val success = rememberTonalPalette(seed = Color(0xFF2E7D32), harmonizeWith = seedColor)
val successContainer = success.toneColor(90)
```

[`samples/custom-theme`](samples/custom-theme) builds a complete theme this way.

## Compose Unstyled

[`material-kolor-unstyled`](material-kolor-unstyled/README.md) turns a scheme into Compose Unstyled
theme values:

```kotlin
val AppTheme = buildThemeV2 {
    val (light, dark) = rememberDynamicLightDarkColors(seedColor = seedColor)
    properties[MaterialKolorTokens.colors] = light

    colorScheme(ColorScheme.Dark) {
        properties[MaterialKolorTokens.colors] = dark
    }
}
```

## Compose Fluent

[`material-kolor-fluent`](material-kolor-fluent/README.md) generates Fluent's accent shades from a
seed:

```kotlin
FluentTheme(colors = rememberFluentColors(seedColor = seedColor)) {
    App()
}
```

## Colors from an image

[`material-kolor-palette`](material-kolor-palette/README.md) uses kmpalette to pick a seed color
from an image:

```kotlin
val seedColor = rememberThemeColor(
    loader = ByteArrayLoader,
    input = imageBytes,
    fallback = MaterialTheme.colorScheme.primary,
)
```

> [!NOTE]
> The `ImageBitmap` helpers in core still work, but they are deprecated and will be removed in
> 7.0.

## Color helpers

`com.materialkolor.ktx` has a few extensions for working with colors directly:

```kotlin
val harmonized = Color.Blue.harmonize(brandColor)
val alsoHarmonized = MaterialTheme.colorScheme.harmonizeWithPrimary(Color.Blue)
val lighter = color.lighten(1.5f)
val warm = color.isWarm()
val readable = textColor.hasEnoughContrast(background, ContrastThreshold.WcagAaNormalText)
```

## Samples

[`samples`](samples) has one small to-do app built four times, once for each UI stack. See
[`samples/README.md`](samples/README.md) for screenshots.

| Sample | Theme from | Run |
|---|---|---|
| [`material3`](samples/material3) | `material-kolor-material3` | `./gradlew :samples:material3:run` |
| [`unstyled`](samples/unstyled) | `material-kolor-unstyled` | `./gradlew :samples:unstyled:run` |
| [`fluent`](samples/fluent) | `material-kolor-fluent` | `./gradlew :samples:fluent:run` |
| [`custom-theme`](samples/custom-theme) | `material-kolor-core` tonal palettes | `./gradlew :samples:custom-theme:run` |

## How the engine is built

`material-color-utilities` is generated from Google's upstream Kotlin source at the revision in
the MCU badge above. A source transformer adjusts it for Kotlin Multiplatform and for this
library's API. [`docs/upstream-psi.md`](docs/upstream-psi.md) describes each change it makes and
how upstream updates are reviewed.

The Compose ideas started from [m3color](https://github.com/Kyant0/m3color).

## License

MaterialKolor is MIT licensed, see [LICENSE](LICENSE). The generated `material-color-utilities`
code is under Google's
[Apache 2.0 license](https://github.com/material-foundation/material-color-utilities/blob/main/LICENSE).
