package com.materialkolor.sample.customtheme.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.ktx.hasEnoughContrast
import kotlin.test.Test
import kotlin.test.assertTrue

class AppColorsContrastTest {
    @Test
    fun everyPublishedPair_reachesAaNormalText() {
        for (seed in TestSeeds) {
            for (isDark in listOf(false, true)) {
                val colors = appColors(seed = seed, isDark = isDark)
                for (pair in colors.contrastPairs) {
                    assertTrue(
                        actual = pair.foreground.hasEnoughContrast(
                            other = pair.background,
                            threshold = ContrastThreshold.WCAG_AA_NORMAL_TEXT,
                        ),
                        message = "seed=$seed isDark=$isDark pair=${pair.name} " +
                            "ratio=${pair.foreground.contrastRatio(pair.background)}",
                    )
                }
            }
        }
    }

    @Test
    fun everySlot_isEitherHalfOfAPairOrDeclaredDecorative() {
        val colors = appColors(seed = TestSeeds.first(), isDark = false)
        val paired = colors.contrastPairs
            .flatMap { pair -> listOf(pair.foreground, pair.background) }
            .toSet()

        val unaccounted = colors.slots
            .filterKeys { name -> name !in AppColors.DecorativeSlots }
            .filterValues { color -> color !in paired }
            .keys

        assertTrue(
            actual = unaccounted.isEmpty(),
            message = "slots with no contrast pair and no decorative declaration: $unaccounted",
        )
    }
}

internal val TestSeeds: List<Color> = listOf(
    Color(0xFF6750A4),
    Color(0xFFB3261E),
    Color(0xFF006D3B),
    Color(0xFFFFD600),
    Color(0xFF1B1B1F),
)
