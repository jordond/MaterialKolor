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
 * A scheme that places the source color in Scheme.primaryContainer.
 *
 * Primary Container is the source color, adjusted for color relativity. It maintains constant
 * appearance in light mode and dark mode. This adds ~5 tone in light mode, and subtracts ~5 tone in
 * dark mode.
 *
 * Tertiary Container is the complement to the source color, using TemperatureCache. It also
 * maintains constant appearance.
 */
public class SchemeFidelity(
    sourceColorHct: Hct,
    isDark: Boolean,
    contrastLevel: Double,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    platform: Platform = Platform.Default,
) : DynamicScheme(
        sourceColorHct = sourceColorHct,
        variant = Variant.FIDELITY,
        isDark = isDark,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
        platform = platform,
        primaryPalette = ColorSpecs
            .get(specVersion)
            .getPrimaryPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
        secondaryPalette = ColorSpecs
            .get(specVersion)
            .getSecondaryPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
        tertiaryPalette = ColorSpecs
            .get(specVersion)
            .getTertiaryPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
        neutralPalette = ColorSpecs
            .get(specVersion)
            .getNeutralPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
        neutralVariantPalette = ColorSpecs
            .get(specVersion)
            .getNeutralVariantPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
        errorPalette = ColorSpecs
            .get(specVersion)
            .getErrorPalette(
                variant = Variant.FIDELITY,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            ),
    )
