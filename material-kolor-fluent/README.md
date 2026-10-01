# material-kolor-fluent

This module generates [Compose Fluent](https://github.com/Compose-Fluent/compose-fluent-ui) accent
colors from a seed.

Fluent ships with one accent color, Windows blue, and its `generateShades` function is a lookup
table with a single entry. Any other accent you give it comes back as that same blue. This adapter
generates the shades from your color instead.

## Install

```kotlin
implementation("com.materialkolor:material-kolor-fluent:6.0.0")
implementation("io.github.compose-fluent:fluent:<latest-version>")
```

## Usage

```kotlin
@Composable
fun App() {
    FluentTheme(colors = rememberFluentColors(seedColor = Color(0xFF6750A4))) {
        Text("Themed from a seed color")
    }
}
```

`rememberFluentColors` takes the same options as `rememberDynamicScheme`, including `style` and
`contrastLevel`. If you already have a scheme, `scheme.toFluentColors()` uses its primary palette
and whether it's dark.

To build the shades from a different palette, convert any `TonalPalette`:

```kotlin
val fromScheme = scheme.secondaryPalette.toFluentShades()
val fromSeed = TonalPalette.from(seedColor).toFluentShades()
```

These two give different results. A `PaletteStyle` adjusts chroma when it builds a scheme, so with
`TonalSpot` the accent is calmer than the seed you passed in. `TonalPalette.from(seedColor)` keeps
the seed's chroma as it is.

## Shades

Each of Fluent's seven shades is read from the palette at a fixed tone. The tones match the
lightness of Microsoft's Windows blue shades, rounded to the nearest 5:

| Shade    | Windows blue | Its tone | Tone used |
| -------- | ------------ | -------- | --------- |
| `dark3`  | `#001968`    | 13.8     | 15        |
| `dark2`  | `#003D92`    | 27.9     | 30        |
| `dark1`  | `#005EB7`    | 40.3     | 40        |
| `base`   | `#0078D4`    | 49.7     | 50        |
| `light1` | `#0093F9`    | 59.6     | 60        |
| `light2` | `#60CCFE`    | 77.8     | 80        |
| `light3` | `#98ECFE`    | 88.8     | 90        |

The hue of Microsoft's shades shifts by 64 degrees from the darkest to the lightest. A tonal palette
keeps one hue, so the generated shades match the lightness of Microsoft's but all share your seed's
hue.

A seed with little chroma gives shades with little chroma, so a grey seed produces seven greys.

## Animation

To fade between seeds instead of switching instantly, wrap the colors in `animateFluentColors`:

```kotlin
val colors = animateFluentColors(rememberFluentColors(seedColor = ThemeSettings.seedColor))

FluentTheme(colors = colors) {
    Button(onClick = { ThemeSettings.seedColor = Color(0xFF00695C) }) {
        Text("Teal")
    }
}
```

The seven shades animate, and the color groups that Fluent derives from them follow along.
`system` and `controlOnImage` don't change, because Fluent builds them from its own constants. Going
from light to dark switches instantly, because Fluent derives both from the same shades and a dark
flag, so there are no two sets of colors to animate between.

## Limitations

Fluent's `success`, `caution` and `critical` colors are on `Colors.system`, which Fluent builds from
constants with no way to set them. The scheme's secondary, tertiary and error palettes therefore go
unused.

## Sample

[`samples/fluent`](../samples/fluent) is the Tasks sample built with Fluent components. Run it with
`./gradlew :samples:fluent:run` to change seeds, switch between light and dark, and compare the
generated shades with the single blue Fluent falls back to without the adapter.
