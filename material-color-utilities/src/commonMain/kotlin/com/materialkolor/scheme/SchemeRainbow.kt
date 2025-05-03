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

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.ColorSpecs
import com.materialkolor.hct.Hct

/**
 * A playful theme - the source color's hue does not appear in the theme.
 */
public class SchemeRainbow(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
) : DynamicScheme(
        sourceColorHct = sourceColorHct,
        variant = Variant.RAINBOW,
        isDark = isDark,
        contrastLevel = contrastLevel,
        primaryPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getPrimaryPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
        secondaryPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getSecondaryPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
        tertiaryPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getTertiaryPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
        neutralPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getNeutralPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
        neutralVariantPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getNeutralVariantPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
        errorPalette = ColorSpecs
            .get(ColorSpec.SpecVersion.SPEC_2021)
            .getErrorPalette(
                Variant.RAINBOW,
                sourceColorHct,
                isDark,
                Platform.PHONE,
                contrastLevel,
            ),
    )
