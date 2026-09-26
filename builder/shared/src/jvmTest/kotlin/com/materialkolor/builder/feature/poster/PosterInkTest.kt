package com.materialkolor.builder.feature.poster

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

/**
 * What poster text has to reach on the seed, WCAG AA for body text.
 */
private const val TEXT_RATIO = 4.5

@OptIn(ExperimentalTestApi::class)
class PosterInkTest {
    /**
     * Poster text is set in the strong and the muted text roles, and the surface inks both from
     * [PosterColors]. Muted text takes the ink too, so both have to read on every seed.
     */
    @Test
    fun posterText_hundredSeedsInEverySkin_holdsBodyTextContrastOnTheSeed() =
        runComposeUiTest {
            val seeds = hundredSeeds()
            val result = ThemeResolver().resolve(ThemeDocument.Default)
            val inks = mutableListOf<Triple<Argb, Library, List<Pair<String, Color>>>>()
            setContent {
                Library.entries.forEach { library ->
                    BuilderTheme(
                        skin = Skin(library = library, expressive = false),
                        result = result,
                        isDark = false,
                        reducedMotion = true,
                    ) {
                        seeds.forEach { seed ->
                            PosterSurface(PosterColors.of(seed)) {
                                val tokens = LocalBuilderTokens.current
                                inks += Triple(
                                    seed,
                                    library,
                                    listOf("text" to tokens.textStrong, "muted" to tokens.textMuted),
                                )
                            }
                        }
                    }
                }
            }
            waitForIdle()

            inks.map { (seed, library) -> seed to library }.toSet().size shouldBe seeds.size * Library.entries.size
            inks.forEach { (seed, library, roles) ->
                roles.forEach { (role, ink) ->
                    withClue("$role ink $ink on seed $seed in $library") {
                        ink.contrastRatio(seed.toColor()) shouldBeGreaterThanOrEqual TEXT_RATIO
                    }
                }
            }
        }

    /**
     * A hundred seeds, the extremes, a mid tone on every 20 degrees of hue where ink sits closest to
     * the line, and the rest drawn at random.
     */
    private fun hundredSeeds(): List<Argb> {
        val extremes = listOf(0x000000, 0xFFFFFF, 0x777777, 0x808080, 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00)
            .map { rgb -> Argb(rgb) }
        val midTones = (0 until 360 step 20).map { hue -> Argb(Hct.from(hue.toDouble(), 48.0, 52.0).toInt()) }
        val random = Random(303)
        val drawn = List(100 - extremes.size - midTones.size) { Argb(random.nextInt(0x1000000)) }
        return (extremes + midTones + drawn).also { seeds -> seeds.toSet().size shouldBe 100 }
    }
}
