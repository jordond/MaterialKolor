package com.materialkolor

import com.materialkolor.scheme.Variant

/**
 * The style of the palette to generate.
 *
 * Mapped to [Variant] in the Material Design guidelines.
 */
public enum class PaletteStyle {

    /**
     * A calm theme, sedated colors that aren't particularly chromatic.
     */
    TonalSpot,

    /**
     * A theme that's slightly more chromatic than monochrome, which is purely black / white / gray.
     */
    Neutral,

    /**
     * A loud theme, colorfulness is maximum for Primary palette, increased for others.
     */
    Vibrant,

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    Expressive,

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    Rainbow,

    /**
     * A playful theme - the source color's hue does not appear in the theme.
     */
    FruitSalad,

    /**
     * A monochrome theme, colors are purely black / white / gray.
     */
    Monochrome,

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
    Fidelity,

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
    Content,
}