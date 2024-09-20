package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.internal.asVariant
import com.materialkolor.palettes.TonalPalette
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

public fun DynamicScheme(
    sourceColor: Color,
    style: PaletteStyle,
    isDark: Boolean,
    contrastLevel: Double,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color = DynamicScheme.defaultErrorPalette.keyColor.toColor(),
): DynamicScheme {
    val defaults = sourceColor.toDynamicScheme(isDark, style, contrastLevel)

    return DynamicScheme(
        sourceColorHct = sourceColor.toHct(),
        variant = style.asVariant,
        isDark = isDark,
        contrastLevel = contrastLevel,
        primaryPalette = primary?.toTonalPalette() ?: defaults.primaryPalette,
        secondaryPalette = secondary?.toTonalPalette() ?: defaults.secondaryPalette,
        tertiaryPalette = tertiary?.toTonalPalette() ?: defaults.tertiaryPalette,
        neutralPalette = neutral?.toTonalPalette() ?: defaults.neutralPalette,
        neutralVariantPalette = neutralVariant?.toTonalPalette() ?: defaults.neutralVariantPalette,
        errorPalette = error.toTonalPalette(),
    )
}