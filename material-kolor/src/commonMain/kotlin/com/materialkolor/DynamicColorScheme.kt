package com.materialkolor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.scheme.DynamicScheme

/**
 * Create and remember a custom [ColorScheme] based on the provided colors.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [seedColor].
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[primary] The primary color of the scheme.
 * @param[secondary] The secondary color of the scheme.
 * @param[tertiary] The tertiary color of the scheme.
 * @param[neutral] The neutral color of the scheme.
 * @param[neutralVariant] The neutral variant color of the scheme.
 * @param[error] The error color of the scheme.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[modifyColorScheme] A lambda to modify the created [ColorScheme].
 */
@Composable
public fun rememberDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme = remember(
    seedColor,
    isDark,
    isAmoled,
    primary,
    secondary,
    tertiary,
    neutral,
    neutralVariant,
    error,
    style,
    contrastLevel,
    isExtendedFidelity,
    modifyColorScheme,
) {
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = isAmoled,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        isExtendedFidelity = isExtendedFidelity,
        modifyColorScheme = modifyColorScheme,
    )
}

/**
 * Create a custom [ColorScheme] based on the provided colors.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [seedColor].
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[primary] The primary color of the scheme.
 * @param[secondary] The secondary color of the scheme.
 * @param[tertiary] The tertiary color of the scheme.
 * @param[neutral] The neutral color of the scheme.
 * @param[neutralVariant] The neutral variant color of the scheme.
 * @param[error] The error color of the scheme.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[modifyColorScheme] A lambda to modify the created [ColorScheme].
 */
public fun dynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme {
    val scheme = DynamicScheme(
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

    return scheme.toColorScheme(isAmoled, isExtendedFidelity, modifyColorScheme)
}

/**
 * Create and remember a custom [ColorScheme] based on the provided colors. Uses [primary] as the seed color.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [primary].
 *
 * @param[primary] The color to base the scheme on, and primary color of the scheme.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[secondary] The secondary color of the scheme.
 * @param[tertiary] The tertiary color of the scheme.
 * @param[neutral] The neutral color of the scheme.
 * @param[neutralVariant] The neutral variant color of the scheme.
 * @param[error] The error color of the scheme.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[modifyColorScheme] A lambda to modify the created [ColorScheme].
 */
@Composable
public fun rememberDynamicColorScheme(
    primary: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme = remember(
    primary,
    isDark,
    isAmoled,
    secondary,
    tertiary,
    neutral,
    neutralVariant,
    error,
    style,
    contrastLevel,
    isExtendedFidelity,
    modifyColorScheme,
) {
    dynamicColorScheme(
        primary = primary,
        isDark = isDark,
        isAmoled = isAmoled,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        isExtendedFidelity = isExtendedFidelity,
        modifyColorScheme = modifyColorScheme,
    )
}

/**
 * Create a custom [ColorScheme] based on the provided colors. Uses [primary] as the seed color.
 *
 * If a color is not provided, then the color palette will be generated from the [style] and [primary].
 *
 * @param[primary] The color to base the scheme on, and primary color of the scheme.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[secondary] The secondary color of the scheme.
 * @param[tertiary] The tertiary color of the scheme.
 * @param[neutral] The neutral color of the scheme.
 * @param[neutralVariant] The neutral variant color of the scheme.
 * @param[error] The error color of the scheme.
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[modifyColorScheme] A lambda to modify the created [ColorScheme].
 */
public fun dynamicColorScheme(
    primary: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme = dynamicColorScheme(
    seedColor = primary,
    isDark = isDark,
    isAmoled = isAmoled,
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    neutral = neutral,
    neutralVariant = neutralVariant,
    error = error,
    style = style,
    contrastLevel = contrastLevel,
    isExtendedFidelity = isExtendedFidelity,
    modifyColorScheme = modifyColorScheme,
)

/**
 * Create the actual [ColorScheme] based on the given [DynamicScheme].
 */
public fun DynamicScheme.toColorScheme(
    isAmoled: Boolean,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme {
    val colors = MaterialKolors(scheme = this, isAmoled, isExtendedFidelity)
    return ColorScheme(
        background = colors.background(),
        error = colors.error(),
        errorContainer = colors.errorContainer(),
        inverseOnSurface = colors.inverseOnSurface(),
        inversePrimary = colors.inversePrimary(),
        inverseSurface = colors.inverseSurface(),
        onBackground = colors.onBackground(),
        onError = colors.onError(),
        onErrorContainer = colors.onErrorContainer(),
        onPrimary = colors.onPrimary(),
        onPrimaryContainer = colors.onPrimaryContainer(),
        onSecondary = colors.onSecondary(),
        onSecondaryContainer = colors.onSecondaryContainer(),
        onSurface = colors.onSurface(),
        onSurfaceVariant = colors.onSurfaceVariant(),
        onTertiary = colors.onTertiary(),
        onTertiaryContainer = colors.onTertiaryContainer(),
        outline = colors.outline(),
        outlineVariant = colors.outlineVariant(),
        primary = colors.primary(),
        primaryContainer = colors.primaryContainer(),
        scrim = colors.scrim(),
        secondary = colors.secondary(),
        secondaryContainer = colors.secondaryContainer(),
        surface = colors.surface(),
        surfaceTint = colors.surfaceTint(),
        surfaceBright = colors.surfaceBright(),
        surfaceDim = colors.surfaceDim(),
        surfaceContainer = colors.surfaceContainer(),
        surfaceContainerHigh = colors.surfaceContainerHigh(),
        surfaceContainerHighest = colors.surfaceContainerHighest(),
        surfaceContainerLow = colors.surfaceContainerLow(),
        surfaceContainerLowest = colors.surfaceContainerLowest(),
        surfaceVariant = colors.surfaceVariant(),
        tertiary = colors.tertiary(),
        tertiaryContainer = colors.tertiaryContainer(),
    ).let { modifyColorScheme?.invoke(it) ?: it }
}