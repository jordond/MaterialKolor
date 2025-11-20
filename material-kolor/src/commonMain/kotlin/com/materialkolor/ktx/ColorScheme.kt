package com.materialkolor.ktx

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * Creates an animated version of a [ColorScheme] where all colors smoothly animate when changed.
 *
 * This composable function creates a new [ColorScheme] where each color property will animate from
 * its previous value to its new value whenever the input [colorScheme] changes.
 *
 * @param colorScheme The source color scheme to animate from/to.
 * @param animationSpec The animation specification to control how each color animates.
 * @param label A debugging label used to identify this animation.
 * @return A new [ColorScheme] instance with animated color properties.
 */
@Composable
public fun animateColorScheme(
    colorScheme: ColorScheme,
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color> =
        { defaultColorSpring },
    label: String = "ColorSchemeAnimation",
): ColorScheme {
    val transition = updateTransition(targetState = colorScheme, label = label)

    val primary = transition.animatePrimaryColors(animationSpec)
    val secondary = transition.animateSecondaryColors(animationSpec)
    val tertiary = transition.animateTertiaryColors(animationSpec)
    val surface = transition.animateSurfaceColors(animationSpec)
    val backgroundError = transition.animateBackgroundAndErrorColors(animationSpec)
    val utility = transition.animateUtilityColors(animationSpec)
    val fixed = transition.animateFixedColors(animationSpec)

    return colorScheme.copy(
        primary = primary[0],
        onPrimary = primary[1],
        primaryContainer = primary[2],
        onPrimaryContainer = primary[3],
        inversePrimary = primary[4],
        secondary = secondary[0],
        onSecondary = secondary[1],
        secondaryContainer = secondary[2],
        onSecondaryContainer = secondary[3],
        tertiary = tertiary[0],
        onTertiary = tertiary[1],
        tertiaryContainer = tertiary[2],
        onTertiaryContainer = tertiary[3],
        background = backgroundError[0],
        onBackground = backgroundError[1],
        surface = surface[0],
        onSurface = surface[1],
        surfaceVariant = surface[2],
        onSurfaceVariant = surface[3],
        surfaceTint = surface[4],
        inverseSurface = utility[0],
        inverseOnSurface = utility[1],
        error = backgroundError[2],
        onError = backgroundError[3],
        errorContainer = backgroundError[4],
        onErrorContainer = backgroundError[5],
        outline = utility[2],
        outlineVariant = utility[3],
        scrim = utility[4],
        surfaceBright = surface[5],
        surfaceDim = surface[6],
        surfaceContainer = surface[7],
        surfaceContainerHigh = surface[8],
        surfaceContainerHighest = surface[9],
        surfaceContainerLow = surface[10],
        surfaceContainerLowest = surface[11],
        primaryFixed = fixed[0],
        primaryFixedDim = fixed[1],
        onPrimaryFixed = fixed[2],
        onPrimaryFixedVariant = fixed[3],
        secondaryFixed = fixed[4],
        secondaryFixedDim = fixed[5],
        onSecondaryFixed = fixed[6],
        onSecondaryFixedVariant = fixed[7],
        tertiaryFixed = fixed[8],
        tertiaryFixedDim = fixed[9],
        onTertiaryFixed = fixed[10],
        onTertiaryFixedVariant = fixed[11],
    )
}

@Composable
private fun Transition<ColorScheme>.animatePrimaryColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val primary by animateColor(
        label = "color_primary",
        targetValueByState = { it.primary },
        transitionSpec = animationSpec,
    )
    val onPrimary by animateColor(
        label = "color_onPrimary",
        targetValueByState = { it.onPrimary },
        transitionSpec = animationSpec,
    )
    val primaryContainer by animateColor(
        label = "color_primaryContainer",
        targetValueByState = { it.primaryContainer },
        transitionSpec = animationSpec,
    )
    val onPrimaryContainer by animateColor(
        label = "color_onPrimaryContainer",
        targetValueByState = { it.onPrimaryContainer },
        transitionSpec = animationSpec,
    )
    val inversePrimary by animateColor(
        label = "color_inversePrimary",
        targetValueByState = { it.inversePrimary },
        transitionSpec = animationSpec,
    )
    return listOf(primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary)
}

