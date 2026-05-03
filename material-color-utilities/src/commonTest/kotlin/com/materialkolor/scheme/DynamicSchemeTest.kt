package com.materialkolor.scheme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DynamicSchemeTest {
    private val red = Hct.fromInt(-0x10000)

    private fun palette(hue: Double): TonalPalette =
        TonalPalette.fromHueAndChroma(hue, 50.0)

    @Test
    fun emptySourceList_throws() {
        assertFailsWith<IllegalArgumentException> {
            DynamicScheme(
                sourceColorHctList = emptyList(),
                variant = Variant.TONAL_SPOT,
                isDark = false,
                contrastLevel = 0.0,
                primaryPalette = palette(red.hue),
                secondaryPalette = palette(red.hue),
                tertiaryPalette = palette(red.hue),
                neutralPalette = palette(red.hue),
                neutralVariantPalette = palette(red.hue),
            )
        }
    }

    @Test
    fun singleSourceCtor_populatesList() {
        val scheme = DynamicScheme(
            sourceColorHct = red,
            variant = Variant.TONAL_SPOT,
            isDark = false,
            contrastLevel = 0.0,
            platform = DynamicScheme.Platform.PHONE,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            primaryPalette = palette(red.hue),
            secondaryPalette = palette(red.hue),
            tertiaryPalette = palette(red.hue),
            neutralPalette = palette(red.hue),
            neutralVariantPalette = palette(red.hue),
        )
        assertEquals(1, scheme.sourceColorHctList.size)
        assertEquals(red, scheme.sourceColorHctList.first())
        assertEquals(red, scheme.sourceColorHct)
    }
}
