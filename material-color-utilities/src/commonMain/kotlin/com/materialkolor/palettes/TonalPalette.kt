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
import kotlin.math.round

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
            return TonalPalette(hue, chroma, createKeyColor(hue, chroma))
        }

        /**
         * The key color is the first tone, starting from T50, matching the given hue and chroma.
         *
         * @param[hue] H in HCT
         * @param[chroma] C in HCT
         * @return HCT representation of the key color.
         */
        private fun createKeyColor(hue: Double, chroma: Double): Hct {
            val startTone = 50.0
            var smallestDeltaHct = Hct.from(hue, chroma, startTone)
            var smallestDelta: Double = abs(smallestDeltaHct.chroma - chroma)
            // Starting from T50, check T+/-delta to see if they match the requested
            // chroma.
            //
            // Starts from T50 because T50 has the most chroma available, on
            // average. Thus it is most likely to have a direct answer and minimize
            // iteration.
            var delta = 1.0
            while (delta < 50.0) {

                // Termination condition rounding instead of minimizing delta to avoid
                // case where requested chroma is 16.51, and the closest chroma is 16.49.
                // Error is minimized, but when rounded and displayed, requested chroma
                // is 17, key color's chroma is 16.
                if (round(chroma) == round(smallestDeltaHct.chroma)) {
                    return smallestDeltaHct
                }
                val hctAdd = Hct.from(hue, chroma, startTone + delta)
                val hctAddDelta: Double = abs(hctAdd.chroma - chroma)
                if (hctAddDelta < smallestDelta) {
                    smallestDelta = hctAddDelta
                    smallestDeltaHct = hctAdd
                }
                val hctSubtract = Hct.from(hue, chroma, startTone - delta)
                val hctSubtractDelta: Double = abs(hctSubtract.chroma - chroma)
                if (hctSubtractDelta < smallestDelta) {
                    smallestDelta = hctSubtractDelta
                    smallestDeltaHct = hctSubtract
                }
                delta += 1.0
            }

            return smallestDeltaHct
        }
    }
}