@Composable
private fun Transition<ColorScheme>.animateSecondaryColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val secondary by animateColor(
        label = "color_secondary",
        targetValueByState = { it.secondary },
        transitionSpec = animationSpec,
    )
    val onSecondary by animateColor(
        label = "color_onSecondary",
        targetValueByState = { it.onSecondary },
        transitionSpec = animationSpec,
    )
    val secondaryContainer by animateColor(
        label = "color_secondaryContainer",
        targetValueByState = { it.secondaryContainer },
        transitionSpec = animationSpec,
    )
    val onSecondaryContainer by animateColor(
        label = "color_onSecondaryContainer",
        targetValueByState = { it.onSecondaryContainer },
        transitionSpec = animationSpec,
    )
    return listOf(secondary, onSecondary, secondaryContainer, onSecondaryContainer)
}

@Composable
private fun Transition<ColorScheme>.animateTertiaryColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val tertiary by animateColor(
        label = "color_tertiary",
        targetValueByState = { it.tertiary },
        transitionSpec = animationSpec,
    )
    val onTertiary by animateColor(
        label = "color_onTertiary",
        targetValueByState = { it.onTertiary },
        transitionSpec = animationSpec,
    )
    val tertiaryContainer by animateColor(
        label = "color_tertiaryContainer",
        targetValueByState = { it.tertiaryContainer },
        transitionSpec = animationSpec,
    )
    val onTertiaryContainer by animateColor(
        label = "color_onTertiaryContainer",
        targetValueByState = { it.onTertiaryContainer },
        transitionSpec = animationSpec,
    )
    return listOf(tertiary, onTertiary, tertiaryContainer, onTertiaryContainer)
}

@Composable
private fun Transition<ColorScheme>.animateSurfaceColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val surface by animateColor(
        label = "color_surface",
        targetValueByState = { it.surface },
        transitionSpec = animationSpec,
    )
    val onSurface by animateColor(
        label = "color_onSurface",
        targetValueByState = { it.onSurface },
        transitionSpec = animationSpec,
    )
    val surfaceVariant by animateColor(
        label = "color_surfaceVariant",
        targetValueByState = { it.surfaceVariant },
        transitionSpec = animationSpec,
    )
    val onSurfaceVariant by animateColor(
        label = "color_onSurfaceVariant",
        targetValueByState = { it.onSurfaceVariant },
        transitionSpec = animationSpec,
    )
    val surfaceTint by animateColor(
        label = "color_surfaceTint",
        targetValueByState = { it.surfaceTint },
        transitionSpec = animationSpec,
    )
    val surfaceBright by animateColor(
        label = "color_surfaceBright",
        targetValueByState = { it.surfaceBright },
        transitionSpec = animationSpec,
    )
    val surfaceDim by animateColor(
        label = "color_surfaceDim",
        targetValueByState = { it.surfaceDim },
        transitionSpec = animationSpec,
    )
    val surfaceContainer by animateColor(
        label = "color_surfaceContainer",
        targetValueByState = { it.surfaceContainer },
        transitionSpec = animationSpec,
    )
    val surfaceContainerHigh by animateColor(
        label = "color_surfaceContainerHigh",
        targetValueByState = { it.surfaceContainerHigh },
        transitionSpec = animationSpec,
    )
    val surfaceContainerHighest by animateColor(
        label = "color_surfaceContainerHighest",
        targetValueByState = { it.surfaceContainerHighest },
        transitionSpec = animationSpec,
    )
    val surfaceContainerLow by animateColor(
        label = "color_surfaceContainerLow",
        targetValueByState = { it.surfaceContainerLow },
        transitionSpec = animationSpec,
    )
    val surfaceContainerLowest by animateColor(
        label = "color_surfaceContainerLowest",
        targetValueByState = { it.surfaceContainerLowest },
        transitionSpec = animationSpec,
    )
    return listOf(
        surface,
        onSurface,
        surfaceVariant,
        onSurfaceVariant,
        surfaceTint,
        surfaceBright,
        surfaceDim,
        surfaceContainer,
        surfaceContainerHigh,
        surfaceContainerHighest,
        surfaceContainerLow,
        surfaceContainerLowest,
    )
}

