/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.materialkolor.palettes

import com.materialkolor.hct.Hct
import dev.drewhamilton.poko.Poko
import kotlin.math.abs

/**
 * A convenience class for retrieving colors that are constant in hue and chroma, but vary in tone.
 *
 * @param[hue] The hue of the Tonal Palette, in HCT. Ranges from 0 to 360.
 * @param[chroma] The chroma of the Tonal Palette, in HCT. Ranges from 0 to ~130 (for sRGB gamut).
 * @param[keyColor] The key color is the first tone, starting from T50, that matches the palette's chroma.
 */
@Poko
public class TonalPalette private constructor(
    public val hue: Double,
    public val chroma: Double,
    public val keyColor: Hct,
) {

    private var cache: MutableMap<Int, Int> = HashMap()

    /**
     * Create an ARGB color with HCT hue and chroma of this Tones instance, and the provided HCT tone.
     *
     * @param[tone] HCT tone, measured from 0 to 100.
     * @return ARGB representation of a color with that tone.
     */
    public fun tone(tone: Int): Int {
        var color = cache[tone]
        if (color == null) {
            color = Hct.from(hue, chroma, tone.toDouble()).toInt()
            cache[tone] = color
        }

        return color
    }

    /**
     * Given a tone, use hue and chroma of palette to create a color, and return it as HCT.
     *
     * @param[tone] HCT tone, measured from 0 to 100.
     * @return HCT representation of a color with that tone.
     */
    public fun getHct(tone: Double): Hct = Hct.from(hue, chroma, tone)

    /**
     * Key color is a color that represents the hue and chroma of a tonal palette.
     */
    private class KeyColor(private val hue: Double, private val requestedChroma: Double) {

        /**
         * Cache that maps tone to max chroma to avoid duplicated HCT calculation.
         */
        private val chromaCache: HashMap<Int, Double> = HashMap()

        /**
         * Creates a key color from a [hue] and a [chroma]. The key color is the first tone,
         * starting from T50, matching the given hue and chroma.
         *
         * @return Key color [Hct]
         */
        fun create(): Hct {
            // Pivot around T50 because T50 has the most chroma available, on
            // average. Thus it is most likely to have a direct answer.
            val pivotTone = 50
            val toneStepSize = 1

            // Epsilon to accept values slightly higher than the requested chroma.
            val epsilon = 0.01

            // Binary search to find the tone that can provide a chroma that is closest
            // to the requested chroma.
            var lowerTone = 0
            var upperTone = 100
            while (lowerTone < upperTone) {
                val midTone = (lowerTone + upperTone) / 2
                val isAscending = maxChroma(midTone) < maxChroma(midTone + toneStepSize)
                val sufficientChroma = maxChroma(midTone) >= requestedChroma - epsilon

                if (sufficientChroma) {
                    // Either range [lowerTone, midTone] or [midTone, upperTone] has
                    // the answer, so search in the range that is closer the pivot tone.
                    if (abs((lowerTone - pivotTone)) < abs((upperTone - pivotTone))) {
                        upperTone = midTone
                    } else {
                        if (lowerTone == midTone) {
                            return Hct.from(this.hue, this.requestedChroma, lowerTone.toDouble())
                        }
                        lowerTone = midTone
                    }
                } else {
                    // As there is no sufficient chroma in the midTone, follow the direction to
                    // the chroma peak.
                    if (isAscending) {
                        lowerTone = midTone + toneStepSize
                    } else {
                        // Keep midTone for potential chroma peak.
                        upperTone = midTone
                    }
                }
            }

            return Hct.from(this.hue, this.requestedChroma, lowerTone.toDouble())
        }

        // Find the maximum chroma for a given tone
        private fun maxChroma(tone: Int): Double {
            return chromaCache.getOrPut(tone) {
                Hct.from(hue, MAX_CHROMA_VALUE, tone.toDouble()).chroma
            }
        }

        companion object {

            private const val MAX_CHROMA_VALUE = 200.0
        }
    }

    public companion object {

        /**
         * Create tones using the HCT hue and chroma from a color.
         *
         * @param[argb] ARGB representation of a color
         * @return Tones matching that color's hue and chroma.
         */
        public fun fromInt(argb: Int): TonalPalette {
            return fromHct(Hct.fromInt(argb))
        }

        /**
         * Create tones using a HCT color.
         *
         * @param[hct] HCT representation of a color.
         * @return Tones matching that color's hue and chroma.
         */
        public fun fromHct(hct: Hct): TonalPalette {
            return TonalPalette(hct.hue, hct.chroma, hct)
        }

        /**
         * Create tones from a defined HCT hue and chroma.
         *
         * @param[hue] HCT hue
         * @param[chroma] HCT chroma
         * @return Tones matching hue and chroma.
         */
        public fun fromHueAndChroma(hue: Double, chroma: Double): TonalPalette {
            val keyColor = KeyColor(hue, chroma).create()
            return TonalPalette(hue, chroma, keyColor)
        }
    }
}
