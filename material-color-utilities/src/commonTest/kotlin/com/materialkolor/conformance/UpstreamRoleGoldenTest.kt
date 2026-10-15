package com.materialkolor.conformance

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.DynamicScheme.Platform
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFidelity
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeRainbow
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import kotlin.test.Test
import kotlin.test.assertEquals

class UpstreamRoleGoldenTest {
    @Test
    fun directArgbGoldensFromPinnedRawKotlin() {
        val rows = upstreamRoleGoldens.split("\n\n")
        assertEquals(
            23,
            rows.size,
            "Pins how many scheme cases the fixture covers. Only conformance.GenerateGoldenFixtures " +
                "changes this count; update it alongside a reviewed regeneration.",
        )
        for (row in rows) {
            val lines = row.lines()
            val label = lines.first()
            val expectations = lines.drop(1).joinToString(" ")
            val fields = label.split('|')
            val spec = SpecVersion.valueOf(fields[1])
            val dark = fields[2].toBooleanStrict()
            val contrast = fields[3].toDouble()
            val platform = Platform.valueOf(fields[4])
            val seeds = fields[5].split(',').map { Hct.fromInt(it.toUInt(16).toInt()) }
            val scheme = when (fields[0]) {
                "TonalSpot" -> SchemeTonalSpot(seeds, dark, contrast, spec, platform)
                "Neutral" -> SchemeNeutral(seeds, dark, contrast, spec, platform)
                "Vibrant" -> SchemeVibrant(seeds, dark, contrast, spec, platform)
                "Expressive" -> SchemeExpressive(seeds, dark, contrast, spec, platform)
                "Fidelity" -> SchemeFidelity(seeds, dark, contrast, spec, platform)
                "Content" -> SchemeContent(seeds, dark, contrast, spec, platform)
                "Rainbow" -> SchemeRainbow(seeds, dark, contrast, spec, platform)
                "FruitSalad" -> SchemeFruitSalad(seeds, dark, contrast, spec, platform)
                "Monochrome" -> SchemeMonochrome(seeds, dark, contrast, spec, platform)
                "Cmf" -> SchemeCmf(seeds, dark, contrast, spec, platform)
                else -> error("Unknown fixture $label")
            }
            val actual = roleArgb(scheme)
            val expected = expectations.split(' ').associate { entry ->
                val (role, argb) = entry.split('=')
                role to if (argb == "null") null else argb.toUInt(16).toInt()
            }
            assertEquals(
                59,
                expected.size,
                "$label pins the role inventory every fixture row carries. Only a reviewed " +
                    "regeneration changes this count, and roleArgb below must change with it.",
            )
            assertEquals(expected.keys, actual.keys, "$label role names")
            for ((role, argb) in expected) {
                assertEquals(argb, actual[role], "$label/$role expected ${argb.hex()}, actual ${actual[role].hex()}")
            }
        }
    }

    private fun Int?.hex(): String = this?.toUInt()?.toString(16) ?: "null"

    // Duplicated, deliberately, by ROLE_NAMES in
    // tools/mcu-upstream/src/test/java/conformance/ReferenceMcu.kt. The two modules cannot share a source
    // set, and this copy is what catches fixture inventory drift, so keep both lists identical,
    // same names, same order, same count.
    private fun roleArgb(scheme: DynamicScheme): Map<String, Int?> {
        val colors = MaterialDynamicColors()
        return listOf(
            "primaryPaletteKeyColor" to colors.primaryPaletteKeyColor,
            "secondaryPaletteKeyColor" to colors.secondaryPaletteKeyColor,
            "tertiaryPaletteKeyColor" to colors.tertiaryPaletteKeyColor,
            "neutralPaletteKeyColor" to colors.neutralPaletteKeyColor,
            "neutralVariantPaletteKeyColor" to colors.neutralVariantPaletteKeyColor,
            "errorPaletteKeyColor" to colors.errorPaletteKeyColor,
            "background" to colors.background,
            "onBackground" to colors.onBackground,
            "surface" to colors.surface,
            "surfaceDim" to colors.surfaceDim,
            "surfaceBright" to colors.surfaceBright,
            "surfaceContainerLowest" to colors.surfaceContainerLowest,
            "surfaceContainerLow" to colors.surfaceContainerLow,
            "surfaceContainer" to colors.surfaceContainer,
            "surfaceContainerHigh" to colors.surfaceContainerHigh,
            "surfaceContainerHighest" to colors.surfaceContainerHighest,
            "onSurface" to colors.onSurface,
            "surfaceVariant" to colors.surfaceVariant,
            "onSurfaceVariant" to colors.onSurfaceVariant,
            "inverseSurface" to colors.inverseSurface,
            "inverseOnSurface" to colors.inverseOnSurface,
            "outline" to colors.outline,
            "outlineVariant" to colors.outlineVariant,
            "shadow" to colors.shadow,
            "scrim" to colors.scrim,
            "surfaceTint" to colors.surfaceTint,
            "primary" to colors.primary,
            "primaryDim" to colors.primaryDim,
            "onPrimary" to colors.onPrimary,
            "primaryContainer" to colors.primaryContainer,
            "onPrimaryContainer" to colors.onPrimaryContainer,
            "inversePrimary" to colors.inversePrimary,
            "primaryFixed" to colors.primaryFixed,
            "primaryFixedDim" to colors.primaryFixedDim,
            "onPrimaryFixed" to colors.onPrimaryFixed,
            "onPrimaryFixedVariant" to colors.onPrimaryFixedVariant,
            "secondary" to colors.secondary,
            "secondaryDim" to colors.secondaryDim,
            "onSecondary" to colors.onSecondary,
            "secondaryContainer" to colors.secondaryContainer,
            "onSecondaryContainer" to colors.onSecondaryContainer,
            "secondaryFixed" to colors.secondaryFixed,
            "secondaryFixedDim" to colors.secondaryFixedDim,
            "onSecondaryFixed" to colors.onSecondaryFixed,
            "onSecondaryFixedVariant" to colors.onSecondaryFixedVariant,
            "tertiary" to colors.tertiary,
            "tertiaryDim" to colors.tertiaryDim,
            "onTertiary" to colors.onTertiary,
            "tertiaryContainer" to colors.tertiaryContainer,
            "onTertiaryContainer" to colors.onTertiaryContainer,
            "tertiaryFixed" to colors.tertiaryFixed,
            "tertiaryFixedDim" to colors.tertiaryFixedDim,
            "onTertiaryFixed" to colors.onTertiaryFixed,
            "onTertiaryFixedVariant" to colors.onTertiaryFixedVariant,
            "error" to colors.error,
            "errorDim" to colors.errorDim,
            "onError" to colors.onError,
            "errorContainer" to colors.errorContainer,
            "onErrorContainer" to colors.onErrorContainer,
        ).associate { (name, color) -> name to color?.getArgb(scheme) }
    }
}
