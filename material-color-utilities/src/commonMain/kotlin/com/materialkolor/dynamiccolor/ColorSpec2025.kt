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
import com.materialkolor.dynamiccolor.ToneDeltaPair.DeltaConstraint
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.DynamicScheme.Platform
import com.materialkolor.scheme.Variant
import kotlin.math.max
import kotlin.math.min

/**
 * [ColorSpec] implementation for the 2025 spec.
 */
public class ColorSpec2025 : ColorSpec2021() {
    // Surfaces [S]
    public override fun background(): DynamicColor {
        // Remapped to surface for 2025 spec.
        val color2025 = surface().toBuilder().setName("background").build()
        return super
            .background()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onBackground(): DynamicColor {
        // Remapped to onSurface for 2025 spec.
        val color2025 = onSurface().toBuilder().setName("on_background")
            .setTone { s -> if (s.platform == Platform.WATCH) 100.0 else onSurface().getTone(s) }
            .build()
        return super
            .onBackground()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surface(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) {
                            return@setTone 4.0
                        } else {
                            if (s.neutralPalette.keyColor.isYellow()) {
                                return@setTone 99.0
                            } else if (s.variant === Variant.VIBRANT) {
                                return@setTone 97.0
                            } else {
                                return@setTone 98.0
                            }
                        }
                    } else {
                        return@setTone 0.0
                    }
                }.setIsBackground(true)
                .build()
        return super
            .surface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceDim(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_dim")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.isDark) {
                        return@setTone 4.0
                    } else {
                        if (s.neutralPalette.keyColor.isYellow()) {
                            return@setTone 90.0
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setTone 85.0
                        } else {
                            return@setTone 87.0
                        }
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (!s.isDark) {
                        if (s.variant === Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.5
                        } else if (s.variant === Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant === Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (s.neutralPalette.keyColor
                                    .isYellow()
                            ) {
                                2.7
                            } else {
                                1.75
                            }
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setChromaMultiplier 1.36
                        }
                    }
                    1.0
                }.build()
        return super
            .surfaceDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceBright(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_bright")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.isDark) {
                        return@setTone 18.0
                    } else {
                        if (s.neutralPalette.keyColor.isYellow()) {
                            return@setTone 99.0
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setTone 97.0
                        } else {
                            return@setTone 98.0
                        }
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (s.isDark) {
                        if (s.variant === Variant.NEUTRAL) {
                            return@setChromaMultiplier 2.5
                        } else if (s.variant === Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.7
                        } else if (s.variant === Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (s.neutralPalette.keyColor
                                    .isYellow()
                            ) {
                                2.7
                            } else {
                                1.75
                            }
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setChromaMultiplier 1.36
                        }
                    }
                    1.0
                }.build()
        return super
            .surfaceBright()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceContainerLowest(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_container_lowest")
                .setPalette { s -> s.neutralPalette }
                .setTone { s -> if (s.isDark) 0.0 else 100.0 }
                .setIsBackground(true)
                .build()
        return super
            .surfaceContainerLowest()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceContainerLow(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_container_low")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) {
                            return@setTone 6.0
                        } else {
                            if (s.neutralPalette.keyColor.isYellow()) {
                                return@setTone 98.0
                            } else if (s.variant === Variant.VIBRANT) {
                                return@setTone 95.0
                            } else {
                                return@setTone 96.0
                            }
                        }
                    } else {
                        return@setTone 15.0
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.variant === Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.3
                        } else if (s.variant === Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.25
                        } else if (s.variant === Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (s.neutralPalette.keyColor
                                    .isYellow()
                            ) {
                                1.3
                            } else {
                                1.15
                            }
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setChromaMultiplier 1.08
                        }
                    }
                    1.0
                }.build()
        return super
            .surfaceContainerLow()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_container")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) {
                            return@setTone 9.0
                        } else {
                            if (s.neutralPalette.keyColor.isYellow()) {
                                return@setTone 96.0
                            } else if (s.variant === Variant.VIBRANT) {
                                return@setTone 92.0
                            } else {
                                return@setTone 94.0
                            }
                        }
                    } else {
                        return@setTone 20.0
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.variant === Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.6
                        } else if (s.variant === Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.4
                        } else if (s.variant === Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (s.neutralPalette.keyColor
                                    .isYellow()
                            ) {
                                1.6
                            } else {
                                1.3
                            }
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setChromaMultiplier 1.15
                        }
                    }
                    1.0
                }.build()
        return super
            .surfaceContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceContainerHigh(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_container_high")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) {
                            return@setTone 12.0
                        } else {
                            if (s.neutralPalette.keyColor.isYellow()) {
                                return@setTone 94.0
                            } else if (s.variant === Variant.VIBRANT) {
                                return@setTone 90.0
                            } else {
                                return@setTone 92.0
                            }
                        }
                    } else {
                        return@setTone 25.0
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.variant === Variant.NEUTRAL) {
                            return@setChromaMultiplier 1.9
                        } else if (s.variant === Variant.TONAL_SPOT) {
                            return@setChromaMultiplier 1.5
                        } else if (s.variant === Variant.EXPRESSIVE) {
                            return@setChromaMultiplier if (s.neutralPalette.keyColor
                                    .isYellow()
                            ) {
                                1.95
                            } else {
                                1.45
                            }
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setChromaMultiplier 1.22
                        }
                    }
                    1.0
                }.build()
        return super
            .surfaceContainerHigh()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceContainerHighest(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("surface_container_highest")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.isDark) {
                        return@setTone 15.0
                    } else {
                        if (s.neutralPalette.keyColor.isYellow()) {
                            return@setTone 92.0
                        } else if (s.variant === Variant.VIBRANT) {
                            return@setTone 88.0
                        } else {
                            return@setTone 90.0
                        }
                    }
                }.setIsBackground(true)
                .setChromaMultiplier { s ->
                    if (s.variant === Variant.NEUTRAL) {
                        return@setChromaMultiplier 2.2
                    } else if (s.variant === Variant.TONAL_SPOT) {
                        return@setChromaMultiplier 1.7
                    } else if (s.variant === Variant.EXPRESSIVE) {
                        return@setChromaMultiplier if (s.neutralPalette.keyColor
                                .isYellow()
                        ) {
                            2.3
                        } else {
                            1.6
                        }
                    } else if (s.variant === Variant.VIBRANT) {
                        return@setChromaMultiplier 1.29
                    }
                    1.0
                }.build()
        return super
            .surfaceContainerHighest()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_surface")
                .setPalette { s -> s.neutralPalette }
                .setTone { s ->
                    if (s.variant == Variant.VIBRANT) {
                        tMaxC(s.neutralPalette, 0.0, 100.0, 1.1)
                    } else {
                        DynamicColor.getInitialToneFromBackground { scheme ->
                            if (scheme.platform == Platform.PHONE) {
                                if (scheme.isDark) {
                                    surfaceBright()
                                } else {
                                    surfaceDim()
                                }
                            } else {
                                surfaceContainerHigh()
                            }
                        }(s)
                    }
                }.setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        when {
                            s.variant === Variant.NEUTRAL -> 2.2
                            s.variant === Variant.TONAL_SPOT -> 1.7
                            s.variant === Variant.EXPRESSIVE -> {
                                if (s.neutralPalette.keyColor.isYellow()) {
                                    if (s.isDark) 3.0 else 2.3
                                } else {
                                    1.6
                                }
                            }
                            else -> 1.0
                        }
                    } else {
                        1.0
                    }
                }.setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.isDark && s.platform == Platform.PHONE) getContrastCurve(11.0)
                    else getContrastCurve(9.0)
                }.build()
        return super
            .onSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceVariant(): DynamicColor {
        // Remapped to surfaceContainerHighest for 2025 spec.
        val color2025 =
            surfaceContainerHighest().toBuilder().setName("surface_variant").build()
        return super
            .surfaceVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onSurfaceVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_surface_variant")
                .setPalette { s -> s.neutralPalette }
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        when {
                            s.variant === Variant.NEUTRAL -> 2.2
                            s.variant === Variant.TONAL_SPOT -> 1.7
                            s.variant === Variant.EXPRESSIVE -> {
                                if (s.neutralPalette.keyColor.isYellow()) {
                                    if (s.isDark) 3.0 else 2.3
                                } else {
                                    1.6
                                }
                            }
                            else -> 1.0
                        }
                    } else {
                        1.0
                    }
                }.setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) getContrastCurve(6.0) else getContrastCurve(4.5)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onSurfaceVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun inverseSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("inverse_surface")
                .setPalette { s -> s.neutralPalette }
                .setTone { s -> if (s.isDark) 98.0 else 4.0 }
                .setIsBackground(true)
                .build()
        return super
            .inverseSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun inverseOnSurface(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("inverse_on_surface")
                .setPalette { s -> s.neutralPalette }
                .setBackground { s -> inverseSurface() }
                .setContrastCurve { s -> getContrastCurve(7.0) }
                .build()
        return super
            .inverseOnSurface()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun outline(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("outline")
                .setPalette { s -> s.neutralPalette }
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        when {
                            s.variant === Variant.NEUTRAL -> 2.2
                            s.variant === Variant.TONAL_SPOT -> 1.7
                            s.variant === Variant.EXPRESSIVE -> {
                                if (s.neutralPalette.keyColor.isYellow()) {
                                    if (s.isDark) 3.0 else 2.3
                                } else {
                                    1.6
                                }
                            }
                            else -> 1.0
                        }
                    } else {
                        1.0
                    }
                }.setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(3.0)
                    } else {
                        getContrastCurve(
                            4.5,
                        )
                    }
                }.build()
        return super
            .outline()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun outlineVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("outline_variant")
                .setPalette { s -> s.neutralPalette }
                .setChromaMultiplier { s ->
                    if (s.platform === Platform.PHONE) {
                        when {
                            s.variant === Variant.NEUTRAL -> 2.2
                            s.variant === Variant.TONAL_SPOT -> 1.7
                            s.variant === Variant.EXPRESSIVE -> {
                                if (s.neutralPalette.keyColor.isYellow()) {
                                    if (s.isDark) 3.0 else 2.3
                                } else {
                                    1.6
                                }
                            }
                            s.variant === Variant.VIBRANT -> 1.29
                            else -> 1.0
                        }
                    } else {
                        1.0
                    }
                }.setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(1.5)
                    } else {
                        getContrastCurve(
                            3.0,
                        )
                    }
                }.build()
        return super
            .outlineVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun surfaceTint(): DynamicColor {
        // Remapped to primary for 2025 spec.
        val color2025 = primary().toBuilder().setName("surface_tint").build()
        return super
            .surfaceTint()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Primaries [P]

    public override fun primary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("primary")
                .setPalette { s -> s.primaryPalette }
                .setTone { s ->
                    if (s.variant === Variant.NEUTRAL) {
                        if (s.platform === Platform.PHONE) {
                            return@setTone if (s.isDark) 80.0 else 40.0
                        } else {
                            return@setTone 90.0
                        }
                    } else if (s.variant === Variant.TONAL_SPOT) {
                        if (s.platform === Platform.PHONE) {
                            if (s.isDark) {
                                return@setTone 80.0
                            } else {
                                return@setTone tMaxC(s.primaryPalette)
                            }
                        } else {
                            return@setTone tMaxC(s.primaryPalette, 0.0, 90.0)
                        }
                    } else if (s.variant === Variant.EXPRESSIVE) {
                        return@setTone tMaxC(
                            s.primaryPalette,
                            0.0,
                            (
                                if (s.primaryPalette.keyColor.isYellow()) {
                                    25
                                } else if (s.primaryPalette.keyColor.isCyan()) {
                                    88
                                } else {
                                    98
                                }
                            ).toDouble(),
                        )
                    } else { // VIBRANT
                        return@setTone tMaxC(
                            s.primaryPalette,
                            0.0,
                            (if (s.primaryPalette.keyColor.isCyan()) 88 else 98).toDouble(),
                        )
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(4.5)
                    } else {
                        getContrastCurve(
                            7.0,
                        )
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.PHONE) {
                        ToneDeltaPair(
                            roleA = primaryContainer(),
                            roleB = primary(),
                            delta = 5.0,
                            polarity = TonePolarity.RELATIVE_LIGHTER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.build()

        return super
            .primary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun primaryDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("primary_dim")
            .setPalette { s -> s.primaryPalette }
            .setTone { s ->
                when {
                    s.variant === Variant.NEUTRAL -> 85.0
                    s.variant === Variant.TONAL_SPOT -> tMaxC(s.primaryPalette, 0.0, 90.0)
                    else -> tMaxC(s.primaryPalette)
                }
            }.setIsBackground(true)
            .setBackground { s -> surfaceContainerHigh() }
            .setContrastCurve { s -> getContrastCurve(4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = primaryDim(),
                    roleB = primary(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = DeltaConstraint.FARTHER,
                )
            }.build()

    public override fun onPrimary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_primary")
                .setPalette { s -> s.primaryPalette }
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        primary()
                    } else {
                        primaryDim()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onPrimary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun primaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("primary_container")
                .setPalette { s -> s.primaryPalette }
                .setTone { s ->
                    if (s.platform === Platform.WATCH) {
                        return@setTone 30.0
                    } else if (s.variant === Variant.NEUTRAL) {
                        return@setTone if (s.isDark) 30.0 else 90.0
                    } else if (s.variant === Variant.TONAL_SPOT) {
                        return@setTone if (s.isDark) {
                            tMinC(s.primaryPalette, 35.0, 93.0)
                        } else {
                            tMaxC(s.primaryPalette, 0.0, 90.0)
                        }
                    } else if (s.variant === Variant.EXPRESSIVE) {
                        return@setTone if (s.isDark) {
                            tMaxC(s.primaryPalette, 30.0, 93.0)
                        } else {
                            tMaxC(
                                s.primaryPalette,
                                78.0,
                                (
                                    if (s.primaryPalette.keyColor
                                            .isCyan()
                                    ) {
                                        88
                                    } else {
                                        90
                                    }
                                ).toDouble(),
                            )
                        }
                    } else { // VIBRANT
                        return@setTone if (s.isDark) {
                            tMinC(s.primaryPalette, 66.0, 93.0)
                        } else {
                            tMaxC(
                                s.primaryPalette,
                                66.0,
                                (
                                    if (s.primaryPalette.keyColor
                                            .isCyan()
                                    ) {
                                        88
                                    } else {
                                        93
                                    }
                                ).toDouble(),
                            )
                        }
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground null
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.WATCH) {
                        ToneDeltaPair(
                            roleA = primaryContainer(),
                            roleB = primaryDim(),
                            delta = 10.0,
                            polarity = TonePolarity.DARKER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE && s.contrastLevel > 0) {
                        getContrastCurve(1.5)
                    } else {
                        null
                    }
                }.build()
        return super
            .primaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onPrimaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_primary_container")
                .setPalette { s -> s.primaryPalette }
                .setBackground { s -> primaryContainer() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onPrimaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun inversePrimary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("inverse_primary")
                .setPalette { s -> s.primaryPalette }
                .setTone { s -> tMaxC(s.primaryPalette) }
                .setBackground { s -> inverseSurface() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .inversePrimary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Secondaries [Q]

    public override fun secondary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("secondary")
                .setPalette { s -> s.secondaryPalette }
                .setTone { s ->
                    when {
                        s.platform === Platform.WATCH -> {
                            if (s.variant === Variant.NEUTRAL) {
                                90.0
                            } else {
                                tMaxC(
                                    palette = s.secondaryPalette,
                                    lowerBound = 0.0,
                                    upperBound = 90.0,
                                )
                            }
                        }

                        s.variant === Variant.NEUTRAL -> {
                            if (s.isDark) {
                                tMinC(
                                    palette = s.secondaryPalette,
                                    lowerBound = 0.0,
                                    upperBound = 98.0,
                                )
                            } else {
                                tMaxC(s.secondaryPalette)
                            }
                        }

                        s.variant === Variant.VIBRANT -> {
                            tMaxC(
                                palette = s.secondaryPalette,
                                lowerBound = 0.0,
                                upperBound = (if (s.isDark) 90 else 98).toDouble(),
                            )
                        }

                        else -> { // EXPRESSIVE and TONAL_SPOT
                            if (s.isDark) 80.0 else tMaxC(s.secondaryPalette)
                        }
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(4.5)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.PHONE) {
                        ToneDeltaPair(
                            roleA = secondaryContainer(),
                            roleB = secondary(),
                            delta = 5.0,
                            polarity = TonePolarity.RELATIVE_LIGHTER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.build()
        return super
            .secondary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun secondaryDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("secondary_dim")
            .setPalette { s -> s.secondaryPalette }
            .setTone { s ->
                if (s.variant === Variant.NEUTRAL) {
                    85.0
                } else {
                    tMaxC(s.secondaryPalette, 0.0, 90.0)
                }
            }.setIsBackground(true)
            .setBackground { s -> surfaceContainerHigh() }
            .setContrastCurve { s -> getContrastCurve(4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = secondaryDim(),
                    roleB = secondary(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = DeltaConstraint.FARTHER,
                )
            }.build()

    public override fun onSecondary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_secondary")
                .setPalette { s -> s.secondaryPalette }
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) secondary() else secondaryDim()
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onSecondary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun secondaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("secondary_container")
                .setPalette { s -> s.secondaryPalette }
                .setTone { s ->
                    when {
                        s.platform === Platform.WATCH -> 30.0
                        s.variant === Variant.VIBRANT -> {
                            if (s.isDark) {
                                tMinC(s.secondaryPalette, 30.0, 40.0)
                            } else {
                                tMaxC(s.secondaryPalette, 84.0, 90.0)
                            }
                        }

                        s.variant === Variant.EXPRESSIVE -> {
                            if (s.isDark) 15.0 else tMaxC(s.secondaryPalette, 90.0, 95.0)
                        }

                        else -> if (s.isDark) 25.0 else 90.0
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.WATCH) {
                        ToneDeltaPair(
                            roleA = secondaryContainer(),
                            roleB = secondaryDim(),
                            delta = 10.0,
                            polarity = TonePolarity.DARKER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE && s.contrastLevel > 0) {
                        getContrastCurve(1.5)
                    } else {
                        null
                    }
                }.build()
        return super
            .secondaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onSecondaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_secondary_container")
                .setPalette { s -> s.secondaryPalette }
                .setBackground { s -> secondaryContainer() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onSecondaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Tertiaries [T]

    public override fun tertiary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("tertiary")
                .setPalette { s -> s.tertiaryPalette }
                .setTone { s ->
                    if (s.platform === Platform.WATCH) {
                        if (s.variant === Variant.TONAL_SPOT) {
                            tMaxC(s.tertiaryPalette, 0.0, 90.0)
                        } else {
                            tMaxC(s.tertiaryPalette)
                        }
                    } else if (s.variant === Variant.EXPRESSIVE || s.variant === Variant.VIBRANT) {
                        val upperBound = if (s.tertiaryPalette.keyColor.isCyan()) {
                            88
                        } else {
                            if (s.isDark) 98 else 100
                        }
                        tMaxC(
                            palette = s.tertiaryPalette,
                            lowerBound = 0.0,
                            upperBound = upperBound.toDouble(),
                        )
                    } else { // NEUTRAL and TONAL_SPOT
                        if (s.isDark) {
                            tMaxC(
                                palette = s.tertiaryPalette,
                                lowerBound = 0.0,
                                upperBound = 98.0,
                            )
                        } else {
                            tMaxC(s.tertiaryPalette)
                        }
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(4.5)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.PHONE) {
                        ToneDeltaPair(
                            roleA = tertiaryContainer(),
                            roleB = tertiary(),
                            delta = 5.0,
                            polarity = TonePolarity.RELATIVE_LIGHTER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.build()
        return super
            .tertiary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun tertiaryDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("tertiary_dim")
            .setPalette { s -> s.tertiaryPalette }
            .setTone { s ->
                if (s.variant === Variant.TONAL_SPOT) {
                    tMaxC(s.tertiaryPalette, 0.0, 90.0)
                } else {
                    tMaxC(s.tertiaryPalette)
                }
            }.setIsBackground(true)
            .setBackground { s -> surfaceContainerHigh() }
            .setContrastCurve { s -> getContrastCurve(4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = tertiaryDim(),
                    roleB = tertiary(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = DeltaConstraint.FARTHER,
                )
            }.build()

    public override fun onTertiary(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_tertiary")
                .setPalette { s -> s.tertiaryPalette }
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) tertiary() else tertiaryDim()
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onTertiary()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun tertiaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("tertiary_container")
                .setPalette { s -> s.tertiaryPalette }
                .setTone { s ->
                    if (s.platform === Platform.WATCH) {
                        if (s.variant === Variant.TONAL_SPOT) {
                            tMaxC(s.tertiaryPalette, 0.0, 90.0)
                        } else {
                            tMaxC(s.tertiaryPalette)
                        }
                    } else {
                        when {
                            s.variant === Variant.NEUTRAL -> {
                                if (s.isDark) {
                                    tMaxC(s.tertiaryPalette, 0.0, 93.0)
                                } else {
                                    tMaxC(s.tertiaryPalette, 0.0, 96.0)
                                }
                            }

                            s.variant === Variant.TONAL_SPOT -> {
                                tMaxC(
                                    palette = s.tertiaryPalette,
                                    lowerBound = 0.0,
                                    upperBound = (if (s.isDark) 93 else 100).toDouble(),
                                )
                            }

                            s.variant === Variant.EXPRESSIVE -> {
                                val upperBound = if (s.tertiaryPalette.keyColor.isCyan()) {
                                    88
                                } else {
                                    if (s.isDark) 93 else 100
                                }

                                tMaxC(
                                    palette = s.tertiaryPalette,
                                    lowerBound = 75.0,
                                    upperBound = upperBound.toDouble(),
                                )
                            }

                            else -> { // VIBRANT
                                if (s.isDark) {
                                    tMaxC(s.tertiaryPalette, 0.0, 93.0)
                                } else {
                                    tMaxC(s.tertiaryPalette, 72.0, 100.0)
                                }
                            }
                        }
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        return@setBackground if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        return@setBackground null
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.WATCH) {
                        ToneDeltaPair(
                            roleA = tertiaryContainer(),
                            roleB = tertiaryDim(),
                            delta = 10.0,
                            polarity = TonePolarity.DARKER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE && s.contrastLevel > 0) {
                        getContrastCurve(1.5)
                    } else {
                        null
                    }
                }.build()
        return super
            .tertiaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onTertiaryContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_tertiary_container")
                .setPalette { s -> s.tertiaryPalette }
                .setBackground { s -> tertiaryContainer() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onTertiaryContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Errors [E]

    public override fun error(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("error")
                .setPalette { s -> s.errorPalette }
                .setTone { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) {
                            tMinC(
                                palette = s.errorPalette,
                                lowerBound = 0.0,
                                upperBound = 98.0,
                            )
                        } else {
                            tMaxC(s.errorPalette)
                        }
                    } else {
                        tMinC(s.errorPalette)
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        surfaceContainerHigh()
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(4.5)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.PHONE) {
                        ToneDeltaPair(
                            roleA = errorContainer(),
                            roleB = error(),
                            delta = 5.0,
                            polarity = TonePolarity.RELATIVE_LIGHTER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.build()
        return super
            .error()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun errorDim(): DynamicColor =
        DynamicColor.Companion
            .Builder()
            .setName("error_dim")
            .setPalette { s -> s.errorPalette }
            .setTone { s -> tMinC(s.errorPalette) }
            .setIsBackground(true)
            .setBackground { s -> surfaceContainerHigh() }
            .setContrastCurve { s -> getContrastCurve(4.5) }
            .setToneDeltaPair { s ->
                ToneDeltaPair(
                    roleA = errorDim(),
                    roleB = error(),
                    delta = 5.0,
                    polarity = TonePolarity.DARKER,
                    deltaConstraint = DeltaConstraint.FARTHER,
                )
            }.build()

    public override fun onError(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_error")
                .setPalette { s -> s.errorPalette }
                .setBackground { s -> if (s.platform === Platform.PHONE) error() else errorDim() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(6.0)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onError()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun errorContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("error_container")
                .setPalette { s -> s.errorPalette }
                .setTone { s ->
                    if (s.platform === Platform.WATCH) {
                        30.0
                    } else {
                        if (s.isDark) {
                            tMinC(
                                palette = s.errorPalette,
                                lowerBound = 30.0,
                                upperBound = 93.0,
                            )
                        } else {
                            tMaxC(s.errorPalette, 0.0, 90.0)
                        }
                    }
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }.setToneDeltaPair { s ->
                    if (s.platform === Platform.WATCH) {
                        ToneDeltaPair(
                            roleA = errorContainer(),
                            roleB = errorDim(),
                            delta = 10.0,
                            polarity = TonePolarity.DARKER,
                            deltaConstraint = DeltaConstraint.FARTHER,
                        )
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform === Platform.PHONE && s.contrastLevel > 0) {
                        getContrastCurve(1.5)
                    } else {
                        null
                    }
                }.build()
        return super
            .errorContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onErrorContainer(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_error_container")
                .setPalette { s -> s.errorPalette }
                .setBackground { s -> errorContainer() }
                .setContrastCurve { s ->
                    if (s.platform === Platform.PHONE) {
                        getContrastCurve(4.5)
                    } else {
                        getContrastCurve(7.0)
                    }
                }.build()
        return super
            .onErrorContainer()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Primary Fixed Colors [PF]

    public override fun primaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("primary_fixed")
                .setPalette { s -> s.primaryPalette }
                .setTone { s ->
                    val tempS = DynamicScheme.from(s, false, 0.0)
                    primaryContainer().getTone(tempS)
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform === Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform == Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }.build()
        return super
            .primaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun primaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("primary_fixed_dim")
                .setPalette { s -> s.primaryPalette }
                .setTone { s -> primaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s ->
                    ToneDeltaPair(
                        roleA = primaryFixedDim(),
                        roleB = primaryFixed(),
                        delta = 5.0,
                        polarity = TonePolarity.DARKER,
                        deltaConstraint = DeltaConstraint.EXACT,
                    )
                }.build()
        return super
            .primaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onPrimaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_primary_fixed")
                .setPalette { s -> s.primaryPalette }
                .setBackground { s -> primaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(7.0) }
                .build()
        return super
            .onPrimaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onPrimaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_primary_fixed_variant")
                .setPalette { s -> s.primaryPalette }
                .setBackground { s -> primaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(4.5) }
                .build()
        return super
            .onPrimaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Secondary Fixed Colors [QF]

    public override fun secondaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("secondary_fixed")
                .setPalette { s -> s.secondaryPalette }
                .setTone { s ->
                    val tempS: DynamicScheme = DynamicScheme.from(s, isDark = false, 0.0)
                    secondaryContainer().getTone(tempS)
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform == Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform == Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }.build()
        return super
            .secondaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun secondaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("secondary_fixed_dim")
                .setPalette { s -> s.secondaryPalette }
                .setTone { s -> secondaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s ->
                    ToneDeltaPair(
                        roleA = secondaryFixedDim(),
                        roleB = secondaryFixed(),
                        delta = 5.0,
                        polarity = TonePolarity.DARKER,
                        deltaConstraint = DeltaConstraint.EXACT,
                    )
                }.build()
        return super
            .secondaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onSecondaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_secondary_fixed")
                .setPalette { s -> s.secondaryPalette }
                .setBackground { s -> secondaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(7.0) }
                .build()
        return super
            .onSecondaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onSecondaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_secondary_fixed_variant")
                .setPalette { s -> s.secondaryPalette }
                .setBackground { s -> secondaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(4.5) }
                .build()
        return super
            .onSecondaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Tertiary Fixed Colors [TF]

    public override fun tertiaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("tertiary_fixed")
                .setPalette { s -> s.tertiaryPalette }
                .setTone { s ->
                    val tempS = DynamicScheme.from(s, isDark = false, 0.0)
                    tertiaryContainer().getTone(tempS)
                }.setIsBackground(true)
                .setBackground { s ->
                    if (s.platform == Platform.PHONE) {
                        if (s.isDark) surfaceBright() else surfaceDim()
                    } else {
                        null
                    }
                }.setContrastCurve { s ->
                    if (s.platform == Platform.PHONE && s.contrastLevel > 0) getContrastCurve(1.5) else null
                }.build()
        return super
            .tertiaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun tertiaryFixedDim(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("tertiary_fixed_dim")
                .setPalette { s -> s.tertiaryPalette }
                .setTone { s -> tertiaryFixed().getTone(s) }
                .setIsBackground(true)
                .setToneDeltaPair { s ->
                    ToneDeltaPair(
                        roleA = tertiaryFixedDim(),
                        roleB = tertiaryFixed(),
                        delta = 5.0,
                        polarity = TonePolarity.DARKER,
                        deltaConstraint = DeltaConstraint.EXACT,
                    )
                }.build()
        return super
            .tertiaryFixedDim()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onTertiaryFixed(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_tertiary_fixed")
                .setPalette { s -> s.tertiaryPalette }
                .setBackground { s -> tertiaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(7.0) }
                .build()
        return super
            .onTertiaryFixed()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun onTertiaryFixedVariant(): DynamicColor {
        val color2025 =
            DynamicColor.Companion
                .Builder()
                .setName("on_tertiary_fixed_variant")
                .setPalette { s -> s.tertiaryPalette }
                .setBackground { s -> tertiaryFixedDim() }
                .setContrastCurve { s -> getContrastCurve(4.5) }
                .build()
        return super
            .onTertiaryFixedVariant()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Android-only Colors

    public override fun controlActivated(): DynamicColor {
        // Remapped to primaryContainer for 2025 spec.
        val color2025 = primaryContainer().toBuilder().setName("control_activated").build()
        return super
            .controlActivated()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun controlNormal(): DynamicColor {
        // Remapped to onSurfaceVariant for 2025 spec.
        val color2025 = onSurfaceVariant().toBuilder().setName("control_normal").build()
        return super
            .controlNormal()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    public override fun textPrimaryInverse(): DynamicColor {
        // Remapped to inverseOnSurface for 2025 spec.
        val color2025 = inverseOnSurface().toBuilder().setName("text_primary_inverse").build()
        return super
            .textPrimaryInverse()
            .toBuilder()
            .extendSpecVersion(ColorSpec.SpecVersion.SPEC_2025, color2025)
            .build()
    }

    // Color value calculations

    public override fun getHct(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Hct {
        // This is crucial for aesthetics: we aren't simply the taking the standard color
        // and changing its tone for contrast. Rather, we find the tone for contrast, then
        // use the specified chroma from the palette to construct a new color.
        //
        // For example, this enables colors with standard tone of T90, which has limited chroma, to
        // "recover" intended chroma as contrast increases.
        val palette: TonalPalette = color.palette(scheme)
        val tone = getTone(scheme, color)
        val hue: Double = palette.hue
        val chromaMultiplier = color.chromaMultiplier?.invoke(scheme) ?: 1
        val chroma: Double = palette.chroma * chromaMultiplier.toDouble()

        return Hct.from(hue, chroma, tone)
    }

    public override fun getTone(
        scheme: DynamicScheme,
        color: DynamicColor,
    ): Double {
        val toneDeltaPair: ToneDeltaPair? = color.toneDeltaPair?.invoke(scheme)

        // Case 0: tone delta pair.
        if (toneDeltaPair != null) {
            val roleA = toneDeltaPair.roleA
            val roleB = toneDeltaPair.roleB
            val polarity = toneDeltaPair.polarity
            val constraint: DeltaConstraint = toneDeltaPair.deltaConstraint
            val absoluteDelta =
                if (polarity == TonePolarity.DARKER ||
                    (polarity == TonePolarity.RELATIVE_LIGHTER && scheme.isDark) ||
                    (polarity == TonePolarity.RELATIVE_DARKER && !scheme.isDark)
                ) {
                    -toneDeltaPair.delta
                } else {
                    toneDeltaPair.delta
                }

            val amRoleA = color.name == roleA.name
            val selfRole = if (amRoleA) roleA else roleB
            val referenceRole = if (amRoleA) roleB else roleA
            var selfTone: Double = selfRole.tone(scheme)
            val referenceTone = referenceRole.getTone(scheme)
            val relativeDelta = absoluteDelta * (if (amRoleA) 1 else -1)

            when (constraint) {
                DeltaConstraint.EXACT ->
                    selfTone =
                        (referenceTone + relativeDelta).coerceIn(0.0, 100.0)

                DeltaConstraint.NEARER -> if (relativeDelta > 0) {
                    val t = selfTone.coerceIn(referenceTone, referenceTone + relativeDelta)
                    selfTone = t.coerceIn(0.0, 100.0)
                } else {
                    val t = selfTone.coerceIn(referenceTone + relativeDelta, referenceTone)
                    selfTone = t.coerceIn(0.0, 100.0)
                }

                DeltaConstraint.FARTHER -> selfTone = if (relativeDelta > 0) {
                    selfTone.coerceIn(referenceTone + relativeDelta, 100.0)
                } else {
                    selfTone.coerceIn(0.0, referenceTone + relativeDelta)
                }
            }

            if (color.background != null && color.contrastCurve != null) {
                val background = color.background(scheme)
                val contrastCurve: ContrastCurve? = color.contrastCurve(scheme)
                if (background != null && contrastCurve != null) {
                    val bgTone = background.getTone(scheme)
                    val selfContrast = contrastCurve.get(scheme.contrastLevel)
                    val ratioOfTones = Contrast.ratioOfTones(tone1 = bgTone, tone2 = selfTone)
                    selfTone =
                        if (ratioOfTones >= selfContrast && scheme.contrastLevel >= 0) {
                            selfTone
                        } else {
                            DynamicColor.foregroundTone(bgTone, selfContrast)
                        }
                }
            }

            // This can avoid the awkward tones for background colors including the access fixed
            // colors. Accent fixed dim colors should not be adjusted.
            if (color.isBackground && !color.name.endsWith("_fixed_dim")) {
                selfTone = if (selfTone >= 57) {
                    selfTone.coerceIn(65.0, 100.0)
                } else {
                    selfTone.coerceIn(0.0, 49.0)
                }
            }

            return selfTone
        } else {
            // Case 1: No tone delta pair; just solve for itself.
            var answer: Double = color.tone(scheme)

            val bgTone = color.background?.invoke(scheme)?.getTone(scheme)
            val desiredRatio = color.contrastCurve?.invoke(scheme)?.get(scheme.contrastLevel)
            if (bgTone == null || desiredRatio == null) {
                return answer // No adjustment for colors with no background.
            }
            // Recalculate the tone from desired contrast ratio if the current
            // contrast ratio is not enough or desired contrast level is decreasing
            // (<0).
            val ratioOfTones = Contrast.ratioOfTones(tone1 = bgTone, tone2 = answer)
            answer =
                if (ratioOfTones >= desiredRatio && scheme.contrastLevel >= 0) {
                    answer
                } else {
                    DynamicColor.foregroundTone(bgTone, desiredRatio)
                }

            // This can avoid the awkward tones for background colors including the access
            // fixed colors. Accent fixed dim colors should not be adjusted.
            if (color.isBackground && !color.name.endsWith("_fixed_dim")) {
                answer = if (answer >= 57) {
                    answer.coerceIn(65.0, 100.0)
                } else {
                    answer.coerceIn(0.0, 49.0)
                }
            }

            val secondBackground = color.secondBackground?.invoke(scheme)?.getTone(scheme)
            if (secondBackground == null) {
                return answer
            }

            // Case 2: Adjust for dual backgrounds.
            val bgTone1 = bgTone
            val bgTone2 = secondBackground
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
                return if (lightOption < 0) 100.0 else lightOption
            }
            if (availables.size == 1) {
                return availables.first()
            }
            return if (darkOption < 0) 0.0 else darkOption
        }
    }

    // Scheme Palettes

    override fun getPrimaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) {
                    (if (sourceColorHct.isBlue()) 12.0 else 8.0)
                } else {
                    (if (sourceColorHct.isBlue()) 16.0 else 12.0)
                },
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE && isDark) 26.0 else 32.0,
            )

            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) (if (isDark) 36.0 else 48.0) else 40.0,
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) 74.0 else 56.0,
            )

            else -> super.getPrimaryPalette(
                variant = variant,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            )
        }

    override fun getSecondaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) {
                    if (sourceColorHct.isBlue()) 6.0 else 4.0
                } else {
                    if (sourceColorHct.isBlue()) 10.0 else 6.0
                },
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(sourceColorHct.hue, 16.0)
            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(
                        0.0,
                        105.0,
                        140.0,
                        204.0,
                        253.0,
                        278.0,
                        300.0,
                        333.0,
                        360.0,
                    ),
                    rotations = doubleArrayOf(
                        -160.0,
                        155.0,
                        -100.0,
                        96.0,
                        -96.0,
                        -156.0,
                        -165.0,
                        -160.0,
                    ),
                ),
                chroma = if (platform === Platform.PHONE) (if (isDark) 16.0 else 24.0) else 24.0,
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(0.0, 38.0, 105.0, 140.0, 333.0, 360.0),
                    rotations = doubleArrayOf(-14.0, 10.0, -14.0, 10.0, -14.0),
                ),
                chroma = if (platform === Platform.PHONE) 56.0 else 36.0,
            )

            else -> super.getSecondaryPalette(
                variant,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            )
        }

    override fun getTertiaryPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(
                        0.0,
                        38.0,
                        105.0,
                        161.0,
                        204.0,
                        278.0,
                        333.0,
                        360.0,
                    ),
                    rotations = doubleArrayOf(-32.0, 26.0, 10.0, -39.0, 24.0, -15.0, -32.0),
                ),
                chroma = if (platform === Platform.PHONE) 20.0 else 36.0,
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(0.0, 20.0, 71.0, 161.0, 333.0, 360.0),
                    rotations = doubleArrayOf(-40.0, 48.0, -32.0, 40.0, -32.0),
                ),
                chroma = if (platform === Platform.PHONE) 28.0 else 32.0,
            )

            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(
                        0.0,
                        105.0,
                        140.0,
                        204.0,
                        253.0,
                        278.0,
                        300.0,
                        333.0,
                        360.0,
                    ),
                    rotations = doubleArrayOf(
                        -165.0,
                        160.0,
                        -105.0,
                        101.0,
                        -101.0,
                        -160.0,
                        -170.0,
                        -165.0,
                    ),
                ),
                chroma = 48.0,
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = DynamicScheme.getRotatedHue(
                    sourceColorHct = sourceColorHct,
                    hueBreakpoints = doubleArrayOf(
                        0.0,
                        38.0,
                        71.0,
                        105.0,
                        140.0,
                        161.0,
                        253.0,
                        333.0,
                        360.0,
                    ),
                    rotations = doubleArrayOf(-72.0, 35.0, 24.0, -24.0, 62.0, 50.0, 62.0, -72.0),
                ),
                chroma = 56.0,
            )

            else -> super.getTertiaryPalette(
                variant = variant,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            )
        }

    override fun getNeutralPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) 1.4 else 6.0,
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = if (platform === Platform.PHONE) 5.0 else 10.0,
            )

            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = getExpressiveNeutralHue(sourceColorHct),
                chroma = getExpressiveNeutralChroma(sourceColorHct, isDark, platform),
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = getVibrantNeutralHue(sourceColorHct),
                chroma = getVibrantNeutralChroma(sourceColorHct, platform),
            )

            else -> super.getNeutralPalette(
                variant = variant,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            )
        }

    override fun getNeutralVariantPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette =
        when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = (if (platform === Platform.PHONE) 1.4 else 6.0) * 2.2,
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = sourceColorHct.hue,
                chroma = (if (platform === Platform.PHONE) 5.0 else 10.0) * 1.7,
            )

            Variant.EXPRESSIVE -> {
                val expressiveNeutralHue = getExpressiveNeutralHue(sourceColorHct)
                val expressiveNeutralChroma =
                    getExpressiveNeutralChroma(sourceColorHct, isDark, platform)
                val d = if (expressiveNeutralHue >= 105 && expressiveNeutralHue < 125) 1.6 else 2.3
                TonalPalette.fromHueAndChroma(
                    hue = expressiveNeutralHue,
                    chroma = expressiveNeutralChroma * d,
                )
            }

            Variant.VIBRANT -> {
                val vibrantNeutralHue = getVibrantNeutralHue(sourceColorHct)
                val vibrantNeutralChroma = getVibrantNeutralChroma(sourceColorHct, platform)
                TonalPalette.fromHueAndChroma(vibrantNeutralHue, vibrantNeutralChroma * 1.29)
            }

            else -> super.getNeutralVariantPalette(
                variant = variant,
                sourceColorHct = sourceColorHct,
                isDark = isDark,
                platform = platform,
                contrastLevel = contrastLevel,
            )
        }

    override fun getErrorPalette(
        variant: Variant,
        sourceColorHct: Hct,
        isDark: Boolean,
        platform: Platform,
        contrastLevel: Double,
    ): TonalPalette? {
        val errorHue = DynamicScheme.getPiecewiseValue(
            sourceColorHct = sourceColorHct,
            hueBreakpoints = doubleArrayOf(0.0, 3.0, 13.0, 23.0, 33.0, 43.0, 153.0, 273.0, 360.0),
            hues = doubleArrayOf(12.0, 22.0, 32.0, 12.0, 22.0, 32.0, 22.0, 12.0),
        )

        return when (variant) {
            Variant.NEUTRAL -> TonalPalette.fromHueAndChroma(
                hue = errorHue,
                chroma = if (platform === Platform.PHONE) 50.0 else 40.0,
            )

            Variant.TONAL_SPOT -> TonalPalette.fromHueAndChroma(
                hue = errorHue,
                chroma = if (platform === Platform.PHONE) 60.0 else 48.0,
            )

            Variant.EXPRESSIVE -> TonalPalette.fromHueAndChroma(
                hue = errorHue,
                chroma = if (platform === Platform.PHONE) 64.0 else 48.0,
            )

            Variant.VIBRANT -> TonalPalette.fromHueAndChroma(
                hue = errorHue,
                chroma = if (platform === Platform.PHONE) 80.0 else 60.0,
            )

            else -> super.getErrorPalette(
                variant,
                sourceColorHct,
                isDark,
                platform,
                contrastLevel,
            )
        }
    }

    public companion object {
        private fun findBestToneForChroma(
            hue: Double,
            chroma: Double,
            tone: Double,
            byDecreasingTone: Boolean,
        ): Double {
            var tone = tone
            var answer = tone
            var bestCandidate: Hct = Hct.from(hue, chroma, answer)
            while (bestCandidate.chroma < chroma) {
                if (tone < 0 || tone > 100) {
                    break
                }
                tone += if (byDecreasingTone) -1.0 else 1.0
                val newCandidate: Hct = Hct.from(hue, chroma, tone)
                if (bestCandidate.chroma < newCandidate.chroma) {
                    bestCandidate = newCandidate
                    answer = tone
                }
            }
            return answer
        }

        private fun tMaxC(palette: TonalPalette): Double = tMaxC(palette, 0.0, 100.0)

        private fun tMaxC(
            palette: TonalPalette,
            lowerBound: Double,
            upperBound: Double,
            chromaMultiplier: Double = 1.0,
        ): Double {
            val answer =
                findBestToneForChroma(palette.hue, palette.chroma * chromaMultiplier, 100.0, true)

            return answer.coerceIn(lowerBound, upperBound)
        }

        private fun tMinC(palette: TonalPalette): Double = tMinC(palette, 0.0, 100.0)

        private fun tMinC(
            palette: TonalPalette,
            lowerBound: Double,
            upperBound: Double,
        ): Double {
            val answer = findBestToneForChroma(palette.hue, palette.chroma, 0.0, false)
            return answer.coerceIn(lowerBound, upperBound)
        }

        private fun getContrastCurve(defaultContrast: Double): ContrastCurve =
            when (defaultContrast) {
                1.5 -> ContrastCurve(1.5, 1.5, 3.0, 4.5)
                3.0 -> ContrastCurve(3.0, 3.0, 4.5, 7.0)
                4.5 -> ContrastCurve(4.5, 4.5, 7.0, 11.0)
                6.0 -> ContrastCurve(6.0, 6.0, 7.0, 11.0)
                7.0 -> ContrastCurve(7.0, 7.0, 11.0, 21.0)
                9.0 -> ContrastCurve(9.0, 9.0, 11.0, 21.0)
                11.0 -> ContrastCurve(11.0, 11.0, 21.0, 21.0)
                21.0 -> ContrastCurve(21.0, 21.0, 21.0, 21.0)
                else -> ContrastCurve(defaultContrast, defaultContrast, 7.0, 21.0)
            }

        private fun getExpressiveNeutralHue(sourceColorHct: Hct): Double =
            DynamicScheme.getRotatedHue(
                sourceColorHct = sourceColorHct,
                hueBreakpoints = doubleArrayOf(0.0, 71.0, 124.0, 253.0, 278.0, 300.0, 360.0),
                rotations = doubleArrayOf(10.0, 0.0, 10.0, 0.0, 10.0, 0.0),
            )

        private fun getExpressiveNeutralChroma(
            sourceColorHct: Hct,
            isDark: Boolean,
            platform: Platform,
        ): Double {
            val neutralHue = getExpressiveNeutralHue(sourceColorHct)

            val value =
                if (platform === Platform.PHONE) {
                    if (isDark) {
                        if (Hct.isYellow(neutralHue)) 6 else 14
                    } else {
                        18
                    }
                } else {
                    12
                }
            return value.toDouble()
        }

        private fun getVibrantNeutralHue(sourceColorHct: Hct): Double =
            DynamicScheme.getRotatedHue(
                sourceColorHct = sourceColorHct,
                hueBreakpoints = doubleArrayOf(0.0, 38.0, 105.0, 140.0, 333.0, 360.0),
                rotations = doubleArrayOf(-14.0, 10.0, -14.0, 10.0, -14.0),
            )

        private fun getVibrantNeutralChroma(
            sourceColorHct: Hct,
            platform: Platform,
        ): Double {
            val neutralHue = getVibrantNeutralHue(sourceColorHct)
            val value = if (platform === Platform.PHONE) 28 else (if (Hct.isBlue(neutralHue)) 28 else 20)
            return value.toDouble()
        }
    }
}
