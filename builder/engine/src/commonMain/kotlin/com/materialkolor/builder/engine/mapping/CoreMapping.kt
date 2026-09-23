package com.materialkolor.builder.engine.mapping

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme

/*
 * The one place the document's own types turn into the library's.
 *
 * Contrast needs no function here. The document keeps it in hundredths and ContrastLevel.toDouble
 * already hands the engine the double it wants.
 */

/**
 * The library style for this document style.
 *
 * Only [Style.Cmf] reads [cmfTertiarySeed]. The document keeps that seed in its own field, so it
 * is passed in rather than carried by the style, and every other style ignores it.
 */
public fun Style.toPaletteStyle(cmfTertiarySeed: Argb?): PaletteStyle =
    when (this) {
        Style.TonalSpot -> PaletteStyle.TonalSpot
        Style.Neutral -> PaletteStyle.Neutral
        Style.Vibrant -> PaletteStyle.Vibrant
        Style.Expressive -> PaletteStyle.Expressive
        Style.Rainbow -> PaletteStyle.Rainbow
        Style.FruitSalad -> PaletteStyle.FruitSalad
        Style.Monochrome -> PaletteStyle.Monochrome
        Style.Fidelity -> PaletteStyle.Fidelity
        Style.Content -> PaletteStyle.Content
        Style.Cmf -> PaletteStyle.Cmf(tertiarySeedColor = cmfTertiarySeed?.toColor())
    }

/**
 * The library spec version for this document spec.
 */
public fun SpecVersion.toCore(): ColorSpec.SpecVersion =
    when (this) {
        SpecVersion.Spec2021 -> ColorSpec.SpecVersion.SPEC_2021
        SpecVersion.Spec2025 -> ColorSpec.SpecVersion.SPEC_2025
        SpecVersion.Spec2026 -> ColorSpec.SpecVersion.SPEC_2026
    }

/**
 * The document spec for a library spec version, used to read back the spec a scheme really ran.
 */
public fun ColorSpec.SpecVersion.toDomain(): SpecVersion =
    when (this) {
        ColorSpec.SpecVersion.SPEC_2021 -> SpecVersion.Spec2021
        ColorSpec.SpecVersion.SPEC_2025 -> SpecVersion.Spec2025
        ColorSpec.SpecVersion.SPEC_2026 -> SpecVersion.Spec2026
    }

/**
 * The library platform for this document platform.
 */
public fun SchemePlatform.toCore(): DynamicScheme.Platform =
    when (this) {
        SchemePlatform.Phone -> DynamicScheme.Platform.PHONE
        SchemePlatform.Watch -> DynamicScheme.Platform.WATCH
    }

/**
 * The Compose color for this document color. An [Argb] is always opaque, so the result is too.
 */
public fun Argb.toColor(): Color = Color(value)
