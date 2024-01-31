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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

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
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme by remember(seedColor, useDarkTheme, style, contrastLevel) {
        derivedStateOf {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = useDarkTheme,
                style = style,
                contrastLevel = contrastLevel,
            )
        }
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
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun AnimatedDynamicMaterialTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme by remember(seedColor, useDarkTheme, style, contrastLevel) {
        derivedStateOf {
            dynamicColorScheme(
                seedColor = seedColor,
                isDark = useDarkTheme,
                style = style,
                contrastLevel = contrastLevel,
            )
        }
    }

    val animatedColorScheme = colors.copy(
        primary = animateColorAsState(colors.primary, animationSpec).value,
        primaryContainer = animateColorAsState(colors.primaryContainer, animationSpec).value,
        secondary = animateColorAsState(colors.secondary, animationSpec).value,
        secondaryContainer = animateColorAsState(colors.secondaryContainer, animationSpec).value,
        tertiary = animateColorAsState(colors.tertiary, animationSpec).value,
        tertiaryContainer = animateColorAsState(colors.tertiaryContainer, animationSpec).value,
        background = animateColorAsState(colors.background, animationSpec).value,
        surface = animateColorAsState(colors.surface, animationSpec).value,
        error = animateColorAsState(colors.error, animationSpec).value,
        onPrimary = animateColorAsState(colors.onPrimary, animationSpec).value,
        onSecondary = animateColorAsState(colors.onSecondary, animationSpec).value,
        onTertiary = animateColorAsState(colors.onTertiary, animationSpec).value,
        onBackground = animateColorAsState(colors.onBackground, animationSpec).value,
        onSurface = animateColorAsState(colors.onSurface, animationSpec).value,
        onError = animateColorAsState(colors.onError, animationSpec).value,
    )

    CompositionLocalProvider(LocalDynamicMaterialThemeSeed provides seedColor) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            shapes = shapes,
            typography = typography,
            content = content,
        )
    }
}