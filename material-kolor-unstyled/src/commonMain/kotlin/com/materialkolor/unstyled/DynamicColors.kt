package com.materialkolor.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.rememberDynamicScheme

/**
 * Generates a scheme from [seedColor] and remembers every [MaterialKolorTokens] token paired with
 * its color.
 *
 * The result is a value for a theme property, so your theme decides where it goes. Put the light
 * colors in the base values and the dark colors in the [com.composeunstyled.theme.ColorScheme.Dark]
 * block, next to anything else that scheme changes.
 *
 * ```kotlin
 * val AppTheme = buildThemeV2 {
 *     // Set this property to enable color transitions.
 *     colorSchemeTransitionSpec = tween(300)
 *
 *     properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = false)
 *
 *     colorScheme(ColorScheme.Dark) {
 *         properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = true)
 *     }
 * }
 * ```
 *
 * Base values are the fallback for every color scheme that does not set the property, so the light
 * colors also cover [com.composeunstyled.theme.ColorScheme.Light] and any scheme of your own that
 * leaves colors alone. A custom scheme gets its own colors the same way.
 *
 * ```kotlin
 * colorScheme(Sepia) {
 *     properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = Color(0xff704214), isDark = false)
 * }
 * ```
 *
 * The parameters are the ones [rememberDynamicScheme] takes. [isDark] has no default because where
 * you call this decides the mode, not the system setting.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[primary] The primary color of the scheme.
 * @param[secondary] The secondary color of the scheme.
 * @param[tertiary] The tertiary color of the scheme.
 * @param[neutral] The neutral color of the scheme.
 * @param[neutralVariant] The neutral variant color of the scheme.
 * @param[error] The error color of the scheme.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[specVersion] The version of the color specification to use.
 * @param[platform] The platform to use for the scheme.
 * @return The remembered token values, the same map [DynamicScheme.toThemeValues] gives.
 */
@Composable
public fun rememberDynamicColors(
    seedColor: Color,
    isDark: Boolean,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: DynamicScheme.Platform = DynamicScheme.Platform.Default,
): Map<ThemeToken<Color>, Color> {
    val scheme = rememberDynamicScheme(
        seedColor = seedColor,
        isDark = isDark,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
    )

    return remember(scheme) { scheme.toThemeValues() }
}
