package com.materialkolor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors

/**
 * A Material Theme that adapts to the given seed color and the provided custom colors.
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see dynamicColorScheme
 * @see PaletteStyle
 * @param[seedColor] The seed color to use for generating the color scheme.
 * @param[useDarkTheme] Whether to use a dark theme or not.
 * @param[withAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[primary] The custom primary color of the color scheme.
 * @param[secondary] The custom secondary color of the color scheme.
 * @param[tertiary] The custom tertiary color of the color scheme.
 * @param[neutral] The custom neutral color of the color scheme.
 * @param[neutralVariant] The custom neutral variant color of the color scheme.
 * @param[error] The custom error color of the color scheme.
 * @param[style] The style of the color scheme.
 * @param[contrastLevel] The contrast level of the color scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[animate] Whether to animate the color scheme or not.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    withAmoled: Boolean = false,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    isExtendedFidelity: Boolean = false,
    animate: Boolean = false,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    content: @Composable () -> Unit,
) {
    val state = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = useDarkTheme,
        isAmoled = withAmoled,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        extendedFidelity = isExtendedFidelity,
    )

    DynamicMaterialTheme(
        state = state,
        shapes = shapes,
        typography = typography,
        animate = animate,
        animationSpec = animationSpec,
        content = content,
    )
}

/**
 * A Material Theme that adapts to the given seed color and the provided custom colors.
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see dynamicColorScheme
 * @see PaletteStyle
 * @param[primary] The primary color of the color scheme.
 * @param[useDarkTheme] Whether to use a dark theme or not.
 * @param[withAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[secondary] The custom secondary color of the color scheme.
 * @param[tertiary] The custom tertiary color of the color scheme.
 * @param[neutral] The custom neutral color of the color scheme.
 * @param[neutralVariant] The custom neutral variant color of the color scheme.
 * @param[error] The custom error color of the color scheme.
 * @param[style] The style of the color scheme.
 * @param[contrastLevel] The contrast level of the color scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[animate] Whether to animate the color scheme or not.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    primary: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    withAmoled: Boolean = false,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    isExtendedFidelity: Boolean = false,
    animate: Boolean = false,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    content: @Composable () -> Unit,
) {
    val state = rememberDynamicMaterialThemeState(
        primary = primary,
        isDark = useDarkTheme,
        isAmoled = withAmoled,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        extendedFidelity = isExtendedFidelity,
    )

    DynamicMaterialTheme(
        state = state,
        shapes = shapes,
        typography = typography,
        animate = animate,
        animationSpec = animationSpec,
        content = content,
    )
}

/**
 * A Material Theme that adapts to the given [DynamicMaterialThemeState.seedColor].
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see DynamicMaterialThemeState
 * @see PaletteStyle
 * @param[state] The state of the dynamic material theme.
 * @param[animate] Whether to animate the color scheme or not.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    state: DynamicMaterialThemeState,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    animate: Boolean = false,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    content: @Composable () -> Unit,
) {
    val colorScheme = state.colorScheme
    val scheme =
        if (!animate) colorScheme
        else {
            colorScheme.copy(
                primary = colorScheme.primary.animate(animationSpec),
                primaryContainer = colorScheme.primaryContainer.animate(animationSpec),
                secondary = colorScheme.secondary.animate(animationSpec),
                secondaryContainer = colorScheme.secondaryContainer.animate(animationSpec),
                tertiary = colorScheme.tertiary.animate(animationSpec),
                tertiaryContainer = colorScheme.tertiaryContainer.animate(animationSpec),
                background = colorScheme.background.animate(animationSpec),
                surface = colorScheme.surface.animate(animationSpec),
                surfaceTint = colorScheme.surfaceTint.animate(animationSpec),
                surfaceBright = colorScheme.surfaceBright.animate(animationSpec),
                surfaceDim = colorScheme.surfaceDim.animate(animationSpec),
                surfaceContainer = colorScheme.surfaceContainer.animate(animationSpec),
                surfaceContainerHigh = colorScheme.surfaceContainerHigh.animate(animationSpec),
                surfaceContainerHighest = colorScheme.surfaceContainerHighest.animate(animationSpec),
                surfaceContainerLow = colorScheme.surfaceContainerLow.animate(animationSpec),
                surfaceContainerLowest = colorScheme.surfaceContainerLowest.animate(animationSpec),
                surfaceVariant = colorScheme.surfaceVariant.animate(animationSpec),
                error = colorScheme.error.animate(animationSpec),
                errorContainer = colorScheme.errorContainer.animate(animationSpec),
                onPrimary = colorScheme.onPrimary.animate(animationSpec),
                onPrimaryContainer = colorScheme.onPrimaryContainer.animate(animationSpec),
                onSecondary = colorScheme.onSecondary.animate(animationSpec),
                onSecondaryContainer = colorScheme.onSecondaryContainer.animate(animationSpec),
                onTertiary = colorScheme.onTertiary.animate(animationSpec),
                onTertiaryContainer = colorScheme.onTertiaryContainer.animate(animationSpec),
                onBackground = colorScheme.onBackground.animate(animationSpec),
                onSurface = colorScheme.onSurface.animate(animationSpec),
                onSurfaceVariant = colorScheme.onSurfaceVariant.animate(animationSpec),
                onError = colorScheme.onError.animate(animationSpec),
                onErrorContainer = colorScheme.onErrorContainer.animate(animationSpec),
                inversePrimary = colorScheme.inversePrimary.animate(animationSpec),
                inverseSurface = colorScheme.inverseSurface.animate(animationSpec),
                inverseOnSurface = colorScheme.inverseOnSurface.animate(animationSpec),
                outline = colorScheme.outline.animate(animationSpec),
                outlineVariant = colorScheme.outlineVariant.animate(animationSpec),
                scrim = colorScheme.scrim.animate(animationSpec),
            )
        }

    CompositionLocalProvider(LocalDynamicMaterialThemeSeed provides state.seedColor) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}

@Composable
private fun Color.animate(animationSpec: AnimationSpec<Color>): Color {
    return animateColorAsState(this, animationSpec).value
}