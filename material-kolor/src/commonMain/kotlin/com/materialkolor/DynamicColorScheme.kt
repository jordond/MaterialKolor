package com.materialkolor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.getColor
import com.materialkolor.ktx.toDynamicScheme

/**
 * Creates and remember a [ColorScheme] based on the given [seedColor] and [isDark] mode.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 */
@Composable
public fun rememberDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    isExtendedFidelity: Boolean = false,
): ColorScheme = remember(seedColor, isDark, style, contrastLevel, isExtendedFidelity) {
    dynamicColorScheme(seedColor, isDark, style, contrastLevel, isExtendedFidelity)
}

/**
 * Creates a [ColorScheme] based on the given [seedColor] and [isDark] mode.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 */
public fun dynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
): ColorScheme {
    val scheme = seedColor.toDynamicScheme(isDark, style, contrastLevel)
    val colors = MaterialDynamicColors(isExtendedFidelity)

    return ColorScheme(
        background = colors.background().getColor(scheme),
        error = colors.error().getColor(scheme),
        errorContainer = colors.errorContainer().getColor(scheme),
        inverseOnSurface = colors.inverseOnSurface().getColor(scheme),
        inversePrimary = colors.inversePrimary().getColor(scheme),
        inverseSurface = colors.inverseSurface().getColor(scheme),
        onBackground = colors.onBackground().getColor(scheme),
        onError = colors.onError().getColor(scheme),
        onErrorContainer = colors.onErrorContainer().getColor(scheme),
        onPrimary = colors.onPrimary().getColor(scheme),
        onPrimaryContainer = colors.onPrimaryContainer().getColor(scheme),
        onSecondary = colors.onSecondary().getColor(scheme),
        onSecondaryContainer = colors.onSecondaryContainer().getColor(scheme),
        onSurface = colors.onSurface().getColor(scheme),
        onSurfaceVariant = colors.onSurfaceVariant().getColor(scheme),
        onTertiary = colors.onTertiary().getColor(scheme),
        onTertiaryContainer = colors.onTertiaryContainer().getColor(scheme),
        outline = colors.outline().getColor(scheme),
        outlineVariant = colors.outlineVariant().getColor(scheme),
        primary = colors.primary().getColor(scheme),
        primaryContainer = colors.primaryContainer().getColor(scheme),
        scrim = colors.scrim().getColor(scheme),
        secondary = colors.secondary().getColor(scheme),
        secondaryContainer = colors.secondaryContainer().getColor(scheme),
        surface = colors.surface().getColor(scheme),
        surfaceTint = colors.surfaceTint().getColor(scheme),
        surfaceVariant = colors.surfaceVariant().getColor(scheme),
        tertiary = colors.tertiary().getColor(scheme),
        tertiaryContainer = colors.tertiaryContainer().getColor(scheme),
    )
}
