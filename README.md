<img width="500px" src="art/materialkolor-logo.png" alt="logo"/>
<br />

![Maven Central](https://img.shields.io/maven-central/v/com.materialkolor/material-kolor)
[![Kotlin](https://img.shields.io/badge/kotlin-v1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Build](https://github.com/jordond/materialkolor/actions/workflows/ci.yml/badge.svg)](https://github.com/jordond/materialkolor/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/jordond/MaterialKolor)](https://opensource.org/license/mit/)

[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.6.0--beta01-blue)](https://github.com/JetBrains/compose-multiplatform)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-DB413D.svg?style=flat)
![badge-js](http://img.shields.io/badge/platform-js%2Fwasm-FDD835.svg?style=flat)

A Compose Multiplatform library for creating dynamic Material Design 3 color palettes from any
color. Similar to generating a theme
from [m3.matierial.io](https://m3.material.io/theme-builder#/custom).

The KDoc is published at [docs.materialkolor.com](docs.materialkolor.com)

<img width="300px" src="art/ios-demo.gif" />

## Table of Contents

- [Platforms](#platforms)
- [Inspiration](#inspiration)
- [Setup](#setup)
  - [Multiplatform](#multiplatform)
  - [Single Platform](#single-platform)
  - [Version Catalog](#version-catalog)
- [Usage](#usage)
- [Generating from an Image](#generating-from-an-image)
  - [Advanced](#advanced)
- [Demo](#demo)
- [License](#license)
    - [Changes from original source](#changes-from-original-source)

## Platforms

This library is written for Compose Multiplatform, and can be used on the following platforms:

- Android
- iOS
- JVM (Desktop)
- JavaScript/wasm (Browser)

You can see a demo running at [demo.materialkolor.com](https://demo.materialkolor.com).

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
                implementation("com.materialkolor:material-kolor:1.4.0-alpha04")
            }
        }
    }
}
```

### Single Platform

For an Android only project, add the dependency to app level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.materialkolor:material-kolor:1.4.0-alpha04")
}
```

### Version Catalog

```toml
[versions]
materialKolor = "1.4.0-alpha03"

[libraries]
materialKolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }
```

## Usage

To generate a custom `ColorScheme` you simply need to call `dynamicColorScheme()` with your target
seed color:

```kotlin
@Composable
fun MyTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(seedColor, useDarkTheme)

    MaterialTheme(
        colors = colorScheme.toMaterialColors(),
        content = content
    )
}
```

You can also pass in
a [`PaletteStyle`](material-kolor/src/commonMain/kotlin/com/materialkolor/PaletteStyle.kt) to
customize the generated palette:

```kotlin
dynamicColorScheme(
    seedColor = seedColor,
    isDark = useDarkTheme,
    style = PaletteStyle.Vibrant,
)
```

See [`Theme.kt`](demo/composeApp/src/commonMain/kotlin/com/materialkolor/demo/theme/Theme.kt) from
the demo
for a full example.

### DynamicMaterialTheme

A `DynamicMaterialTheme` Composable is also available. It is a wrapper around `MaterialTheme` that
uses `dynamicColorScheme()` to generate a `ColorScheme` for you.

Example:

```kotlin
@Composable
fun MyTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = useDarkTheme,
        content = content
    )
}
```

Also included is a `AnimatedDynamicMaterialTheme` which animates the color scheme changes.

See [`Theme.kt`](demo/composeApp/src/commonMain/kotlin/com/materialkolor/demo/theme/Theme.kt) for an
example.

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

  AnimatedDynamicMaterialTheme(
    seedColor = seedColor,
    content = content
  )
}
```

### Advanced

For a more advanced use-case you can use my other
library [kmPalette](https://github.com/jordond/kmpalette).

You can get the dominant color from an image, or you can also generate a color palette.

Follow the instructions there to set it up, then as an example. You can use it to generate a color
theme from a remote image:

```kotlin
@Composable
fun SampleTheme(
  imageUrl: Url, // Url("http://example.com/image.jpg")
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val networkLoader = rememberNetworkLoader()
  val dominantColorState = rememberDominantColorState(loader = networkLoader)
  LaunchedEffect(imageUrl) {
    dominantColorState.updateFrom(imageUrl)
  }

  AnimatedDynamicMaterialTheme(
    seedColor = dominantColorState.color,
    isDark = useDarkTheme,
    content = content
  )
}
```

## Demo

A demo app is available in the `demo` directory. It is a Compose Multiplatform app that runs on
Android, iOS, Desktop and browser.

Visit [demo.materialkolor.com](https://demo.materialkolor.com)

See the [README](demo/README.md) for more information.

## License

The module `material-color-utilities` is licensed under the Apache License, Version 2.0. See
their [LICENSE](material-color-utilities/src/commonMain/kotlin/com/materialkolor/LICENSE) and their
repository [here](https://github.com/material-foundation/material-color-utilities) for more
information.

### Changes from original source

- Convert Java code to Kotlin
- Convert library to Kotlin Multiplatform

For the remaining code see [LICENSE](LICENSE) for more information.