@Composable
private fun Transition<ColorScheme>.animateBackgroundAndErrorColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val background by animateColor(
        label = "color_background",
        targetValueByState = { it.background },
        transitionSpec = animationSpec,
    )
    val onBackground by animateColor(
        label = "color_onBackground",
        targetValueByState = { it.onBackground },
        transitionSpec = animationSpec,
    )
    val error by animateColor(
        label = "color_error",
        targetValueByState = { it.error },
        transitionSpec = animationSpec,
    )
    val onError by animateColor(
        label = "color_onError",
        targetValueByState = { it.onError },
        transitionSpec = animationSpec,
    )
    val errorContainer by animateColor(
        label = "color_errorContainer",
        targetValueByState = { it.errorContainer },
        transitionSpec = animationSpec,
    )
    val onErrorContainer by animateColor(
        label = "color_onErrorContainer",
        targetValueByState = { it.onErrorContainer },
        transitionSpec = animationSpec,
    )
    return listOf(background, onBackground, error, onError, errorContainer, onErrorContainer)
}

@Composable
private fun Transition<ColorScheme>.animateUtilityColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val inverseSurface by animateColor(
        label = "color_inverseSurface",
        targetValueByState = { it.inverseSurface },
        transitionSpec = animationSpec,
    )
    val inverseOnSurface by animateColor(
        label = "color_inverseOnSurface",
        targetValueByState = { it.inverseOnSurface },
        transitionSpec = animationSpec,
    )
    val outline by animateColor(
        label = "color_outline",
        targetValueByState = { it.outline },
        transitionSpec = animationSpec,
    )
    val outlineVariant by animateColor(
        label = "color_outlineVariant",
        targetValueByState = { it.outlineVariant },
        transitionSpec = animationSpec,
    )
    val scrim by animateColor(
        label = "color_scrim",
        targetValueByState = { it.scrim },
        transitionSpec = animationSpec,
    )
    return listOf(inverseSurface, inverseOnSurface, outline, outlineVariant, scrim)
}

@Composable
private fun Transition<ColorScheme>.animateFixedColors(
    animationSpec: @Composable Transition.Segment<ColorScheme>.() -> FiniteAnimationSpec<Color>,
): List<Color> {
    val primaryFixed by animateColor(
        label = "color_primaryFixed",
        targetValueByState = { it.primaryFixed },
        transitionSpec = animationSpec,
    )
    val primaryFixedDim by animateColor(
        label = "color_primaryFixedDim",
        targetValueByState = { it.primaryFixedDim },
        transitionSpec = animationSpec,
    )
    val onPrimaryFixed by animateColor(
        label = "color_onPrimaryFixed",
        targetValueByState = { it.onPrimaryFixed },
        transitionSpec = animationSpec,
    )
    val onPrimaryFixedVariant by animateColor(
        label = "color_onPrimaryFixedVariant",
        targetValueByState = { it.onPrimaryFixedVariant },
        transitionSpec = animationSpec,
    )
    val secondaryFixed by animateColor(
        label = "color_secondaryFixed",
        targetValueByState = { it.secondaryFixed },
        transitionSpec = animationSpec,
    )
    val secondaryFixedDim by animateColor(
        label = "color_secondaryFixedDim",
        targetValueByState = { it.secondaryFixedDim },
        transitionSpec = animationSpec,
    )
    val onSecondaryFixed by animateColor(
        label = "color_onSecondaryFixed",
        targetValueByState = { it.onSecondaryFixed },
        transitionSpec = animationSpec,
    )
    val onSecondaryFixedVariant by animateColor(
        label = "color_onSecondaryFixedVariant",
        targetValueByState = { it.onSecondaryFixedVariant },
        transitionSpec = animationSpec,
    )
    val tertiaryFixed by animateColor(
        label = "color_tertiaryFixed",
        targetValueByState = { it.tertiaryFixed },
        transitionSpec = animationSpec,
    )
    val tertiaryFixedDim by animateColor(
        label = "color_tertiaryFixedDim",
        targetValueByState = { it.tertiaryFixedDim },
        transitionSpec = animationSpec,
    )
    val onTertiaryFixed by animateColor(
        label = "color_onTertiaryFixed",
        targetValueByState = { it.onTertiaryFixed },
        transitionSpec = animationSpec,
    )
    val onTertiaryFixedVariant by animateColor(
        label = "color_onTertiaryFixedVariant",
        targetValueByState = { it.onTertiaryFixedVariant },
        transitionSpec = animationSpec,
    )
    return listOf(
        primaryFixed,
        primaryFixedDim,
        onPrimaryFixed,
        onPrimaryFixedVariant,
        secondaryFixed,
        secondaryFixedDim,
        onSecondaryFixed,
        onSecondaryFixedVariant,
        tertiaryFixed,
        tertiaryFixedDim,
        onTertiaryFixed,
        onTertiaryFixedVariant,
    )
}
