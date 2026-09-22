package com.materialkolor.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.ThemeBuilder
import com.composeunstyled.theme.ThemeBuilderV2
import com.composeunstyled.theme.ThemeProperty
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.rememberDynamicScheme

/**
 * Writes [scheme] into [property] as MaterialKolor tokens.
 *
 * Use this when the theme decides which scheme it wants, for example a single pinned scheme or one
 * that came from application state.
 *
 * ```kotlin
 * val AppTheme = buildThemeV2 {
 *     dynamicColors(DynamicScheme(seedColor = Color(0xff8811aa), isDark = false))
 * }
 * ```
 *
 * @param[scheme] The scheme whose roles become token values.
 * @param[property] The property to write, [MaterialKolorTokens.colors] unless the application owns
 * its own.
 */
public fun ThemeBuilder.dynamicColors(
    scheme: DynamicScheme,
    property: ThemeProperty<Color> = MaterialKolorTokens.colors,
) {
    properties[property] = scheme.toThemeValues()
}

/**
 * Generates a light and a dark scheme from [seedColor] and wires both into the theme.
 *
 * The light scheme becomes the base value of [property] and the dark scheme becomes the override for
 * [ColorScheme.Dark], so `AppTheme { }` follows the system setting and `AppTheme(colorScheme = ColorScheme.Light) { }`
 * pins one. Only [ColorScheme.Light] and [ColorScheme.Dark] are wired, so a custom scheme name reads the light values
 * unless the theme overrides them.
 *
 * To animate, set [ThemeBuilderV2.colorSchemeTransitionSpec] on the builder and Unstyled animates every color token
 * whenever it changes, whether the seed moved or the scheme flipped between light and dark.
 *
 * ```kotlin
 * val AppTheme = buildThemeV2 {
 *     // Set this property to enable color transitions.
 *     colorSchemeTransitionSpec = tween(300)
 *
 *     dynamicColorSchemes(seedColor = Color(0xff8811aa))
 * }
 * ```
 *
 * The parameters are the ones [rememberDynamicScheme] takes, minus `isDark` which both schemes own.
 *
 * @param[seedColor] The color to base the schemes on.
 * @param[primary] The primary color of the schemes.
 * @param[secondary] The secondary color of the schemes.
 * @param[tertiary] The tertiary color of the schemes.
 * @param[neutral] The neutral color of the schemes.
 * @param[neutralVariant] The neutral variant color of the schemes.
 * @param[error] The error color of the schemes.
 * @param[style] The style of the schemes.
 * @param[contrastLevel] The contrast level of the schemes.
 * @param[specVersion] The version of the color specification to use.
 * @param[platform] The platform to use for the schemes.
 * @param[property] The property to write, [MaterialKolorTokens.colors] unless the application owns its own.
 */
@Composable
public fun ThemeBuilderV2.dynamicColorSchemes(
    seedColor: Color,
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
    property: ThemeProperty<Color> = MaterialKolorTokens.colors,
) {
    val lightScheme = rememberDynamicScheme(
        seedColor = seedColor,
        isDark = false,
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

    val darkScheme = rememberDynamicScheme(
        seedColor = seedColor,
        isDark = true,
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

    val lightValues = remember(lightScheme) { lightScheme.toThemeValues() }
    val darkValues = remember(darkScheme) { darkScheme.toThemeValues() }

    properties[property] = lightValues

    colorScheme(ColorScheme.Dark) {
        properties[property] = darkValues
    }
}
