# material-kolor-palette

This module picks seed colors from images. [kmpalette](https://github.com/jordond/kmpalette) loads
and quantizes the image off the main thread, and this module scores the colors it finds with the
same scoring Android uses for wallpapers.

## Install

```kotlin
implementation("com.materialkolor:material-kolor-palette:6.0.0")
```

The module brings in `material-kolor-core` and kmpalette, and it supports the same platforms as
core.

## Usage

Anything kmpalette can load works as input. Pass a loader along with the input it loads:

```kotlin
@Composable
fun DynamicTheme(bytes: ByteArray, content: @Composable () -> Unit) {
    val seedColor = rememberThemeColor(
        loader = ByteArrayLoader,
        input = bytes,
        fallback = MaterialTheme.colorScheme.primary,
    )

    DynamicMaterialTheme(seedColor = seedColor, content = content)
}
```

`rememberThemeColors` returns a ranked list instead of a single color, and
`rememberPainterThemeColor` takes a `Painter`. For URLs, files and base64 strings, add the matching
kmpalette extension artifact and pass its loader.

If you already have a `PaletteState`, you can build the scheme from it directly:

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

    MaterialTheme(colorScheme = scheme.toColorScheme(), content = content)
}
```

To score a `Palette` you generated yourself, use `Palette.themeColors`, `Palette.themeColor`,
`Palette.themeColorOrNull` or `Palette.seedColorOrNull`. `Palette.Builder.generate` is a suspend
function, so call it from a coroutine on `Dispatchers.Default`.

## Replacing the core image helpers

The `ImageBitmap` helpers in `material-kolor-core` (`themeColors`, `themeColor`, `themeColorOrNull`,
`rememberThemeColors`, `rememberThemeColor` and `QuantizerCelebi.quantize(ImageBitmap)`) are
deprecated and will be removed in 7.0. The functions in this module replace them. If you start
from an `ImageBitmap`, generate a kmpalette `PaletteState` from it:

```kotlin
// Before, material-kolor-core
val seed = rememberThemeColor(image = bitmap, fallback = fallback)

// After, material-kolor-palette
val palette = rememberPaletteState()
LaunchedEffect(bitmap) { palette.generate(bitmap) }
val seed = palette.themeColor(fallback = fallback)
```
