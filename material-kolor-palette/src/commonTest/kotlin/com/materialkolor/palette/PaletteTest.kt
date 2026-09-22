package com.materialkolor.palette

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.palette.graphics.Palette
import com.kmpalette.palette.graphics.Target
import com.materialkolor.hct.Hct
import com.materialkolor.score.Score
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaletteTest {
    private val fallback = Color.Blue

    private val colorfulSwatches =
        listOf(
            Palette.Swatch(0xFFE53935.toInt(), 800),
            Palette.Swatch(0xFF1E88E5.toInt(), 400),
            Palette.Swatch(0xFF43A047.toInt(), 200),
            Palette.Swatch(0xFFFDD835.toInt(), 100),
        )

    // Every grey sits below the chroma cutoff, so the filtered scoring keeps none of them.
    private val greySwatches =
        listOf(
            Palette.Swatch(0xFF808080.toInt(), 900),
            Palette.Swatch(0xFF404040.toInt(), 300),
            Palette.Swatch(0xFFC0C0C0.toInt(), 100),
        )

    @Test
    fun themeColors_matchesScoreOverTheSameSwatches() {
        val palette = Palette.from(colorfulSwatches)
        val expected =
            Score
                .score(
                    colorsToPopulation = colorfulSwatches.associate { swatch -> swatch.rgb to swatch.population },
                    desired = 4,
                    fallbackColorArgb = fallback.toArgb(),
                    filter = true,
                ).map { argb -> Color(argb) }

        assertEquals(expected, palette.themeColors(fallback))
    }

    @Test
    fun themeColors_fallsBackWhenNoSwatchSuitsATheme() {
        val palette = Palette.from(greySwatches)

        assertEquals(listOf(fallback), palette.themeColors(fallback))
        assertEquals(fallback, palette.themeColor(fallback))
    }

    @Test
    fun themeColorOrNull_isNullWhenNoSwatchSuitsATheme() {
        val palette = Palette.from(greySwatches)

        assertNull(palette.themeColorOrNull())
    }

    @Test
    fun themeColor_withoutFilterKeepsAGrey() {
        val palette = Palette.from(greySwatches)
        val unfiltered = palette.themeColor(fallback, filter = false)

        // Scoring weights chroma and hue spread rather than population, so which grey wins is the
        // engine's business. What matters is that turning the filter off stops the fallback.
        assertTrue(
            unfiltered.red == unfiltered.green && unfiltered.green == unfiltered.blue,
            "expected a grey, was $unfiltered",
        )
    }

    @Test
    fun seedColorOrNull_prefersTheVibrantSwatch() {
        // Palettes built from swatches carry no targets, so the vibrant one has to be asked for.
        val palette = Palette.Builder(colorfulSwatches).addTarget(Target.VIBRANT).generate()
        val vibrant = assertNotNull(palette.vibrantSwatch, "the colorful fixture should have a vibrant swatch")

        assertEquals(Color(vibrant.rgb), palette.seedColorOrNull())
    }

    @Test
    fun seedColorOrNull_fallsBackToTheDominantSwatch() {
        val palette = Palette.Builder(greySwatches).addTarget(Target.VIBRANT).generate()

        assertNull(palette.vibrantSwatch, "greys should not fill the vibrant target")
        assertEquals(Color(greySwatches.first().rgb), palette.seedColorOrNull())
    }

    @Test
    fun seedColorOrNull_isNullWhenThePaletteHasNoSwatches() {
        // Every pixel is near black, which the palette's own filter drops before scoring starts.
        val black = IntArray(16) { 0xFF000000.toInt() }
        val palette = Palette.from(pixels = black, width = 4, height = 4).generate()

        assertTrue(palette.swatches.isEmpty(), "expected the filter to drop every swatch")
        assertNull(palette.seedColorOrNull())
    }

    @Test
    fun toHct_matchesTheSwatchColor() {
        val swatch = Palette.Swatch(0xFF00BCD4.toInt(), 1)
        val expected = Hct.fromInt(swatch.rgb)
        val actual = swatch.toHct()

        assertEquals(expected.hue, actual.hue)
        assertEquals(expected.chroma, actual.chroma)
        assertEquals(expected.tone, actual.tone)
    }
}
