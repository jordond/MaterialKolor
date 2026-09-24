<img width="500px" src="art/materialkolor-logo.png" alt="logo"/>
<br />

![Maven Central](https://img.shields.io/maven-central/v/com.materialkolor/material-kolor)
[![Kotlin](https://img.shields.io/badge/kotlin-v2.4.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
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
- [Tonal Ramps](#tonal-ramps)
    - [Building Your Own Theme](#building-your-own-theme)
- [Extensions](#extensions)
    - [Harmonize Colors](#harmonize-colors)
    - [Lighten and Darken](#lighten-and-darken)
    - [Color Temperature](#color-temperature)
- [Generating from an Image](#generating-from-an-image)
    - [Palette module](#palette-module)
- [Samples](#samples)
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

Upgrading from 5.x? The [6.0 migration guide](docs/migration-6.0.md) covers the intentional API
changes.

### Multiplatform

To add to a multiplatform project, add the dependency to the common source-set:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.materialkolor:material-kolor-material3:5.0.1")
            }
        }
    }
}
```

`material-kolor-material3` is the artifact a Material3 app needs. It brings in
`material-kolor-core` as an `api` dependency, so `PaletteStyle`, `MaterialKolors` and the `ktx`
helpers come with it.

If you theme something other than Material3, depend on the core artifact on its own.

```kotlin
implementation("com.materialkolor:material-kolor-core:5.0.1")
```

If your app uses Compose Unstyled instead of Material3, depend on the adapter artifact.

```kotlin
implementation("com.materialkolor:material-kolor-unstyled:5.0.1")
```

`material-kolor-unstyled` is for Compose Unstyled apps. It brings in `material-kolor-core` as an
`api` dependency, so it does not need Material3. It does **not** bring Compose Unstyled itself, so
declare the version you want alongside it.

```kotlin
implementation("com.composables:composeunstyled-theming:2.10.0")
```

On JVM and Android nothing is brought in for you, so this line is required. On iOS, JS and Wasm the
klib format makes the dependency part of the artifact whether we like it or not, so you get a
version by default there; declaring your own still overrides it.

If your app uses Compose Fluent, depend on the Fluent adapter artifact.

```kotlin
implementation("com.materialkolor:material-kolor-fluent:5.0.1")
```

`material-kolor-fluent` brings in `material-kolor-core` as an `api` dependency. It does **not**
bring [Compose Fluent](https://github.com/Compose-Fluent/compose-fluent-ui) itself, so declare the
version you want alongside it.

```kotlin
implementation("io.github.compose-fluent:fluent:v0.1.0")
```

On JVM and Android nothing is brought in for you, so this line is required. On iOS, JS and Wasm the
klib format makes the dependency part of the artifact whether we like it or not, so you get a
version by default there; declaring your own still overrides it.

The adapter has no macOS native target, because Fluent does not publish one.

If you seed your theme from images, depend on the palette artifact.

```kotlin
implementation("com.materialkolor:material-kolor-palette:5.0.1")
```

`material-kolor-palette` brings in `material-kolor-core` and
[kmpalette](https://github.com/jordond/kmpalette) as `api` dependencies. It ships on the same
platforms as core.

### Single Platform

For an Android only project, add the dependency to app level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.materialkolor:material-kolor-material3:5.0.1")
}
```

### Version Catalog

```toml
[versions]
materialKolor = "5.0.1"

[libraries]
materialKolor-core = { module = "com.materialkolor:material-kolor-core", version.ref = "materialKolor" }
materialKolor-material3 = { module = "com.materialkolor:material-kolor-material3", version.ref = "materialKolor" }
materialKolor-unstyled = { module = "com.materialkolor:material-kolor-unstyled", version.ref = "materialKolor" }
materialKolor-palette = { module = "com.materialkolor:material-kolor-palette", version.ref = "materialKolor" }
materialKolor-fluent = { module = "com.materialkolor:material-kolor-fluent", version.ref = "materialKolor" }
```

### Without compose

If you don't use Compose and don't need any of the extension functions provided by
`material-kolor-core`, you can use the `material-color-utilities` artifact instead.
It is a Kotlin Multiplatform port of
Google's [Material Color Utilities](https://github.com/material-foundation/material-color-utilities).

```toml
[versions]
materialKolor = "5.0.1"

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
a [`PaletteStyle`](material-kolor-core/src/commonMain/kotlin/com/materialkolor/PaletteStyle.kt) to
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

[`samples/material3`](samples/material3) builds the [Tasks sample](#samples) on Material 3
components themed by `DynamicMaterialTheme`. Run it with `./gradlew :samples:material3:run`.

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

## Tonal Ramps

Roles are one projection of a scheme, the one Material defines. The same scheme carries the six
tonal ramps those roles were cut from, and you can read any tone off them:

```kotlin
val scheme = rememberDynamicScheme(seedColor = seedColor, isDark = isDark)

val pressed = scheme.primaryPalette.toneColor(30)
val sunken = scheme.neutralPalette.toneColor(94)
val hairline = scheme.neutralVariantPalette.toneColor(85)
```

Reach for a ramp whenever you want a tone Material never gave a name to. A pressed or raised state,
a gradient stop, a border that is one step stronger than the last one, a severity scale. Reach for a
role for everything else, because roles already solved the accent and container problem and there is
no reason to redo that work.

Whatever tone you pick, `onTone` gives you a content color from the same ramp that is readable on
top of it:

```kotlin
val badge = scheme.tertiaryPalette.toneColor(90)
val badgeText = scheme.tertiaryPalette.onTone(90)
```

By default it targets WCAG AA for normal text. Pass a different `ContrastThreshold` if you need
large text or AAA. When the ramp cannot reach the ratio in either direction, you get the nearer end
of it, which is the most readable color the palette has.

Themes usually want more accents than a scheme has ramps. `rememberTonalPalette` builds one from any
seed, optionally pulling it towards the scheme's seed first so it looks like it belongs:

```kotlin
val success = rememberTonalPalette(seed = Color(0xFF2E7D32), harmonizeWith = seedColor)

val successContainer = success.toneColor(90)
val onSuccessContainer = success.onTone(90)
```

### Building Your Own Theme

You do not need Material3 to use MaterialKolor. `material-kolor-core` has no dependency on it, so a
theme with its own shape can be generated from one seed the same way a `ColorScheme` is:

```kotlin
@Composable
fun AppTheme(seed: Color, isDark: Boolean, content: @Composable () -> Unit) {
    val scheme = rememberDynamicScheme(seedColor = seed, isDark = isDark)
    val kolors = remember(scheme) { MaterialKolors(scheme) }
    val success = rememberTonalPalette(seed = SuccessSeed, harmonizeWith = seed)

    val colors = AppColors(
        primary = kolors.primary(),
        onPrimary = kolors.onPrimary(),
        primaryPressed = scheme.primaryPalette.toneColor(if (isDark) 70 else 32),
        success = success.toneColor(if (isDark) 80 else 40),
        onSuccess = success.onTone(if (isDark) 80 else 40),
    )

    CompositionLocalProvider(LocalAppColors provides colors, content = content)
}
```

[`samples/custom-theme`](samples/custom-theme) is a working version of that. Seven accent families
instead of three, pressed and raised states, three surface steps, a border ramp and five decorative
category colors, all from one seed plus eight accent seeds. It builds the [Tasks sample](#samples)
on Compose Foundation alone, and its Palette tab shows every color the theme generates.

Run it with `./gradlew :samples:custom-theme:run`.

## Compose Unstyled

`material-kolor-unstyled` adapts a MaterialKolor scheme to
[Compose Unstyled](https://composeunstyled.com) theming. Your app keeps `buildThemeV2`, its text
style, its indication and its selection colors. The adapter only writes color tokens.

```kotlin
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.PaletteStyle
import com.materialkolor.unstyled.MaterialKolorTokens
import com.materialkolor.unstyled.dynamicColorSchemes

object ThemeSettings {
    var seedColor by mutableStateOf(Color(0xFF6750A4))
}

val AppTheme = buildThemeV2 {
    colorSchemeTransitionSpec = tween(300)
    dynamicColorSchemes(seedColor = ThemeSettings.seedColor, style = PaletteStyle.Vibrant)
}

@Composable
fun App() {
    AppTheme {
        val colors = Theme[MaterialKolorTokens.colors]
        Column(Modifier.background(colors[MaterialKolorTokens.surface])) {
            Text("Hello", color = colors[MaterialKolorTokens.onSurface])
            Button(onClick = { ThemeSettings.seedColor = Color(0xFF00695C) }) {
                Text("Teal")
            }
        }
    }
}
```

The builder lambda is composable, so it reads `ThemeSettings.seedColor` on every recomposition and
the theme regenerates when the button sets a new one. Light is the base, dark is the
`ColorScheme.Dark` override, so `AppTheme { }` follows the system and
`AppTheme(colorScheme = ColorScheme.Dark) { }` pins one.

`dynamicColorSchemes` owns the `ColorScheme.Dark` override. Unstyled keeps one override per color
scheme and each `colorScheme(ColorScheme.Dark) { }` replaces the one before it, so a dark block of
your own in the same theme would either drop the dark tokens or be dropped by them. Put dark-only
settings in the trailing `dark` block instead. It runs inside the same override, after the tokens.

```kotlin
val AppTheme = buildThemeV2 {
    defaultContentColor = Color(0xFF1D1B20)
    dynamicColorSchemes(seedColor = ThemeSettings.seedColor) {
        defaultContentColor = Color(0xFFE6E0E9)
    }
}
```

For a scheme of your own, write the override yourself and call `dynamicColors` inside it.

```kotlin
val Sepia = ColorScheme("sepia")

val AppTheme = buildThemeV2 {
    dynamicColorSchemes(seedColor = ThemeSettings.seedColor)
    colorScheme(Sepia) {
        dynamicColors(rememberDynamicScheme(seedColor = Color(0xFF704214), isDark = false))
    }
}
```

Set `defaultIndication` on the builder. Left unset, Unstyled falls back to an indication that
foundation's `clickable` rejects, and the first plain `clickable` throws. Setting it only reaches
`LocalIndication`, though. `UnstyledButton`, `UnstyledCheckbox`, `UnstyledSwitch`, the radio group
and the tab group all default their `indication` parameter to `null`, so pass
`LocalIndication.current` to each of them, or wrap them in your own components that do. The
[`samples/unstyled`](samples/unstyled) components show one way.

The adapter never animates. Set `colorSchemeTransitionSpec` on the builder, as above, and Unstyled
animates every color token whenever it changes, whether the seed moved or the scheme flipped
between light and dark.

If your app owns its own token vocabulary, build the values with the DSL instead.

```kotlin
val appColors = ThemeProperty<Color>("app.colors")
val accent = ThemeToken<Color>("accent")
val onAccent = ThemeToken<Color>("on_accent")
val canvas = ThemeToken<Color>("canvas")

val AppTheme = buildThemeV2 {
    val light = rememberDynamicScheme(ThemeSettings.seedColor, isDark = false)
    val dark = rememberDynamicScheme(ThemeSettings.seedColor, isDark = true)

    properties[appColors] = light.themeValues {
        accent to primary()
        onAccent to onPrimary()
        canvas to surfaceContainerLow()
    }
    colorScheme(ColorScheme.Dark) {
        properties[appColors] = dark.themeValues {
            accent to primary()
            onAccent to onPrimary()
            canvas to surfaceContainerLow()
        }
    }
}
```

Every `MaterialKolors` role is available inside the block, and `dynamicColors(scheme)` writes the
whole role set for a scheme you built yourself.

The adapter publishes android, jvm, js, wasmJs, iosArm64 and iosSimulatorArm64, because Compose
Unstyled has no macOS native target. Android minSdk 23 and Java 17 bytecode both come from Unstyled.
Core keeps its own floor.

[`samples/unstyled`](samples/unstyled) builds the [Tasks sample](#samples) on Compose Unstyled with
this adapter. Run it with `./gradlew :samples:unstyled:run`.

## Compose Fluent

`material-kolor-fluent` gives [Compose Fluent](https://github.com/Compose-Fluent/compose-fluent-ui)
an accent it can actually theme from. Fluent ships one accent colour, Windows blue, and its
`generateShades` is a lookup with a single entry, so every other accent silently comes back as that
same blue. The adapter replaces the lookup with a generated ramp.

```kotlin
import com.materialkolor.fluent.rememberFluentColors
import io.github.composefluent.FluentTheme

@Composable
fun App() {
    FluentTheme(colors = rememberFluentColors(seedColor = Color(0xFF6750A4))) {
        Text("Themed from a seed colour")
    }
}
```

`rememberFluentColors` takes the same parameters as `rememberDynamicScheme`, so `style`,
`contrastLevel` and the rest work the way they do everywhere else. If you already have a scheme,
`scheme.toFluentColors()` reads its primary ramp and its dark flag. If you want a Fluent theme
built on some other ramp, any `TonalPalette` converts:

```kotlin
val shades = scheme.secondaryPalette.toFluentShades()
val shades = TonalPalette.from(seedColor).toFluentShades()
```

Those two are not the same, and the difference is worth knowing. A `PaletteStyle` reshapes chroma
on the way into a scheme, so `TonalSpot` gives a calmer accent than the seed you handed it, while
`TonalPalette.from(seedColor)` keeps the seed as it was.

The seven shades are anchored to the lightness of Microsoft's own Windows blue family, rounded to
the nearest five:

| Shade | Windows blue | its tone | tone used here |
|---|---|---|---|
| `dark3` | `#001968` | 13.8 | 15 |
| `dark2` | `#003D92` | 27.9 | 30 |
| `dark1` | `#005EB7` | 40.3 | 40 |
| `base` | `#0078D4` | 49.7 | 50 |
| `light1` | `#0093F9` | 59.6 | 60 |
| `light2` | `#60CCFE` | 77.8 | 80 |
| `light3` | `#98ECFE` | 88.8 | 90 |

Microsoft's ramp shifts hue by 64 degrees from its darkest shade to its lightest, and a tonal
palette holds hue steady, so this matches the lightness of that ramp rather than reproducing it.
That is the right trade for a generated accent: you get a ramp of one colour instead of an
imitation of a blue you did not ask for.

To fade between seeds rather than cut, wrap the colours in `animateFluentColors`.

```kotlin
@Composable
fun App() {
    val colors = animateFluentColors(
        rememberFluentColors(seedColor = ThemeSettings.seedColor),
    )

    FluentTheme(colors = colors) {
        Button(onClick = { ThemeSettings.seedColor = Color(0xFF00695C) }) {
            Text("Teal")
        }
    }
}
```

The seven shades animate and the groups derived from them follow. `system` and `controlOnImage`
hold still, because they are built from Fluent's own constants and never depended on the accent.
Switching between light and dark is a cut rather than a fade for the same reason: Fluent derives
both from one set of shades and a flag, so there is no pair of colours to move between.

Fluent's `success`, `caution` and `critical` live on `Colors.system`, which is built from constants
with no setter a caller can reach, so a scheme's secondary, tertiary and error ramps have nowhere
to go and are left alone.

A seed with little chroma gives a Fluent theme with little chroma. A grey seed produces seven
greys, which is the ramp working rather than a fault.

[`samples/fluent`](samples/fluent) builds the [Tasks sample](#samples) on Fluent components. Run it
with `./gradlew :samples:fluent:run` to switch seeds, flip light and dark, and see the generated ramp
beside the single blue Fluent falls back to on its own.

Platforms: JVM, Android, iOS, JS and Wasm. No macOS native target, Java 17 bytecode from Fluent,
and the Android floor is core's own 21.

## Extensions

Included in the library are some extensions for working with colors. You can check out
the [/ktx](material-kolor-core/src/commonMain/kotlin/com/materialkolor/ktx) package for more
information.

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

The `ImageBitmap` helpers in core are deprecated as of 6.0 and go away in 7.0. New code should use
the [palette module](#palette-module) below. Until then you can still call
`ImageBitmap.themeColors()`,
`ImageBitmap.themeColor()` or the `@Composable` function `rememberThemeColors()` or
`rememberThemeColor()`:

```kotlin
fun calculateSeedColor(bitmap: ImageBitmap): Color {
    val suitableColors = bitmap.themeColors(fallback = Color.Blue)
    return suitableColors.first()
}
```

All of these sample the image down to a 128 by 128 pixel budget before quantizing. Pass `sampleArea`
if you want a different budget, or zero and below to read every pixel.

See [
`ImageBitmap.kt`](material-kolor-core/src/commonMain/kotlin/com/materialkolor/ktx/ImageBitmap.kt)
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

### Palette module

`material-kolor-palette` starts from [kmpalette](https://github.com/jordond/kmpalette) instead of
an `ImageBitmap`. kmpalette loads and quantizes the image off the main thread, and this module
scores
the swatches that come back with the same scoring Android applies to wallpapers.

Anything kmpalette can load is an input, so you pass a loader and the thing it loads:

```kotlin
@Composable
fun DynamicTheme(bytes: ByteArray, content: @Composable () -> Unit) {
    val seedColor = rememberThemeColor(
        loader = ByteArrayLoader,
        input = bytes,
        fallback = MaterialTheme.colorScheme.primary,
    )

    DynamicMaterialTheme(
        seedColor = seedColor,
        content = content,
    )
}
```

If you already hold a `PaletteState`, build the scheme straight from it:

```kotlin
@Composable
fun DynamicTheme(image: ImageBitmap, content: @Composable () -> Unit) {
    val palette = rememberPaletteState()
    LaunchedEffect(image) { palette.generate(image) }

    val scheme = rememberDynamicScheme(
        palette = palette,
        fallback = MaterialTheme.colorScheme.primary,
        isDark = isSystemInDarkTheme(),
    )

    MaterialTheme(
        colorScheme = scheme.toColorScheme(),
        content = content,
    )
}
```

`Palette.themeColors()`, `Palette.themeColor()`, `Palette.themeColorOrNull()` and
`Palette.seedColorOrNull()` are there for when you want to score a palette you generated yourself,
and `rememberPainterThemeColor()` starts from a `Painter`. For base64 strings, network URLs and
files, add the matching kmpalette extension artifact and pass its loader.

## Samples

The [samples](samples) are one small app, Tasks, built four times. A to-do list with a seed picker
and a light and dark switch on top. The behaviour, the copy and the data live in one shared module,
so the four differ only in their UI stack and in how they turn a seed into a theme.

| Sample | UI | Theme from | Run it |
|---|---|---|---|
| [`custom-theme`](samples/custom-theme) | Compose Foundation | `material-kolor-core` tonal ramps | `./gradlew :samples:custom-theme:run` |
| [`fluent`](samples/fluent) | Compose Fluent | `material-kolor-fluent` | `./gradlew :samples:fluent:run` |
| [`material3`](samples/material3) | Compose Material 3 | `material-kolor-material3` | `./gradlew :samples:material3:run` |
| [`unstyled`](samples/unstyled) | Compose Unstyled | `material-kolor-unstyled` | `./gradlew :samples:unstyled:run` |

[`samples/README.md`](samples/README.md) has the full spec.

## License

The module `material-color-utilities` is licensed under the Apache License, Version 2.0. See
their [LICENSE](material-color-utilities/src/commonMain/kotlin/com/materialkolor/LICENSE) and their
repository [here](https://github.com/material-foundation/material-color-utilities) for more
information.

### Changes from original source

- Transform library to Kotlin Multiplatform

For the remaining code see [LICENSE](LICENSE) for more information.
