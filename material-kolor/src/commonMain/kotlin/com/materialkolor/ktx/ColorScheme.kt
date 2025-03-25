package com.materialkolor.ktx

import androidx.compose.animation.core.AnimationSpec
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
    animationSpec: AnimationSpec<Color> = defaultColorSpring,
    label: String = "ColorSchemeAnimation",
): ColorScheme {
    val primary by colorScheme.primary.animate(
        animationSpec = animationSpec,
        label = "color_primary",
    )
    val primaryContainer by colorScheme.primaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_primaryContainer",
    )
    val secondary by colorScheme.secondary.animate(
        animationSpec = animationSpec,
        label = "color_secondary",
    )
    val secondaryContainer by colorScheme.secondaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_secondaryContainer",
    )
    val tertiary by colorScheme.tertiary.animate(
        animationSpec = animationSpec,
        label = "color_tertiary",
    )
    val tertiaryContainer by colorScheme.tertiaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_tertiaryContainer",
    )
    val background by colorScheme.background.animate(
        animationSpec = animationSpec,
        label = "color_background",
    )
    val surface by colorScheme.surface.animate(
        animationSpec = animationSpec,
        label = "color_surface",
    )
    val surfaceTint by colorScheme.surfaceTint.animate(
        animationSpec = animationSpec,
        label = "color_surfaceTint",
    )
    val surfaceBright by colorScheme.surfaceBright.animate(
        animationSpec = animationSpec,
        label = "color_surfaceBright",
    )
    val surfaceDim by colorScheme.surfaceDim.animate(
        animationSpec = animationSpec,
        label = "color_surfaceDim",
    )
    val surfaceContainer by colorScheme.surfaceContainer.animate(
        animationSpec = animationSpec,
        label = "color_surfaceContainer",
    )
    val surfaceContainerHigh by colorScheme.surfaceContainerHigh.animate(
        animationSpec = animationSpec,
        label = "color_surfaceContainerHigh",
    )
    val surfaceContainerHighest by colorScheme.surfaceContainerHighest.animate(
        animationSpec = animationSpec,
        label = "color_surfaceContainerHighest",
    )
    val surfaceContainerLow by colorScheme.surfaceContainerLow.animate(
        animationSpec = animationSpec,
        label = "color_surfaceContainerLow",
    )
    val surfaceContainerLowest by colorScheme.surfaceContainerLowest.animate(
        animationSpec = animationSpec,
        label = "color_surfaceContainerLowest",
    )
    val surfaceVariant by colorScheme.surfaceVariant.animate(
        animationSpec = animationSpec,
        label = "color_surfaceVariant",
    )
    val error by colorScheme.error.animate(
        animationSpec = animationSpec,
        label = "color_error",
    )
    val errorContainer by colorScheme.errorContainer.animate(
        animationSpec = animationSpec,
        label = "color_errorContainer",
    )
    val onPrimary by colorScheme.onPrimary.animate(
        animationSpec = animationSpec,
        label = "color_onPrimary",
    )
    val onPrimaryContainer by colorScheme.onPrimaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_onPrimaryContainer",
    )
    val onSecondary by colorScheme.onSecondary.animate(
        animationSpec = animationSpec,
        label = "color_onSecondary",
    )
    val onSecondaryContainer by colorScheme.onSecondaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_onSecondaryContainer",
    )
    val onTertiary by colorScheme.onTertiary.animate(
        animationSpec = animationSpec,
        label = "color_onTertiary",
    )
    val onTertiaryContainer by colorScheme.onTertiaryContainer.animate(
        animationSpec = animationSpec,
        label = "color_onTertiaryContainer",
    )
    val onBackground by colorScheme.onBackground.animate(
        animationSpec = animationSpec,
        label = "color_onBackground",
    )
    val onSurface by colorScheme.onSurface.animate(
        animationSpec = animationSpec,
        label = "color_onSurface",
    )
    val onSurfaceVariant by colorScheme.onSurfaceVariant.animate(
        animationSpec = animationSpec,
        label = "color_onSurfaceVariant",
    )
    val onError by colorScheme.onError.animate(
        animationSpec = animationSpec,
        label = "color_onError",
    )
    val onErrorContainer by colorScheme.onErrorContainer.animate(
        animationSpec = animationSpec,
        label = "color_onErrorContainer",
    )
    val inversePrimary by colorScheme.inversePrimary.animate(
        animationSpec = animationSpec,
        label = "color_inversePrimary",
    )
    val inverseSurface by colorScheme.inverseSurface.animate(
        animationSpec = animationSpec,
        label = "color_inverseSurface",
    )
    val inverseOnSurface by colorScheme.inverseOnSurface.animate(
        animationSpec = animationSpec,
        label = "color_inverseOnSurface",
    )
    val outline by colorScheme.outline.animate(
        animationSpec = animationSpec,
        label = "color_outline",
    )
    val outlineVariant by colorScheme.outlineVariant.animate(
        animationSpec = animationSpec,
        label = "color_outlineVariant",
    )
    val scrim by colorScheme.scrim.animate(
        animationSpec = animationSpec,
        label = "color_scrim",
    )
    return colorScheme.copy(
        primary = primary,
        onPrimary = primaryContainer,
        primaryContainer = secondary,
        onPrimaryContainer = secondaryContainer,
        inversePrimary = tertiary,
        secondary = tertiaryContainer,
        onSecondary = background,
        secondaryContainer = surface,
        onSecondaryContainer = surfaceTint,
        tertiary = surfaceBright,
        onTertiary = surfaceDim,
        tertiaryContainer = surfaceContainer,
        onTertiaryContainer = surfaceContainerHigh,
        background = surfaceContainerHighest,
        onBackground = surfaceContainerLow,
        surface = surfaceContainerLowest,
        onSurface = surfaceVariant,
        surfaceVariant = error,
        onSurfaceVariant = errorContainer,
        surfaceTint = onPrimary,
        inverseSurface = onPrimaryContainer,
        inverseOnSurface = onSecondary,
        error = onSecondaryContainer,
        onError = onTertiary,
        errorContainer = onTertiaryContainer,
        onErrorContainer = onBackground,
        outline = onSurface,
        outlineVariant = onSurfaceVariant,
        scrim = onError,
        surfaceBright = onErrorContainer,
        surfaceDim = inversePrimary,
        surfaceContainer = inverseSurface,
        surfaceContainerHigh = inverseOnSurface,
        surfaceContainerHighest = outline,
        surfaceContainerLow = outlineVariant,
        surfaceContainerLowest = scrim,
    )
}