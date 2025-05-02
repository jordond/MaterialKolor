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

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.ColorSpecs
import com.materialkolor.hct.Hct

/**
 * A theme that's slightly more chromatic than monochrome, which is purely black / white / gray.
 */
public class SchemeNeutral(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: Platform = Platform.Default,
) : DynamicScheme(
    sourceColorHct = sourceColorHct,
    variant = Variant.NEUTRAL,
    isDark = isDark,
    contrastLevel = contrastLevel,
    primaryPalette = ColorSpecs
        .get(specVersion)
        .getPrimaryPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
    secondaryPalette = ColorSpecs
        .get(specVersion)
        .getSecondaryPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
    tertiaryPalette = ColorSpecs
        .get(specVersion)
        .getTertiaryPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
    neutralPalette = ColorSpecs
        .get(specVersion)
        .getNeutralPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
    neutralVariantPalette = ColorSpecs
        .get(specVersion)
        .getNeutralVariantPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
    errorPalette = ColorSpecs
        .get(specVersion)
        .getErrorPalette(Variant.NEUTRAL, sourceColorHct, isDark, platform, contrastLevel),
)
