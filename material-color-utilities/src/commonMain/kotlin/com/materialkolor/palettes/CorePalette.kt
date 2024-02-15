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
import kotlin.math.max
import kotlin.math.min

/**
 * An intermediate concept between the key color for a UI theme, and a full color scheme. 5 sets of
 * tones are generated, all except one use the same hue as the key color, and all vary in chroma.
 *
 * @constructor Create a new CorePalette
 * @param[argb] ARGB representation of a color
 * @param[isContent] Whether the color is used as a content color
 */
public class CorePalette private constructor(argb: Int, isContent: Boolean) {

    public val a1: TonalPalette
    public val a2: TonalPalette
    public val a3: TonalPalette
    public val n1: TonalPalette
    public val n2: TonalPalette
    public val error: TonalPalette

    init {
        val hct = Hct.fromInt(argb)
        val hue = hct.hue
        val chroma = hct.chroma
        if (isContent) {
            a1 = TonalPalette.fromHueAndChroma(hue = hue, chroma = chroma)
            a2 = TonalPalette.fromHueAndChroma(hue = hue, chroma = chroma / 3.0)
            a3 = TonalPalette.fromHueAndChroma(hue = hue + 60.0, chroma = chroma / 2.0)
            n1 = TonalPalette.fromHueAndChroma(hue = hue, chroma = min(chroma / 12.0, 4.0))
            n2 = TonalPalette.fromHueAndChroma(hue = hue, chroma = min(chroma / 6.0, 8.0))
        } else {
            a1 = TonalPalette.fromHueAndChroma(hue = hue, chroma = max(48.0, chroma))
            a2 = TonalPalette.fromHueAndChroma(hue = hue, chroma = 16.0)
            a3 = TonalPalette.fromHueAndChroma(hue = hue + 60.0, chroma = 24.0)
            n1 = TonalPalette.fromHueAndChroma(hue = hue, chroma = 4.0)
            n2 = TonalPalette.fromHueAndChroma(hue = hue, chroma = 8.0)
        }
        error = TonalPalette.fromHueAndChroma(hue = 25.0, chroma = 84.0)
    }

    public companion object {

        /**
         * Create key tones from a color.
         *
         * @param argb ARGB representation of a color
         */
        public fun of(argb: Int): CorePalette = CorePalette(argb = argb, isContent = false)

        /**
         * Create content key tones from a color.
         *
         * @param argb ARGB representation of a color
         */
        public fun contentOf(argb: Int): CorePalette = CorePalette(argb = argb, isContent = true)
    }
}
