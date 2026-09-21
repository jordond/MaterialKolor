package com.materialkolor

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.Variant
import dev.drewhamilton.poko.Poko

/**
 * The style of the palette to generate.
 *
 * Mapped to [Variant] in the Material Design guidelines.
 */
public sealed interface PaletteStyle {

    /**
     * A calm theme, sedated colors that aren't particularly chromatic.
     */
    public data object TonalSpot : PaletteStyle

    /**
     * A theme that's slightly more chromatic than monochrome, which is purely black / white / gray.
     */
    public data object Neutral : PaletteStyle

    /**
     * A loud theme, colorfulness is maximum for Primary palette, increased for others.
     */
    public data object Vibrant : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object Expressive : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object Rainbow : PaletteStyle

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    public data object FruitSalad : PaletteStyle

    /**
     * A monochrome theme, colors are purely black / white / gray.
     */
    public data object Monochrome : PaletteStyle

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     * Tertiary Container is the complement to the source color, using TemperatureCache. It also
     * maintains constant appearance.
     */
    public data object Fidelity : PaletteStyle

    /**
     * A scheme that places the source color in Scheme.primaryContainer.
     *
     * Primary Container is the source color, adjusted for color relativity. It maintains constant
     * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
     * dark mode.
     *
     * Tertiary Container is an analogous color, specifically, the analog of a color wheel divided
     * into 6, and the precise analog is the one found by increasing hue. This is a scientifically
     * grounded equivalent to rotating hue clockwise by 60 degrees. It also maintains constant
     * appearance.
     */
    public data object Content : PaletteStyle

    /**
     * A theme built on the 2026 spec's CMF (Color, Material, Finish) design discipline, which can
     * take a second seed color.
     *
     * Pass [tertiarySourceColor] to seed the tertiary palette from a color of its own. Leave it
     * `null` and the tertiary palette is derived from the primary source color, the way the other
     * styles behave.
     *
     * @param[tertiarySourceColor] An optional second source color, used to seed the tertiary palette.
     */
    @Poko
    public class Cmf(
        public val tertiarySourceColor: Color? = null,
    ) : PaletteStyle

    public companion object {

        /**
         * Every style that can be listed without extra input, for menus, pickers and tests.
         *
         * [PaletteStyle] is a sealed interface rather than an enum, so it has no `entries`. [Cmf]
         * carries a color, so this list holds one with no tertiary source color. Build your own
         * [Cmf] when you want to seed the tertiary palette.
         */
        public val KnownStyles: List<PaletteStyle> = listOf(
            TonalSpot,
            Neutral,
            Vibrant,
            Expressive,
            Rainbow,
            FruitSalad,
            Monochrome,
            Fidelity,
            Content,
            Cmf(),
        )
    }
}
