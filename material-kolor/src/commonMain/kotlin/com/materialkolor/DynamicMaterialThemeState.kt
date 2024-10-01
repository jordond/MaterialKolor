package com.materialkolor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.rememberDynamicScheme
import com.materialkolor.scheme.DynamicScheme

/**
 * Creates a [DynamicMaterialThemeState] that can be remembered across compositions using custom colors.
 *
 * @param[seedColor] The initial seed color to generate the color scheme.
 * @param[isDark] The initial dark mode state.
 * @param[isAmoled] The initial Amoled state.
 * @param[primary] A custom color to modify the primary color in the generated color scheme.
 * @param[secondary] A custom color to modify the secondary color in the generated color scheme.
 * @param[tertiary] A custom color to modify the tertiary color in the generated color scheme.
 * @param[neutral] A custom color to modify the neutral color in the generated color scheme.
 * @param[neutralVariant] A custom color to modify the neutral variant color in the generated color scheme.
 * @param[error] A custom color to modify the error color in the generated color scheme.
 * @param[style] The initial palette style.
 * @param[contrastLevel] The initial contrast level.
 * @param[extendedFidelity] The initial extended fidelity state.
 * @param[modifyColorScheme] Use this callback to modify the color scheme once it has been generated.
 * Note that if you modify a color in the scheme, the on* color might not have enough contrast.
 */
@Composable
public fun rememberDynamicMaterialThemeState(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    extendedFidelity: Boolean = false,
    modifyColorScheme: (DynamicMaterialThemeState.(ColorScheme) -> ColorScheme)? = null,
): DynamicMaterialThemeState {
    return remember(
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
        extendedFidelity,
        modifyColorScheme,
    ) {
        DynamicMaterialThemeState(
            initialSeedColor = seedColor,
            initialIsDark = isDark,
            initialIsAmoled = isAmoled,
            initialStyle = style,
            initialContrastLevel = contrastLevel,
            initialExtendedFidelity = extendedFidelity,
            initialPrimary = primary,
            initialSecondary = secondary,
            initialTertiary = tertiary,
            initialNeutral = neutral,
            initialNeutralVariant = neutralVariant,
            initialError = error,
            modifyColorScheme = modifyColorScheme,
        )
    }
}

/**
 * Creates a [DynamicMaterialThemeState] that can be remembered across compositions using custom colors.
 *
 * **Note:** The [primary] color will be used as the seed color.
 *
 * @param[primary] The initial seed color to generate the color scheme.
 * @param[isDark] The initial dark mode state.
 * @param[isAmoled] The initial Amoled state.
 * @param[secondary] A custom color to modify the secondary color in the generated color scheme.
 * @param[tertiary] A custom color to modify the tertiary color in the generated color scheme.
 * @param[neutral] A custom color to modify the neutral color in the generated color scheme.
 * @param[neutralVariant] A custom color to modify the neutral variant color in the generated color scheme.
 * @param[error] A custom color to modify the error color in the generated color scheme.
 * @param[style] The initial palette style.
 * @param[contrastLevel] The initial contrast level.
 * @param[extendedFidelity] The initial extended fidelity state.
 * @param[modifyColorScheme] Use this callback to modify the color scheme once it has been generated.
 * Note that if you modify a color in the scheme, the on* color might not have enough contrast.
 */
@Composable
public fun rememberDynamicMaterialThemeState(
    primary: Color,
    isDark: Boolean,
    isAmoled: Boolean = false,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    extendedFidelity: Boolean = false,
    modifyColorScheme: (DynamicMaterialThemeState.(ColorScheme) -> ColorScheme)? = null,
): DynamicMaterialThemeState {
    return remember(
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
        extendedFidelity,
        modifyColorScheme,
    ) {
        DynamicMaterialThemeState(
            initialSeedColor = primary,
            initialIsDark = isDark,
            initialIsAmoled = isAmoled,
            initialStyle = style,
            initialContrastLevel = contrastLevel,
            initialExtendedFidelity = extendedFidelity,
            initialPrimary = primary,
            initialSecondary = secondary,
            initialTertiary = tertiary,
            initialNeutral = neutral,
            initialNeutralVariant = neutralVariant,
            initialError = error,
            modifyColorScheme = modifyColorScheme,
        )
    }
}

