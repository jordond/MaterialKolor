package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ContrastTest {
    private val tolerance = 1e-9

    @Test
    fun contrastRatio_matchesTheWcagRatioOfLuminances() {
        assertEquals(21.0, Color.White.contrastRatio(Color.Black), tolerance)
        assertEquals(4.542224959605252, Color.White.contrastRatio(Color(0xFF767676)), tolerance)
        assertEquals(3.563554571256263, Color(0xFF4285F4).contrastRatio(Color.White), tolerance)
        assertEquals(3.9515606815407303, Color(0xFF6750A4).contrastRatio(Color(0xFFFFC107)), tolerance)
    }

    @Test
    fun contrastRatio_isSymmetricAndOneForTheSameColor() {
        val blue = Color(0xFF4285F4)

        assertEquals(blue.contrastRatio(Color.White), Color.White.contrastRatio(blue), tolerance)
        assertEquals(1.0, blue.contrastRatio(blue), tolerance)
    }

    @Test
    fun tonalContrastRatio_matchesTheRatioOfHctTones() {
        assertEquals(21.0, Color.White.tonalContrastRatio(Color.Black), tolerance)
        assertEquals(4.542224959605251, Color.White.tonalContrastRatio(Color(0xFF767676)), tolerance)
        assertEquals(3.5635545712562635, Color(0xFF4285F4).tonalContrastRatio(Color.White), tolerance)
        assertEquals(3.9515606815407303, Color(0xFF6750A4).tonalContrastRatio(Color(0xFFFFC107)), tolerance)
    }
}
