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
package com.materialkolor.dynamiccolor

import com.materialkolor.dislike.DislikeAnalyzer
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.Variant
import dev.drewhamilton.poko.Poko
import kotlin.math.abs
import kotlin.math.max

/**
 * Named colors, otherwise known as tokens, or roles, in the Material Design system.
 *
 * @param[isExtendedFidelity] Whether to use the extended fidelity color set.
 * see [MaterialColorUtilities](https://github.com/material-foundation/material-color-utilities/commit/c3681e12b72202723657b9ce5cf8dfdf7efb0781)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Poko
public class MaterialDynamicColors(
    private val isExtendedFidelity: Boolean = false,
) {

    public fun highestSurface(scheme: DynamicScheme): DynamicColor {
        return if (scheme.isDark) surfaceBright() else surfaceDim()
    }

    // Compatibility Keys Colors for Android
    public fun primaryPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "primary_palette_key_color",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> scheme.primaryPalette.keyColor.tone },
    )

    public fun secondaryPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "secondary_palette_key_color",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme -> scheme.secondaryPalette.keyColor.tone },
    )

    public fun tertiaryPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "tertiary_palette_key_color",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme -> scheme.tertiaryPalette.keyColor.tone },
    )

    public fun errorPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "error_palette_key_color",
        palette = { scheme -> scheme.errorPalette },
        tone = { scheme -> scheme.errorPalette.keyColor.tone },
    )

    public fun neutralPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "neutral_palette_key_color",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> scheme.neutralPalette.keyColor.tone },
    )

    public fun neutralVariantPaletteKeyColor(): DynamicColor = DynamicColor.fromPalette(
        name = "neutral_variant_palette_key_color",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> scheme.neutralVariantPalette.keyColor.tone },
    )

    public fun background(): DynamicColor = DynamicColor(
        name = "background",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 6.0 else 98.0 },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun onBackground(): DynamicColor = DynamicColor(
        name = "on_background",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 90.0 else 10.0 },
        isBackground = false,
        background = { background() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 3.0, 4.5, 7.0),
        toneDeltaPair = null,
    )

    public fun surface(): DynamicColor = DynamicColor(
        name = "surface",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 6.0 else 98.0 },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceDim(): DynamicColor = DynamicColor(
        name = "surface_dim",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) 6.0
            else ContrastCurve(87.0, 87.0, 80.0, 75.0).get(scheme.contrastLevel)
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceBright(): DynamicColor = DynamicColor(
        name = "surface_bright",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(24.0, 24.0, 29.0, 34.0).get(scheme.contrastLevel)
            else 98.0
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceContainerLowest(): DynamicColor = DynamicColor(
        name = "surface_container_lowest",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(4.0, 4.0, 2.0, 0.0).get(scheme.contrastLevel)
            else 100.0
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceContainerLow(): DynamicColor = DynamicColor(
        name = "surface_container_low",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(10.0, 10.0, 11.0, 12.0).get(scheme.contrastLevel)
            else ContrastCurve(96.0, 96.0, 96.0, 95.0).get(scheme.contrastLevel)
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceContainer(): DynamicColor = DynamicColor(
        name = "surface_container",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(12.0, 12.0, 16.0, 20.0).get(scheme.contrastLevel)
            else ContrastCurve(94.0, 94.0, 92.0, 90.0).get(scheme.contrastLevel)
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceContainerHigh(): DynamicColor = DynamicColor(
        name = "surface_container_high",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(17.0, 17.0, 21.0, 25.0).get(scheme.contrastLevel)
            else ContrastCurve(92.0, 92.0, 88.0, 85.0).get(scheme.contrastLevel)
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceContainerHighest(): DynamicColor = DynamicColor(
        name = "surface_container_highest",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme ->
            if (scheme.isDark) ContrastCurve(22.0, 22.0, 26.0, 30.0).get(scheme.contrastLevel)
            else ContrastCurve(90.0, 90.0, 84.0, 80.0).get(scheme.contrastLevel)
        },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun onSurface(): DynamicColor = DynamicColor(
        name = "on_surface",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 90.0 else 10.0 },
        isBackground = false,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun surfaceVariant(): DynamicColor = DynamicColor(
        name = "surface_variant",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 30.0 else 90.0 },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun onSurfaceVariant(): DynamicColor = DynamicColor(
        name = "on_surface_variant",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 80.0 else 30.0 },
        isBackground = false,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun inverseSurface(): DynamicColor = DynamicColor(
        name = "inverse_surface",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 90.0 else 20.0 },
        isBackground = false,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun inverseOnSurface(): DynamicColor = DynamicColor(
        name = "inverse_on_surface",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 20.0 else 95.0 },
        isBackground = false,
        background = { inverseSurface() },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun outline(): DynamicColor = DynamicColor(
        name = "outline",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 60.0 else 50.0 },
        isBackground = false,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.5, 3.0, 4.5, 7.0),
        toneDeltaPair = null,
    )

    public fun outlineVariant(): DynamicColor = DynamicColor(
        name = "outline_variant",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 30.0 else 80.0 },
        isBackground = false,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = null,
    )

    public fun shadow(): DynamicColor = DynamicColor(
        name = "shadow",
        palette = { scheme -> scheme.neutralPalette },
        tone = { 0.0 },
        isBackground = false,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun scrim(): DynamicColor = DynamicColor(
        name = "scrim",
        palette = { scheme -> scheme.neutralPalette },
        tone = { 0.0 },
        isBackground = false,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun surfaceTint(): DynamicColor = DynamicColor(
        name = "surface_tint",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (scheme.isDark) 80.0 else 40.0 },
        isBackground = true,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
    )

    public fun primary(): DynamicColor = DynamicColor(
        name = "primary",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 100.0 else 0.0
                else -> if (scheme.isDark) 80.0 else 40.0
            }
        },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = primaryContainer(),
                roleB = primary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        },
    )

    public fun onPrimary(): DynamicColor = DynamicColor(
        name = "on_primary",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 10.0 else 90.0
                else -> if (scheme.isDark) 20.0 else 100.0
            }
        },
        isBackground = false,
        background = { primary() },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun primaryContainer(): DynamicColor = DynamicColor(
        name = "primary_container",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme ->
            when {
                isFidelity(scheme) -> scheme.sourceColorHct.tone
                isMonochrome(scheme) -> if (scheme.isDark) 85.0 else 25.0
                else -> if (scheme.isDark) 30.0 else 90.0
            }
        },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = primaryContainer(),
                roleB = primary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        }
    )

    public fun onPrimaryContainer(): DynamicColor = DynamicColor(
        name = "on_primary_container",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme ->
            when {
                isFidelity(scheme) -> {
                    DynamicColor.foregroundTone(primaryContainer().tone(scheme), 4.5)
                }
                isMonochrome(scheme) -> if (scheme.isDark) 0.0 else 100.0
                else -> if (scheme.isDark) 90.0 else 30.0
            }
        },
        isBackground = false,
        background = { primaryContainer() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun inversePrimary(): DynamicColor = DynamicColor(
        name = "inverse_primary",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (scheme.isDark) 40.0 else 80.0 },
        isBackground = false,
        background = { inverseSurface() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
        toneDeltaPair = null,
    )

    public fun secondary(): DynamicColor = DynamicColor(
        name = "secondary",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme -> if (scheme.isDark) 80.0 else 40.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = secondaryContainer(),
                roleB = secondary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        }
    )

    public fun onSecondary(): DynamicColor = DynamicColor(
        name = "on_secondary",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 10.0 else 100.0
                else -> if (scheme.isDark) 20.0 else 100.0
            }
        },
        isBackground = false,
        background = { secondary() },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun secondaryContainer(): DynamicColor = DynamicColor(
        name = "secondary_container",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme ->
            val initialTone = if (scheme.isDark) 30.0 else 90.0
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 30.0 else 85.0
                !isFidelity(scheme) -> initialTone
                else -> findDesiredChromaByTone(
                    hue = scheme.secondaryPalette.hue,
                    chroma = scheme.secondaryPalette.chroma,
                    tone = initialTone,
                    byDecreasingTone = !scheme.isDark
                )
            }
        },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = secondaryContainer(),
                roleB = secondary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        }
    )

    public fun onSecondaryContainer(): DynamicColor = DynamicColor(
        name = "on_secondary_container",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 90.0 else 10.0
                !isFidelity(scheme) -> if (scheme.isDark) 90.0 else 30.0
                else -> DynamicColor.foregroundTone(secondaryContainer().tone(scheme), 4.5)
            }
        },
        isBackground = false,
        background = { secondaryContainer() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun tertiary(): DynamicColor = DynamicColor(
        name = "tertiary",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 90.0 else 25.0
                else -> if (scheme.isDark) 80.0 else 40.0
            }
        },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = tertiaryContainer(),
                roleB = tertiary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        }
    )

    public fun onTertiary(): DynamicColor = DynamicColor(
        name = "on_tertiary",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 10.0 else 90.0
                else -> if (scheme.isDark) 20.0 else 100.0
            }
        },
        isBackground = false,
        background = { tertiary() },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun tertiaryContainer(): DynamicColor = DynamicColor(
        name = "tertiary_container",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 60.0 else 49.0
                !isFidelity(scheme) -> if (scheme.isDark) 30.0 else 90.0
                else -> {
                    val proposedHct = scheme.tertiaryPalette.getHct(scheme.sourceColorHct.tone)
                    DislikeAnalyzer.fixIfDisliked(proposedHct).tone
                }
            }
        },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = tertiaryContainer(),
                roleB = tertiary(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        }
    )

    public fun onTertiaryContainer(): DynamicColor = DynamicColor(
        name = "on_tertiary_container",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 0.0 else 100.0
                !isFidelity(scheme) -> if (scheme.isDark) 90.0 else 30.0
                else -> DynamicColor.foregroundTone(tertiaryContainer().tone(scheme), 4.5)
            }
        },
        isBackground = false,
        background = { tertiaryContainer() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun error(): DynamicColor = DynamicColor(
        name = "error",
        palette = { scheme -> scheme.errorPalette },
        tone = { scheme -> if (scheme.isDark) 80.0 else 40.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = errorContainer(),
                roleB = error(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        },
    )

    public fun onError(): DynamicColor = DynamicColor(
        name = "on_error",
        palette = { scheme -> scheme.errorPalette },
        tone = { scheme -> if (scheme.isDark) 20.0 else 100.0 },
        isBackground = false,
        background = { error() },
        secondBackground = null,
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun errorContainer(): DynamicColor = DynamicColor(
        name = "error_container",
        palette = { scheme -> scheme.errorPalette },
        tone = { scheme -> if (scheme.isDark) 30.0 else 90.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = errorContainer(),
                roleB = error(),
                delta = 10.0,
                polarity = TonePolarity.NEARER,
                stayTogether = false,
            )
        },
    )

    public fun onErrorContainer(): DynamicColor = DynamicColor(
        name = "on_error_container",
        palette = { scheme -> scheme.errorPalette },
        tone = { scheme ->
            when {
                isMonochrome(scheme) -> if (scheme.isDark) 90.0 else 10.0
                else -> if (scheme.isDark) 90.0 else 10.0
            }
        },
        isBackground = false,
        background = { errorContainer() },
        secondBackground = null,
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun primaryFixed(): DynamicColor = DynamicColor(
        name = "primary_fixed",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 40.0 else 90.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = primaryFixed(),
                roleB = primaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        }
    )

    public fun primaryFixedDim(): DynamicColor = DynamicColor(
        name = "primary_fixed_dim",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 30.0 else 80.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = primaryFixed(),
                roleB = primaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        }
    )

    public fun onPrimaryFixed(): DynamicColor = DynamicColor(
        name = "on_primary_fixed",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 100.0 else 10.0 },
        isBackground = false,
        background = { primaryFixedDim() },
        secondBackground = { primaryFixed() },
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun onPrimaryFixedVariant(): DynamicColor = DynamicColor(
        name = "on_primary_fixed_variant",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 90.0 else 30.0 },
        isBackground = false,
        background = { primaryFixedDim() },
        secondBackground = { primaryFixed() },
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun secondaryFixed(): DynamicColor = DynamicColor(
        name = "secondary_fixed",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 80.0 else 90.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = secondaryFixed(),
                roleB = secondaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        },
    )

    public fun secondaryFixedDim(): DynamicColor = DynamicColor(
        name = "secondary_fixed_dim",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 70.0 else 80.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = secondaryFixed(),
                roleB = secondaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        }
    )

    public fun onSecondaryFixed(): DynamicColor = DynamicColor(
        name = "on_secondary_fixed",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { 10.0 },
        isBackground = false,
        background = { secondaryFixedDim() },
        secondBackground = { secondaryFixed() },
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun onSecondaryFixedVariant(): DynamicColor = DynamicColor(
        name = "on_secondary_fixed_variant",
        palette = { scheme -> scheme.secondaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 25.0 else 30.0 },
        isBackground = false,
        background = { secondaryFixedDim() },
        secondBackground = { secondaryFixed() },
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    public fun tertiaryFixed(): DynamicColor = DynamicColor(
        name = "tertiary_fixed",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 40.0 else 90.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = tertiaryFixed(),
                roleB = tertiaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        },
    )

    public fun tertiaryFixedDim(): DynamicColor = DynamicColor(
        name = "tertiary_fixed_dim",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 30.0 else 80.0 },
        isBackground = true,
        background = { scheme -> highestSurface(scheme) },
        secondBackground = null,
        contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
        toneDeltaPair = {
            ToneDeltaPair(
                roleA = tertiaryFixed(),
                roleB = tertiaryFixedDim(),
                delta = 10.0,
                polarity = TonePolarity.LIGHTER,
                stayTogether = true,
            )
        },
    )

    public fun onTertiaryFixed(): DynamicColor = DynamicColor(
        name = "on_tertiary_fixed",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 100.0 else 10.0 },
        isBackground = false,
        background = { tertiaryFixedDim() },
        secondBackground = { tertiaryFixed() },
        contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
        toneDeltaPair = null,
    )

    public fun onTertiaryFixedVariant(): DynamicColor = DynamicColor(
        name = "on_tertiary_fixed_variant",
        palette = { scheme -> scheme.tertiaryPalette },
        tone = { scheme -> if (isMonochrome(scheme)) 90.0 else 30.0 },
        isBackground = false,
        background = { tertiaryFixedDim() },
        secondBackground = { tertiaryFixed() },
        contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
        toneDeltaPair = null,
    )

    /**
     * These colors were present in Android framework before Android U, and used by MDC controls. They
     * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
     * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
     * and those won't adjust for contrast, avoiding issues.
     *
     * colorControlActivated documented as colorAccent in M3 & GM3.
     * colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
     * Android used Material's Container as Primary/Secondary/Tertiary at launch.
     * Therefore, this is a duplicated version of Primary Container.
     *
     * For example, if the same color is on a white background _and_ black background, there's no
     * way to increase contrast with either without losing contrast with the other.
     */
    public fun controlActivated(): DynamicColor = DynamicColor.fromPalette(
        name = "control_activated",
        palette = { scheme -> scheme.primaryPalette },
        tone = { scheme -> if (scheme.isDark) 30.0 else 90.0 },
    )

    /**
     * colorControlNormal documented as textColorSecondary in M3 & GM3.
     * In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
     * which is Neutral Variant T30/80 in light/dark.
     */
    public fun controlNormal(): DynamicColor = DynamicColor.fromPalette(
        name = "control_normal",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 80.0 else 30.0 },
    )

    /**
     * colorControlHighlight documented, in both M3 & GM3:
     *
     * Light mode: #1f000000 dark mode: #33ffffff.
     *
     * These are black and white with some alpha.
     * 1F hex = 31 decimal; 31 / 255 = 12% alpha.
     * 33 hex = 51 decimal; 51 / 255 = 20% alpha.
     *
     * DynamicColors do not support alpha currently, and _may_ not need it for this use case,
     * depending on how MDC resolved alpha for the other cases.
     * Returning black in dark mode, white in light mode.
     */
    public fun controlHighlight(): DynamicColor = DynamicColor(
        name = "control_highlight",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 100.0 else 0.0 },
        isBackground = false,
        background = null,
        secondBackground = null,
        contrastCurve = null,
        toneDeltaPair = null,
        opacity = { scheme -> if (scheme.isDark) 0.20 else 0.12 })

    /**
     * textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
     */
    public fun textPrimaryInverse(): DynamicColor = DynamicColor.fromPalette(
        name = "text_primary_inverse",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 10.0 else 90.0 },
    )

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
     * V30/NV80
     */
    public fun textSecondaryAndTertiaryInverse(): DynamicColor = DynamicColor.fromPalette(
        name = "text_secondary_and_tertiary_inverse",
        palette = { scheme -> scheme.neutralVariantPalette },
        tone = { scheme -> if (scheme.isDark) 30.0 else 80.0 },
    )

    /**
     * textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
     */
    public fun textPrimaryInverseDisableOnly(): DynamicColor = DynamicColor.fromPalette(
        name = "text_primary_inverse_disable_only",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 10.0 else 90.0 },
    )

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
     * in both M3 & GM3, as N10/N90
     */
    public fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor = DynamicColor.fromPalette(
        name = "text_secondary_and_tertiary_inverse_disabled",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 10.0 else 90.0 },
    )

    /**
     * textColorHintInverse documented, in both M3 & GM3, as N10/N90
     */
    public fun textHintInverse(): DynamicColor = DynamicColor.fromPalette(
        name = "text_hint_inverse",
        palette = { scheme -> scheme.neutralPalette },
        tone = { scheme -> if (scheme.isDark) 10.0 else 90.0 },
    )

    private fun isFidelity(scheme: DynamicScheme): Boolean {
        if (isExtendedFidelity
            && scheme.variant != Variant.MONOCHROME
            && scheme.variant != Variant.NEUTRAL
        ) return true

        return scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT
    }

    public companion object {

        private fun isMonochrome(scheme: DynamicScheme): Boolean {
            return scheme.variant === Variant.MONOCHROME
        }

        private fun findDesiredChromaByTone(
            hue: Double,
            chroma: Double,
            tone: Double,
            byDecreasingTone: Boolean,
        ): Double {
            var answer = tone
            var closestToChroma = Hct.from(hue, chroma, tone)
            if (closestToChroma.chroma < chroma) {
                var chromaPeak = closestToChroma.chroma
                while (closestToChroma.chroma < chroma) {
                    answer += if (byDecreasingTone) -1.0 else 1.0
                    val potentialSolution = Hct.from(hue, chroma, answer)
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
