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
 * A playful theme - the source color's hue does not appear in the theme.
 */
public class SchemeExpressive(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: Platform = Platform.Default,
) : DynamicScheme(
        sourceColorHct = sourceColorHct,
        variant = Variant.EXPRESSIVE,
        isDark = isDark,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
        primaryPalette = ColorSpecs
            .get(specVersion)
            .getPrimaryPalette(Variant.EXPRESSIVE, sourceColorHct, isDark, platform, contrastLevel),
        secondaryPalette = ColorSpecs
            .get(specVersion)
            .getSecondaryPalette(
                Variant.EXPRESSIVE,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            ),
        tertiaryPalette = ColorSpecs
            .get(specVersion)
            .getTertiaryPalette(
                Variant.EXPRESSIVE,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            ),
        neutralPalette = ColorSpecs
            .get(specVersion)
            .getNeutralPalette(Variant.EXPRESSIVE, sourceColorHct, isDark, platform, contrastLevel),
        neutralVariantPalette = ColorSpecs
            .get(specVersion)
            .getNeutralVariantPalette(
                Variant.EXPRESSIVE,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            ),
        errorPalette = ColorSpecs
            .get(specVersion)
            .getErrorPalette(Variant.EXPRESSIVE, sourceColorHct, isDark, platform, contrastLevel),
    )
