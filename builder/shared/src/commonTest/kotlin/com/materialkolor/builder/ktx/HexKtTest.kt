package com.materialkolor.builder.ktx

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HexKtTest {

    @Test
    fun testValidHexColors() {
        assertEquals(Color(0xFFFF0000), "#FF0000".parseHexToColor())
        assertEquals(Color(0xFF00FF00), "#00FF00".parseHexToColor())
        assertEquals(Color(0xFF0000FF), "#0000FF".parseHexToColor())
        assertEquals(Color(0xFFFFFFFF), "#FFFFFF".parseHexToColor())
        assertEquals(Color(0xFF000000), "#000000".parseHexToColor())
    }

    @Test
    fun testValidHexColorsWithoutHash() {
        assertEquals(Color(0xFFFF0000), "FF0000".parseHexToColor())
        assertEquals(Color(0xFF00FF00), "00FF00".parseHexToColor())
        assertEquals(Color(0xFF0000FF), "0000FF".parseHexToColor())
    }

    @Test
    fun testValidHexColorsWithAlpha() {
        assertEquals(Color(0x80FF0000), "#80FF0000".parseHexToColor())
        assertEquals(Color(0x80FF0000), "80FF0000".parseHexToColor())
        assertEquals(Color(0x00FFFFFF), "#00FFFFFF".parseHexToColor())
        assertEquals(Color(0xFFFFFFFF), "#FFFFFFFF".parseHexToColor())
    }

    @Test
    fun testInvalidHexColors() {
        assertNull("".parseHexToColor())
        assertNull("#".parseHexToColor())
        assertNull("#12345".parseHexToColor())
        assertNull("#123456789".parseHexToColor())
        assertNull("#GHIJKL".parseHexToColor())
        assertNull("GHIJKL".parseHexToColor())
        assertNull("#FF00FF00FF".parseHexToColor())
    }

    @Test
    fun testCaseInsensitivity() {
        assertEquals(Color(0xFFABCDEF), "#abcdef".parseHexToColor())
        assertEquals(Color(0xFFABCDEF), "ABCDEF".parseHexToColor())
        assertEquals(Color(0x80ABCDEF), "#80abcdef".parseHexToColor())
        assertEquals(Color(0x80ABCDEF), "80ABCDEF".parseHexToColor())
    }
}