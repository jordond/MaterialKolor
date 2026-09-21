package com.materialkolor.serialization

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaletteStyleSerializerTest {
    @Test
    fun objectsEncodeUnderTheOldEnumNames() {
        assertEquals("\"TonalSpot\"", Json.encodeToString(PaletteStyle.TonalSpot as PaletteStyle))
    }

    @Test
    fun aSeededCmfEncodesItsSeed() {
        val style: PaletteStyle = PaletteStyle.Cmf(Color(0xFF7D5260))

        assertEquals("\"Cmf:FF7D5260\"", Json.encodeToString(style))
    }

    @Test
    fun everyStyleRoundTrips() {
        val styles = PaletteStyle.KnownStyles + PaletteStyle.Cmf(Color(0xFF7D5260))
        for (style in styles) {
            assertEquals(style, Json.decodeFromString<PaletteStyle>(Json.encodeToString(style)))
        }
    }

    @Test
    fun anUnknownStyleFails() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<PaletteStyle>("\"nope\"")
        }
    }
}
