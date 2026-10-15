package com.materialkolor.builder.engine.color

import com.materialkolor.builder.domain.color.Argb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HctReadoutTest {
    @Test
    fun of_pureRed_readsCoreHueChromaAndTone() {
        val readout = HctReadout.of(Argb(0xFF0000))

        assertEquals(27.4, readout.hue, absoluteTolerance = 0.5)
        assertEquals(113.4, readout.chroma, absoluteTolerance = 0.5)
        assertEquals(53.2, readout.tone, absoluteTolerance = 0.5)
    }

    @Test
    fun of_whiteAndBlack_readTheEndsOfTone() {
        val white = HctReadout.of(Argb(0xFFFFFF))
        val black = HctReadout.of(Argb(0x000000))

        assertEquals(100.0, white.tone, absoluteTolerance = 0.01)
        assertEquals(0.0, black.tone, absoluteTolerance = 0.01)
        assertTrue(white.chroma < 5.0, "white reads next to no chroma, got ${white.chroma}")
    }
}
