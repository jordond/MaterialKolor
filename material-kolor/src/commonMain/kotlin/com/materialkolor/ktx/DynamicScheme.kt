package com.materialkolor.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.internal.asVariant
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/**
 * Get the [Color] used to generate the [DynamicScheme].
 */
public val DynamicScheme.sourceColor: Color
    get() = Color(sourceColorArgb)

/**
 * Generate a [DynamicScheme] based on the given [Color].
 *
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[style] The style of the scheme, see [PaletteStyle].
 * @param[contrastLevel] The contrast level of the scheme.
 * @return The generated [DynamicScheme].
 */
public fun Color.toDynamicScheme(
    isDark: Boolean,
    style: PaletteStyle,
    contrastLevel: Double = Contrast.Default.value,
): DynamicScheme {
    val hct = toHct()
    return when (style) {
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.Vibrant -> SchemeVibrant(hct, isDark, contrastLevel)
        PaletteStyle.Expressive -> SchemeExpressive(hct, isDark, contrastLevel)
        PaletteStyle.Rainbow -> SchemeRainbow(hct, isDark, contrastLevel)
        PaletteStyle.FruitSalad -> SchemeFruitSalad(hct, isDark, contrastLevel)
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Fidelity -> SchemeFidelity(hct, isDark, contrastLevel)
        PaletteStyle.Content -> SchemeContent(hct, isDark, contrastLevel)
    }
}

/**
 * Create and remember a [DynamicScheme] based on the provided colors.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [seedColor].
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
 */
@Composable
public fun rememberDynamicScheme(
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
): DynamicScheme = remember(
    seedColor,
    isDark,
    primary,
    secondary,
    tertiary,
    neutral,
    neutralVariant,
    error,
    style,
    contrastLevel,
) {
    DynamicScheme(
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
    )
}

/**
 * Create a [DynamicScheme] based on the provided colors.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [seedColor].
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
 */
public fun DynamicScheme(
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
): DynamicScheme {
    val defaults = seedColor.toDynamicScheme(isDark, style, contrastLevel)

    return DynamicScheme(
        sourceColorHct = seedColor.toHct(),
        variant = style.asVariant,
        isDark = isDark,
        contrastLevel = contrastLevel,
        primaryPalette = primary?.toTonalPalette() ?: defaults.primaryPalette,
        secondaryPalette = secondary?.toTonalPalette() ?: defaults.secondaryPalette,
        tertiaryPalette = tertiary?.toTonalPalette() ?: defaults.tertiaryPalette,
        neutralPalette = neutral?.toTonalPalette() ?: defaults.neutralPalette,
        neutralVariantPalette = neutralVariant?.toTonalPalette() ?: defaults.neutralVariantPalette,
        errorPalette = error?.toTonalPalette() ?: defaults.errorPalette,
    )
}