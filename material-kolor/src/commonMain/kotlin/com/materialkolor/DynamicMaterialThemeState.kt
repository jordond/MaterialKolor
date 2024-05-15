package com.materialkolor

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Creates a [DynamicMaterialThemeState] that can be remembered across compositions.
 *
 * @param seedColor The initial seed color to generate the color scheme.
 * @param isDark The initial dark mode state.
 * @param style The initial palette style.
 * @param contrastLevel The initial contrast level.
 * @param extendedFidelity The initial extended fidelity state.
 * @param modifyColorScheme Use this callback to modify the color scheme once it has been generated.
 * Note that if you modify a color in the scheme, the on* color might not have enough contrast.
 */
@Composable
public fun rememberDynamicMaterialThemeState(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    extendedFidelity: Boolean = false,
    modifyColorScheme: (DynamicMaterialThemeState.(ColorScheme) -> ColorScheme)? = null,
): DynamicMaterialThemeState {
    return rememberSaveable(
        seedColor,
        isDark,
        style,
        contrastLevel,
        extendedFidelity,
        modifyColorScheme,
        saver = DynamicMaterialThemeState.Saver(modifyColorScheme),
    ) {
        DynamicMaterialThemeState(
            initialSeedColor = seedColor,
            initialIsDark = isDark,
            initialStyle = style,
            initialContrastLevel = contrastLevel,
            initialExtendedFidelity = extendedFidelity,
            modifyColorScheme = modifyColorScheme,
        )
    }
}

/**
 * State object that holds the current values for a dynamic material theme.
 *
 * Use [rememberDynamicMaterialThemeState] to create an instance of this class.
 *
 * @param initialSeedColor The initial seed color to generate the color scheme.
 * @param initialIsDark The initial dark mode state.
 * @param initialStyle The initial palette style.
 * @param initialContrastLevel The initial contrast level.
 * @param initialExtendedFidelity The initial extended fidelity state.
 * @param modifyColorScheme Use this callback to modify the color scheme once it has been generated.
 * Note that if you modify a color in the scheme, the on* color might not have enough contrast.
 */
@Stable
public class DynamicMaterialThemeState internal constructor(
    initialSeedColor: Color,
    initialIsDark: Boolean,
    initialStyle: PaletteStyle,
    initialContrastLevel: Double,
    initialExtendedFidelity: Boolean,
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
     * The generated color scheme based on the current state.
     */
    public val colorScheme: ColorScheme
        @Composable
        get() = rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = style,
            contrastLevel = contrastLevel,
            isExtendedFidelity = isExtendedFidelity,
            modifyColorScheme = modifyColorScheme?.let { callback ->
                { scheme -> callback(this, scheme) }
            },
        )

    public companion object {

        internal fun Saver(
            modifyColorScheme: (DynamicMaterialThemeState.(ColorScheme) -> ColorScheme)? = null,
        ) = listSaver(
            save = { state ->
                listOf(
                    state.seedColor.value.toLong(),
                    state.isDark,
                    state.style.name,
                    state.contrastLevel,
                    state.isExtendedFidelity,
                )
            },
            restore = { state ->
                DynamicMaterialThemeState(
                    initialSeedColor = Color(state[0] as Long),
                    initialIsDark = state[1] as Boolean,
                    initialStyle = PaletteStyle.valueOf(state[2] as String),
                    initialContrastLevel = state[3] as Double,
                    initialExtendedFidelity = state[4] as Boolean,
                    modifyColorScheme = modifyColorScheme,
                )
            },
        )
    }
}