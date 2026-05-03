package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.scheme.SchemeCmf
import com.materialkolor.scheme.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DynamicSchemeCmfTest {
    private val red = Color.Red
    private val blue = Color.Blue

    @Test
    fun cmfStyle_producesSchemeCmf() {
        val scheme = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(),
        )
        assertTrue(scheme is SchemeCmf)
        assertEquals(Variant.CMF, scheme.variant)
    }

    @Test
    fun cmfStyle_forcesSpec2026_whenCallerPassesOlderSpec() {
        val scheme = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(),
            specVersion = ColorSpec.SpecVersion.SPEC_2021,
        )
        assertEquals(ColorSpec.SpecVersion.SPEC_2026, scheme.specVersion)
    }

    @Test
    fun cmfStyle_singleSource_listHasOneEntry() {
        val scheme = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(tertiarySourceColor = null),
        )
        assertEquals(1, scheme.sourceColorHctList.size)
    }

    @Test
    fun cmfStyle_dualSource_listHasTwoEntries() {
        val scheme = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(tertiarySourceColor = blue),
        )
        assertEquals(2, scheme.sourceColorHctList.size)
    }

    @Test
    fun cmfStyle_dualSource_changesTertiaryFromSingleSource() {
        val singleSource = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(),
        )
        val dualSource = red.toDynamicScheme(
            isDark = false,
            style = PaletteStyle.Cmf(tertiarySourceColor = blue),
        )
        assertNotEquals(
            singleSource.tertiaryPalette.tone(50),
            dualSource.tertiaryPalette.tone(50),
        )
    }
}
