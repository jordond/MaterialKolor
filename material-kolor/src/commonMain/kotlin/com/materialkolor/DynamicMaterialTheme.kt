package com.materialkolor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors

/**
 * A Material Theme that adapts to the given seed color.
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see dynamicColorScheme
 * @see PaletteStyle
 * @param[seedColor] The seed color to use for generating the color scheme.
 * @param[useDarkTheme] Whether to use a dark theme or not.
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
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    isExtendedFidelity: Boolean = false,
    animate: Boolean = false,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    content: @Composable () -> Unit,
) {
    val dynamicColorScheme: ColorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = useDarkTheme,
        style = style,
        contrastLevel = contrastLevel,
        isExtendedFidelity = isExtendedFidelity,
    )

    val colorScheme =
        if (!animate) dynamicColorScheme
        else {
            dynamicColorScheme.copy(
                primary = dynamicColorScheme.primary.animate(animationSpec),
                primaryContainer = dynamicColorScheme.primaryContainer.animate(animationSpec),
                secondary = dynamicColorScheme.secondary.animate(animationSpec),
                secondaryContainer = dynamicColorScheme.secondaryContainer.animate(animationSpec),
                tertiary = dynamicColorScheme.tertiary.animate(animationSpec),
                tertiaryContainer = dynamicColorScheme.tertiaryContainer.animate(animationSpec),
                background = dynamicColorScheme.background.animate(animationSpec),
                surface = dynamicColorScheme.surface.animate(animationSpec),
                error = dynamicColorScheme.error.animate(animationSpec),
                onPrimary = dynamicColorScheme.onPrimary.animate(animationSpec),
                onSecondary = dynamicColorScheme.onSecondary.animate(animationSpec),
                onTertiary = dynamicColorScheme.onTertiary.animate(animationSpec),
                onBackground = dynamicColorScheme.onBackground.animate(animationSpec),
                onSurface = dynamicColorScheme.onSurface.animate(animationSpec),
                onError = dynamicColorScheme.onError.animate(animationSpec),
            )
        }

    CompositionLocalProvider(LocalDynamicMaterialThemeSeed provides seedColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}

/**
 * A Material Theme that adapts to the given seed color and animates the color scheme.
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see dynamicColorScheme
 * @see PaletteStyle
 * @param[seedColor] The seed color to use for generating the color scheme.
 * @param[useDarkTheme] Whether to use a dark theme or not.
 * @param[style] The style of the color scheme.
 * @param[contrastLevel] The contrast level of the color scheme.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set. See [MaterialDynamicColors].
 * @param[content] The Composable content of the theme.
 */
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Use DynamicMaterialTheme with animate = true instead."
)
@Composable
public fun AnimatedDynamicMaterialTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    isExtendedFidelity: Boolean = false,
    content: @Composable () -> Unit,
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        useDarkTheme = useDarkTheme,
        style = style,
        contrastLevel = contrastLevel,
        shapes = shapes,
        typography = typography,
        isExtendedFidelity = isExtendedFidelity,
        animate = true,
        animationSpec = animationSpec,
        content = content,
    )
}

@Composable
private fun Color.animate(animationSpec: AnimationSpec<Color>): Color {
    return animateColorAsState(this, animationSpec).value
}