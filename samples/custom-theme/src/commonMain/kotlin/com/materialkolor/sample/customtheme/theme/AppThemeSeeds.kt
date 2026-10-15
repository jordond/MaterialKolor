package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class AppThemeSeeds(
    public val stock: Color = Color(0xFFF1E6CC),
    public val pink: Color = Color(0xFFFF48B0),
    public val blue: Color = Color(0xFF0078BF),
    public val yellow: Color = Color(0xFFFFE800),
) {
    public companion object {
        public val Default: AppThemeSeeds = AppThemeSeeds()
    }
}
