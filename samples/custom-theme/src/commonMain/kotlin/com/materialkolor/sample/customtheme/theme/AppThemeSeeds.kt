package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The accent seeds the theme owns on top of the one seed the scheme is generated from.
 *
 * They are inputs, the way a brand color is an input, and every slot in [AppColors] is derived from them. Swapping
 * one moves a whole family or a whole decorative accent, in both light and dark, without touching the theme.
 *
 * @property[love] Seed for the affection family, a warm red.
 * @property[cold] Seed for the cold family, a clear blue.
 * @property[warm] Seed for the warm family, an amber.
 * @property[coffee] Seed for the coffee category color.
 * @property[matcha] Seed for the matcha category color.
 * @property[iced] Seed for the iced drink category color.
 * @property[tea] Seed for the tea category color.
 * @property[chocolate] Seed for the chocolate category color.
 */
@Immutable
public data class AppThemeSeeds(
    public val love: Color = Color(0xFFE05263),
    public val cold: Color = Color(0xFF3E92CC),
    public val warm: Color = Color(0xFFF0A202),
    public val coffee: Color = Color(0xFF6F4E37),
    public val matcha: Color = Color(0xFF8DB600),
    public val iced: Color = Color(0xFF7EC8E3),
    public val tea: Color = Color(0xFFC9A227),
    public val chocolate: Color = Color(0xFF3F2212),
) {
    public companion object {
        public val Default: AppThemeSeeds = AppThemeSeeds()
    }
}
