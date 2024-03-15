package com.materialkolor.ktx

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Animate all colors in this [ColorScheme] using the provided [animationSpec].
 */
@Composable
public fun ColorScheme.animate(
    animationSpec: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessLow),
): ColorScheme = this.copy(
    primary = this.primary.animate(animationSpec),
    primaryContainer = this.primaryContainer.animate(animationSpec),
    secondary = this.secondary.animate(animationSpec),
    secondaryContainer = this.secondaryContainer.animate(animationSpec),
    tertiary = this.tertiary.animate(animationSpec),
    tertiaryContainer = this.tertiaryContainer.animate(animationSpec),
    background = this.background.animate(animationSpec),
    surface = this.surface.animate(animationSpec),
    surfaceTint = this.surfaceTint.animate(animationSpec),
    surfaceBright = this.surfaceBright.animate(animationSpec),
    surfaceDim = this.surfaceDim.animate(animationSpec),
    surfaceContainer = this.surfaceContainer.animate(animationSpec),
    surfaceContainerHigh = this.surfaceContainerHigh.animate(animationSpec),
    surfaceContainerHighest = this.surfaceContainerHighest.animate(animationSpec),
    surfaceContainerLow = this.surfaceContainerLow.animate(animationSpec),
    surfaceContainerLowest = this.surfaceContainerLowest.animate(animationSpec),
    surfaceVariant = this.surfaceVariant.animate(animationSpec),
    error = this.error.animate(animationSpec),
    errorContainer = this.errorContainer.animate(animationSpec),
    onPrimary = this.onPrimary.animate(animationSpec),
    onPrimaryContainer = this.onPrimaryContainer.animate(animationSpec),
    onSecondary = this.onSecondary.animate(animationSpec),
    onSecondaryContainer = this.onSecondaryContainer.animate(animationSpec),
    onTertiary = this.onTertiary.animate(animationSpec),
    onTertiaryContainer = this.onTertiaryContainer.animate(animationSpec),
    onBackground = this.onBackground.animate(animationSpec),
    onSurface = this.onSurface.animate(animationSpec),
    onSurfaceVariant = this.onSurfaceVariant.animate(animationSpec),
    onError = this.onError.animate(animationSpec),
    onErrorContainer = this.onErrorContainer.animate(animationSpec),
    inversePrimary = this.inversePrimary.animate(animationSpec),
    inverseSurface = this.inverseSurface.animate(animationSpec),
    inverseOnSurface = this.inverseOnSurface.animate(animationSpec),
    outline = this.outline.animate(animationSpec),
    outlineVariant = this.outlineVariant.animate(animationSpec),
    scrim = this.scrim.animate(animationSpec),
)

@Composable
private fun Color.animate(animationSpec: AnimationSpec<Color>): Color {
    return animateColorAsState(this, animationSpec).value
}