package com.materialkolor.builder.domain.color

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArgbTest {
    @Test
    fun argb_colorWithoutAlpha_getsAFullAlphaByte() {
        assertEquals(0xFFD9653B.toInt(), Argb(0x00D9653B).value)
        assertEquals(0xFF000000.toInt(), Argb(0x00000000).value)
    }

    @Test
    fun argb_colorWithPartialAlpha_getsAFullAlphaByte() {
        assertEquals(0xFFD9653B.toInt(), Argb(0x7FD9653B).value)
        assertEquals(Argb(0xFFD9653B.toInt()), Argb(0x7FD9653B))
    }

    @Test
    fun argb_channels_readTheirOwnByte() {
        val color = Argb(0x00D9653B)

        assertEquals(0xD9, color.red)
        assertEquals(0x65, color.green)
        assertEquals(0x3B, color.blue)
    }

    @Test
    fun argb_toHex_isUppercaseWithoutTheAlphaByte() {
        assertEquals("#D9653B", Argb(0xFFD9653B.toInt()).toHex())
        assertEquals("#000000", Argb(0xFF000000.toInt()).toHex())
        assertEquals("#0A0B0C", Argb(0x000A0B0C).toHex())
    }

    @Test
    fun argb_fromHex_readsBothLengthsWithAndWithoutTheHash() {
        val expected = Argb(0xFFD9653B.toInt())

        assertEquals(expected, Argb.fromHex("#D9653B"))
        assertEquals(expected, Argb.fromHex("D9653B"))
        assertEquals(expected, Argb.fromHex("#FFD9653B"))
        assertEquals(expected, Argb.fromHex("#00D9653B"))
    }

    @Test
    fun argb_fromHex_rejectsTextThatIsNotAColor() {
        assertFailsWith<IllegalArgumentException> { Argb.fromHex("#D965") }
        assertFailsWith<IllegalArgumentException> { Argb.fromHex("") }
    }

    @Test
    fun argb_everyChannelValue_survivesAJsonRoundTrip() {
        val json = Json

        (0..0xFF).forEach { channel ->
            val color = Argb((channel shl 16) or (channel shl 8) or channel)
            val text = json.encodeToString(ArgbSerializer, color)
            assertEquals(color, json.decodeFromString(ArgbSerializer, text), text)
        }
    }
}
