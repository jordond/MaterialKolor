package com.materialkolor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.getColor
import com.materialkolor.ktx.toDynamicScheme
import com.materialkolor.scheme.DynamicScheme

/**
 * Creates and remember a [ColorScheme] based on the given [seedColor] and [isDark] mode.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 */
@Composable
public fun rememberDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme = remember(
    seedColor,
    isDark,
    isAmoled,
    style,
    contrastLevel,
    isExtendedFidelity,
    modifyColorScheme,
) {
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = isAmoled,
        style = style,
        contrastLevel = contrastLevel,
        isExtendedFidelity = isExtendedFidelity,
        modifyColorScheme = modifyColorScheme,
    )
}

/**
 * Creates a [ColorScheme] based on the given [seedColor] and [isDark] mode.
 *
 * @param[seedColor] The color to base the scheme on.
 * @param[isDark] Whether the scheme should be dark or light.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[style] The style of the scheme.
 * @param[contrastLevel] The contrast level of the scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 */
public fun dynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme {
    val scheme = seedColor.toDynamicScheme(isDark, style, contrastLevel)
    return createScheme(scheme, isDark, isAmoled, isExtendedFidelity, modifyColorScheme)
}

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
        sourceColor = seedColor,
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

    return createScheme(scheme, isDark, isAmoled, isExtendedFidelity, modifyColorScheme)
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
internal fun createScheme(
    scheme: DynamicScheme,
    isDark: Boolean,
    isAmoled: Boolean,
    isExtendedFidelity: Boolean = false,
    modifyColorScheme: ((ColorScheme) -> ColorScheme)? = null,
): ColorScheme {
    val colors = MaterialDynamicColors(isExtendedFidelity)
    return ColorScheme(
        background = if (isDark && isAmoled) Color.Black else colors.background().getColor(scheme),
        error = colors.error().getColor(scheme),
        errorContainer = colors.errorContainer().getColor(scheme),
        inverseOnSurface = colors.inverseOnSurface().getColor(scheme),
        inversePrimary = colors.inversePrimary().getColor(scheme),
        inverseSurface = colors.inverseSurface().getColor(scheme),
        onBackground = if (isDark && isAmoled) Color.White else colors.onBackground().getColor(scheme),
        onError = colors.onError().getColor(scheme),
        onErrorContainer = colors.onErrorContainer().getColor(scheme),
        onPrimary = colors.onPrimary().getColor(scheme),
        onPrimaryContainer = colors.onPrimaryContainer().getColor(scheme),
        onSecondary = colors.onSecondary().getColor(scheme),
        onSecondaryContainer = colors.onSecondaryContainer().getColor(scheme),
        onSurface = if (isDark && isAmoled) Color.White else colors.onSurface().getColor(scheme),
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
        surface = if (isDark && isAmoled) Color.Black else colors.surface().getColor(scheme),
        surfaceTint = colors.surfaceTint().getColor(scheme),
        surfaceBright = colors.surfaceBright().getColor(scheme),
        surfaceDim = colors.surfaceDim().getColor(scheme),
        surfaceContainer = colors.surfaceContainer().getColor(scheme),
        surfaceContainerHigh = colors.surfaceContainerHigh().getColor(scheme),
        surfaceContainerHighest = colors.surfaceContainerHighest().getColor(scheme),
        surfaceContainerLow = colors.surfaceContainerLow().getColor(scheme),
        surfaceContainerLowest = colors.surfaceContainerLowest().getColor(scheme),
        surfaceVariant = colors.surfaceVariant().getColor(scheme),
        tertiary = colors.tertiary().getColor(scheme),
        tertiaryContainer = colors.tertiaryContainer().getColor(scheme),
    ).let { modifyColorScheme?.invoke(it) ?: it }
}