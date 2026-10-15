package com.materialkolor.builder.domain.color

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorSpacesTest {
    @Test
    fun srgbToLinear_everyByte_roundTrips() {
        (0..0xFF).forEach { level ->
            val channel = level / 255.0
            val back = ColorSpaces.linearToSrgb(ColorSpaces.srgbToLinear(channel))
            assertEquals(channel, back, absoluteTolerance = 1e-9, message = "level $level")
        }
    }

    @Test
    fun linearToSrgb_negativeChannel_staysNegative() {
        assertTrue(ColorSpaces.linearToSrgb(-0.2) < 0)
    }

    @Test
    fun toOklab_blackAndWhite_sitAtTheEndsOfLightness() {
        val white = Argb(0xFFFFFF).toOklab()
        val black = Argb(0x000000).toOklab()

        assertEquals(1.0, white.l, absoluteTolerance = 1e-4)
        assertEquals(0.0, white.toOklch().c, absoluteTolerance = 1e-4)
        assertEquals(0.0, black.l, absoluteTolerance = 1e-9)
    }

    @Test
    fun toOklab_red_matchesTheReferenceOklch() {
        val red = Argb(0xFF0000).toOklab().toOklch()

        assertEquals(0.62796, red.l, absoluteTolerance = 1e-4)
        assertEquals(0.25768, red.c, absoluteTolerance = 1e-4)
        assertEquals(29.2339, red.h, absoluteTolerance = 1e-2)
    }

    @Test
    fun oklch_sampledColors_roundTripToTheSameArgbWithoutClamping() {
        sampledColors().forEach { color ->
            val mapped = ColorSpaces.oklchToArgb(color.toOklab().toOklch())

            assertEquals(color, mapped.argb)
            assertFalse(mapped.clamped, color.toHex())
        }
    }

    @Test
    fun oklchToArgb_greyAtAnyLightness_fitsWithoutClamping() {
        (0..100).forEach { step ->
            val mapped = ColorSpaces.oklchToArgb(Oklch(l = step / 100.0, c = 0.0, h = 0.0))

            assertFalse(mapped.clamped, "lightness $step")
            assertEquals(mapped.argb.red, mapped.argb.green)
            assertEquals(mapped.argb.green, mapped.argb.blue)
        }
    }

    @Test
    fun oklchToArgb_colorOutsideSrgb_givesUpChromaButKeepsHue() {
        listOf(0.0, 60.0, 150.0, 200.0, 264.0, 330.0).forEach { hue ->
            val mapped = ColorSpaces.oklchToArgb(Oklch(l = 0.6, c = 0.45, h = hue))
            val landed = mapped.argb.toOklab().toOklch()

            assertTrue(mapped.clamped, "hue $hue")
            assertEquals(0.6, landed.l, absoluteTolerance = 0.01, message = "hue $hue")
            assertEquals(0.0, hueDistance(hue, landed.h), absoluteTolerance = 3.0, message = "hue $hue")
        }
    }

    @Test
    fun oklchToArgb_hugeChroma_stillLandsOnTheGamutEdge() {
        val mapped = ColorSpaces.oklchToArgb(Oklch(l = 0.6, c = 1e300, h = 30.0))
        val landed = mapped.argb.toOklab().toOklch()

        assertTrue(mapped.clamped)
        assertTrue(landed.c > 0.15, "chroma ${landed.c}")
    }

    @Test
    fun hslToSrgb_primariesAndGrey_matchCss() {
        assertEquals(Argb(0xFF0000), ColorSpaces.srgbToArgb(ColorSpaces.hslToSrgb(0.0, 1.0, 0.5)))
        assertEquals(Argb(0x00FF00), ColorSpaces.srgbToArgb(ColorSpaces.hslToSrgb(120.0, 1.0, 0.5)))
        assertEquals(Argb(0x0000FF), ColorSpaces.srgbToArgb(ColorSpaces.hslToSrgb(240.0, 1.0, 0.5)))
        assertEquals(Argb(0x808080), ColorSpaces.srgbToArgb(ColorSpaces.hslToSrgb(77.0, 0.0, 0.5)))
    }

    private fun sampledColors(): List<Argb> {
        val levels = 0..0xFF step 17
        return levels.flatMap { red ->
            levels.flatMap { green -> levels.map { blue -> Argb((red shl 16) or (green shl 8) or blue) } }
        }
    }

    private fun hueDistance(
        first: Double,
        second: Double,
    ): Double {
        val gap = abs(first - second) % 360
        return if (gap > 180) 360 - gap else gap
    }
}
