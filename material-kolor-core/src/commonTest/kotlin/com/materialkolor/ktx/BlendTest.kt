package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import com.materialkolor.blend.Blend
import kotlin.test.Test
import kotlin.test.assertEquals

class BlendTest {
    private val blue = Color(0xFF0000FF)
    private val red = Color(0xFFFF0000)
    private val googleBlue = Color(0xFF4285F4)
    private val amber = Color(0xFFFFC107)

    @Test
    fun blend_mixesInCam16UcsAndDefaultsToHalfway() {
        assertEquals(Color(0xFF9A4A86), blue.blend(red))
        assertEquals(Color(0xFF9A4A86), Blend.blend(from = blue, to = red, amount = 0.5f))
        assertEquals(Color(0xFF8898BC), googleBlue.blend(amber, amount = 0.25f))
    }

    @Test
    fun blend_returnsAnEndpointAtZeroAndOne() {
        assertEquals(googleBlue, googleBlue.blend(amber, amount = 0f))
        assertEquals(amber, googleBlue.blend(amber, amount = 1f))
    }

    @Test
    fun blendHue_movesOnlyTheHueAndDefaultsToHalfway() {
        assertEquals(Color(0xFF8E007B), blue.blendHue(red))
        assertEquals(Color(0xFF8E007B), Blend.blendHue(from = blue, to = red, amount = 0.5f))
        assertEquals(Color(0xFF4085F4), googleBlue.blendHue(amber, amount = 0.25f))
    }
}
