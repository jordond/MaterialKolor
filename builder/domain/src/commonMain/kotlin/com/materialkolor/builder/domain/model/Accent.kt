package com.materialkolor.builder.domain.model

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An extra color family the theme owns, on top of the ones the scheme already names.
 *
 * A brand red, a status green, a category color. Each one is a seed plus the tones its four slots
 * are cut at, so the family behaves like primary or secondary without the scheme knowing about it.
 *
 * @property[name] What the family is called, and what the exported property is named after.
 * @property[seed] The color the family is built from.
 * @property[harmonize] Whether the seed is pulled toward the theme seed before the ramp is built.
 * @property[light] The tones the family is cut at in light mode.
 * @property[dark] The tones the family is cut at in dark mode.
 * @property[threshold] The contrast the on colors of this family have to clear.
 */
@Immutable
@Serializable
public data class Accent(
    @SerialName("name")
    public val name: String,
    @SerialName("seed")
    public val seed: Argb,
    @SerialName("harmonize")
    public val harmonize: Boolean = true,
    @SerialName("light")
    public val light: FamilyTones = FamilyTones(color = 40, container = 90),
    @SerialName("dark")
    public val dark: FamilyTones = FamilyTones(color = 80, container = 30),
    @SerialName("threshold")
    public val threshold: OnColorThreshold = OnColorThreshold.AaNormal,
)

/**
 * The two tones a color family is cut at in one mode.
 *
 * @property[color] The tone of the family's accent slot.
 * @property[container] The tone of the family's container slot.
 */
@Immutable
@Serializable
public data class FamilyTones(
    @SerialName("color")
    public val color: Int,
    @SerialName("container")
    public val container: Int,
) {
    init {
        require(color in 0..100) { "A tone is 0 to 100, got color $color" }
        require(container in 0..100) { "A tone is 0 to 100, got container $container" }
    }
}

/**
 * The contrast ratio an on color has to clear against the color it sits on.
 *
 * @property[code] The number the share codec writes for this threshold.
 * @property[ratio] The ratio itself, as the contrast checker reports it.
 */
@Serializable
public enum class OnColorThreshold(
    override val code: Int,
    public val ratio: Double,
) : CodedEnum {
    /**
     * WCAG AA for body text.
     */
    @SerialName("AaNormal")
    AaNormal(code = 0, ratio = 4.5),

    /**
     * WCAG AA for large text, the looser of the two AA rules.
     */
    @SerialName("AaLarge")
    AaLarge(code = 1, ratio = 3.0),

    /**
     * WCAG AAA, the strictest of the three.
     */
    @SerialName("Aaa")
    Aaa(code = 2, ratio = 7.0),
}
