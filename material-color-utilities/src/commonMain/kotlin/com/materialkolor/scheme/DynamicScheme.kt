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
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.utils.MathUtils
import kotlin.math.min

/**
 * Provides important settings for creating colors dynamically, and 6 color palettes. Requires: 1. A
 * color. (source color) 2. A theme. (Variant) 3. Whether or not its dark mode. 4. Contrast level.
 * (-1 to 1, currently contrast ratio 3.0 and 7.0)
 *
 * @property sourceColorHct The source color HCT.
 * @property variant The theme.
 * @property isDark Whether or not the theme is in dark mode.
 * @property platform The platform on which this scheme is intended to be used.
 * @property contrastLevel Value from -1 to 1. -1 represents minimum contrast. 0 represents standard
 * (i.e. the design as spec'd), and 1 represents maximum contrast.
 * @property specVersion The version of the color specification.
 * @property primaryPalette The primary color palette.
 * @property secondaryPalette The secondary color palette.
 * @property tertiaryPalette The tertiary color palette.
 * @property neutralPalette Palette The neutral color palette.
 * @property neutralVariantPalette The neutral variant color palette.
 * @property errorPalette The error color palette.
 * @constructor Creates a DynamicScheme.
 */
public open class DynamicScheme(
    public val sourceColorHct: Hct,
    public val variant: Variant,
    public val isDark: Boolean,
    public val contrastLevel: Double,
    public val primaryPalette: TonalPalette,
    public val secondaryPalette: TonalPalette,
    public val tertiaryPalette: TonalPalette,
    public val neutralPalette: TonalPalette,
    public val neutralVariantPalette: TonalPalette,
    public val platform: Platform = Platform.PHONE,
    public val specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    public val errorPalette: TonalPalette = TonalPalette.fromHueAndChroma(hue = 25.0, chroma = 84.0),
) {
    /**
     * The platform on which this scheme is intended to be used.
     */
    public enum class Platform {
        PHONE,
        WATCH,
        ;

        public companion object {
            public val Default: Platform = PHONE
        }
    }

    public constructor(
        sourceColorHct: Hct,
        variant: Variant,
        isDark: Boolean,
        contrastLevel: Double,
        platform: Platform = Platform.PHONE,
        specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
        primaryPalette: TonalPalette,
        secondaryPalette: TonalPalette,
        tertiaryPalette: TonalPalette,
        neutralPalette: TonalPalette,
        neutralVariantPalette: TonalPalette,
        errorPalette: TonalPalette?
    ) : this(
        sourceColorHct = sourceColorHct,
        variant = variant,
        isDark = isDark,
        contrastLevel = contrastLevel,
        platform = platform,
        specVersion = specVersion,
        primaryPalette = primaryPalette,
        secondaryPalette = secondaryPalette,
        tertiaryPalette = tertiaryPalette,
        neutralPalette = neutralPalette,
        neutralVariantPalette = neutralVariantPalette,
        errorPalette = errorPalette ?: TonalPalette.fromHueAndChroma(hue = 25.0, chroma = 84.0)
    )

    private val materialColors: MaterialDynamicColors = MaterialDynamicColors()

    public val sourceColorArgb: Int = sourceColorHct.toInt()

    public fun getHct(dynamicColor: DynamicColor): Hct = dynamicColor.getHct(this)

    public fun getArgb(dynamicColor: DynamicColor): Int = dynamicColor.getArgb(this)

    public val primaryPaletteKeyColor: Int
        get() = getArgb(materialColors.primaryPaletteKeyColor())

    public val secondaryPaletteKeyColor: Int
        get() = getArgb(materialColors.secondaryPaletteKeyColor())

    public val tertiaryPaletteKeyColor: Int
        get() = getArgb(materialColors.tertiaryPaletteKeyColor())

    public val neutralPaletteKeyColor: Int
        get() = getArgb(materialColors.neutralPaletteKeyColor())

    public val neutralVariantPaletteKeyColor: Int
        get() = getArgb(materialColors.neutralVariantPaletteKeyColor())

    public val background: Int
        get() = getArgb(materialColors.background())

    public val onBackground: Int
        get() = getArgb(materialColors.onBackground())

    public val surface: Int
        get() = getArgb(materialColors.surface())

    public val surfaceDim: Int
        get() = getArgb(materialColors.surfaceDim())

    public val surfaceBright: Int
        get() = getArgb(materialColors.surfaceBright())

    public val surfaceContainerLowest: Int
        get() = getArgb(materialColors.surfaceContainerLowest())

    public val surfaceContainerLow: Int
        get() = getArgb(materialColors.surfaceContainerLow())

    public val surfaceContainer: Int
        get() = getArgb(materialColors.surfaceContainer())

    public val surfaceContainerHigh: Int
        get() = getArgb(materialColors.surfaceContainerHigh())

    public val surfaceContainerHighest: Int
        get() = getArgb(materialColors.surfaceContainerHighest())

    public val onSurface: Int
        get() = getArgb(materialColors.onSurface())

    public val surfaceVariant: Int
        get() = getArgb(materialColors.surfaceVariant())

    public val onSurfaceVariant: Int
        get() = getArgb(materialColors.onSurfaceVariant())

    public val inverseSurface: Int
        get() = getArgb(materialColors.inverseSurface())

    public val inverseOnSurface: Int
        get() = getArgb(materialColors.inverseOnSurface())

    public val outline: Int
        get() = getArgb(materialColors.outline())

    public val outlineVariant: Int
        get() = getArgb(materialColors.outlineVariant())

    public val shadow: Int
        get() = getArgb(materialColors.shadow())

    public val scrim: Int
        get() = getArgb(materialColors.scrim())

    public val surfaceTint: Int
        get() = getArgb(materialColors.surfaceTint())

    public val primary: Int
        get() = getArgb(materialColors.primary())

    public val onPrimary: Int
        get() = getArgb(materialColors.onPrimary())

    public val primaryContainer: Int
        get() = getArgb(materialColors.primaryContainer())

    public val onPrimaryContainer: Int
        get() = getArgb(materialColors.onPrimaryContainer())

    public val inversePrimary: Int
        get() = getArgb(materialColors.inversePrimary())

    public val secondary: Int
        get() = getArgb(materialColors.secondary())

    public val onSecondary: Int
        get() = getArgb(materialColors.onSecondary())

    public val secondaryContainer: Int
        get() = getArgb(materialColors.secondaryContainer())

    public val onSecondaryContainer: Int
        get() = getArgb(materialColors.onSecondaryContainer())

    public val tertiary: Int
        get() = getArgb(materialColors.tertiary())

    public val onTertiary: Int
        get() = getArgb(materialColors.onTertiary())

    public val tertiaryContainer: Int
        get() = getArgb(materialColors.tertiaryContainer())

    public val onTertiaryContainer: Int
        get() = getArgb(materialColors.onTertiaryContainer())

    public val error: Int
        get() = getArgb(materialColors.error())

    public val onError: Int
        get() = getArgb(materialColors.onError())

    public val errorContainer: Int
        get() = getArgb(materialColors.errorContainer())

    public val onErrorContainer: Int
        get() = getArgb(materialColors.onErrorContainer())

    public val primaryFixed: Int
        get() = getArgb(materialColors.primaryFixed())

    public val primaryFixedDim: Int
        get() = getArgb(materialColors.primaryFixedDim())

    public val onPrimaryFixed: Int
        get() = getArgb(materialColors.onPrimaryFixed())

    public val onPrimaryFixedVariant: Int
        get() = getArgb(materialColors.onPrimaryFixedVariant())

    public val secondaryFixed: Int
        get() = getArgb(materialColors.secondaryFixed())

    public val secondaryFixedDim: Int
        get() = getArgb(materialColors.secondaryFixedDim())

    public val onSecondaryFixed: Int
        get() = getArgb(materialColors.onSecondaryFixed())

    public val onSecondaryFixedVariant: Int
        get() = getArgb(materialColors.onSecondaryFixedVariant())

    public val tertiaryFixed: Int
        get() = getArgb(materialColors.tertiaryFixed())

    public val tertiaryFixedDim: Int
        get() = getArgb(materialColors.tertiaryFixedDim())

    public val onTertiaryFixed: Int
        get() = getArgb(materialColors.onTertiaryFixed())

    public val onTertiaryFixedVariant: Int
        get() = getArgb(materialColors.onTertiaryFixedVariant())

    public val controlActivated: Int
        get() = getArgb(materialColors.controlActivated())

    public val controlNormal: Int
        get() = getArgb(materialColors.controlNormal())

    public val controlHighlight: Int
        get() = getArgb(materialColors.controlHighlight())

    public val textPrimaryInverse: Int
        get() = getArgb(materialColors.textPrimaryInverse())

    public val textSecondaryAndTertiaryInverse: Int
        get() = getArgb(materialColors.textSecondaryAndTertiaryInverse())

    public val textPrimaryInverseDisableOnly: Int
        get() = getArgb(materialColors.textPrimaryInverseDisableOnly())

    public val textSecondaryAndTertiaryInverseDisabled: Int
        get() = getArgb(materialColors.textSecondaryAndTertiaryInverseDisabled())

    public val textHintInverse: Int
        get() = getArgb(materialColors.textHintInverse())

    public companion object {
        /**
         * Returns a new DynamicScheme instance with the same properties as the given scheme, but with
         * the given dark mode setting.
         */
        public fun from(other: DynamicScheme, isDark: Boolean): DynamicScheme = DynamicScheme(
            sourceColorHct = other.sourceColorHct,
            variant = other.variant,
            isDark = isDark,
            contrastLevel = other.contrastLevel,
            primaryPalette = other.primaryPalette,
            secondaryPalette = other.secondaryPalette,
            tertiaryPalette = other.tertiaryPalette,
            neutralPalette = other.neutralPalette,
            neutralVariantPalette = other.neutralVariantPalette,
            platform = other.platform,
            specVersion = other.specVersion,
            errorPalette = other.errorPalette,
        )

        /**
         * Returns a new hue based on a piecewise function and input color hue.
         *
         * For example, for the following function:
         *
         * ```
         * result = 26, if 0 <= hue < 101;
         * result = 39, if 101 <= hue < 210;
         * result = 28, if 210 <= hue < 360.
         * ```
         *
         * Call the function as:
         *
         * ```
         * val hueBreakpoints = listOf(0, 101, 210, 360)
         * val hues = listOf(26, 39, 28)
         * val result = scheme.piecewise(sourceColor, hueBreakpoints, hues)
         * ```
         *
         * @param sourceColorHct The input value.
         * @param hueBreakpoints The breakpoints, in sorted order. No default lower or upper bounds are
         * assumed.
         * @param hues The hues that should be applied when source color's hue is >= the same index in
         * hueBreakpoints array, and < the hue at the next index in hueBreakpoints array. Otherwise,
         * the source color's hue is returned.
         */
        public fun getPiecewiseValue(
            sourceColorHct: Hct,
            hueBreakpoints: DoubleArray,
            hues: DoubleArray
        ): Double {
            val size = min(hueBreakpoints.size - 1, hues.size)
            val sourceHue: Double = sourceColorHct.hue
            for (i in 0..<size) {
                if (sourceHue >= hueBreakpoints[i] && sourceHue < hueBreakpoints[i + 1]) {
                    return MathUtils.sanitizeDegrees(hues[i])
                }
            }
            // No condition matched, return the source value.
            return sourceHue
        }

        /**
         * Returns a shifted hue based on a piecewise function and input color hue.
         *
         *
         * For example, for the following function:
         *
         * ```
         * result = hue + 26, if 0 <= hue < 101
         * result = hue - 39, if 101 <= hue < 210
         * result = hue + 28, if 210 <= hue < 360
         * ```
         *
         *
         * call the function as:
         *
         * ```
         * val hueBreakpoints = listOf(0, 101, 210, 360)
         * val rotations = listOf(26, -39, 28)
         * val result = scheme.getRotatedHue(sourceColor, hueBreakpoints, rotations)
         * ```
         *
         * @param sourceColorHct the source color of the theme, in HCT.
         * @param hueBreakpoints The "breakpoints", i.e. the hues at which a rotation should be apply. No
         * default lower or upper bounds are assumed.
         * @param rotations The rotation that should be applied when source color's hue is >= the same
         * index in hues array, and < the hue at the next index in hues array. Otherwise, the source
         * color's hue is returned.
         */
        public fun getRotatedHue(
            sourceColorHct: Hct,
            hueBreakpoints: DoubleArray,
            rotations: DoubleArray
        ): Double {
            var rotation: Double = getPiecewiseValue(sourceColorHct, hueBreakpoints, rotations)
            if (min(hueBreakpoints.size - 1, rotations.size) <= 0) {
                // No condition matched, return the source hue.
                rotation = 0.0
            }

            return MathUtils.sanitizeDegrees(sourceColorHct.hue + rotation)
        }
    }
}
