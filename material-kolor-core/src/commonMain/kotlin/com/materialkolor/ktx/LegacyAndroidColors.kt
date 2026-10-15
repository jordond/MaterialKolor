/*
 * Copyright 2021 Google LLC
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
package com.materialkolor.ktx

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.dynamiccolor.extendSpecVersion

/**
 * Legacy Android control accent, retained for existing MaterialKolor themes and exports.
 */
public val MaterialDynamicColors.controlActivated: DynamicColor
    get() = DynamicColor(
        name = "control_activated",
        palette = { it.primaryPalette },
        tone = { if (it.isDark) 30.0 else 90.0 },
        isBackground = true,
    ).extendSpecVersion(SpecVersion.SPEC_2025, primaryContainer.aliasedAs("control_activated"))

/**
 * Legacy Android secondary control color, mapped to on-surface-variant from the 2025 spec.
 */
public val MaterialDynamicColors.controlNormal: DynamicColor
    get() = DynamicColor(
        name = "control_normal",
        palette = { it.neutralVariantPalette },
        tone = { if (it.isDark) 80.0 else 30.0 },
    ).extendSpecVersion(SpecVersion.SPEC_2025, onSurfaceVariant.aliasedAs("control_normal"))

/**
 * Legacy Android control highlight, translucent white in dark mode and black in light mode.
 */
public val MaterialDynamicColors.controlHighlight: DynamicColor
    get() = DynamicColor(
        name = "control_highlight",
        palette = { it.neutralPalette },
        tone = { if (it.isDark) 100.0 else 0.0 },
        opacity = { if (it.isDark) 0.20 else 0.12 },
    )

/**
 * Legacy Android inverse primary text, mapped to inverse-on-surface from the 2025 spec.
 */
public val MaterialDynamicColors.textPrimaryInverse: DynamicColor
    get() = inverseTextColor("text_primary_inverse")
        .extendSpecVersion(SpecVersion.SPEC_2025, inverseOnSurface.aliasedAs("text_primary_inverse"))

/**
 * Legacy Android inverse secondary and tertiary text.
 */
public val MaterialDynamicColors.textSecondaryAndTertiaryInverse: DynamicColor
    get() = DynamicColor(
        name = "text_secondary_and_tertiary_inverse",
        palette = { it.neutralVariantPalette },
        tone = { if (it.isDark) 30.0 else 80.0 },
    )

/**
 * Legacy Android inverse primary text for controls with a separate disabled state.
 */
public val MaterialDynamicColors.textPrimaryInverseDisableOnly: DynamicColor
    get() = inverseTextColor("text_primary_inverse_disable_only")

/**
 * Legacy Android disabled inverse secondary and tertiary text.
 */
public val MaterialDynamicColors.textSecondaryAndTertiaryInverseDisabled: DynamicColor
    get() = inverseTextColor("text_secondary_and_tertiary_inverse_disabled")

/**
 * Legacy Android inverse hint text.
 */
public val MaterialDynamicColors.textHintInverse: DynamicColor
    get() = inverseTextColor("text_hint_inverse")

private fun inverseTextColor(name: String): DynamicColor =
    DynamicColor(
        name = name,
        palette = { it.neutralPalette },
        tone = { if (it.isDark) 10.0 else 90.0 },
    )

/**
 * Re-exposes this color's *resolved* values under [legacyName] without handing the legacy name to
 * the spec resolver.
 *
 * `extendSpecVersion` requires the replacement to carry the same name as the color it extends, but
 * the 2025 spec resolves tone-delta pairs by comparing `color.name` against the pair's roles, so a
 * renamed `copy()` of a canonical role stops matching its own role and is resolved as the opposite
 * one. Instead of copying the definition, this evaluates the canonical color (which keeps its real
 * name, and therefore its pair identity) and only forwards the finished tone, palette and chroma
 * multiplier. Background/contrast-curve/tone-delta hooks are deliberately omitted. The tone handed
 * back is already fully resolved, so the spec must not adjust it a second time.
 */
private fun DynamicColor.aliasedAs(legacyName: String): DynamicColor {
    val canonical = this
    return DynamicColor(
        name = legacyName,
        palette = { scheme -> canonical.palette(scheme) },
        isBackground = canonical.isBackground,
        chromaMultiplier = { scheme -> canonical.chromaMultiplier?.invoke(scheme) ?: 1.0 },
        tone = { scheme -> canonical.getTone(scheme) },
        opacity = canonical.opacity,
    )
}
