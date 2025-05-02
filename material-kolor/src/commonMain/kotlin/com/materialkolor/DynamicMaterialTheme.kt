package com.materialkolor

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.ktx.defaultColorSpring
import com.materialkolor.scheme.DynamicScheme

/**
 * A Material Theme that adapts to the given seed color and the provided custom colors.
 *
 * You can access the current seed color via [LocalDynamicMaterialThemeSeed].
 *
 * @see dynamicColorScheme
 * @see PaletteStyle
 * @param[seedColor] The seed color to use for generating the color scheme.
 * @param[useDarkTheme] Whether to use a dark theme or not.
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[primary] The custom primary color of the color scheme.
 * @param[secondary] The custom secondary color of the color scheme.
 * @param[tertiary] The custom tertiary color of the color scheme.
 * @param[neutral] The custom neutral color of the color scheme.
 * @param[neutralVariant] The custom neutral variant color of the color scheme.
 * @param[error] The custom error color of the color scheme.
 * @param[style] The style of the color scheme.
 * @param[contrastLevel] The contrast level of the color scheme.
 * @param[specVersion] The spec version of the color scheme.
 * @param[platform] The platform of the color scheme.
 * @param[shapes] The shapes of the theme.
 * @param[typography] The typography of the theme.
 * @param[animate] Whether to animate the color scheme or not.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    seedColor: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    primary: Color? = null,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: DynamicScheme.Platform = DynamicScheme.Platform.Default,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    animate: Boolean = false,
    animationSpec: FiniteAnimationSpec<Color> = defaultColorSpring,
    content: @Composable () -> Unit,
) {
    val state = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = useDarkTheme,
        isAmoled = isAmoled,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
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
 * @param[isAmoled] Whether the dark scheme is used with Amoled screen (Pure dark).
 * @param[secondary] The custom secondary color of the color scheme.
 * @param[tertiary] The custom tertiary color of the color scheme.
 * @param[neutral] The custom neutral color of the color scheme.
 * @param[neutralVariant] The custom neutral variant color of the color scheme.
 * @param[error] The custom error color of the color scheme.
 * @param[style] The style of the color scheme.
 * @param[contrastLevel] The contrast level of the color scheme.
 * @param[specVersion] The spec version of the color scheme.
 * @param[platform] The platform of the color scheme.
 * @param[shapes] The shapes of the theme.
 * @param[typography] The typography of the theme.
 * @param[animate] Whether to animate the color scheme or not.
 * @param[animationSpec] The animation spec to use for animating the color scheme.
 * @param[content] The Composable content of the theme.
 */
@Composable
public fun DynamicMaterialTheme(
    primary: Color,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    secondary: Color? = null,
    tertiary: Color? = null,
    neutral: Color? = null,
    neutralVariant: Color? = null,
    error: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = Contrast.Default.value,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: DynamicScheme.Platform = DynamicScheme.Platform.Default,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    animate: Boolean = false,
    animationSpec: FiniteAnimationSpec<Color> = defaultColorSpring,
    content: @Composable () -> Unit,
) {
    val state = rememberDynamicMaterialThemeState(
        primary = primary,
        isDark = useDarkTheme,
        isAmoled = isAmoled,
        secondary = secondary,
        tertiary = tertiary,
        neutral = neutral,
        neutralVariant = neutralVariant,
        error = error,
        style = style,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
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
    animationSpec: FiniteAnimationSpec<Color> = defaultColorSpring,
    content: @Composable () -> Unit,
) {
    val colorScheme = state.colorScheme
    val scheme =
        if (!animate) {
            colorScheme
        } else {
            animateColorScheme(colorScheme = colorScheme, animationSpec = animationSpec)
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
