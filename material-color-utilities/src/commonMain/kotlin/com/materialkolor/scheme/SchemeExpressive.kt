/*
 * Copyright 2022 Google LLC
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
package com.materialkolor.scheme

import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.utils.MathUtils

/**
 * A playful theme - the source color's hue does not appear in the theme.
 */
public class SchemeExpressive(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
) : DynamicScheme(
    sourceColorHct = sourceColorHct,
    variant = Variant.EXPRESSIVE,
    isDark = isDark,
    contrastLevel = contrastLevel,
    primaryPalette = TonalPalette.fromHueAndChroma(
        hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 240.0),
        chroma = 40.0,
    ),
    secondaryPalette = TonalPalette.fromHueAndChroma(
        hue = getRotatedHue(sourceColorHct, HUES, SECONDARY_ROTATIONS),
        chroma = 24.0,
    ),
    tertiaryPalette = TonalPalette.fromHueAndChroma(
        hue = getRotatedHue(sourceColorHct, HUES, TERTIARY_ROTATIONS),
        chroma = 32.0,
    ),
    neutralPalette = TonalPalette.fromHueAndChroma(
        hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 15.0),
        chroma = 8.0,
    ),
    neutralVariantPalette = TonalPalette.fromHueAndChroma(
        hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 15.0),
        chroma = 12.0,
    ),
) {

    private companion object {

        // NOMUTANTS--arbitrary increments/decrements, correctly, still passes tests.
        private val HUES = doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0)

        private val SECONDARY_ROTATIONS =
            doubleArrayOf(45.0, 95.0, 45.0, 20.0, 45.0, 90.0, 45.0, 45.0, 45.0)

        private val TERTIARY_ROTATIONS =
            doubleArrayOf(120.0, 120.0, 20.0, 45.0, 20.0, 15.0, 20.0, 120.0, 120.0)
    }
}
