package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorTest {
    @Test
    fun toHex_writesUppercaseRgbWithAHashPrefix() {
        assertEquals("#4285F4", Color(0xFF4285F4).toHex())
        assertEquals("#000000", Color.Black.toHex())
        assertEquals("#FFFFFF", Color.White.toHex())
    }

    @Test
    fun toHex_addsAlphaOnlyWhenTheColorIsTranslucentOrAsked() {
        assertEquals("#804285F4", Color(0x804285F4).toHex())
        assertEquals("#00000000", Color.Transparent.toHex())
        assertEquals("#FF4285F4", Color(0xFF4285F4).toHex(alwaysIncludeAlpha = true))
    }

    @Test
    fun toHex_dropsThePrefixWhenAsked() {
        assertEquals("4285F4", Color(0xFF4285F4).toHex(includePrefix = false))
        assertEquals("0A0B0C", Color(0xFF0A0B0C).toHex(includePrefix = false))
    }

    @Test
    fun toHex_keepsACustomPrefixAsGiven() {
        assertEquals("0x4285F4", Color(0xFF4285F4).toHex(prefix = "0x"))
        assertEquals("0xFF4285F4", Color(0xFF4285F4).toHex(prefix = "0x", alwaysIncludeAlpha = true))
        assertEquals("hex:ABCDEF", Color(0xFFABCDEF).toHex(prefix = "hex:"))
    }

    @Test
    fun isLight_splitsAtHalfRelativeLuminance() {
        // Grey BB sits at 49.7 percent luminance and grey BC at 50.3 percent.
        assertFalse(Color(0xFFBBBBBB).isLight())
        assertTrue(Color(0xFFBCBCBC).isLight())

        assertTrue(Color.White.isLight())
        assertTrue(Color(0xFFFFC107).isLight())
        assertFalse(Color.Black.isLight())
        assertFalse(Color(0xFF4285F4).isLight())
    }

    @Test
    fun isLight_isFalseForTransparent() {
        assertFalse(Color.Transparent.isLight())
    }

    @Test
    fun isDisliked_flagsDarkYellowGreens() {
        for (argb in listOf(0xFF95884B, 0xFF716B40, 0xFFB08E00, 0xFF4C4308, 0xFF464521, 0xFF808000)) {
            assertTrue(Color(argb).isDisliked(), "expected ${argb.toString(16)} to be disliked")
        }

        assertFalse(Color(0xFF4285F4).isDisliked())
        assertFalse(Color(0xFFFFC107).isDisliked())
    }

    @Test
    fun fixIfDisliked_lightensDislikedColorsToTheUpstreamResult() {
        val fixes = mapOf(
            0xFF95884B to 0xFFBAAB6B,
            0xFF716B40 to 0xFFB4AC7C,
            0xFFB08E00 to 0xFFCCA726,
            0xFF4C4308 to 0xFFB9AC68,
            0xFF464521 to 0xFFB0AD80,
        )

        for ((disliked, fixed) in fixes) {
            val result = Color(disliked).fixIfDisliked()
            assertEquals(Color(fixed), result, "fixing ${disliked.toString(16)}")
            assertFalse(result.isDisliked())
        }
    }

    @Test
    fun fixIfDisliked_leavesLikedColorsAlone() {
        assertEquals(Color(0xFF4285F4), Color(0xFF4285F4).fixIfDisliked())
    }
}
