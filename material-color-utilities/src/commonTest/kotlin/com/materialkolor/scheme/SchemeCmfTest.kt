package com.materialkolor.scheme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class SchemeCmfTest {
    private val red = Hct.fromInt(-0x10000)
    private val blue = Hct.fromInt(-0xffff01)

    @Test
    fun singleSourceCtor_setsVariantAndSpec() {
        val scheme = SchemeCmf(
            sourceColorHct = red,
            isDark = false,
            contrastLevel = 0.0,
        )
        assertEquals(Variant.CMF, scheme.variant)
        assertEquals(ColorSpec.SpecVersion.SPEC_2026, scheme.specVersion)
        assertEquals(1, scheme.sourceColorHctList.size)
        assertSame(red, scheme.sourceColorHctList.first())
    }

    @Test
    fun listSourceCtor_storesAllColors() {
        val scheme = SchemeCmf(
            sourceColorHctList = listOf(red, blue),
            isDark = false,
            contrastLevel = 0.0,
        )
        assertEquals(2, scheme.sourceColorHctList.size)
        assertSame(red, scheme.sourceColorHctList[0])
        assertSame(blue, scheme.sourceColorHctList[1])
        assertSame(red, scheme.sourceColorHct)
    }

    @Test
    fun nonSpec2026_throws() {
        assertFailsWith<IllegalArgumentException> {
            SchemeCmf(
                sourceColorHct = red,
                isDark = false,
                contrastLevel = 0.0,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SchemeCmf(
                sourceColorHct = red,
                isDark = false,
                contrastLevel = 0.0,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
            )
        }
    }

    @Test
    fun secondSource_changesTertiaryPalette() {
        val singleSource = SchemeCmf(
            sourceColorHctList = listOf(red),
            isDark = false,
            contrastLevel = 0.0,
        )
        val dualSource = SchemeCmf(
            sourceColorHctList = listOf(red, blue),
            isDark = false,
            contrastLevel = 0.0,
        )
        assertNotEquals(
            singleSource.tertiaryPalette.tone(50),
            dualSource.tertiaryPalette.tone(50),
        )
    }
}
