package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * @see toColors for how the slots are derived.
 */
@Immutable
public data class AppColors(
    public val isLight: Boolean,
    public val paper: Color,
    public val paperShade: Color,
    public val paperEdge: Color,
    public val ink: Color,
    public val inkSoft: Color,
    public val primary: Color,
    public val onPrimary: Color,
    public val error: Color,
    public val onError: Color,
    public val pink: Color,
    public val onPink: Color,
    public val blue: Color,
    public val onBlue: Color,
    public val yellow: Color,
    public val onYellow: Color,
    public val highlight: Color,
    public val scrim: Color,
) {
    /**
     * How one ink lands on another. On light stock the inks multiply like real ink does. The dark stock prints them
     * as if they were fluorescent, so overlaps get brighter instead of darker.
     */
    public val overprint: BlendMode
        get() = if (isLight) BlendMode.Multiply else BlendMode.Screen
}
