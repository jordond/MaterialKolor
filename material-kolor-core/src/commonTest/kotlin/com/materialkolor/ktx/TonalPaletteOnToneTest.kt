package com.materialkolor.ktx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TonalPaletteOnToneTest {
    private val seeds = listOf(
        Color(0xFF6750A4),
        Color(0xFFB3261E),
        Color(0xFF006D3B),
        Color(0xFFFFD600),
        Color(0xFF000000),
    )

    private val tones = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    @Test
    fun onTone_reachesAaNormalText_forEveryToneOfEverySeed() {
        for (seed in seeds) {
            val palette = TonalPalette.from(seed)
            for (tone in tones) {
                val background = palette.toneColor(tone)
                val content = palette.onTone(tone)
                assertTrue(
                    actual = content.hasEnoughContrast(background, ContrastThreshold.WCAG_AA_NORMAL_TEXT),
                    message = "seed=$seed tone=$tone ratio=${content.contrastRatio(background)}",
                )
            }
        }
    }

    @Test
    fun onTone_reachesAaaNormalText_whenTheRampAllowsIt() {
        val palette = TonalPalette.from(Color(0xFF6750A4))

        // Tones near the middle of the ramp are the ones with room on both sides for 7:1.
        for (tone in listOf(0, 10, 90, 100)) {
            val background = palette.toneColor(tone)
            val content = palette.onTone(tone, ContrastThreshold.WCAG_AAA_NORMAL_TEXT)
            assertTrue(
                actual = content.hasEnoughContrast(background, ContrastThreshold.WCAG_AAA_NORMAL_TEXT),
                message = "tone=$tone ratio=${content.contrastRatio(background)}",
            )
        }
    }

    @Test
    fun onTone_prefersDarkContent_onLightBackgrounds() {
        val palette = TonalPalette.from(Color(0xFF6750A4))

        val content = palette.onTone(90)
        assertTrue(
            actual = content.luminance() < palette.toneColor(90).luminance(),
            message = "expected dark content on a tone 90 background",
        )
    }

    @Test
    fun onTone_prefersLightContent_onDarkBackgrounds() {
        val palette = TonalPalette.from(Color(0xFF6750A4))

        val content = palette.onTone(10)
        assertTrue(
            actual = content.luminance() > palette.toneColor(10).luminance(),
            message = "expected light content on a tone 10 background",
        )
    }

    @Test
    fun onTone_fallsBackToTheEndOfTheRamp_whenTheRatioIsUnreachable() {
        val palette = TonalPalette.from(Color(0xFF6750A4))

        // No tone in a 0 to 100 ramp reaches 7:1 against a mid tone in either direction, so the
        // nearer end of the ramp wins.
        assertEquals(palette.toneColor(0), palette.onTone(50, ContrastThreshold.WCAG_AAA_NORMAL_TEXT))
        assertEquals(palette.toneColor(100), palette.onTone(49, ContrastThreshold.WCAG_AAA_NORMAL_TEXT))
    }
}
