package com.materialkolor.palette

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.hct.Hct
import com.materialkolor.score.Score
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun themeColor_withoutFilterKeepsTheDominantGrey() {
        val palette = Palette.from(greySwatches)
        val dominant = Hct.fromInt(greySwatches.first().rgb)
        val unfiltered = palette.themeColor(fallback, filter = false)

        assertEquals(Color(dominant.toInt()), unfiltered)
    }

    @Test
    fun seedColorOrNull_prefersTheVibrantSwatch() {
        val palette = Palette.from(colorfulSwatches)
        val vibrant = palette.vibrantSwatch

        assertTrue(vibrant != null, "the colorful fixture should have a vibrant swatch")
        assertEquals(Color(vibrant.rgb), palette.seedColorOrNull())
    }

    @Test
    fun seedColorOrNull_fallsBackToTheDominantSwatch() {
        val palette = Palette.from(greySwatches)

        assertNull(palette.vibrantSwatch, "greys should not fill the vibrant target")
        assertEquals(Color(greySwatches.first().rgb), palette.seedColorOrNull())
    }

    @Test
    fun seedColorOrNull_isNullForAnEmptyPalette() {
        assertNull(Palette.from(emptyList()).seedColorOrNull())
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
