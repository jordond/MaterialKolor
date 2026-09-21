package com.materialkolor

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun everyKnownStyleRoundTripsThroughStorage() {
        for (style in PaletteStyle.KnownStyles) {
            assertEquals(style, PaletteStyle.fromStorageString(style.toStorageString()))
        }
    }

    @Test
    fun aSeededCmfCarriesItsSeedThroughStorage() {
        val style = PaletteStyle.Cmf(Color(0xFF7D5260))

        assertEquals("Cmf:FF7D5260", style.toStorageString())
        assertEquals(style, PaletteStyle.fromStorageString(style.toStorageString()))
        assertEquals(style, PaletteStyle.fromStorageString("Cmf:ff7d5260"))
    }

    @Test
    fun brokenCmfSeedsAreNull() {
        assertNull(PaletteStyle.fromStorageString("Cmf:FF7D52"))
        assertNull(PaletteStyle.fromStorageString("Cmf:"))
        assertNull(PaletteStyle.fromStorageString("Cmf:ZZZZZZZZ"))
        assertNull(PaletteStyle.fromStorageString("Cmf:+FFFFFFF"))
        assertNull(PaletteStyle.fromStorageString("Cmf:-FFFFFFF"))
    }
}
