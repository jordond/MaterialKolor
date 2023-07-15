/*
 * Copyright 2023 Google LLC
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
import com.materialkolor.utils.MathUtils
import com.materialkolor.palettes.TonalPalette

/** A playful theme - the source color's hue does not appear in the theme.  */
class SchemeFruitSalad(sourceColorHct: Hct, isDark: Boolean, contrastLevel: Double) : DynamicScheme(
    sourceColorHct,
    Variant.FRUIT_SALAD,
    isDark,
    contrastLevel,
    TonalPalette.fromHueAndChroma(
        MathUtils.sanitizeDegreesDouble(sourceColorHct.getHue() - 50.0), 48.0),
    TonalPalette.fromHueAndChroma(
        MathUtils.sanitizeDegreesDouble(sourceColorHct.getHue() - 50.0), 36.0),
    TonalPalette.fromHueAndChroma(sourceColorHct.getHue(), 36.0),
    TonalPalette.fromHueAndChroma(sourceColorHct.getHue(), 10.0),
    TonalPalette.fromHueAndChroma(sourceColorHct.getHue(), 16.0))