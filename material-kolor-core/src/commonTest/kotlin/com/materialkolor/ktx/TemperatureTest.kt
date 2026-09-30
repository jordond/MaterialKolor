package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.temperature.TemperatureCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemperatureTest {
    @Test
    fun temperature_matchesTheUpstreamRawTemperature() {
        assertEquals(2.35135384724946, TemperatureCache.temperature(Color(0xFFFF0000)), 1e-9)
        assertEquals(-1.3934773537528051, TemperatureCache.temperature(Color(0xFF0000FF)), 1e-9)
        assertEquals(1.391163279941776, TemperatureCache.temperature(Color(0xFFFFC107)), 1e-9)
    }

    @Test
    fun isWarm_holdsForRedsAndYellows() {
        assertTrue(Color(0xFFFF0000).isWarm())
        assertTrue(Color(0xFFFFC107).isWarm())

        assertFalse(Color(0xFF0000FF).isWarm())
        assertFalse(Color(0xFF808080).isWarm())
    }

    @Test
    fun isCool_holdsForBluesGreensAndGrey() {
        assertTrue(Color(0xFF0000FF).isCool())
        assertTrue(Color(0xFF4285F4).isCool())
        assertTrue(Color(0xFF00FF00).isCool())
        assertTrue(Color(0xFF00FFFF).isCool())
        assertTrue(Color(0xFF808080).isCool())

        assertFalse(Color(0xFFFF0000).isCool())
    }
}
