/*
 * Copyright 2026 Google LLC
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
package com.materialkolor.dynamiccolor

import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.Variant

/**
 * [ColorSpec] implementation for the 2026 spec.
 */
public open class ColorSpec2026 : ColorSpec2025() {

    private fun findBestToneForChroma(
        hue: Double,
        chroma: Double,
        tone: Double,
        byDecreasingTone: Boolean,
    ): Double {
        var currentTone = tone
        var answer = currentTone
        var bestCandidate = Hct.from(hue, chroma, answer)
        while (bestCandidate.chroma < chroma) {
            if (currentTone < 0 || currentTone > 100) {
                break
            }
            currentTone += if (byDecreasingTone) -1.0 else 1.0
            val newCandidate = Hct.from(hue, chroma, currentTone)
            if (bestCandidate.chroma < newCandidate.chroma) {
                bestCandidate = newCandidate
                answer = currentTone
            }
        }
        return answer
    }

    private fun tMaxC(
        palette: TonalPalette,
        lowerBound: Double = 0.0,
        upperBound: Double = 100.0,
        chromaMultiplier: Double = 1.0,
    ): Double {
        val answer =
            findBestToneForChroma(palette.hue, palette.chroma * chromaMultiplier, 100.0, true)
        return answer.coerceIn(lowerBound, upperBound)
    }

    private fun tMinC(
        palette: TonalPalette,
        lowerBound: Double = 0.0,
        upperBound: Double = 100.0,
    ): Double {
        val answer = findBestToneForChroma(palette.hue, palette.chroma, 0.0, false)
        return answer.coerceIn(lowerBound, upperBound)
    }

    private fun getContrastCurve(defaultContrast: Double): ContrastCurve {
        return when (defaultContrast) {
            1.5 -> ContrastCurve(1.5, 1.5, 3.0, 5.5)
            3.0 -> ContrastCurve(3.0, 3.0, 4.5, 7.0)
            4.5 -> ContrastCurve(4.5, 4.5, 7.0, 11.0)
            6.0 -> ContrastCurve(6.0, 6.0, 7.0, 11.0)
            7.0 -> ContrastCurve(7.0, 7.0, 11.0, 21.0)
            9.0 -> ContrastCurve(9.0, 9.0, 11.0, 21.0)
            11.0 -> ContrastCurve(11.0, 11.0, 21.0, 21.0)
            21.0 -> ContrastCurve(21.0, 21.0, 21.0, 21.0)
            else -> ContrastCurve(defaultContrast, defaultContrast, 7.0, 21.0)
        }
    }

    // Surfaces [S]

