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

import com.materialkolor.scheme.DynamicScheme
import dev.drewhamilton.poko.Poko
import kotlin.reflect.KFunction

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
    public fun highestSurface(scheme: DynamicScheme): DynamicColor = colorSpec.highestSurface(scheme)

    // Compatibility Keys Colors for Android
    public fun primaryPaletteKeyColor(): DynamicColor = colorSpec.primaryPaletteKeyColor()

    public fun secondaryPaletteKeyColor(): DynamicColor = colorSpec.secondaryPaletteKeyColor()

    public fun tertiaryPaletteKeyColor(): DynamicColor = colorSpec.tertiaryPaletteKeyColor()

    public fun errorPaletteKeyColor(): DynamicColor = colorSpec.errorPaletteKeyColor()

    public fun neutralPaletteKeyColor(): DynamicColor = colorSpec.neutralPaletteKeyColor()

    public fun neutralVariantPaletteKeyColor(): DynamicColor = colorSpec.neutralVariantPaletteKeyColor()
    public fun background(): DynamicColor = colorSpec.background()

    public fun onBackground(): DynamicColor = colorSpec.onBackground()

    public fun surface(): DynamicColor = colorSpec.surface()

    public fun surfaceDim(): DynamicColor = colorSpec.surfaceDim()

    public fun surfaceBright(): DynamicColor = colorSpec.surfaceBright()

    public fun surfaceContainerLowest(): DynamicColor = colorSpec.surfaceContainerLowest()

    public fun surfaceContainerLow(): DynamicColor = colorSpec.surfaceContainerLow()

    public fun surfaceContainer(): DynamicColor = colorSpec.surfaceContainer()

    public fun surfaceContainerHigh(): DynamicColor = colorSpec.surfaceContainerHigh()

    public fun surfaceContainerHighest(): DynamicColor = colorSpec.surfaceContainerHighest()

    public fun onSurface(): DynamicColor = colorSpec.onSurface()

    public fun surfaceVariant(): DynamicColor = colorSpec.surfaceVariant()

    public fun onSurfaceVariant(): DynamicColor = colorSpec.onSurfaceVariant()

    public fun inverseSurface(): DynamicColor = colorSpec.inverseSurface()

    public fun inverseOnSurface(): DynamicColor = colorSpec.inverseOnSurface()

    public fun outline(): DynamicColor = colorSpec.outline()

    public fun outlineVariant(): DynamicColor = colorSpec.outlineVariant()

    public fun shadow(): DynamicColor = colorSpec.shadow()

    public fun scrim(): DynamicColor = colorSpec.scrim()

    public fun surfaceTint(): DynamicColor = colorSpec.surfaceTint()

    public fun primary(): DynamicColor = colorSpec.primary()

    public fun onPrimary(): DynamicColor = colorSpec.onPrimary()

    public fun primaryContainer(): DynamicColor = colorSpec.primaryContainer()

    public fun onPrimaryContainer(): DynamicColor = colorSpec.onPrimaryContainer()

    public fun inversePrimary(): DynamicColor = colorSpec.inversePrimary()

    public fun secondary(): DynamicColor = colorSpec.secondary()

    public fun onSecondary(): DynamicColor = colorSpec.onSecondary()

    public fun secondaryContainer(): DynamicColor = colorSpec.secondaryContainer()

    public fun onSecondaryContainer(): DynamicColor = colorSpec.onSecondaryContainer()

    public fun tertiary(): DynamicColor = colorSpec.tertiary()

    public fun onTertiary(): DynamicColor = colorSpec.onTertiary()

    public fun tertiaryContainer(): DynamicColor = colorSpec.tertiaryContainer()

    public fun onTertiaryContainer(): DynamicColor = colorSpec.onTertiaryContainer()

    public fun error(): DynamicColor = colorSpec.error()

    public fun onError(): DynamicColor = colorSpec.onError()

    public fun errorContainer(): DynamicColor = colorSpec.errorContainer()

    public fun onErrorContainer(): DynamicColor = colorSpec.onErrorContainer()

    public fun primaryFixed(): DynamicColor = colorSpec.primaryFixed()

    public fun primaryFixedDim(): DynamicColor = colorSpec.primaryFixedDim()

    public fun onPrimaryFixed(): DynamicColor = colorSpec.onPrimaryFixed()

    public fun onPrimaryFixedVariant(): DynamicColor = colorSpec.onPrimaryFixedVariant()

    public fun secondaryFixed(): DynamicColor = colorSpec.secondaryFixed()

    public fun secondaryFixedDim(): DynamicColor = colorSpec.secondaryFixedDim()

    public fun onSecondaryFixed(): DynamicColor = colorSpec.onSecondaryFixed()

    public fun onSecondaryFixedVariant(): DynamicColor = colorSpec.onSecondaryFixedVariant()

    public fun tertiaryFixed(): DynamicColor = colorSpec.tertiaryFixed()

    public fun tertiaryFixedDim(): DynamicColor = colorSpec.tertiaryFixedDim()

    public fun onTertiaryFixed(): DynamicColor = colorSpec.onTertiaryFixed()

    public fun onTertiaryFixedVariant(): DynamicColor = colorSpec.onTertiaryFixedVariant()

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
    public fun controlActivated(): DynamicColor = colorSpec.controlActivated()

    /**
     * colorControlNormal documented as textColorSecondary in M3 & GM3.
     * In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
     * which is Neutral Variant T30/80 in light/dark.
     */
    public fun controlNormal(): DynamicColor = colorSpec.controlNormal()

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
    public fun controlHighlight(): DynamicColor = colorSpec.controlHighlight()

    /**
     * textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
     */
    public fun textPrimaryInverse(): DynamicColor = colorSpec.textPrimaryInverse()

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
     * V30/NV80
     */
    public fun textSecondaryAndTertiaryInverse(): DynamicColor = colorSpec.textSecondaryAndTertiaryInverse()

    /**
     * textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
     */
    public fun textPrimaryInverseDisableOnly(): DynamicColor = colorSpec.textPrimaryInverseDisableOnly()

    /**
     * textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
     * in both M3 & GM3, as N10/N90
     */
    public fun textSecondaryAndTertiaryInverseDisabled(): DynamicColor =
        colorSpec.textSecondaryAndTertiaryInverseDisabled()

    /**
     * textColorHintInverse documented, in both M3 & GM3, as N10/N90
     */
    public fun textHintInverse(): DynamicColor = colorSpec.textHintInverse()

    public fun allDynamicColors(): List<KFunction<DynamicColor>> =
        listOf(
            this::highestSurface,
            this::primaryPaletteKeyColor,
            this::secondaryPaletteKeyColor,
            this::tertiaryPaletteKeyColor,
            this::errorPaletteKeyColor,
            this::neutralPaletteKeyColor,
            this::neutralVariantPaletteKeyColor,
            this::background,
            this::onBackground,
            this::surface,
            this::surfaceDim,
            this::surfaceBright,
            this::surfaceContainerLowest,
            this::surfaceContainerLow,
            this::surfaceContainer,
            this::surfaceContainerHigh,
            this::surfaceContainerHighest,
            this::onSurface,
            this::surfaceVariant,
            this::onSurfaceVariant,
            this::inverseSurface,
            this::inverseOnSurface,
            this::outline,
            this::outlineVariant,
            this::shadow,
            this::scrim,
            this::surfaceTint,
            this::primary,
            this::onPrimary,
            this::primaryContainer,
            this::onPrimaryContainer,
            this::inversePrimary,
            this::secondary,
            this::onSecondary,
            this::secondaryContainer,
            this::onSecondaryContainer,
            this::tertiary,
            this::onTertiary,
            this::tertiaryContainer,
            this::onTertiaryContainer,
            this::error,
            this::onError,
            this::errorContainer,
            this::onErrorContainer,
            this::primaryFixed,
            this::primaryFixedDim,
            this::onPrimaryFixed,
            this::onPrimaryFixedVariant,
            this::secondaryFixed,
            this::secondaryFixedDim,
            this::onSecondaryFixed,
            this::onSecondaryFixedVariant,
            this::tertiaryFixed,
            this::tertiaryFixedDim,
            this::onTertiaryFixed,
            this::onTertiaryFixedVariant,
            this::controlActivated,
            this::controlNormal,
            this::controlHighlight,
            this::textPrimaryInverse,
            this::textSecondaryAndTertiaryInverse,
            this::textPrimaryInverseDisableOnly,
            this::textSecondaryAndTertiaryInverseDisabled,
            this::textHintInverse,
        )

    public companion object {
        private val colorSpec = ColorSpec2025()
    }
}
