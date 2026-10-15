package com.materialkolor.builder.preview.fluent

import com.materialkolor.builder.domain.audit.FluentShade

/**
 * The accent shades Fluent's Trips paints for itself, beyond what its controls paint, in the mode
 * [isDark] picks.
 *
 * Fluent takes only its accent ramp from the theme, so the scene and the tinted thumbnail are cut
 * from the seven shades the way Windows tints its own art. The sky is the shade furthest from the
 * text, the ridges step towards it, and the middle ridge takes the accent fill's own shade.
 *
 * @param[isDark] Whether the pane shows dark mode.
 */
internal class TripsShades(
    isDark: Boolean,
) {
    /**
     * The sky behind the scene.
     */
    val sky: FluentShade = if (isDark) FluentShade.Dark3 else FluentShade.Light3

    /**
     * The sun over the ridges.
     */
    val sun: FluentShade = if (isDark) FluentShade.Dark1 else FluentShade.Light1

    /**
     * The three ridges, farthest first.
     */
    val ridges: List<FluentShade> =
        listOf(FluentShade.Base, fillAccentShade(isDark), if (isDark) FluentShade.Light3 else FluentShade.Dark3)

    /**
     * The fill of the thumbnail the second trip gets, with its icon in the accent text color.
     */
    val tint: FluentShade = if (isDark) FluentShade.Dark2 else FluentShade.Light2

    /**
     * The accent text of the day's plan, the links and the tinted thumbnail's icon.
     */
    val accentText: FluentShade = accentTextShade(isDark)

    /**
     * The accent fill every accent control paints.
     */
    val accentFill: FluentShade = fillAccentShade(isDark)

    /**
     * Every shade the scene paints, sky first.
     */
    val scene: List<FluentShade> get() = listOf(sky, sun) + ridges
}
