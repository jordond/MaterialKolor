/*
 * Copyright 2025 Google LLC
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

import com.materialkolor.contrast.Contrast
import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.DynamicScheme.Platform
import com.materialkolor.scheme.Variant
import com.materialkolor.temperature.TemperatureCache
import com.materialkolor.utils.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * [ColorSpec] implementation for the 2021 spec.
 */
public open class ColorSpec2021 : ColorSpec {
    // Main Palettes

    override fun primaryPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary_palette_key_color")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> s.primaryPalette.keyColor.tone }
            .build()

    override fun secondaryPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary_palette_key_color")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> s.secondaryPalette.keyColor.tone }
            .build()

    override fun tertiaryPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary_palette_key_color")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s -> s.tertiaryPalette.keyColor.tone }
            .build()

    override fun neutralPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("neutral_palette_key_color")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> s.neutralPalette.keyColor.tone }
            .build()

    override fun neutralVariantPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("neutral_variant_palette_key_color")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> s.neutralVariantPalette.keyColor.tone }
            .build()

    override fun errorPaletteKeyColor(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("error_palette_key_color")
            .setPalette { s -> s.errorPalette }
            .setTone { s -> s.errorPalette.keyColor.tone }
            .build()

    // Surfaces [S]

    override fun background(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("background")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 6.0 else 98.0 }
            .setIsBackground(true)
            .build()

    override fun onBackground(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_background")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 90.0 else 10.0 }
            .setBackground { s -> background() }
            .setContrastCurve { s -> ContrastCurve(3.0, 3.0, 4.5, 7.0) }
            .build()

    override fun surface(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 6.0 else 98.0 }
            .setIsBackground(true)
            .build()

    override fun surfaceDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_dim")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    6.0
                } else {
                    ContrastCurve(
                        low = 87.0,
                        normal = 87.0,
                        medium = 80.0,
                        high = 75.0,
                    ).get(s.contrastLevel)
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceBright(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_bright")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(
                        low = 24.0,
                        normal = 24.0,
                        medium = 29.0,
                        high = 34.0,
                    ).get(s.contrastLevel)
                } else {
                    98.0
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceContainerLowest(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_container_lowest")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(
                        low = 4.0,
                        normal = 4.0,
                        medium = 2.0,
                        high = 0.0,
                    ).get(s.contrastLevel)
                } else {
                    100.0
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceContainerLow(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_container_low")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(10.0, 10.0, 11.0, 12.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(96.0, 96.0, 96.0, 95.0).get(s.contrastLevel)
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_container")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(12.0, 12.0, 16.0, 20.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(94.0, 94.0, 92.0, 90.0).get(s.contrastLevel)
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceContainerHigh(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_container_high")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(17.0, 17.0, 21.0, 25.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(92.0, 92.0, 88.0, 85.0).get(s.contrastLevel)
                }
            }.setIsBackground(true)
            .build()

    override fun surfaceContainerHighest(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_container_highest")
            .setPalette { s -> s.neutralPalette }
            .setTone { s ->
                if (s.isDark) {
                    ContrastCurve(22.0, 22.0, 26.0, 30.0).get(s.contrastLevel)
                } else {
                    ContrastCurve(90.0, 90.0, 84.0, 80.0).get(s.contrastLevel)
                }
            }.setIsBackground(true)
            .build()

    override fun onSurface(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_surface")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 90.0 else 10.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun surfaceVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_variant")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .build()

    override fun onSurfaceVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_surface_variant")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 80.0 else 30.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    override fun inverseSurface(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("inverse_surface")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 90.0 else 20.0 }
            .setIsBackground(true)
            .build()

    override fun inverseOnSurface(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("inverse_on_surface")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 20.0 else 95.0 }
            .setBackground { s -> inverseSurface() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun outline(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("outline")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 60.0 else 50.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.5, 3.0, 4.5, 7.0) }
            .build()

    override fun outlineVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("outline_variant")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 30.0 else 80.0 }
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .build()

    override fun shadow(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("shadow")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> 0.0 }
            .build()

    override fun scrim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("scrim")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> 0.0 }
            .build()

    override fun surfaceTint(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("surface_tint")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .build()

    // Primaries [P]

    override fun primary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary")
            .setPalette { s -> s.primaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 100.0 else 0.0
                }
                if (s.isDark) 80.0 else 40.0
            }.setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = primaryContainer(),
                    roleB = primary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun primaryDim(): DynamicColor? = null

    override fun onPrimary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_primary")
            .setPalette { s -> s.primaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 10.0 else 90.0
                }
                if (s.isDark) 20.0 else 100.0
            }.setBackground { s -> primary() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun primaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary_container")
            .setPalette { s -> s.primaryPalette }
            .setTone { s ->
                if (isFidelity(s)) {
                    return@setTone s.sourceColorHct.tone
                }
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 85.0 else 25.0
                }
                if (s.isDark) 30.0 else 90.0
            }.setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = primaryContainer(),
                    roleB = primary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun onPrimaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_primary_container")
            .setPalette { s -> s.primaryPalette }
            .setTone { s ->
                if (isFidelity(s)) {
                    return@setTone DynamicColor.foregroundTone(
                        bgTone = primaryContainer().tone(s),
                        ratio = 4.5,
                    )
                }
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 0.0 else 100.0
                }
                if (s.isDark) 90.0 else 30.0
            }.setBackground { s -> primaryContainer() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    override fun inversePrimary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("inverse_primary")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (s.isDark) 40.0 else 80.0 }
            .setBackground { s -> inverseSurface() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .build()

    // Secondaries [Q]

    override fun secondary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = secondaryContainer(),
                    roleB = secondary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun secondaryDim(): DynamicColor? = null

    override fun onSecondary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_secondary")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 10.0 else 100.0
                }
                if (s.isDark) 20.0 else 100.0
            }.setBackground { s -> secondary() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun secondaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary_container")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s ->
                val initialTone = if (s.isDark) 30.0 else 90.0
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 30.0 else 85.0
                }
                if (!isFidelity(s)) {
                    return@setTone initialTone
                }
                findDesiredChromaByTone(
                    hue = s.secondaryPalette.hue,
                    chroma = s.secondaryPalette.chroma,
                    tone = initialTone,
                    byDecreasingTone = !s.isDark,
                )
            }.setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = secondaryContainer(),
                    roleB = secondary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun onSecondaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_secondary_container")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 90.0 else 10.0
                }
                if (!isFidelity(s)) {
                    return@setTone if (s.isDark) 90.0 else 30.0
                }
                DynamicColor.foregroundTone(secondaryContainer().tone(s), 4.5)
            }.setBackground { s -> secondaryContainer() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    // Tertiaries [T]

    override fun tertiary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 90.0 else 25.0
                }
                if (s.isDark) 80.0 else 40.0
            }.setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = tertiaryContainer(),
                    roleB = tertiary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun tertiaryDim(): DynamicColor? = null

    override fun onTertiary(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_tertiary")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 10.0 else 90.0
                }
                if (s.isDark) 20.0 else 100.0
            }.setBackground { s -> tertiary() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun tertiaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary_container")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 60.0 else 49.0
                }
                if (!isFidelity(s)) {
                    return@setTone if (s.isDark) 30.0 else 90.0
                }
                val proposedHct = s.tertiaryPalette.getHct(s.sourceColorHct.tone)
                DislikeAnalyzer.fixIfDisliked(proposedHct).tone
            }.setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = tertiaryContainer(),
                    roleB = tertiary(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun onTertiaryContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_tertiary_container")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 0.0 else 100.0
                }
                if (!isFidelity(s)) {
                    return@setTone if (s.isDark) 90.0 else 30.0
                }
                DynamicColor.foregroundTone(tertiaryContainer().tone(s), 4.5)
            }.setBackground { s -> tertiaryContainer() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    // Errors [E]

    override fun error(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("error")
            .setPalette { s -> s.errorPalette }
            .setTone { s -> if (s.isDark) 80.0 else 40.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 7.0) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = errorContainer(),
                    roleB = error(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun errorDim(): DynamicColor? = null

    override fun onError(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_error")
            .setPalette { s -> s.errorPalette }
            .setTone { s -> if (s.isDark) 20.0 else 100.0 }
            .setBackground { s -> error() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun errorContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("error_container")
            .setPalette { s -> s.errorPalette }
            .setTone { s -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                @Suppress("DEPRECATION")
                ToneDeltaPair(
                    roleA = errorContainer(),
                    roleB = error(),
                    delta = 10.0,
                    polarity = TonePolarity.NEARER,
                    stayTogether = false,
                )
            }.build()

    override fun onErrorContainer(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_error_container")
            .setPalette { s -> s.errorPalette }
            .setTone { s ->
                if (isMonochrome(s)) {
                    return@setTone if (s.isDark) 90.0 else 10.0
                }
                if (s.isDark) 90.0 else 30.0
            }.setBackground { s -> errorContainer() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    // Primary Fixed Colors [PF]

    override fun primaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary_fixed")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (isMonochrome(s)) 40.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = this.primaryFixed(),
                    roleB = this.primaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun primaryFixedDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary_fixed_dim")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (isMonochrome(s)) 30.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = primaryFixed(),
                    roleB = primaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun onPrimaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_primary_fixed")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (isMonochrome(s)) 100.0 else 10.0 }
            .setBackground { s -> primaryFixedDim() }
            .setSecondBackground { s -> primaryFixed() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun onPrimaryFixedVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_primary_fixed_variant")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (isMonochrome(s)) 90.0 else 30.0 }
            .setBackground { s -> primaryFixedDim() }
            .setSecondBackground { s -> primaryFixed() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    // Secondary Fixed Colors [QF]

    override fun secondaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary_fixed")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> if (isMonochrome(s)) 80.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = secondaryFixed(),
                    roleB = secondaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun secondaryFixedDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary_fixed_dim")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> if (isMonochrome(s)) 70.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = secondaryFixed(),
                    roleB = secondaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun onSecondaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_secondary_fixed")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> 10.0 }
            .setBackground { s -> secondaryFixedDim() }
            .setSecondBackground { s -> secondaryFixed() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun onSecondaryFixedVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_secondary_fixed_variant")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s -> if (isMonochrome(s)) 25.0 else 30.0 }
            .setBackground { s -> secondaryFixedDim() }
            .setSecondBackground { s -> secondaryFixed() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    // Tertiary Fixed Colors [TF]

    override fun tertiaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary_fixed")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s -> if (isMonochrome(s)) 40.0 else 90.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = tertiaryFixed(),
                    roleB = tertiaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun tertiaryFixedDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary_fixed_dim")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s -> if (isMonochrome(s)) 30.0 else 80.0 }
            .setIsBackground(true)
            .setBackground(this::highestSurface)
            .setContrastCurve { s -> ContrastCurve(1.0, 1.0, 3.0, 4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = tertiaryFixed(),
                    roleB = tertiaryFixedDim(),
                    delta = 10.0,
                    polarity = TonePolarity.LIGHTER,
                    stayTogether = true,
                )
            }.build()

    override fun onTertiaryFixed(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_tertiary_fixed")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s -> if (isMonochrome(s)) 100.0 else 10.0 }
            .setBackground { s -> tertiaryFixedDim() }
            .setSecondBackground { s -> tertiaryFixed() }
            .setContrastCurve { s -> ContrastCurve(4.5, 7.0, 11.0, 21.0) }
            .build()

    override fun onTertiaryFixedVariant(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("on_tertiary_fixed_variant")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s -> if (isMonochrome(s)) 90.0 else 30.0 }
            .setBackground { s -> tertiaryFixedDim() }
            .setSecondBackground { s -> tertiaryFixed() }
            .setContrastCurve { s -> ContrastCurve(3.0, 4.5, 7.0, 11.0) }
            .build()

    /*
     * Android-only Colors
     * These colors were present in Android framework before Android U, and used by MDC controls. They
     * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
     * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
     * and those won't adjust for contrast, avoiding issues.
     *
     *
     * For example, if the same color is on a white background _and_ black background, there's no
     * way to increase contrast with either without losing contrast with the other.
     */

    /**
     * colorControlActivated documented as colorAccent in M3 & GM3.
     * colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
     * Android used Material's Container as Primary/Secondary/Tertiary at launch.
     * Therefore, this is a duplicated version of Primary Container.
     */
    override fun controlActivated(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("control_activated")
            .setPalette { s -> s.primaryPalette }
            .setTone { s -> if (s.isDark) 30.0 else 90.0 }
            .setIsBackground(true)
            .build()

    /**
     * colorControlNormal documented as textColorSecondary in M3 & GM3.
     * In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
     * which is Neutral Variant T30/80 in light/dark.
     */
    override fun controlNormal(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("control_normal")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 80.0 else 30.0 }
            .build()

    /**
     * colorControlHighlight documented, in both M3 & GM3:
     * Light mode: #1f000000 dark mode: #33ffffff.
     * These are black and white with some alpha.
     * 1F hex = 31 decimal; 31 / 255 = 12% alpha.
     * 33 hex = 51 decimal; 51 / 255 = 20% alpha.
     * DynamicColors do not support alpha currently, and _may_ not need it for this use case,
     * depending on how MDC resolved alpha for the other cases.
     * Returning black in dark mode, white in light mode.
     */
    override fun controlHighlight(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("control_highlight")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 100.0 else 0.0 }
            .setOpacity { s -> if (s.isDark) 0.20 else 0.12 }
            .build()

    /**
     * textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
     */
    override fun textPrimaryInverse(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("text_primary_inverse")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 10.0 else 90.0 }
            .build()

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
     * NV30/NV80
     */
    override fun textSecondaryAndTertiaryInverse(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("text_secondary_and_tertiary_inverse")
            .setPalette { s -> s.neutralVariantPalette }
            .setTone { s -> if (s.isDark) 30.0 else 80.0 }
            .build()

    /**
     * textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
     */
    override fun textPrimaryInverseDisableOnly(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("text_primary_inverse_disable_only")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 10.0 else 90.0 }
            .build()

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
     * in both M3 & GM3, as N10/N90
     */
    override fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("text_secondary_and_tertiary_inverse_disabled")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 10.0 else 90.0 }
            .build()

    /**
     * textColorHintInverse documented, in both M3 & GM3, as N10/N90
     */
    override fun textHintInverse(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("text_hint_inverse")
            .setPalette { s -> s.neutralPalette }
            .setTone { s -> if (s.isDark) 10.0 else 90.0 }
            .build()

    // Other

    override fun highestSurface(s: DynamicScheme): DynamicColor =
        if (s.isDark) surfaceBright() else surfaceDim()

    private fun isFidelity(scheme: DynamicScheme): Boolean =
        scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT

    // Color value calculations

    override fun getHct(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Hct {
        // This is crucial for aesthetics: we aren't simply the taking the standard color
        // and changing its tone for contrast. Rather, we find the tone for contrast, then
        // use the specified chroma from the palette to construct a new color.
        //
        // For example, this enables colors with standard tone of T90, which has limited chroma, to
        // "recover" intended chroma as contrast increases.
        val tone = getTone(scheme, color)
        return color.palette.invoke(scheme).getHct(tone)
    }

    override fun getTone(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Double {
        val decreasingContrast = scheme.contrastLevel < 0
        val toneDeltaPair: ToneDeltaPair? =
            if (color.toneDeltaPair == null) null else color.toneDeltaPair(scheme)

        // Case 1: dual foreground, pair of colors with delta constraint.
        if (toneDeltaPair != null) {
            val roleA = toneDeltaPair.roleA
            val roleB = toneDeltaPair.roleB
            val delta = toneDeltaPair.delta
            val polarity = toneDeltaPair.polarity
            val stayTogether = toneDeltaPair.stayTogether

            @Suppress("DEPRECATION")
            val aIsNearer =
                (
                    polarity == TonePolarity.NEARER ||
                        (polarity == TonePolarity.LIGHTER && !scheme.isDark) ||
                        (polarity == TonePolarity.DARKER && !scheme.isDark)
                )
            val nearer = if (aIsNearer) roleA else roleB
            val farther = if (aIsNearer) roleB else roleA
            val amNearer = color.name == nearer.name
            val expansionDir = (if (scheme.isDark) 1 else -1).toDouble()
            var nTone: Double = nearer.tone(scheme)
            var fTone: Double = farther.tone(scheme)

            // 1st round: solve to min, each
            if (color.background != null && nearer.contrastCurve != null && farther.contrastCurve != null) {
                val bg: DynamicColor? = color.background(scheme)
                val nContrastCurve: ContrastCurve? = nearer.contrastCurve(scheme)
                val fContrastCurve: ContrastCurve? = farther.contrastCurve(scheme)
                if (bg != null && nContrastCurve != null && fContrastCurve != null) {
                    val nContrast = nContrastCurve.get(scheme.contrastLevel)
                    val fContrast = fContrastCurve.get(scheme.contrastLevel)
                    val bgTone = bg.getTone(scheme)

                    // If a color is good enough, it is not adjusted.
                    // Initial and adjusted tones for `nearer`
                    if (Contrast.ratioOfTones(bgTone, nTone) < nContrast) {
                        nTone = DynamicColor.foregroundTone(bgTone, nContrast)
                    }
                    // Initial and adjusted tones for `farther`
                    if (Contrast.ratioOfTones(bgTone, fTone) < fContrast) {
                        fTone = DynamicColor.foregroundTone(bgTone, fContrast)
                    }

                    if (decreasingContrast) {
                        // If decreasing contrast, adjust color to the "bare minimum"
                        // that satisfies contrast.
                        nTone = DynamicColor.foregroundTone(bgTone, nContrast)
                        fTone = DynamicColor.foregroundTone(bgTone, fContrast)
                    }
                }
            }

            // If constraint is not satisfied, try another round.
            if ((fTone - nTone) * expansionDir < delta) {
                // 2nd round: expand farther to match delta.
                fTone = (nTone + delta * expansionDir).coerceIn(0.0, 100.0)
                // If constraint is not satisfied, try another round.
                if ((fTone - nTone) * expansionDir < delta) {
                    // 3rd round: contract nearer to match delta.
                    nTone = (fTone - delta * expansionDir).coerceIn(0.0, 100.0)
                }
            }

            // Avoids the 50-59 awkward zone.
            if (50 <= nTone && nTone < 60) {
                // If `nearer` is in the awkward zone, move it away, together with
                // `farther`.
                if (expansionDir > 0) {
                    nTone = 60.0
                    fTone = max(fTone, nTone + delta * expansionDir)
                } else {
                    nTone = 49.0
                    fTone = min(fTone, nTone + delta * expansionDir)
                }
            } else if (50 <= fTone && fTone < 60) {
                if (stayTogether) {
                    // Fixes both, to avoid two colors on opposite sides of the "awkward
                    // zone".
                    if (expansionDir > 0) {
                        nTone = 60.0
                        fTone = max(fTone, nTone + delta * expansionDir)
                    } else {
                        nTone = 49.0
                        fTone = min(fTone, nTone + delta * expansionDir)
                    }
                } else {
                    // Not required to stay together; fixes just one.
                    fTone = if (expansionDir > 0) {
                        60.0
                    } else {
                        49.0
                    }
                }
            }

            // Returns `nTone` if this color is `nearer`, otherwise `fTone`.
            return if (amNearer) nTone else fTone
        } else {
            // Case 2: No contrast pair; just solve for itself.
            var answer: Double = color.tone(scheme)

            if (color.background == null || color.contrastCurve == null) {
                return answer // No adjustment for colors with no background.
            }

            val bgTone = color.background(scheme)?.getTone(scheme) ?: 0.0
            val desiredRatio = color.contrastCurve(scheme)?.get(scheme.contrastLevel) ?: 0.0

            if (Contrast.ratioOfTones(bgTone, answer) >= desiredRatio) {
                // Don't "improve" what's good enough.
            } else {
                // Rough improvement.
                answer = DynamicColor.foregroundTone(bgTone, desiredRatio)
            }

            if (decreasingContrast) {
                answer = DynamicColor.foregroundTone(bgTone, desiredRatio)
            }

            if (color.isBackground && 50 <= answer && answer < 60) {
                // Must adjust
                answer = if (Contrast.ratioOfTones(49.0, bgTone) >= desiredRatio) {
                    49.0
                } else {
                    60.0
                }
            }

            if (color.secondBackground == null) {
                return answer
            }

            // Case 3: Adjust for dual backgrounds.
            val bgTone1 = color.background(scheme)?.getTone(scheme) ?: 0.0
            val bgTone2 = color.secondBackground(scheme)?.getTone(scheme) ?: 0.0

            val upper: Double = max(bgTone1, bgTone2)
            val lower: Double = min(bgTone1, bgTone2)

            if (Contrast.ratioOfTones(upper, answer) >= desiredRatio &&
                Contrast.ratioOfTones(lower, answer) >= desiredRatio
            ) {
                return answer
            }

            // The darkest light tone that satisfies the desired ratio,
            // or -1 if such ratio cannot be reached.
            val lightOption: Double = Contrast.lighter(upper, desiredRatio)

            // The lightest dark tone that satisfies the desired ratio,
            // or -1 if such ratio cannot be reached.
            val darkOption: Double = Contrast.darker(lower, desiredRatio)

            // Tones suitable for the foreground.
            val availables = mutableListOf<Double>()
            if (lightOption != -1.0) {
                availables.add(lightOption)
            }
            if (darkOption != -1.0) {
                availables.add(darkOption)
            }

            val prefersLight =
                DynamicColor.tonePrefersLightForeground(bgTone1) ||
                    DynamicColor.tonePrefersLightForeground(bgTone2)

            if (prefersLight) {
                return if (lightOption == -1.0) 100.0 else lightOption
            }

            if (availables.size == 1) {
                return availables.first()
            }
            return if (darkOption == -1.0) 0.0 else darkOption
        }
    }

    // Scheme Palettes

    public override fun getPrimaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY,
            -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = sourceColorHct.chroma,
            )
            Variant.FRUIT_SALAD -> TonalPalette.fromHueAndChroma(
                hue = MathUtils.sanitizeDegrees(sourceColorHct.hue - 50.0),
                chroma = 48.0,
            )
            Variant.MONOCHROME -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 12.0)
            Variant.RAINBOW -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 48.0)
            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 36.0)
            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 240),
                chroma = 40.0,
            )
            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 200.0)
        }

    public override fun getSecondaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette {
        when (variant) {
            Variant.CONTENT, Variant.FIDELITY -> return TonalPalette.fromHueAndChroma(
                sourceColorHct.hue,
                max(sourceColorHct.chroma - 32.0, sourceColorHct.chroma * 0.5),
            )

            Variant.FRUIT_SALAD -> return TonalPalette.fromHueAndChroma(
                MathUtils.sanitizeDegrees(sourceColorHct.hue - 50.0),
                36.0,
            )

            Variant.MONOCHROME -> return TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> return TonalPalette.fromHueAndChroma(sourceColorHct.hue, 8.0)
            Variant.RAINBOW -> return TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.TONAL_SPOT -> return TonalPalette.fromHueAndChroma(
                sourceColorHct.hue,
                16.0,
            )

            Variant.EXPRESSIVE -> return TonalPalette.fromHueAndChroma(
                DynamicScheme.getRotatedHue(
                    sourceColorHct,
                    doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0),
                    doubleArrayOf(45.0, 95.0, 45.0, 20.0, 45.0, 90.0, 45.0, 45.0, 45.0),
                ),
                24.0,
            )

            Variant.VIBRANT -> return TonalPalette.fromHueAndChroma(
                DynamicScheme.getRotatedHue(
                    sourceColorHct,
                    doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0),
                    doubleArrayOf(18.0, 15.0, 10.0, 12.0, 15.0, 18.0, 15.0, 12.0, 12.0),
                ),
                24.0,
            )
        }
    }

    public override fun getTertiaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.CONTENT -> TonalPalette.fromHct(
                DislikeAnalyzer.fixIfDisliked(
                    TemperatureCache(sourceColorHct).getAnalogousColors(count = 3, divisions = 6)[2],
                ),
            )
            Variant.FIDELITY -> TonalPalette.fromHct(
                DislikeAnalyzer.fixIfDisliked(TemperatureCache(sourceColorHct).complement),
            )
            Variant.FRUIT_SALAD -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = 36.0,
            )
            Variant.MONOCHROME -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.RAINBOW, Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 60.0),
                chroma = 24.0,
            )
            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0),
                    rotations = doubleArrayOf(120.0, 120.0, 20.0, 45.0, 20.0, 15.0, 20.0, 120.0, 120.0),
                ),
                chroma = 32.0,
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0),
                    rotations = doubleArrayOf(35.0, 30.0, 20.0, 25.0, 30.0, 35.0, 30.0, 25.0, 25.0),
                ),
                chroma = 32.0,
            )
        }

    public override fun getNeutralPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.CONTENT, Variant.FIDELITY -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = sourceColorHct.chroma / 8.0,
            )
            Variant.FRUIT_SALAD -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 10.0)
            Variant.MONOCHROME -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 2.0)
            Variant.RAINBOW -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 6.0)
            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 15),
                chroma = 8.0,
            )
            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 10.0)
        }

    public override fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.CONTENT -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = (sourceColorHct.chroma / 8.0) + 4.0,
            )
            Variant.FIDELITY -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = (sourceColorHct.chroma / 8.0) + 4.0,
            )
            Variant.FRUIT_SALAD -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.MONOCHROME -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 2.0)
            Variant.RAINBOW -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 0.0)
            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 8.0)
            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = MathUtils.sanitizeDegrees(sourceColorHct.hue + 15),
                chroma = 12.0,
            )
            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 12.0)
        }

    public override fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette? =
        when (variant) {
            Variant.CONTENT,
            Variant.FIDELITY,
            Variant.FRUIT_SALAD,
            Variant.MONOCHROME,
            Variant.NEUTRAL,
            Variant.RAINBOW,
            Variant.TONAL_SPOT,
            Variant.EXPRESSIVE,
            Variant.VIBRANT,
            -> null
        }

    public companion object {
        private fun isMonochrome(scheme: DynamicScheme): Boolean = scheme.variant == Variant.MONOCHROME

        private fun findDesiredChromaByTone(
            hue: Double,
            chroma: Double,
            tone: Double,
            byDecreasingTone: Boolean,
        ): Double {
            var answer = tone

            var closestToChroma: Hct = Hct.from(hue, chroma, tone)
            if (closestToChroma.chroma < chroma) {
                var chromaPeak: Double = closestToChroma.chroma
                while (closestToChroma.chroma < chroma) {
                    answer += if (byDecreasingTone) -1.0 else 1.0
                    val potentialSolution: Hct = Hct.from(hue, chroma, answer)
                    if (chromaPeak > potentialSolution.chroma) {
                        break
                    }
                    if (abs(potentialSolution.chroma - chroma) < 0.4) {
                        break
                    }

                    val potentialDelta: Double = abs(potentialSolution.chroma - chroma)
                    val currentDelta: Double = abs(closestToChroma.chroma - chroma)
                    if (potentialDelta < currentDelta) {
                        closestToChroma = potentialSolution
                    }
                    chromaPeak = max(chromaPeak, potentialSolution.chroma)
                }
            }

            return answer
        }
    }
}
