<img width="500px" src="art/materialkolor-logo.png" alt="logo"/>
<br />

![Maven Central](https://img.shields.io/maven-central/v/com.materialkolor/material-kolor)
[![Kotlin](https://img.shields.io/badge/kotlin-v2.2.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Build](https://github.com/jordond/materialkolor/actions/workflows/ci.yml/badge.svg)](https://github.com/jordond/materialkolor/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/jordond/MaterialKolor)](https://opensource.org/license/mit/)

[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.9.1-blue)](https://github.com/JetBrains/compose-multiplatform)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-DB413D.svg?style=flat)
![badge-js](http://img.shields.io/badge/platform-js%2Fwasm-FDD835.svg?style=flat)

A Compose Multiplatform library for creating dynamic Material Design 3 color palettes from any
color.

Check out [MaterialKolor Builder](https://materialkolor.com) to see MaterialKolor in action and
generate your own color schemes. It can export to MaterialKolor code, or plain Material 3 code.

The KDoc is published at [docs.materialkolor.com](https://docs.materialkolor.com)

## Table of Contents

- [Platforms](#platforms)
- [Inspiration](#inspiration)
- [Setup](#setup)
    - [Multiplatform](#multiplatform)
    - [Single Platform](#single-platform)
    - [Version Catalog](#version-catalog)
- [Usage](#usage)
  - [Updated Colors](#updated-colors)
  - [DynamicMaterialTheme](#dynamicmaterialtheme)
  - [DynamicMaterialExpressiveTheme](#dynamicmaterialexpressivetheme)
- [Extensions](#extensions)
    - [Harmonize Colors](#harmonize-colors)
    - [Lighten and Darken](#lighten-and-darken)
    - [Color Temperature](#color-temperature)
- [Generating from an Image](#generating-from-an-image)
- [License](#license)
  - [Changes from original source](#changes-from-original-source)

## Platforms

This library is written for Compose Multiplatform, and can be used on the following platforms:

- Android
- iOS
- JVM (Desktop)
- JavaScript/wasm (Browser)

You can see it in action by using [MaterialKolor Builder](https://materialkolor.com).

## Inspiration

The heart of this library comes from
the [material-color-utilities](https://github.com/material-foundation/material-color-utilities)
repository. It is currently
only a Java library, and I wanted to make it available to Kotlin Multiplatform projects. The source
code was taken and converted into a Kotlin Multiplatform library.

I also incorporated the Compose ideas from another open source
library [m3color](https://github.com/Kyant0/m3color).

## Setup

You can add this library to your project using Gradle.

### Multiplatform

To add to a multiplatform project, add the dependency to the common source-set:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
              implementation("com.materialkolor:material-kolor:4.0.0")
            }
        }
    }
}
```

### Single Platform

For an Android only project, add the dependency to app level `build.gradle.kts`:

```kotlin
dependencies {
  implementation("com.materialkolor:material-kolor:4.0.0")
}
```

### Version Catalog

```toml
[versions]
materialKolor = "4.0.0"

[libraries]
materialKolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }
```

### Without compose

If you don't use Compose and don't need any of the extension functions provided by `material-kolor`,
you can use the `material-color-utilities` artifact instead.
It is a Kotlin Multiplatform port of
Google's [Material Color Utilities](https://github.com/material-foundation/material-color-utilities).

```toml
[versions]
materialKolor = "4.0.0"

[libraries]
materialKolor-utilities = { module = "com.materialkolor:material-color-utilities", version.ref = "materialKolor" }
```

## Usage

To generate a custom `ColorScheme` you simply need to call `dynamicColorScheme()` with your target
seed color:

```kotlin
@Composable
fun MyTheme(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark)

    MaterialTheme(
        colors = colorScheme,
        content = content,
    )
}
```

You can also pass in
a [`PaletteStyle`](material-kolor/src/commonMain/kotlin/com/materialkolor/PaletteStyle.kt) to
customize the generated palette:

```kotlin
dynamicColorScheme(
    seedColor = seedColor,
    isDark = isDark,
  style = PaletteStyle.Expressive,
)
```

### Updated Colors

With the release of Material3 Expressive, Google has added a new color spec used when generating
colors. By default MaterialKolor uses the `SPEC_2021` version. If you want to try out the new colors
you will need to use `ColorSpec.SpecVersion.SPEC_2025`:

```kotlin
val scheme = rememberDynamicColorScheme(
  seedColor = seedColor,
  isDark = isDark,
  specVersion = ColorSpec.SpecVersion.SPEC_2025,
  style = PaletteStyle.Expressive, // Optional but recommended if you are using `MaterialExpressiveTheme`
)
```

### DynamicMaterialTheme

A `DynamicMaterialTheme` Composable is also available. It is a wrapper around `MaterialTheme` that
uses `dynamicColorScheme()` to generate a `ColorScheme` for you. You can animate the color scheme by
passing in `animate = true`.

Example:

```kotlin
@Composable
fun MyTheme(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = isDark,
        animate = true,
        content = content,
    )
}
```

### DynamicMaterialExpressiveTheme

For more vibrant and playful themes, use `DynamicMaterialExpressiveTheme`. This composable is
designed for the Material 3 Expressive design system and defaults to using `PaletteStyle.Expressive`
and `ColorSpec.SpecVersion.SPEC_2025` for optimal color generation.

**Important:** Make sure to use `SPEC_2025` and `PaletteStyle.Expressive` for the best results:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyExpressiveTheme(
  seedColor: Color,
  isDark: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  DynamicMaterialExpressiveTheme(
    seedColor = seedColor,
    motionScheme = MotionScheme.expressive(),
    isDark = isDark,
    animate = true,
    content = content,
  )
}
```

The Expressive theme generates vibrant color schemes where the source color's hue may not directly
appear in the final theme, creating more dynamic and playful color palettes.

## Extensions

Included in the library are some extensions for working with colors. You can check out
the [/ktx](material-kolor/src/commonMain/kotlin/com/materialkolor/ktx) package for more information.

But here are a couple useful examples:

### Harmonize Colors

If you want to harmonize a color with another you can use the `Color.harmonize()` function. You can
read more about color harmonization on
the [Material 3 Documentation](https://m3.material.io/styles/color/advanced/adjust-existing-colors#1cc12e43-237b-45b9-8fe0-9a3549c1f61e).

Example:

```kotlin
val newColor = MaterialTheme.colorScheme.primary.harmonize(Color.Blue)
```

There is an additional function specifically for harmonizing with the primary color:

```kotlin
val newColor = Color.Blue.harmonizeWithPrimary()
```

**Note:** `Color.harmonize()` has an optional parameter `matchSaturation` which when set to `true`
will adjust the saturation from the other color.

### Lighten and Darken

You can lighten or darken a color using the `Color.lighten()` and `Color.darken()` functions.

For example:

```kotlin
val newColor = MaterialTheme.colorScheme.primary.lighten(0.2f)
```

Check out the demo app for a full example.

### Color Temperature

You can determine if a `Color` is warm or cold using the following:

```kotlin
val isWarm = MaterialTheme.colorScheme.primary.isWarm()
val isCold = MaterialTheme.colorScheme.primary.isCold()
```

## Generating from an Image

You can calculate a seed color, or colors that are suitable for UI theming from an image. This is
useful for generating a color scheme from a user's profile picture, or a background image.

To do so you can call `ImageBitmap.themeColors()`, `ImageBitmap.themeColor()` or the `@Composable`
function `rememberThemeColors()` or `rememberThemeColor()`:

```kotlin
fun calculateSeedColor(bitmap: ImageBitmap): Color {
    val suitableColors = bitmap.themeColors(fallback = Color.Blue)
    return suitableColors.first()
}
```

See [`ImageBitmap.kt`](material-kolor/src/commonMain/kotlin/com/materialkolor/ktx/ImageBitmap.kt)
for more information.

Or in Compose land:

```kotlin
@Composable
fun DynamicTheme(image: ImageBitmap, content: @Composable () -> Unit) {
    val seedColor = rememberThemeColor(image, fallback = MaterialTheme.colorScheme.primary)

  DynamicMaterialTheme(
        seedColor = seedColor,
        content = content
    )
}
```

**Note:** This approach can be pretty slow, so I wouldn't really recommend using it in your UI
unless you are eagerly loading the colors.

## License

The module `material-color-utilities` is licensed under the Apache License, Version 2.0. See
their [LICENSE](material-color-utilities/src/commonMain/kotlin/com/materialkolor/LICENSE) and their
repository [here](https://github.com/material-foundation/material-color-utilities) for more
information.

### Changes from original source

- Convert Java code to Kotlin
- Convert library to Kotlin Multiplatform

For the remaining code see [LICENSE](LICENSE) for more information.
