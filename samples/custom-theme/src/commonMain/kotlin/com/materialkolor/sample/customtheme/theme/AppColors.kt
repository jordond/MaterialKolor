package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * There are seven accent families instead of three, the primary accent carries two interaction tones, surfaces come
 * in three steps, and the drink colors are decorative and belong to no family at all.
 *
 * @see appColors for how the slots are derived.
 */
@Immutable
public data class AppColors(
    public val isLight: Boolean,
    public val primary: Color,
    public val onPrimary: Color,
    public val primaryContainer: Color,
    public val onPrimaryContainer: Color,
    public val primaryPressed: Color,
    public val primaryRaised: Color,
    public val secondary: Color,
    public val onSecondary: Color,
    public val secondaryContainer: Color,
    public val onSecondaryContainer: Color,
    public val tertiary: Color,
    public val onTertiary: Color,
    public val tertiaryContainer: Color,
    public val onTertiaryContainer: Color,
    public val error: Color,
    public val onError: Color,
    public val errorContainer: Color,
    public val onErrorContainer: Color,
    public val love: Color,
    public val onLove: Color,
    public val loveContainer: Color,
    public val onLoveContainer: Color,
    public val cold: Color,
    public val onCold: Color,
    public val coldContainer: Color,
    public val onColdContainer: Color,
    public val warm: Color,
    public val onWarm: Color,
    public val warmContainer: Color,
    public val onWarmContainer: Color,
    public val surface: Color,
    public val surfaceRaised: Color,
    public val surfaceSunken: Color,
    public val surfaceInverse: Color,
    public val onSurface: Color,
    public val onSurfaceInverse: Color,
    public val textStrong: Color,
    public val textMuted: Color,
    public val borderFaint: Color,
    public val borderSoft: Color,
    public val borderStrong: Color,
    public val drinkCoffee: Color,
    public val drinkMatcha: Color,
    public val drinkIced: Color,
    public val drinkTea: Color,
    public val drinkChoc: Color,
    public val scrim: Color,
    public val focusRing: Color,
    public val shadow: Color,
)
