package com.materialkolor.ktx

import com.materialkolor.dynamiccolor.ColorSpec.SpecVersion
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme.Platform
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.SchemeTonalSpot
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyAndroidColorsTest {
    @Test
    fun legacy2021RolesMatchOriginalPortArgbIncludingHighlightAlpha() {
        val colors = MaterialDynamicColors()
        // Light and dark ARGB captured from the original b72e784 port, before adopting upstream
        // Kotlin. Each role carries its own two values so reordering cannot desync them.
        val cases: List<Triple<DynamicColor, Long, Long>> = listOf(
            Triple(colors.controlActivated, 0xffd8e2ff, 0xff2b4678),
            Triple(colors.controlNormal, 0xff44474f, 0xffc4c6d0),
            Triple(colors.controlHighlight, 0x1f000000, 0x33ffffff),
            Triple(colors.textPrimaryInverse, 0xffe2e2e9, 0xff1a1b20),
            Triple(colors.textSecondaryAndTertiaryInverse, 0xffc4c6d0, 0xff44474f),
            Triple(colors.textPrimaryInverseDisableOnly, 0xffe2e2e9, 0xff1a1b20),
            Triple(colors.textSecondaryAndTertiaryInverseDisabled, 0xffe2e2e9, 0xff1a1b20),
            Triple(colors.textHintInverse, 0xffe2e2e9, 0xff1a1b20),
        )
        for (isDark in listOf(false, true)) {
            val scheme = SchemeTonalSpot(Hct.fromInt(0xff4285f4.toInt()), isDark, 0.0)
            for ((role, light, dark) in cases) {
                val expected = if (isDark) dark else light
                assertEquals(expected.toInt(), role.getArgb(scheme), "${role.name} isDark=$isDark")
            }
        }
    }

    @Test
    fun newerSpecificationsRemapThreeLegacyRolesToCanonicalProviders() {
        val colors = MaterialDynamicColors()
        val source = Hct.fromInt(0xff4285f4.toInt())
        for (isDark in listOf(false, true)) {
            for (platform in Platform.entries) {
                for (contrast in listOf(-1.0, 0.0, 1.0)) {
                    val schemes = listOf(
                        SchemeTonalSpot(source, isDark, contrast, SpecVersion.SPEC_2025, platform),
                        SchemeCmf(source, isDark, contrast, platform = platform),
                    )
                    for (scheme in schemes) {
                        assertEquals(
                            colors.primaryContainer.getArgb(scheme),
                            colors.controlActivated.getArgb(scheme),
                            scheme.toString(),
                        )
                        assertEquals(
                            colors.onSurfaceVariant.getArgb(scheme),
                            colors.controlNormal.getArgb(scheme),
                            scheme.toString(),
                        )
                        assertEquals(
                            colors.inverseOnSurface.getArgb(scheme),
                            colors.textPrimaryInverse.getArgb(scheme),
                            scheme.toString(),
                        )
                    }
                }
            }
        }
    }
}
