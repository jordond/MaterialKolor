package com.materialkolor.palette

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kmpalette.PaletteState
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.rememberDynamicScheme

/**
 * Create and remember a [DynamicScheme] seeded by the image [palette] generated from.
 *
 * The seed is the scored color of the last successful generation, so the scheme is built from
 * [fallback] until there is a palette and rebuilt from the image once there is one.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and the seed.
 *
 * @param[palette] The [PaletteState] to read the seed color from.
 * @param[fallback] color to seed the scheme with until [palette] has a suitable one.
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
 * @return The generated [DynamicScheme].
 */
@Composable
public fun rememberDynamicScheme(
    palette: PaletteState<*>,
    fallback: Color,
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
): DynamicScheme {
    val seed = palette.themeColor(fallback)

    return rememberDynamicScheme(
        seedColor = seed,
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
}
