package com.materialkolor.ktx

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
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
    animationSpec: FiniteAnimationSpec<Color> = defaultColorSpring,
    label: String = "ColorSchemeAnimation",
): ColorScheme = animateColorScheme(colorScheme, { animationSpec }, label)

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

    val primary by transition.animateColor(
        label = "color_primary",
        targetValueByState = { it.primary },
        transitionSpec = animationSpec,
    )
    val primaryContainer by transition.animateColor(
        label = "color_primaryContainer",
        targetValueByState = { it.primaryContainer },
        transitionSpec = animationSpec,
    )
    val secondary by transition.animateColor(
        label = "color_secondary",
        targetValueByState = { it.secondary },
        transitionSpec = animationSpec,
    )
    val secondaryContainer by transition.animateColor(
        label = "color_secondaryContainer",
        targetValueByState = { it.secondaryContainer },
        transitionSpec = animationSpec,
    )
    val tertiary by transition.animateColor(
        label = "color_tertiary",
        targetValueByState = { it.tertiary },
        transitionSpec = animationSpec,
    )
    val tertiaryContainer by transition.animateColor(
        label = "color_tertiaryContainer",
        targetValueByState = { it.tertiaryContainer },
        transitionSpec = animationSpec,
    )
    val background by transition.animateColor(
        label = "color_background",
        targetValueByState = { it.background },
        transitionSpec = animationSpec,
    )
    val surface by transition.animateColor(
        label = "color_surface",
        targetValueByState = { it.surface },
        transitionSpec = animationSpec,
    )
    val surfaceTint by transition.animateColor(
        label = "color_surfaceTint",
        targetValueByState = { it.surfaceTint },
        transitionSpec = animationSpec,
    )
    val surfaceBright by transition.animateColor(
        label = "color_surfaceBright",
        targetValueByState = { it.surfaceBright },
        transitionSpec = animationSpec,
    )
    val surfaceDim by transition.animateColor(
        label = "color_surfaceDim",
        targetValueByState = { it.surfaceDim },
        transitionSpec = animationSpec,
    )
    val surfaceContainer by transition.animateColor(
        label = "color_surfaceContainer",
        targetValueByState = { it.surfaceContainer },
        transitionSpec = animationSpec,
    )
    val surfaceContainerHigh by transition.animateColor(
        label = "color_surfaceContainerHigh",
        targetValueByState = { it.surfaceContainerHigh },
        transitionSpec = animationSpec,
    )
    val surfaceContainerHighest by transition.animateColor(
        label = "color_surfaceContainerHighest",
        targetValueByState = { it.surfaceContainerHighest },
        transitionSpec = animationSpec,
    )
    val surfaceContainerLow by transition.animateColor(
        label = "color_surfaceContainerLow",
        targetValueByState = { it.surfaceContainerLow },
        transitionSpec = animationSpec,
    )
    val surfaceContainerLowest by transition.animateColor(
        label = "color_surfaceContainerLowest",
        targetValueByState = { it.surfaceContainerLowest },
        transitionSpec = animationSpec,
    )
    val surfaceVariant by transition.animateColor(
        label = "color_surfaceVariant",
        targetValueByState = { it.surfaceVariant },
        transitionSpec = animationSpec,
    )
    val error by transition.animateColor(
        label = "color_error",
        targetValueByState = { it.error },
        transitionSpec = animationSpec,
    )
    val errorContainer by transition.animateColor(
        label = "color_errorContainer",
        targetValueByState = { it.errorContainer },
        transitionSpec = animationSpec,
    )
    val onPrimary by transition.animateColor(
        label = "color_onPrimary",
        targetValueByState = { it.onPrimary },
        transitionSpec = animationSpec,
    )
    val onPrimaryContainer by transition.animateColor(
        label = "color_onPrimaryContainer",
        targetValueByState = { it.onPrimaryContainer },
        transitionSpec = animationSpec,
    )
    val onSecondary by transition.animateColor(
        label = "color_onSecondary",
        targetValueByState = { it.onSecondary },
        transitionSpec = animationSpec,
    )
    val onSecondaryContainer by transition.animateColor(
        label = "color_onSecondaryContainer",
        targetValueByState = { it.onSecondaryContainer },
        transitionSpec = animationSpec,
    )
    val onTertiary by transition.animateColor(
        label = "color_onTertiary",
        targetValueByState = { it.onTertiary },
        transitionSpec = animationSpec,
    )
    val onTertiaryContainer by transition.animateColor(
        label = "color_onTertiaryContainer",
        targetValueByState = { it.onTertiaryContainer },
        transitionSpec = animationSpec,
    )
    val onBackground by transition.animateColor(
        label = "color_onBackground",
        targetValueByState = { it.onBackground },
        transitionSpec = animationSpec,
    )
    val onSurface by transition.animateColor(
        label = "color_onSurface",
        targetValueByState = { it.onSurface },
        transitionSpec = animationSpec,
    )
    val onSurfaceVariant by transition.animateColor(
        label = "color_onSurfaceVariant",
        targetValueByState = { it.onSurfaceVariant },
        transitionSpec = animationSpec,
    )
    val onError by transition.animateColor(
        label = "color_onError",
        targetValueByState = { it.onError },
        transitionSpec = animationSpec,
    )
    val onErrorContainer by transition.animateColor(
        label = "color_onErrorContainer",
        targetValueByState = { it.onErrorContainer },
        transitionSpec = animationSpec,
    )
    val inversePrimary by transition.animateColor(
        label = "color_inversePrimary",
        targetValueByState = { it.inversePrimary },
        transitionSpec = animationSpec,
    )
    val inverseSurface by transition.animateColor(
        label = "color_inverseSurface",
        targetValueByState = { it.inverseSurface },
        transitionSpec = animationSpec,
    )
    val inverseOnSurface by transition.animateColor(
        label = "color_inverseOnSurface",
        targetValueByState = { it.inverseOnSurface },
        transitionSpec = animationSpec,
    )
    val outline by transition.animateColor(
        label = "color_outline",
        targetValueByState = { it.outline },
        transitionSpec = animationSpec,
    )
    val outlineVariant by transition.animateColor(
        label = "color_outlineVariant",
        targetValueByState = { it.outlineVariant },
        transitionSpec = animationSpec,
    )
    val scrim by transition.animateColor(
        label = "color_scrim",
        targetValueByState = { it.scrim },
        transitionSpec = animationSpec,
    )
    return colorScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
    )
}