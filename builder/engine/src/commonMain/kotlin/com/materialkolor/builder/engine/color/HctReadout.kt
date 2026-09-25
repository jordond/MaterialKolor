package com.materialkolor.builder.engine.color

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.hct.Hct

/**
 * A color's hue, chroma and tone, as core's HCT measures them, for a readout beside the color.
 *
 * The values are exact. Rounding them for the screen is up to whoever shows them.
 *
 * @property[hue] The hue in degrees, from 0 up to but not including 360.
 * @property[chroma] How colorful it is, 0 for a gray.
 * @property[tone] How light it is, 0 for black and 100 for white.
 */
@Immutable
public data class HctReadout(
    public val hue: Double,
    public val chroma: Double,
    public val tone: Double,
) {
    public companion object {
        /**
         * Measure [argb].
         */
        public fun of(argb: Argb): HctReadout {
            val hct = Hct.fromInt(argb.value)
            return HctReadout(hue = hct.hue, chroma = hct.chroma, tone = hct.tone)
        }
    }
}
