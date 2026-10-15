package com.materialkolor.builder.domain.model

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One of the six palettes a scheme is built from, which a theme may seed on its own.
 *
 * The declaration order is the order the share codec writes its presence mask in, so the first
 * entry owns the lowest bit. [code] is that bit position.
 *
 * @property[code] The bit the share codec uses for this palette.
 */
@Serializable
public enum class KeyColor(
    override val code: Int,
) : CodedEnum {
    @SerialName("Primary")
    Primary(code = 0),

    @SerialName("Secondary")
    Secondary(code = 1),

    @SerialName("Tertiary")
    Tertiary(code = 2),

    @SerialName("Error")
    Error(code = 3),

    @SerialName("Neutral")
    Neutral(code = 4),

    @SerialName("NeutralVariant")
    NeutralVariant(code = 5),
}

/**
 * The palettes a theme overrides instead of deriving from its seed.
 *
 * A null palette is the ordinary case, it means the scheme derives that palette from the seed the
 * way it always has. Only a palette someone set by hand is held here.
 *
 * @property[primary] Overrides the primary palette.
 * @property[secondary] Overrides the secondary palette.
 * @property[tertiary] Overrides the tertiary palette.
 * @property[error] Overrides the error palette.
 * @property[neutral] Overrides the neutral palette.
 * @property[neutralVariant] Overrides the neutral variant palette.
 */
@Immutable
@Serializable
public data class KeyColors(
    @SerialName("primary")
    public val primary: Argb? = null,
    @SerialName("secondary")
    public val secondary: Argb? = null,
    @SerialName("tertiary")
    public val tertiary: Argb? = null,
    @SerialName("error")
    public val error: Argb? = null,
    @SerialName("neutral")
    public val neutral: Argb? = null,
    @SerialName("neutralVariant")
    public val neutralVariant: Argb? = null,
) {
    /**
     * The color set for [slot], or null when that palette still comes from the seed.
     */
    public operator fun get(slot: KeyColor): Argb? =
        when (slot) {
            KeyColor.Primary -> primary
            KeyColor.Secondary -> secondary
            KeyColor.Tertiary -> tertiary
            KeyColor.Error -> error
            KeyColor.Neutral -> neutral
            KeyColor.NeutralVariant -> neutralVariant
        }

    /**
     * A copy with [slot] set to [value], or back to the seed when [value] is null.
     */
    public fun with(
        slot: KeyColor,
        value: Argb?,
    ): KeyColors =
        when (slot) {
            KeyColor.Primary -> copy(primary = value)
            KeyColor.Secondary -> copy(secondary = value)
            KeyColor.Tertiary -> copy(tertiary = value)
            KeyColor.Error -> copy(error = value)
            KeyColor.Neutral -> copy(neutral = value)
            KeyColor.NeutralVariant -> copy(neutralVariant = value)
        }

    /**
     * Whether every palette still comes from the seed.
     */
    public fun isEmpty(): Boolean = KeyColor.entries.all { slot -> get(slot) == null }
}
