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
package com.materialkolor.blend

import com.materialkolor.hct.Cam16
import com.materialkolor.hct.Hct
import com.materialkolor.utils.ColorUtils.lstarFromArgb
import com.materialkolor.utils.MathUtils.differenceDegrees
import com.materialkolor.utils.MathUtils.rotationDirection
import com.materialkolor.utils.MathUtils.sanitizeDegrees
import kotlin.math.min

/**
 * Functions for blending in HCT and CAM16.
 */
@Suppress("MemberVisibilityCanBePrivate")
public object Blend {
    /**
     * Blend the design color's HCT hue towards the key color's HCT hue, in a way that leaves the
     * original color recognizable and recognizably shifted towards the key color.
     *
     * @param designColor ARGB representation of an arbitrary color.
     * @param sourceColor ARGB representation of the main theme color.
     * @return The design color with a hue shifted towards the system's color, a slightly
     * warmer/cooler variant of the design color's hue.
     */
    public fun harmonize(
        designColor: Int,
        sourceColor: Int,
    ): Int {
        val fromHct = Hct.fromInt(designColor)
        val toHct = Hct.fromInt(sourceColor)
        return harmonize(fromHct, toHct).toInt()
    }

    /**
     * Blend the design color's HCT hue towards the key color's HCT hue, in a way that leaves the
     * original color recognizable and recognizably shifted towards the key color.
     *
     * @param designColor HCT representation of an arbitrary color.
     * @param sourceColor HCT representation of the main theme color.
     * @return The design color with a hue shifted towards the system's color, a slightly
     * warmer/cooler variant of the design color's hue.
     */
    public fun harmonize(
        designColor: Hct,
        sourceColor: Hct,
    ): Hct {
        val differenceDegrees = differenceDegrees(designColor.hue, sourceColor.hue)
        val rotationDegrees: Double = min(differenceDegrees * 0.5, 15.0)
        val rotationDirection = rotationDirection(designColor.hue, sourceColor.hue)
        val outputHue = sanitizeDegrees(designColor.hue + rotationDegrees * rotationDirection)
        return Hct.from(outputHue, designColor.chroma, designColor.tone)
    }

    /**
     * Blends hue from one color into another. The chroma and tone of the original color are
     * maintained.
     *
     * @param from ARGB representation of color
     * @param to ARGB representation of color
     * @param amount how much blending to perform; 0.0 >= and <= 1.0
     * @return from, with a hue blended towards to. Chroma and tone are constant.
     */
    public fun hctHue(
        from: Int,
        to: Int,
        amount: Double,
    ): Int {
        val ucs = cam16Ucs(from, to, amount)
        val ucsCam = Cam16.fromInt(ucs)
        val fromCam = Cam16.fromInt(from)
        val blended = Hct.from(ucsCam.hue, fromCam.chroma, lstarFromArgb(from))
        return blended.toInt()
    }

    /**
     * Blend in CAM16-UCS space.
     *
     * @param from ARGB representation of color
     * @param to ARGB representation of color
     * @param amount how much blending to perform; 0.0 >= and <= 1.0
     * @return from, blended towards to. Hue, chroma, and tone will change.
     */
    public fun cam16Ucs(
        from: Int,
        to: Int,
        amount: Double,
    ): Int {
        val fromCam = Cam16.fromInt(from)
        val toCam = Cam16.fromInt(to)
        val fromJ = fromCam.jstar
        val fromA = fromCam.astar
        val fromB = fromCam.bstar
        val toJ = toCam.jstar
        val toA = toCam.astar
        val toB = toCam.bstar
        val jstar = fromJ + (toJ - fromJ) * amount
        val astar = fromA + (toA - fromA) * amount
        val bstar = fromB + (toB - fromB) * amount
        return Cam16.fromUcs(jstar, astar, bstar).toInt()
    }
}