    override fun surface(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 4.0 else 98.0) else 0.0 },
            isBackground = true,
        )
        return super.surface().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceDim(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_dim",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 4.0 else 87.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = {
                if (it.variant == Variant.CMF) (if (it.isDark) 1.0 else 1.7) else 0.0
            },
        )
        return super.surfaceDim().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceBright(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_bright",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 18.0 else 98.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = {
                if (it.variant == Variant.CMF) (if (it.isDark) 1.7 else 1.0) else 0.0
            },
        )
        return super.surfaceBright().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceContainerLowest(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_container_lowest",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 0.0 else 100.0) else 0.0 },
            isBackground = true,
        )
        return super.surfaceContainerLowest()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceContainerLow(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_container_low",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 6.0 else 96.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.25 else 0.0 },
        )
        return super.surfaceContainerLow()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_container",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 9.0 else 94.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.4 else 0.0 },
        )
        return super.surfaceContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceContainerHigh(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_container_high",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 12.0 else 92.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.5 else 0.0 },
        )
        return super.surfaceContainerHigh()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun surfaceContainerHighest(): DynamicColor {
        val color2026 = DynamicColor(
            name = "surface_container_highest",
            palette = { it.neutralPalette },
            tone = { if (it.variant == Variant.CMF) (if (it.isDark) 15.0 else 90.0) else 0.0 },
            isBackground = true,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
        )
        return super.surfaceContainerHighest()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSurface(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_surface",
            palette = { it.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(if (it.isDark) 11.0 else 9.0) },
        )
        return super.onSurface().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSurfaceVariant(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_surface_variant",
            palette = { it.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(if (it.isDark) 6.0 else 4.5) },
        )
        return super.onSurfaceVariant()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun outline(): DynamicColor {
        val color2026 = DynamicColor(
            name = "outline",
            palette = { it.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(3.0) },
        )
        return super.outline().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun outlineVariant(): DynamicColor {
        val color2026 = DynamicColor(
            name = "outline_variant",
            palette = { it.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(1.5) },
        )
        return super.outlineVariant()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun inverseSurface(): DynamicColor {
        val color2026 = DynamicColor(
            name = "inverse_surface",
            palette = { it.neutralPalette },
            tone = { if (it.isDark) 98.0 else 4.0 },
            isBackground = true,
            chromaMultiplier = { if (it.variant == Variant.CMF) 1.7 else 0.0 },
        )
        return super.inverseSurface()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun inverseOnSurface(): DynamicColor {
        val color2026 = DynamicColor(
            name = "inverse_on_surface",
            palette = { it.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { inverseSurface() },
            contrastCurve = { getContrastCurve(7.0) },
        )
        return super.inverseOnSurface()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    // Primaries [P]

    override fun primary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "primary",
            palette = { it.primaryPalette },
            tone = {
                if (it.sourceColorHct.chroma <= 12) {
                    (if (it.isDark) 80.0 else 40.0)
                } else {
                    it.sourceColorHct.tone
                }
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(4.5) },
            toneDeltaPair = {
                if (it.platform == DynamicScheme.Platform.PHONE) {
                    ToneDeltaPair(
                        roleA = primaryContainer(),
                        roleB = primary(),
                        delta = 5.0,
                        polarity = TonePolarity.RELATIVE_LIGHTER,
                        deltaConstraint = ToneDeltaPair.DeltaConstraint.FARTHER,
                    )
                } else {
                    null
                }
            },
        )
        return super.primary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun primaryDim(): DynamicColor {
        // Remapped to primary in 2026 spec.
        val color2026 = primary().toBuilder().setName("primary_dim").build()
        return super.primaryDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onPrimary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_primary",
            palette = { it.primaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { primary() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onPrimary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun primaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "primary_container",
            palette = { it.primaryPalette },
            tone = {
                if (!it.isDark && it.sourceColorHct.chroma <= 12) {
                    90.0
                } else if (it.sourceColorHct.tone > 55) {
                    it.sourceColorHct.tone.coerceIn(61.0, 90.0)
                } else {
                    it.sourceColorHct.tone.coerceIn(30.0, 49.0)
                }
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.primaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onPrimaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_primary_container",
            palette = { it.primaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { primaryContainer() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onPrimaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun primaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "primary_fixed",
            palette = { it.primaryPalette },
            tone = {
                val tempS = DynamicScheme.from(it, isDark = false, contrastLevel = 0.0)
                primaryContainer().getTone(tempS)
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.primaryFixed().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun primaryFixedDim(): DynamicColor {
        val color2026 = DynamicColor(
            name = "primary_fixed_dim",
            palette = { it.primaryPalette },
            tone = { primaryFixed().getTone(it) },
            isBackground = true,
            background = { highestSurface(it) },
            toneDeltaPair = {
                ToneDeltaPair(
                    roleA = primaryFixedDim(),
                    roleB = primaryFixed(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = ToneDeltaPair.DeltaConstraint.EXACT,
                )
            },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.primaryFixedDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onPrimaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_primary_fixed",
            palette = { it.primaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (primaryFixed().getTone(it) > 57) primaryFixedDim() else primaryFixed()
            },
            contrastCurve = { getContrastCurve(7.0) },
        )
        return super.onPrimaryFixed()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onPrimaryFixedVariant(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_primary_fixed_variant",
            palette = { it.primaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (primaryFixed().getTone(it) > 57) primaryFixedDim() else primaryFixed()
            },
            contrastCurve = { getContrastCurve(4.5) },
        )
        return super.onPrimaryFixedVariant()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    // Secondaries [Q]

    override fun secondary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "secondary",
            palette = { it.secondaryPalette },
            tone = { if (it.isDark) tMinC(it.secondaryPalette) else tMaxC(it.secondaryPalette) },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(4.5) },
            toneDeltaPair = {
                if (it.platform == DynamicScheme.Platform.PHONE) {
                    ToneDeltaPair(
                        roleA = secondaryContainer(),
                        roleB = secondary(),
                        delta = 5.0,
                        polarity = TonePolarity.RELATIVE_LIGHTER,
                        deltaConstraint = ToneDeltaPair.DeltaConstraint.FARTHER,
                    )
                } else {
                    null
                }
            },
        )
        return super.secondary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun secondaryDim(): DynamicColor {
        // Remapped to secondary in 2026 spec.
        val color2026 = secondary().toBuilder().setName("secondary_dim").build()
        return super.secondaryDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSecondary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_secondary",
            palette = { it.secondaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { secondary() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onSecondary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun secondaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "secondary_container",
            palette = { it.secondaryPalette },
            tone = {
                if (it.isDark) {
                    tMinC(it.secondaryPalette, 20.0, 49.0)
                } else {
                    tMaxC(it.secondaryPalette, 61.0, 90.0)
                }
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.secondaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSecondaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_secondary_container",
            palette = { it.secondaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { secondaryContainer() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onSecondaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun secondaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "secondary_fixed",
            palette = { it.secondaryPalette },
            tone = {
                val tempS = DynamicScheme.from(it, isDark = false, contrastLevel = 0.0)
                secondaryContainer().getTone(tempS)
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.secondaryFixed()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun secondaryFixedDim(): DynamicColor {
        val color2026 = DynamicColor(
            name = "secondary_fixed_dim",
            palette = { it.secondaryPalette },
            tone = { secondaryFixed().getTone(it) },
            isBackground = true,
            background = { highestSurface(it) },
            toneDeltaPair = {
                ToneDeltaPair(
                    roleA = secondaryFixedDim(),
                    roleB = secondaryFixed(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = ToneDeltaPair.DeltaConstraint.EXACT,
                )
            },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.secondaryFixedDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSecondaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_secondary_fixed",
            palette = { it.secondaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (secondaryFixed().getTone(it) > 57) secondaryFixedDim() else secondaryFixed()
            },
            contrastCurve = { getContrastCurve(7.0) },
        )
        return super.onSecondaryFixed()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onSecondaryFixedVariant(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_secondary_fixed_variant",
            palette = { it.secondaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (secondaryFixed().getTone(it) > 57) secondaryFixedDim() else secondaryFixed()
            },
            contrastCurve = { getContrastCurve(4.5) },
        )
        return super.onSecondaryFixedVariant()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    // Tertiaries [T]

    override fun tertiary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "tertiary",
            palette = { it.tertiaryPalette },
            tone = { it.sourceColorHctList.getOrNull(1)?.tone ?: it.sourceColorHct.tone },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(4.5) },
            toneDeltaPair = {
                if (it.platform == DynamicScheme.Platform.PHONE) {
                    ToneDeltaPair(
                        roleA = tertiaryContainer(),
                        roleB = tertiary(),
                        delta = 5.0,
                        polarity = TonePolarity.RELATIVE_LIGHTER,
                        deltaConstraint = ToneDeltaPair.DeltaConstraint.FARTHER,
                    )
                } else {
                    null
                }
            },
        )
        return super.tertiary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun tertiaryDim(): DynamicColor {
        // Remapped to tertiary in 2026 spec.
        val color2026 = tertiary().toBuilder().setName("tertiary_dim").build()
        return super.tertiaryDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onTertiary(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_tertiary",
            palette = { it.tertiaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { tertiary() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onTertiary().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun tertiaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "tertiary_container",
            palette = { it.tertiaryPalette },
            tone = {
                val secondarySourceColorHct =
                    it.sourceColorHctList.getOrNull(1) ?: it.sourceColorHct
                if (secondarySourceColorHct.tone > 55) {
                    secondarySourceColorHct.tone.coerceIn(61.0, 90.0)
                } else {
                    secondarySourceColorHct.tone.coerceIn(20.0, 49.0)
                }
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.tertiaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onTertiaryContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_tertiary_container",
            palette = { it.tertiaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { tertiaryContainer() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onTertiaryContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun tertiaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "tertiary_fixed",
            palette = { it.tertiaryPalette },
            tone = {
                val tempS = DynamicScheme.from(it, isDark = false, contrastLevel = 0.0)
                tertiaryContainer().getTone(tempS)
            },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.tertiaryFixed()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun tertiaryFixedDim(): DynamicColor {
        val color2026 = DynamicColor(
            name = "tertiary_fixed_dim",
            palette = { it.tertiaryPalette },
            tone = { tertiaryFixed().getTone(it) },
            isBackground = true,
            background = { highestSurface(it) },
            toneDeltaPair = {
                ToneDeltaPair(
                    roleA = tertiaryFixedDim(),
                    roleB = tertiaryFixed(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = ToneDeltaPair.DeltaConstraint.EXACT,
                )
            },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.tertiaryFixedDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onTertiaryFixed(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_tertiary_fixed",
            palette = { it.tertiaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (tertiaryFixed().getTone(it) > 57) tertiaryFixedDim() else tertiaryFixed()
            },
            contrastCurve = { getContrastCurve(7.0) },
        )
        return super.onTertiaryFixed()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onTertiaryFixedVariant(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_tertiary_fixed_variant",
            palette = { it.tertiaryPalette },
            tone = { 0.0 },
            isBackground = false,
            background = {
                if (tertiaryFixed().getTone(it) > 57) tertiaryFixedDim() else tertiaryFixed()
            },
            contrastCurve = { getContrastCurve(4.5) },
        )
        return super.onTertiaryFixedVariant()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    // Errors [E]

    override fun error(): DynamicColor {
        val color2026 = DynamicColor(
            name = "error",
            palette = { it.errorPalette },
            tone = { tMaxC(it.errorPalette) },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { getContrastCurve(4.5) },
            toneDeltaPair = {
                if (it.platform == DynamicScheme.Platform.PHONE) {
                    ToneDeltaPair(
                        roleA = errorContainer(),
                        roleB = error(),
                        delta = 5.0,
                        polarity = TonePolarity.RELATIVE_LIGHTER,
                        deltaConstraint = ToneDeltaPair.DeltaConstraint.FARTHER,
                    )
                } else {
                    null
                }
            },
        )
        return super.error().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun errorDim(): DynamicColor {
        // Remapped to error in 2026 spec.
        val color2026 = error().toBuilder().setName("error_dim").build()
        return super.errorDim()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onError(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_error",
            palette = { it.errorPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { error() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onError().extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun errorContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "error_container",
            palette = { it.errorPalette },
            tone = { if (it.isDark) tMinC(it.errorPalette) else tMaxC(it.errorPalette) },
            isBackground = true,
            background = { highestSurface(it) },
            contrastCurve = { if (it.contrastLevel > 0) getContrastCurve(1.5) else null },
        )
        return super.errorContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }

    override fun onErrorContainer(): DynamicColor {
        val color2026 = DynamicColor(
            name = "on_error_container",
            palette = { it.errorPalette },
            tone = { 0.0 },
            isBackground = false,
            background = { errorContainer() },
            contrastCurve = { getContrastCurve(6.0) },
        )
        return super.onErrorContainer()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2026, color2026)
    }
}
