package com.materialkolor

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PaletteStyleTest {
    @Test
    fun everyKnownStyleIsFoundByItsName() {
        for (style in PaletteStyle.KnownStyles) {
            assertEquals(style, PaletteStyle.fromName(style.name))
        }
    }

    @Test
    fun unknownNamesAreNull() {
        assertNull(PaletteStyle.fromName("cmf"))
        assertNull(PaletteStyle.fromName("nope"))
    }

    @Test
    fun everyKnownStyleRoundTripsThroughItsString() {
        for (style in PaletteStyle.KnownStyles) {
            assertEquals(style, PaletteStyle.parse(style.toString()))
        }
    }

    @Test
    fun objectsWriteTheirName() {
        assertEquals("TonalSpot", PaletteStyle.TonalSpot.toString())
        assertEquals("Cmf", PaletteStyle.Cmf().toString())
    }

    @Test
    fun aSeededCmfCarriesItsSeedThroughItsString() {
        val style = PaletteStyle.Cmf(Color(0xFF7D5260))

        assertEquals("Cmf:FF7D5260", style.toString())
        assertEquals(style, PaletteStyle.parse(style.toString()))
        assertEquals(style, PaletteStyle.parse("Cmf:ff7d5260"))
    }

    @Test
    fun cmfComparesByItsSeed() {
        val seed = Color(0xFF7D5260)

        assertEquals(PaletteStyle.Cmf(seed), PaletteStyle.Cmf(seed))
        assertEquals(PaletteStyle.Cmf(seed).hashCode(), PaletteStyle.Cmf(seed).hashCode())
        assertNotEquals(PaletteStyle.Cmf(), PaletteStyle.Cmf(seed))
    }

    @Test
    fun brokenCmfSeedsAreNull() {
        assertNull(PaletteStyle.parseOrNull("Cmf:FF7D52"))
        assertNull(PaletteStyle.parseOrNull("Cmf:"))
        assertNull(PaletteStyle.parseOrNull("Cmf:ZZZZZZZZ"))
        assertNull(PaletteStyle.parseOrNull("Cmf:+FFFFFFF"))
        assertNull(PaletteStyle.parseOrNull("Cmf:-FFFFFFF"))
        assertNull(PaletteStyle.parseOrNull("nope"))
    }

    @Test
    fun parseThrowsOnAnythingWeDidNotWrite() {
        assertFailsWith<IllegalArgumentException> {
            PaletteStyle.parse("nope")
        }
        assertFailsWith<IllegalArgumentException> {
            PaletteStyle.parse("Cmf:FF7D52")
        }
    }
}
