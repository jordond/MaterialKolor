package palettes

import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TonalPaletteTest {

    @Test
    fun fromInt() {
        val color = 0xFF0000 // Red
        val expectedPalette = palettes.TonalPalette.fromInt(color)
        val actualPalette = TonalPalette.fromInt(color)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    @Test
    fun fromHct() {
        val expectedHct = hct.Hct.from(0.0, 100.0, 50.0)
        val expectedPalette = palettes.TonalPalette.fromHct(expectedHct)

        val actualHct = Hct.from(0.0, 100.0, 50.0)
        val actualPalette = TonalPalette.fromHct(actualHct)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    @Test
    fun fromHueAndChromaBroken() {
        val hue = 180.0 // Cyan
        val chroma = 50.0
        val expectedPalette = palettes.TonalPalette.fromHueAndChroma(hue, chroma)
        val actualPalette = TonalPalette.fromHueAndChroma(hue, chroma)

        assertPalettesEqual(expectedPalette, actualPalette)
    }

    @Test
    fun tone() {
        val palette = TonalPalette.fromHueAndChroma(240.0, 50.0) // Blue
        val tones = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

        for (tone in tones) {
            val expectedColor = palettes.TonalPalette.fromHueAndChroma(240.0, 50.0).tone(tone)
            val actualColor = palette.tone(tone)
            actualColor shouldBe expectedColor
        }
    }

    @Test
    fun getHct() {
        val palette = TonalPalette.fromHueAndChroma(300.0, 60.0) // Purple
        val tones = listOf(0.0, 25.0, 50.0, 75.0, 100.0)

        for (tone in tones) {
            val expectedHct = palettes.TonalPalette.fromHueAndChroma(300.0, 60.0).getHct(tone)
            val actualHct = palette.getHct(tone)
            assertHctEqual(expectedHct, actualHct)
        }
    }

    private fun assertPalettesEqual(expected: palettes.TonalPalette, actual: TonalPalette) {
        expected.hue shouldBe actual.hue
        expected.chroma shouldBe actual.chroma
        assertHctEqual(expected.keyColor, actual.keyColor)
    }

    private fun assertHctEqual(expected: hct.Hct, actual: Hct) {
        expected.hue shouldBe actual.hue
        expected.chroma shouldBe actual.chroma
        expected.tone shouldBe actual.tone
    }
}