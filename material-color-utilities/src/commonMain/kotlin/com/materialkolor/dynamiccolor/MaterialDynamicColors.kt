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
import kotlin.math.abs
import kotlin.math.max

/** Named colors, otherwise known as tokens, or roles, in the Material Design system.  */ // Prevent lint for Function.apply not being available on Android before API level 14 (4.0.1).
// "AndroidJdkLibsChecker" for Function, "NewApi" for Function.apply().
// A java_library Bazel rule with an Android constraint cannot skip these warnings without this
// annotation; another solution would be to create an android_library rule and supply
// AndroidManifest with an SDK set higher than 14.
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MaterialDynamicColors(
    /* See https://github.com/material-foundation/material-color-utilities/commit/c3681e12b72202723657b9ce5cf8dfdf7efb0781 */
    private val isExtendedFidelity: Boolean = false,
) {

    fun highestSurface(s: DynamicScheme): DynamicColor {
        return if (s.isDark) surfaceBright() else surfaceDim()
    }

    // Compatibility Keys Colors for Android
    fun primaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "primary_palette_key_color",
            palette = { s -> s.primaryPalette },
            tone = { s -> s.primaryPalette.keyColor.getTone() })
    }

    fun secondaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "secondary_palette_key_color",
            palette = { s -> s.secondaryPalette },
            tone = { s -> s.secondaryPalette.keyColor.getTone() })
    }

    fun tertiaryPaletteKeyColor(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "tertiary_palette_key_color",
            palette = { s -> s.tertiaryPalette },
            tone = { s -> s.tertiaryPalette.keyColor.getTone() })
    }

    fun neutralPaletteKeyColor(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "neutral_palette_key_color",
            palette = { s -> s.neutralPalette },
            tone = { s -> s.neutralPalette.keyColor.getTone() })
    }

    fun neutralVariantPaletteKeyColor(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "neutral_variant_palette_key_color",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> s.neutralVariantPalette.keyColor.getTone() })
    }

    fun background(): DynamicColor {
        return DynamicColor(
            name = "background",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 6.0 else 98.0 },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun onBackground(): DynamicColor {
        return DynamicColor(
            name = "on_background",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 90.0 else 10.0 },
            isBackground = false,
            background = { background() },
            secondBackground = null,
            contrastCurve = ContrastCurve(3.0, 3.0, 4.5, 7.0),
            toneDeltaPair = null,
        )
    }

    fun surface(): DynamicColor {
        return DynamicColor(
            name = "surface",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 6.0 else 98.0 },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceDim(): DynamicColor {
        return DynamicColor(
            name = "surface_dim",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) 6.0
                else ContrastCurve(87.0, 87.0, 80.0, 75.0).get(s.contrastLevel)
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceBright(): DynamicColor {
        return DynamicColor(
            name = "surface_bright",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(24.0, 24.0, 29.0, 34.0).get(s.contrastLevel)
                else 98.0
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceContainerLowest(): DynamicColor {
        return DynamicColor(
            name = "surface_container_lowest",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(4.0, 4.0, 2.0, 0.0).get(s.contrastLevel)
                else 100.0
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceContainerLow(): DynamicColor {
        return DynamicColor(
            name = "surface_container_low",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(10.0, 10.0, 11.0, 12.0).get(s.contrastLevel)
                else ContrastCurve(96.0, 96.0, 96.0, 95.0).get(s.contrastLevel)
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceContainer(): DynamicColor {
        return DynamicColor(
            name = "surface_container",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(12.0, 12.0, 16.0, 20.0).get(s.contrastLevel)
                else ContrastCurve(94.0, 94.0, 92.0, 90.0).get(s.contrastLevel)
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceContainerHigh(): DynamicColor {
        return DynamicColor(
            name = "surface_container_high",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(17.0, 17.0, 21.0, 25.0).get(s.contrastLevel)
                else ContrastCurve(92.0, 92.0, 88.0, 85.0).get(s.contrastLevel)
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun surfaceContainerHighest(): DynamicColor {
        return DynamicColor(
            name = "surface_container_highest",
            palette = { s -> s.neutralPalette },
            tone = { s ->
                if (s.isDark) ContrastCurve(22.0, 22.0, 26.0, 30.0).get(s.contrastLevel)
                else ContrastCurve(90.0, 90.0, 84.0, 80.0).get(s.contrastLevel)
            },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun onSurface(): DynamicColor {
        return DynamicColor(
            name = "on_surface",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 90.0 else 10.0 },
            isBackground = false, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null,
        )
    }

    fun surfaceVariant(): DynamicColor {
        return DynamicColor(
            name = "surface_variant",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 30.0 else 90.0 },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
        )
    }

    fun onSurfaceVariant(): DynamicColor {
        return DynamicColor(
            name = "on_surface_variant",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 80.0 else 30.0 },
            isBackground = false, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
            toneDeltaPair = null)
    }

    fun inverseSurface(): DynamicColor {
        return DynamicColor(
            name = "inverse_surface",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 90.0 else 20.0 },
            isBackground = false,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null)
    }

    fun inverseOnSurface(): DynamicColor {
        return DynamicColor(
            name = "inverse_on_surface",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 20.0 else 95.0 },
            isBackground = false,
            background = { inverseSurface() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun outline(): DynamicColor {
        return DynamicColor(
            name = "outline",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 60.0 else 50.0 },
            isBackground = false, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.5, 3.0, 4.5, 7.0),
            toneDeltaPair = null)
    }

    fun outlineVariant(): DynamicColor {
        return DynamicColor(
            name = "outline_variant",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 30.0 else 80.0 },
            isBackground = false, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = null)
    }

    fun shadow(): DynamicColor {
        return DynamicColor(
            name = "shadow",
            palette = { s -> s.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null)
    }

    fun scrim(): DynamicColor {
        return DynamicColor(
            name = "scrim",
            palette = { s -> s.neutralPalette },
            tone = { 0.0 },
            isBackground = false,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null)
    }

    fun surfaceTint(): DynamicColor {
        return DynamicColor(
            name = "surface_tint",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (s.isDark) 80.0 else 40.0 },
            isBackground = true,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null)
    }

    fun primary(): DynamicColor {
        return DynamicColor(
            name = "primary",
            palette = { s -> s.primaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 100.0 else 0.0
                } else {
                    if (s.isDark) 80.0 else 40.0
                }
            },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
            }
        )
    }

    fun onPrimary(): DynamicColor {
        return DynamicColor(
            name = "on_primary",
            palette = { s -> s.primaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 90.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            },
            isBackground = false,
            background = { primary() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun primaryContainer(): DynamicColor {
        return DynamicColor(
            name = "primary_container",
            palette = { s -> s.primaryPalette },
            tone = { s ->
                if (isFidelity(s)) {
                    s.sourceColorHct.getTone()
                } else if (isMonochrome(s)) {
                    if (s.isDark) 85.0 else 25.0
                } else {
                    if (s.isDark) 30.0 else 90.0
                }
            },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = { ToneDeltaPair(primaryContainer(), primary(), 10.0, TonePolarity.NEARER, false) })
    }

    fun onPrimaryContainer(): DynamicColor {
        return DynamicColor(
            name = "on_primary_container",
            palette = { s -> s.primaryPalette },
            tone = { s ->
                if (isFidelity(s)) {
                    DynamicColor.foregroundTone(primaryContainer().tone(s), 4.5)
                } else if (isMonochrome(s)) {
                    if (s.isDark) 0.0 else 100.0
                } else {
                    if (s.isDark) 90.0 else 10.0
                }
            },
            isBackground = false,
            background = { primaryContainer() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun inversePrimary(): DynamicColor {
        return DynamicColor(
            name = "inverse_primary",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (s.isDark) 40.0 else 80.0 },
            isBackground = false,
            background = { inverseSurface() },
            secondBackground = null,
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
            toneDeltaPair = null)
    }

    fun secondary(): DynamicColor {
        return DynamicColor(
            name = "secondary",
            palette = { s -> s.secondaryPalette },
            tone = { s -> if (s.isDark) 80.0 else 40.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 7.0),
            toneDeltaPair = { ToneDeltaPair(secondaryContainer(), secondary(), 10.0, TonePolarity.NEARER, false) })
    }

    fun onSecondary(): DynamicColor {
        return DynamicColor(
            name = "on_secondary",
            palette = { s -> s.secondaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 100.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            },
            isBackground = false,
            background = { secondary() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun secondaryContainer(): DynamicColor {
        return DynamicColor(
            name = "secondary_container",
            palette = { s -> s.secondaryPalette },
            tone = { s ->
                val initialTone = if (s.isDark) 30.0 else 90.0
                if (isMonochrome(s)) {
                    if (s.isDark) 30.0 else 85.0
                } else if (!isFidelity(s)) {
                    initialTone
                } else {
                    findDesiredChromaByTone(
                        hue = s.secondaryPalette.hue,
                        chroma = s.secondaryPalette.chroma,
                        tone = initialTone,
                        byDecreasingTone = !s.isDark
                    )
                }
            },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onSecondaryContainer(): DynamicColor {
        return DynamicColor(
            name = "on_secondary_container",
            palette = { s -> s.secondaryPalette },
            tone = { s ->
                if (!isFidelity(s)) {
                    if (s.isDark) 90.0 else 10.0
                } else
                    DynamicColor.foregroundTone(secondaryContainer().tone(s), 4.5)
            },
            isBackground = false,
            background = { secondaryContainer() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun tertiary(): DynamicColor {
        return DynamicColor(
            name = "tertiary",
            palette = { s -> s.tertiaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 90.0 else 25.0
                } else {
                    if (s.isDark) 80.0 else 40.0
                }
            },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onTertiary(): DynamicColor {
        return DynamicColor(
            name = "on_tertiary",
            palette = { s -> s.tertiaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 10.0 else 90.0
                } else {
                    if (s.isDark) 20.0 else 100.0
                }
            },
            isBackground = false,
            background = { tertiary() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun tertiaryContainer(): DynamicColor {
        return DynamicColor(
            name = "tertiary_container",
            palette = { s -> s.tertiaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 60.0 else 49.0
                } else if (!isFidelity(s)) {
                    if (s.isDark) 30.0 else 90.0
                } else {
                    val proposedHct: Hct = s.tertiaryPalette.getHct(s.sourceColorHct.getTone())
                    DislikeAnalyzer.fixIfDisliked(proposedHct).getTone()
                }
            },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onTertiaryContainer(): DynamicColor {
        return DynamicColor(
            name = "on_tertiary_container",
            palette = { s -> s.tertiaryPalette },
            tone = { s ->
                if (isMonochrome(s)) {
                    if (s.isDark) 0.0 else 100.0
                } else if (!isFidelity(s)) {
                    if (s.isDark) 90.0 else 10.0
                } else DynamicColor.foregroundTone(tertiaryContainer().tone(s), 4.5)
            },
            isBackground = false,
            background = { tertiaryContainer() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun error(): DynamicColor {
        return DynamicColor(
            name = "error",
            palette = { s -> s.errorPalette },
            tone = { s -> if (s.isDark) 80.0 else 40.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onError(): DynamicColor {
        return DynamicColor(
            name = "on_error",
            palette = { s -> s.errorPalette },
            tone = { s -> if (s.isDark) 20.0 else 100.0 },
            isBackground = false,
            background = { error() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun errorContainer(): DynamicColor {
        return DynamicColor(
            name = "error_container",
            palette = { s -> s.errorPalette },
            tone = { s -> if (s.isDark) 30.0 else 90.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onErrorContainer(): DynamicColor {
        return DynamicColor(
            name = "on_error_container",
            palette = { s -> s.errorPalette },
            tone = { s -> if (s.isDark) 90.0 else 10.0 },
            isBackground = false,
            background = { errorContainer() },
            secondBackground = null,
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun primaryFixed(): DynamicColor {
        return DynamicColor(
            name = "primary_fixed",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (isMonochrome(s)) 40.0 else 90.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = { ToneDeltaPair(primaryFixed(), primaryFixedDim(), 10.0, TonePolarity.LIGHTER, true) })
    }

    fun primaryFixedDim(): DynamicColor {
        return DynamicColor(
            name = "primary_fixed_dim",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (isMonochrome(s)) 30.0 else 80.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = { ToneDeltaPair(primaryFixed(), primaryFixedDim(), 10.0, TonePolarity.LIGHTER, true) })
    }

    fun onPrimaryFixed(): DynamicColor {
        return DynamicColor(
            name = "on_primary_fixed",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (isMonochrome(s)) 100.0 else 10.0 },
            isBackground = false,
            background = { primaryFixedDim() },
            secondBackground = { primaryFixed() },
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun onPrimaryFixedVariant(): DynamicColor {
        return DynamicColor(
            name = "on_primary_fixed_variant",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (isMonochrome(s)) 90.0 else 30.0 },
            isBackground = false,
            background = { primaryFixedDim() },
            secondBackground = { primaryFixed() },
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
            toneDeltaPair = null)
    }

    fun secondaryFixed(): DynamicColor {
        return DynamicColor(
            name = "secondary_fixed",
            palette = { s -> s.secondaryPalette },
            tone = { s -> if (isMonochrome(s)) 80.0 else 90.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = {
                ToneDeltaPair(
                    secondaryFixed(), secondaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            })
    }

    fun secondaryFixedDim(): DynamicColor {
        return DynamicColor(
            name = "secondary_fixed_dim",
            palette = { s -> s.secondaryPalette },
            tone = { s -> if (isMonochrome(s)) 70.0 else 80.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = {
                ToneDeltaPair(
                    secondaryFixed(), secondaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            })
    }

    fun onSecondaryFixed(): DynamicColor {
        return DynamicColor(
            name = "on_secondary_fixed",
            palette = { s -> s.secondaryPalette },
            tone = { 10.0 },
            isBackground = false,
            background = { secondaryFixedDim() },
            secondBackground = { secondaryFixed() },
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun onSecondaryFixedVariant(): DynamicColor {
        return DynamicColor(
            name = "on_secondary_fixed_variant",
            palette = { s -> s.secondaryPalette },
            tone = { s -> if (isMonochrome(s)) 25.0 else 30.0 },
            isBackground = false,
            background = { secondaryFixedDim() },
            secondBackground = { secondaryFixed() },
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
            toneDeltaPair = null)
    }

    fun tertiaryFixed(): DynamicColor {
        return DynamicColor(
            name = "tertiary_fixed",
            palette = { s -> s.tertiaryPalette },
            tone = { s -> if (isMonochrome(s)) 40.0 else 90.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
            secondBackground = null,
            contrastCurve = ContrastCurve(1.0, 1.0, 3.0, 4.5),
            toneDeltaPair = {
                ToneDeltaPair(
                    tertiaryFixed(), tertiaryFixedDim(), 10.0, TonePolarity.LIGHTER, true)
            })
    }

    fun tertiaryFixedDim(): DynamicColor {
        return DynamicColor(
            name = "tertiary_fixed_dim",
            palette = { s -> s.tertiaryPalette },
            tone = { s -> if (isMonochrome(s)) 30.0 else 80.0 },
            isBackground = true, background = { s: DynamicScheme -> highestSurface(s) },
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
    }

    fun onTertiaryFixed(): DynamicColor {
        return DynamicColor(
            name = "on_tertiary_fixed",
            palette = { s -> s.tertiaryPalette },
            tone = { s -> if (isMonochrome(s)) 100.0 else 10.0 },
            isBackground = false,
            background = { tertiaryFixedDim() },
            secondBackground = { tertiaryFixed() },
            contrastCurve = ContrastCurve(4.5, 7.0, 11.0, 21.0),
            toneDeltaPair = null)
    }

    fun onTertiaryFixedVariant(): DynamicColor {
        return DynamicColor(
            name = "on_tertiary_fixed_variant",
            palette = { s -> s.tertiaryPalette },
            tone = { s -> if (isMonochrome(s)) 90.0 else 30.0 },
            isBackground = false,
            background = { tertiaryFixedDim() },
            secondBackground = { tertiaryFixed() },
            contrastCurve = ContrastCurve(3.0, 4.5, 7.0, 11.0),
            toneDeltaPair = null)
    }

    /**
     * These colors were present in Android framework before Android U, and used by MDC controls. They
     * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
     * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
     * and those won't adjust for contrast, avoiding issues.
     *
     *
     * * For example, if the same color is on a white background _and_ black background, there's no
     * way to increase contrast with either without losing contrast with the other.
     */
    // colorControlActivated documented as colorAccent in M3 & GM3.
    // colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
    // Android used Material's Container as Primary/Secondary/Tertiary at launch.
    // Therefore, this is a duplicated version of Primary Container.
    fun controlActivated(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "control_activated",
            palette = { s -> s.primaryPalette },
            tone = { s -> if (s.isDark) 30.0 else 90.0 },
        )
    }

    // colorControlNormal documented as textColorSecondary in M3 & GM3.
    // In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
    // which is Neutral Variant T30/80 in light/dark.
    fun controlNormal(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "control_normal",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 80.0 else 30.0 },
        )
    }

    // colorControlHighlight documented, in both M3 & GM3:
    // Light mode: #1f000000 dark mode: #33ffffff.
    // These are black and white with some alpha.
    // 1F hex = 31 decimal; 31 / 255 = 12% alpha.
    // 33 hex = 51 decimal; 51 / 255 = 20% alpha.
    // DynamicColors do not support alpha currently, and _may_ not need it for this use case,
    // depending on how MDC resolved alpha for the other cases.
    // Returning black in dark mode, white in light mode.
    fun controlHighlight(): DynamicColor {
        return DynamicColor(
            name = "control_highlight",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 100.0 else 0.0 },
            isBackground = false,
            background = null,
            secondBackground = null,
            contrastCurve = null,
            toneDeltaPair = null,
            opacity = { s -> if (s.isDark) 0.20 else 0.12 })
    }

    // textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
    fun textPrimaryInverse(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "text_primary_inverse",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 10.0 else 90.0 },
        )
    }

    // textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
    // NV30/NV80
    fun textSecondaryAndTertiaryInverse(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "text_secondary_and_tertiary_inverse",
            palette = { s -> s.neutralVariantPalette },
            tone = { s -> if (s.isDark) 30.0 else 80.0 },
        )
    }

    // textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
    fun textPrimaryInverseDisableOnly(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "text_primary_inverse_disable_only",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 10.0 else 90.0 },
        )
    }

    // textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
    // in both M3 & GM3, as N10/N90
    fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "text_secondary_and_tertiary_inverse_disabled",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 10.0 else 90.0 },
        )
    }

    // textColorHintInverse documented, in both M3 & GM3, as N10/N90
    fun textHintInverse(): DynamicColor {
        return DynamicColor.fromPalette(
            name = "text_hint_inverse",
            palette = { s -> s.neutralPalette },
            tone = { s -> if (s.isDark) 10.0 else 90.0 },
        )
    }

    private fun isFidelity(scheme: DynamicScheme): Boolean {
        if (isExtendedFidelity
            && scheme.variant != Variant.MONOCHROME
            && scheme.variant != Variant.NEUTRAL
        ) return true

        return scheme.variant == Variant.FIDELITY || scheme.variant == Variant.CONTENT
    }

    companion object {

        private fun isMonochrome(scheme: DynamicScheme): Boolean {
            return scheme.variant === Variant.MONOCHROME
        }

        fun findDesiredChromaByTone(
            hue: Double, chroma: Double, tone: Double, byDecreasingTone: Boolean,
        ): Double {
            var answer = tone
            var closestToChroma = Hct.from(hue, chroma, tone)
            if (closestToChroma.getChroma() < chroma) {
                var chromaPeak = closestToChroma.getChroma()
                while (closestToChroma.getChroma() < chroma) {
                    answer += if (byDecreasingTone) -1.0 else 1.0
                    val potentialSolution = Hct.from(hue, chroma, answer)
                    if (chromaPeak > potentialSolution.getChroma()) {
                        break
                    }
                    if (abs(potentialSolution.getChroma() - chroma) < 0.4) {
                        break
                    }
                    val potentialDelta: Double = abs(potentialSolution.getChroma() - chroma)
                    val currentDelta: Double = abs(closestToChroma.getChroma() - chroma)
                    if (potentialDelta < currentDelta) {
                        closestToChroma = potentialSolution
                    }
                    chromaPeak = max(chromaPeak, potentialSolution.getChroma())
                }
            }
            return answer
        }
    }
}
