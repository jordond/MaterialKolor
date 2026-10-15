# Unstyled sample

The [Tasks sample](../README.md) built on [Compose Unstyled](https://composeunstyled.com).

An Unstyled theme is a set of properties, and a color scheme can override any of them, not just the colors.
This sample's theme has four properties.

- `MaterialKolorTokens.colors`, filled by `rememberDynamicLightDarkColors` from `material-kolor-unstyled`.
- `ShapeTokens.shapes`, including a custom luggage tag `Shape` for the task tags.
- `ShadowTokens.shadows`, the drop shadows and the inner shadow for sunken controls.
- `GradientTokens.gradients`, the fills for accents, raised and sunken surfaces and the page backdrop.

The shadows and gradients name their colors by `MaterialKolorTokens` token instead of holding a `Color`, so they
follow the seed and animate with the rest of the theme. The dark scheme sets its own colors, shadows and gradients
and keeps the base shapes. A drop shadow barely shows on a dark surface, so there the raised shadows turn into glows
in the primary color.

```kotlin
buildThemeV2 {
    val (light, dark) = rememberDynamicLightDarkColors(seedColor = seed)
    properties[MaterialKolorTokens.colors] = light
    properties[ShapeTokens.shapes] = TasksShapes
    properties[ShadowTokens.shadows] = LightShadows
    properties[GradientTokens.gradients] = LightGradients

    colorScheme(ColorScheme.Dark) {
        properties[MaterialKolorTokens.colors] = dark
        properties[ShadowTokens.shadows] = DarkShadows
        properties[GradientTokens.gradients] = DarkGradients
    }
}
```

The Palette tab shows every token the theme resolves for the current seed and mode.

Run it with `./gradlew :samples:unstyled:run`.

![unstyled sample](../screenshots/images/unstyled.png)
