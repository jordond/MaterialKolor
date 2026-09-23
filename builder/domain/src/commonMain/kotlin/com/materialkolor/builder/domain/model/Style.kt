package com.materialkolor.builder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The palette style a theme is generated with, the document's mirror of `PaletteStyle`.
 *
 * The document keeps its own type because [Cmf] carries a second seed in the library and the
 * document holds that seed in its own field instead, and because the share codec needs a number
 * per style that never moves. [code] is that number. Reordering the entries is free, changing a
 * [code] breaks every link anyone ever shared.
 *
 * @property[code] The number the share codec writes for this style.
 */
@Serializable
public enum class Style(
    override val code: Int,
) : CodedEnum {
    /** A calm theme, sedated colors that are not particularly chromatic. */
    @SerialName("TonalSpot")
    TonalSpot(code = 0),

    /** Slightly more chromatic than [Monochrome]. */
    @SerialName("Neutral")
    Neutral(code = 1),

    /** A loud theme, colorfulness is at its maximum for the primary palette. */
    @SerialName("Vibrant")
    Vibrant(code = 2),

    /** A playful theme, the seed's hue does not appear in the result. */
    @SerialName("Expressive")
    Expressive(code = 3),

    /** A playful theme built from a spread of hues. */
    @SerialName("Rainbow")
    Rainbow(code = 4),

    /** A playful theme built from a wider spread of hues than [Rainbow]. */
    @SerialName("FruitSalad")
    FruitSalad(code = 5),

    /** Black, white and gray only. */
    @SerialName("Monochrome")
    Monochrome(code = 6),

    /** Keeps the seed itself in the primary container, with a complementary tertiary. */
    @SerialName("Fidelity")
    Fidelity(code = 7),

    /** Keeps the seed itself in the primary container, with an analogous tertiary. */
    @SerialName("Content")
    Content(code = 8),

    /** The 2026 color, material and finish style, which takes a second seed for its tertiary. */
    @SerialName("Cmf")
    Cmf(code = 9),
}
