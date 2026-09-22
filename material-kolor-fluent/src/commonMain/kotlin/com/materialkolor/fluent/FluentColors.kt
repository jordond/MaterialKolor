package com.materialkolor.fluent

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.rememberDynamicScheme
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import io.github.composefluent.Colors
import io.github.composefluent.Shades

/**
 * Turns a tonal ramp into the seven shades Fluent themes from.
 *
 * Fluent ships one accent, Windows blue, and `generateShades` is a lookup with a single entry, so
 * any other accent silently comes back as that blue. This is the way round it. Give Fluent a ramp
 * that was generated rather than looked up.
 *
 * The tones below are anchored to the lightness of Microsoft's own Windows blue family, rounded to
 * the nearest five.
 *
 * | Shade | Windows blue | its tone | tone used here |
 * |---|---|---|---|
 * | [Shades.dark3] | `#001968` | 13.8 | 15 |
 * | [Shades.dark2] | `#003D92` | 27.9 | 30 |
 * | [Shades.dark1] | `#005EB7` | 40.3 | 40 |
 * | [Shades.base] | `#0078D4` | 49.7 | 50 |
 * | [Shades.light1] | `#0093F9` | 59.6 | 60 |
 * | [Shades.light2] | `#60CCFE` | 77.8 | 80 |
 * | [Shades.light3] | `#98ECFE` | 88.8 | 90 |
 *
 * Microsoft's ramp shifts hue as it goes, 64 degrees from the darkest shade to the lightest, and a
 * tonal palette holds hue steady. So this matches the lightness of that ramp and not its hue. The
 * result is a ramp of one color rather than a reproduction of the Windows one, which is what you
 * want from a generated accent anyway.
 *
 * A seed with little chroma gives a Fluent theme with little chroma. A grey seed produces seven
 * greys, which is the ramp doing its job rather than a fault.
 *
 * ```kotlin
 * // Fluent themed off the scheme's secondary ramp rather than its primary.
 * val shades = scheme.secondaryPalette.toFluentShades()
 * FluentTheme(colors = Colors(shades, darkMode = scheme.isDark)) {
 *     Text("Themed from the secondary ramp")
 * }
 * ```
 *
 * @receiver The ramp to read the seven tones from, usually a scheme's primary palette.
 * @return [Shades] for [Colors], or for anything else taking Fluent's shades.
 */
public fun TonalPalette.toFluentShades(): Shades =
    Shades(
        base = toneColor(BASE_TONE),
        light1 = toneColor(LIGHT_1_TONE),
        light2 = toneColor(LIGHT_2_TONE),
        light3 = toneColor(LIGHT_3_TONE),
        dark1 = toneColor(DARK_1_TONE),
        dark2 = toneColor(DARK_2_TONE),
        dark3 = toneColor(DARK_3_TONE),
    )

/**
 * Builds Fluent [Colors] from this scheme's primary ramp.
 *
 * ```kotlin
 * FluentTheme(colors = scheme.toFluentColors()) {
 *     Text("Themed from a seed colour")
 * }
 * ```
 *
 * Light and dark are the same seven shades with a different flag, because that is how Fluent works.
 * `lightColors` and `darkColors` both call `generateShades` and differ only in the `darkMode` they
 * pass on. This reads [DynamicScheme.isDark] so the two stay in step.
 *
 * Only the primary ramp is used. Fluent's `success`, `caution` and `critical` come from
 * `Colors.system`, which is built from constants and has no setter a caller can reach, so a
 * scheme's secondary, tertiary and error ramps have nowhere to go. Pass another ramp to
 * [toFluentShades] yourself if you want a Fluent theme built on one of them.
 *
 * The ramp this reads is the scheme's, which is not the same as the seed's own. A
 * [com.materialkolor.PaletteStyle] reshapes chroma on the way through, so `TonalSpot` gives a
 * calmer accent than the seed and `Fidelity` stays close to it. To get the seed exactly as it was
 * handed over, skip the scheme.
 *
 * ```kotlin
 * val shades = TonalPalette.from(seedColor).toFluentShades()
 * ```
 *
 * @receiver The scheme to take the primary ramp and the dark flag from.
 * @return [Colors] to hand to `FluentTheme`.
 */
public fun DynamicScheme.toFluentColors(): Colors =
    Colors(
        shades = primaryPalette.toFluentShades(),
        darkMode = isDark,
    )

/**
 * Generates Fluent [Colors] from [seedColor] and remembers them.
 *
 * ```kotlin
 * FluentTheme(colors = rememberFluentColors(seedColor = Color(0xff8811aa))) {
 *     Text("Themed from a seed colour")
 * }
 * ```
 *
 * [Colors] holds fourteen pieces of state and derives twelve groups of colors in its constructor,
 * so building one on every recomposition is work worth skipping. The parameters are the ones
 * [rememberDynamicScheme] takes.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether to build the dark variant.
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
 * @return The remembered [Colors].
 */
@Composable
public fun rememberFluentColors(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
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
): Colors {
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

    return remember(scheme) { scheme.toFluentColors() }
}

private const val DARK_3_TONE = 15
private const val DARK_2_TONE = 30
private const val DARK_1_TONE = 40
private const val BASE_TONE = 50
private const val LIGHT_1_TONE = 60
private const val LIGHT_2_TONE = 80
private const val LIGHT_3_TONE = 90