/**
 * State object that holds the current values for a dynamic material theme.
 *
 * Use [rememberDynamicMaterialThemeState] to create an instance of this class.
 *
 * @param[initialSeedColor] The initial seed color to generate the color scheme.
 * @param[initialIsDark] The initial dark mode state.
 * @param[initialIsAmoled] The initial Amoled state.
 * @param[initialStyle] The initial palette style.
 * @param[initialContrastLevel] The initial contrast level.
 * @param[initialExtendedFidelity] The initial extended fidelity state.
 * @param[initialPrimary] A custom color to modify the primary color in the generated color scheme.
 * @param[initialSecondary] A custom color to modify the secondary color in the generated color scheme.
 * @param[initialTertiary] A custom color to modify the tertiary color in the generated color scheme.
 * @param[initialNeutral] A custom color to modify the neutral color in the generated color scheme.
 * @param[initialNeutralVariant] A custom color to modify the neutral variant color in the generated color scheme.
 * @param[initialError] A custom color to modify the error color in the generated color scheme.
 * @param[modifyColorScheme] Use this callback to modify the color scheme once it has been generated.
 * Note that if you modify a color in the scheme, the on* color might not have enough contrast.
 */
@Suppress("MemberVisibilityCanBePrivate")
@Stable
public class DynamicMaterialThemeState internal constructor(
    initialSeedColor: Color,
    initialIsDark: Boolean,
    initialIsAmoled: Boolean,
    initialStyle: PaletteStyle,
    initialContrastLevel: Double,
    initialExtendedFidelity: Boolean,
    initialPrimary: Color? = null,
    initialSecondary: Color? = null,
    initialTertiary: Color? = null,
    initialNeutral: Color? = null,
    initialNeutralVariant: Color? = null,
    initialError: Color? = null,
    public val modifyColorScheme: (DynamicMaterialThemeState.(ColorScheme) -> ColorScheme)?,
) {

    /**
     * The seed color to generate the color scheme from.
     *
     * @see dynamicColorScheme
     */
    public var seedColor: Color by mutableStateOf(initialSeedColor)

    /**
     * The dark mode state.
     *
     * @see dynamicColorScheme
     */
    public var isDark: Boolean by mutableStateOf(initialIsDark)

    /**
     * The dark mode with Amoled state.
     *
     * @see dynamicColorScheme
     */
    public var isAmoled: Boolean by mutableStateOf(initialIsAmoled)

    /**
     * The palette style.
     *
     * @See PaletteStyle
     * @see dynamicColorScheme
     */
    public var style: PaletteStyle by mutableStateOf(initialStyle)

    /**
     * The contrast level.
     *
     * @see dynamicColorScheme
     */
    public var contrastLevel: Double by mutableStateOf(initialContrastLevel)

    /**
     * The extended fidelity state.
     *
     * @see dynamicColorScheme
     */
    public var isExtendedFidelity: Boolean by mutableStateOf(initialExtendedFidelity)

    /**
     * A custom color to modify the primary color in the generated color scheme.
     *
     * **Note:** If this is set, then this value will be used as the seed color instead of [seedColor].
     */
    public var primary: Color? by mutableStateOf(initialPrimary)

    /**
     * A custom color to modify the secondary color in the generated color scheme.
     */
    public var secondary: Color? by mutableStateOf(initialSecondary)

    /**
     * A custom color to modify the tertiary color in the generated color scheme.
     */
    public var tertiary: Color? by mutableStateOf(initialTertiary)

    /**
     * A custom color to modify the neutral color in the generated color scheme.
     */
    public var neutral: Color? by mutableStateOf(initialNeutral)

    /**
     * A custom color to modify the neutral variant color in the generated color scheme.
     */
    public var neutralVariant: Color? by mutableStateOf(initialNeutralVariant)

    /**
     * A custom color to modify the error color in the generated color scheme.
     */
    public var error: Color? by mutableStateOf(initialError)

    private val isCustomScheme: Boolean
        get() = listOf(primary, secondary, tertiary, neutral, neutralVariant, error)
            .any { it != null }

    public val dynamicScheme: DynamicScheme
        @Composable
        get() {
            val primary = this.primary
            return when {
                primary != null -> rememberDynamicScheme(
                    seedColor = primary,
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
                isCustomScheme -> rememberDynamicScheme(
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
                else -> rememberDynamicScheme(
                    seedColor = seedColor,
                    isDark = isDark,
                    style = style,
                    contrastLevel = contrastLevel,
                )
            }
        }

    /**
     * The generated color scheme based on the current state.
     */
    public val colorScheme: ColorScheme
        @Composable
        get() {
            val primary = this.primary
            val callback: ((ColorScheme) -> ColorScheme)? = modifyColorScheme
                ?.let { callback -> { scheme -> callback(this, scheme) } }

            return when {
                primary != null -> rememberDynamicColorScheme(
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
                    modifyColorScheme = callback,
                )
                isCustomScheme -> rememberDynamicColorScheme(
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
                    modifyColorScheme = callback,
                )
                else -> rememberDynamicColorScheme(
                    seedColor = seedColor,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    primary = null,
                    style = style,
                    contrastLevel = contrastLevel,
                    isExtendedFidelity = isExtendedFidelity,
                    modifyColorScheme = callback,
                )
            }
        }
}