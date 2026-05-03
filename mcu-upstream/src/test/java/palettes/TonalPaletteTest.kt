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

    // Regression: ports upstream fix 1a34bd2 from material-color-utilities.
    // Yellow hues at T99 must route getHct through Hct.fromInt(tone(99)) so the averaged
    // ARGB from tone(Int) drives the HCT result instead of a direct Hct.from(hue,chroma,99).
    @Test
    fun getHct_yellowTone99_matchesAveragedTone() {
        val yellowHues = listOf(105.0, 110.0, 115.0, 120.0, 124.0)
        for (hue in yellowHues) {
            val palette = TonalPalette.fromHueAndChroma(hue, 50.0)
            val expected = Hct.fromInt(palette.tone(99))
            val actual = palette.getHct(99.0)
            actual.hue shouldBe expected.hue
            actual.chroma shouldBe expected.chroma
            actual.tone shouldBe expected.tone
        }
    }

    @Test
    fun getHct_nonYellowTone99_unaffected() {
        val nonYellowHues = listOf(0.0, 60.0, 104.999, 125.0, 240.0, 300.0)
        for (hue in nonYellowHues) {
            val palette = TonalPalette.fromHueAndChroma(hue, 50.0)
            val direct = Hct.from(hue, palette.chroma, 99.0)
            val viaPalette = palette.getHct(99.0)
            viaPalette.hue shouldBe direct.hue
            viaPalette.chroma shouldBe direct.chroma
            viaPalette.tone shouldBe direct.tone
        }
    }

    private fun assertPalettesEqual(
        expected: palettes.TonalPalette,
        actual: TonalPalette,
    ) {
        expected.hue shouldBe actual.hue
        expected.chroma shouldBe actual.chroma
        assertHctEqual(expected.keyColor, actual.keyColor)
    }

    private fun assertHctEqual(
        expected: hct.Hct,
        actual: Hct,
    ) {
        expected.hue shouldBe actual.hue
        expected.chroma shouldBe actual.chroma
        expected.tone shouldBe actual.tone
    }
}
