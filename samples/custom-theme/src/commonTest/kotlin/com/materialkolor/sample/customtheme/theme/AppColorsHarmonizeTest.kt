package com.materialkolor.sample.customtheme.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toHct
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppColorsHarmonizeTest {
    private val seeds = AppThemeSeeds.Default

    @Test
    fun drinkColors_followTheSeed() {
        val warmSeed = Color(0xFFB3261E)
        val coolSeed = Color(0xFF00629E)

        val warmTheme = appColors(seed = warmSeed, isDark = false)
        val coolTheme = appColors(seed = coolSeed, isDark = false)

        val drinks = listOf(
            "coffee" to (warmTheme.drinkCoffee to coolTheme.drinkCoffee),
            "matcha" to (warmTheme.drinkMatcha to coolTheme.drinkMatcha),
            "iced" to (warmTheme.drinkIced to coolTheme.drinkIced),
            "tea" to (warmTheme.drinkTea to coolTheme.drinkTea),
            "choc" to (warmTheme.drinkChoc to coolTheme.drinkChoc),
        )

        for ((name, pair) in drinks) {
            val (warm, cool) = pair
            assertNotEquals(warm.toHct().hue, cool.toHct().hue, "drink $name should follow the seed")
        }
    }

    @Test
    fun drinkColors_stayRecognizable() {
        val theme = appColors(seed = Color(0xFF00629E), isDark = false)

        // Harmonizing shifts hue towards the seed but is meant to leave the color recognizable,
        // so a coffee brown must not arrive at the same hue as the seed.
        val seedHue = Color(0xFF00629E).toHct().hue
        val coffeeHue = theme.drinkCoffee.toHct().hue
        assertTrue(
            actual = hueDistance(seedHue, coffeeHue) > MINIMUM_RECOGNIZABLE_HUE_SHIFT,
            message = "coffee collapsed onto the seed hue, seed=$seedHue coffee=$coffeeHue",
        )
    }

    @Test
    fun unharmonizedPalette_keepsTheSeedHue() {
        val plain = TonalPalette.from(seeds.coffee).toneColor(DECORATIVE_TONE_UNDER_TEST)

        // The theme harmonizes every drink seed. Skipping that step has to leave the hue alone,
        // which is what makes harmonizing a decision rather than something that always happens.
        assertTrue(
            actual = hueDistance(seeds.coffee.toHct().hue, plain.toHct().hue) < UNHARMONIZED_HUE_TOLERANCE,
            message = "an unharmonized palette moved the seed hue, " +
                "seed=${seeds.coffee.toHct().hue} palette=${plain.toHct().hue}",
        )

        val themed = appColors(seed = Color(0xFF00629E), isDark = false).drinkCoffee
        assertNotEquals(plain, themed, "the theme should harmonize, the bare palette should not")
    }

    private fun hueDistance(
        first: Double,
        second: Double,
    ): Double {
        val delta = abs(first - second) % 360.0
        return if (delta > 180.0) 360.0 - delta else delta
    }

    private companion object {
        const val MINIMUM_RECOGNIZABLE_HUE_SHIFT = 10.0
        const val DECORATIVE_TONE_UNDER_TEST = 50
        const val UNHARMONIZED_HUE_TOLERANCE = 2.0
    }
}
