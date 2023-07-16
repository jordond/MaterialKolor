<img width="500px" src="art/materialkolor-logo.png" alt="logo"/>
<br />

![Maven Central](https://img.shields.io/maven-central/v/com.materialkolor/material-kolor)
[![Kotlin](https://img.shields.io/badge/kotlin-v1.8.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Build](https://github.com/jordond/materialkolor/actions/workflows/ci.yml/badge.svg)](https://github.com/jordond/materialkolor/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/jordond/MaterialKolor)](https://opensource.org/license/mit/)

[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-v1.4.1-blue)](https://github.com/JetBrains/compose-multiplatform)
![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-DB413D.svg?style=flat)

A Compose Multiplatform library for creating dynamic Material Design 3 color palettes from any
color. Similar to generating a theme
from [m3.matierial.io](https://m3.material.io/theme-builder#/custom).

<img width="300px" src="art/ios-demo.gif" />

## Table of Contents

- [Platforms](#platforms)
- [Inspiration](#inspiration)
- [Setup](#setup)
    - [Multiplatform](#multiplatform)
    - [Single Platform](#single-platform)
    - [Version Catalog](#version-catalog)
- [Usage](#usage)
    - [Changes from original source](#changes-from-original-source)
- [Demo](#demo)
- [License](#license)

## Platforms

This library is written for Compose Multiplatform, and can be used on the following platforms:

- Android
- iOS
- JVM (Desktop)

A JavaScript (Browser) version is available but untested.

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
                implementation("com.materialkolor:material-kolor:1.0.0")
            }
        }
    }
}
```

### Single Platform

For an Android only project, add the dependency to app level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.materialkolor:material-kolor:1.0.0")
}
```

### Version Catalog

```toml
[versions]
materialKolor = "1.0.0"

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
    val colorScheme = dynamicColorScheme(seedColor, useDarkTheme)

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

## Demo

A demo app is available in the `demo` directory. It is a Compose Multiplatform app that runs on
Android, iOS and Desktop.

**Note:** While the demo does build a Browser version, it doesn't seem to work. However I don't know
if that is the fault of this library or the Compose Multiplatform library. Therefore I haven't
marked Javascript as supported.

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
