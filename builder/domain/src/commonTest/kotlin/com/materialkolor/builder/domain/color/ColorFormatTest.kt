package com.materialkolor.builder.domain.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorFormatTest {
    @Test
    fun textOf_everyFormat_roundTripsThroughColorInput() {
        for (format in ColorFormat.entries) {
            for (color in formatSampledColors()) {
                val text = format.textOf(color)

                assertEquals(ParseResult.Ok(color), ColorInput.parse(text), "$format $text")
            }
        }
    }

    @Test
    fun textOf_red_readsTheWayEachFormatWritesIt() {
        val red = Argb(0xFF0000)

        assertEquals("#FF0000", ColorFormat.Hex.textOf(red))
        assertEquals("rgb(255 0 0)", ColorFormat.Rgb.textOf(red))
        assertEquals("hsl(0 100% 50%)", ColorFormat.Hsl.textOf(red))
        assertTrue(ColorFormat.Oklch.textOf(red).matches(Regex("""oklch\(0\.6279\d 0\.2576\d 29\.23\d\)""")))
    }

    @Test
    fun textOf_grey_hasNoHueOrChroma() {
        val grey = Argb(0x808080)

        assertEquals("hsl(0 0% 50.2%)", ColorFormat.Hsl.textOf(grey))
        assertEquals(0.0, ColorFormat.Oklch.channelsOf(grey)[1])
        assertEquals(0.0, ColorFormat.Oklch.channelsOf(grey)[2])
    }

    @Test
    fun channelsOf_seed_matchesTheCssValues() {
        val seed = Argb(0x6750A4)

        assertEquals(listOf(103.0, 80.0, 164.0), ColorFormat.Rgb.channelsOf(seed))
        assertEquals(ColorFormat.Rgb.channelsOf(seed), ColorFormat.Hex.channelsOf(seed))
        val (hue, saturation, lightness) = ColorFormat.Hsl.channelsOf(seed)
        assertEquals(256.43, hue, absoluteTolerance = 0.01)
        assertEquals(34.43, saturation, absoluteTolerance = 0.01)
        assertEquals(47.84, lightness, absoluteTolerance = 0.01)
    }

    @Test
    fun srgbToHsl_primaries_undoHslToSrgb() {
        for (hue in listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)) {
            val hsl = srgbToHsl(ColorSpaces.hslToSrgb(hue, 1.0, 0.5))

            assertEquals(hue, hsl.hue, absoluteTolerance = 1e-9)
            assertEquals(1.0, hsl.saturation, absoluteTolerance = 1e-9)
            assertEquals(0.5, hsl.lightness, absoluteTolerance = 1e-9)
        }
    }

    private fun formatSampledColors(): List<Argb> {
        val levels = 0..0xFF step 15
        return levels.flatMap { red ->
            levels.flatMap { green -> levels.map { blue -> Argb((red shl 16) or (green shl 8) or blue) } }
        } + listOf(Argb(0x6750A4), Argb(0x010203), Argb(0xFEFDFC), Argb(0x7F807F))
    }
}
