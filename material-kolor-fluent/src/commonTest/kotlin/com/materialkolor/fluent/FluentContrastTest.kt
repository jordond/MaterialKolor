package com.materialkolor.fluent

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.from
import com.materialkolor.palettes.TonalPalette
import io.github.composefluent.Colors
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Fluent pairs its shades with fixed constants rather than with other generated colors, and HCT
 * tone is L*, so every one of these ratios is decided by the tone the adapter picked and not by the
 * seed. These tests are what keeps that true if the tones are ever edited.
 */
class FluentContrastTest {
    private val aa = ContrastThreshold.WCAG_AA_NORMAL_TEXT.threshold

    private val seeds = listOf(
        Color(0xFF0078D4),
        Color(0xFF6750A4),
        Color(0xFFB3261E),
        Color(0xFFFFEB3B),
        Color(0xFF2E7D32),
        Color(0xFF00BCD4),
        Color(0xFFE91E63),
        Color(0xFF7A7A7E),
        Color(0xFF808080),
    )

    @Test
    fun accentPairings_clearAaForEverySeed() {
        for (seed in seeds) {
            for (isDark in listOf(true, false)) {
                val colors = colors(seed, isDark)

                for ((name, pair) in colors.opaqueAccentPairings()) {
                    val ratio = pair.first.contrastRatio(pair.second)
                    assertTrue(
                        actual = ratio >= aa,
                        message = "seed=$seed isDark=$isDark $name was $ratio",
                    )
                }
            }
        }
    }

    @Test
    fun selectedTextBackground_isTheOneBelowAa_andIsMicrosoftsOwnValue() {
        // fillAccent.selectedTextBackground is shades.base, the tone 50 the seed itself sits on.
        // Tone 50 under white is 4.48:1 whatever the hue, which is the value Microsoft ships. It is
        // left alone deliberately, so this test pins the exception rather than the repair.
        for (seed in seeds) {
            for (isDark in listOf(true, false)) {
                val colors = colors(seed, isDark)
                val ratio = colors.fillAccent.selectedTextBackground
                    .contrastRatio(colors.text.onAccent.selectedText)

                assertTrue(
                    actual = ratio < aa,
                    message = "seed=$seed isDark=$isDark cleared AA at $ratio, so the exception is stale",
                )
                assertTrue(
                    actual = ratio > 4.4,
                    message = "seed=$seed isDark=$isDark dropped to $ratio, further below AA than tone 50 should go",
                )
            }
        }
    }

    @Test
    fun accentContrast_doesNotDependOnTheSeed() {
        val ratios = seeds.map { seed ->
            val colors = colors(seed, isDark = true)
            colors.fillAccent.default.contrastRatio(colors.text.onAccent.primary)
        }

        // Any spread here is 8-bit rounding, not hue. This is the property that makes a
        // contrast-repair helper unnecessary for this consumer.
        val spread = ratios.max() - ratios.min()
        assertTrue(actual = spread < 0.2, message = "ratios varied by $spread across seeds: $ratios")
    }

    private fun colors(
        seed: Color,
        isDark: Boolean,
    ): Colors = Colors(TonalPalette.from(seed).toFluentShades(), isDark)

    /**
     * The pairings Fluent actually renders where both sides are opaque. Translucent roles are left
     * out because their ratio depends on whatever is behind them.
     */
    private fun Colors.opaqueAccentPairings(): Map<String, Pair<Color, Color>> =
        mapOf(
            "fillAccent.default on text.onAccent.primary" to (fillAccent.default to text.onAccent.primary),
            "text.accent.primary on background.solid.base" to (text.accent.primary to background.solid.base),
            "text.accent.secondary on background.solid.base" to (text.accent.secondary to background.solid.base),
            "text.accent.tertiary on background.solid.base" to (text.accent.tertiary to background.solid.base),
            // An accent surface with body text on it. text.text.primary carries alpha, so composite
            // it down first, which is what actually reaches the screen.
            "text.text.primary on background.accentAcrylic.base" to
                (
                    text.text.primary.compositeOver(background.accentAcrylic.base) to
                        background.accentAcrylic.base
                ),
        )
}
