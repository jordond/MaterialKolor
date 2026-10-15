# material-kolor-unstyled

This module turns a MaterialKolor scheme into theme values for [Compose Unstyled](https://composeunstyled.com). It doesn't depend on Material 3.

## Install

```kotlin
implementation("com.materialkolor:material-kolor-unstyled:6.0.0")
implementation("com.composables:composeunstyled-theming:<latest-version>")
```

## Usage

`rememberDynamicLightDarkColors` builds the light and dark color sets in one call. Put the light set in the base theme and the dark set in the `ColorScheme.Dark` block:

```kotlin
object ThemeSettings {
    var seedColor by mutableStateOf(Color(0xFF6750A4))
}

val AppTheme = buildThemeV2 {
    colorSchemeTransitionSpec = tween(300)

    val (light, dark) = rememberDynamicLightDarkColors(
        seedColor = ThemeSettings.seedColor,
        style = PaletteStyle.Vibrant,
    )
    properties[MaterialKolorTokens.colors] = light

    colorScheme(ColorScheme.Dark) {
        properties[MaterialKolorTokens.colors] = dark
    }
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

The builder lambda is composable, so when the button changes `seedColor` the theme is regenerated.
`rememberDynamicLightDarkColors` takes the same options as `rememberDynamicScheme`, except for
`isDark`.

### Other color schemes

A scheme of your own needs only one set of colors. `rememberDynamicColors` takes `isDark` and
returns one map:

```kotlin
val Sepia = ColorScheme("sepia")

val AppTheme = buildThemeV2 {
    // Base values and the dark block, as above.

    colorScheme(Sepia) {
        properties[MaterialKolorTokens.colors] =
            rememberDynamicColors(Color(0xFF704214), isDark = false)
    }
}
```

If you already have a `DynamicScheme`, for example one kept in app state, `scheme.toThemeValues()`
returns the same map.

### Animation

Set `colorSchemeTransitionSpec` on the builder, and Unstyled animates every color token when it changes, whether because the seed changed or because the theme switched between light and dark.

### Your own tokens

If your app has its own token names, map the `MaterialKolors` roles onto them:

```kotlin
val appColors = ThemeProperty<Color>("app.colors")
val accent = ThemeToken<Color>("accent")
val onAccent = ThemeToken<Color>("on_accent")
val canvas = ThemeToken<Color>("canvas")

fun MaterialKolors.toAppColors(): Map<ThemeToken<Color>, Color> =
    mapOf(
        accent to primary(),
        onAccent to onPrimary(),
        canvas to surfaceContainerLow(),
    )

val AppTheme = buildThemeV2 {
    val light = rememberDynamicScheme(ThemeSettings.seedColor, isDark = false)
    val dark = rememberDynamicScheme(ThemeSettings.seedColor, isDark = true)

    properties[appColors] = remember(light) { MaterialKolors(light).toAppColors() }

    colorScheme(ColorScheme.Dark) {
        properties[appColors] = remember(dark) { MaterialKolors(dark).toAppColors() }
    }
}
```

## Sample

[`samples/unstyled`](../samples/unstyled) is the Tasks sample built with this adapter. Run it with
`./gradlew :samples:unstyled:run`.
