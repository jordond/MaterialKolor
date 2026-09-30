package com.materialkolor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Every color field of a [ColorScheme] by name, so a test can compare whole schemes and name the
 * field that differs.
 */
internal fun ColorScheme.fields(): Map<String, Color> =
    mapOf(
        "background" to background,
        "error" to error,
        "errorContainer" to errorContainer,
        "inverseOnSurface" to inverseOnSurface,
        "inversePrimary" to inversePrimary,
        "inverseSurface" to inverseSurface,
        "onBackground" to onBackground,
        "onError" to onError,
        "onErrorContainer" to onErrorContainer,
        "onPrimary" to onPrimary,
        "onPrimaryContainer" to onPrimaryContainer,
        "onPrimaryFixed" to onPrimaryFixed,
        "onPrimaryFixedVariant" to onPrimaryFixedVariant,
        "onSecondary" to onSecondary,
        "onSecondaryContainer" to onSecondaryContainer,
        "onSecondaryFixed" to onSecondaryFixed,
        "onSecondaryFixedVariant" to onSecondaryFixedVariant,
        "onSurface" to onSurface,
        "onSurfaceVariant" to onSurfaceVariant,
        "onTertiary" to onTertiary,
        "onTertiaryContainer" to onTertiaryContainer,
        "onTertiaryFixed" to onTertiaryFixed,
        "onTertiaryFixedVariant" to onTertiaryFixedVariant,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "primary" to primary,
        "primaryContainer" to primaryContainer,
        "primaryFixed" to primaryFixed,
        "primaryFixedDim" to primaryFixedDim,
        "scrim" to scrim,
        "secondary" to secondary,
        "secondaryContainer" to secondaryContainer,
        "secondaryFixed" to secondaryFixed,
        "secondaryFixedDim" to secondaryFixedDim,
        "surface" to surface,
        "surfaceBright" to surfaceBright,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceDim" to surfaceDim,
        "surfaceTint" to surfaceTint,
        "surfaceVariant" to surfaceVariant,
        "tertiary" to tertiary,
        "tertiaryContainer" to tertiaryContainer,
        "tertiaryFixed" to tertiaryFixed,
        "tertiaryFixedDim" to tertiaryFixedDim,
    )
