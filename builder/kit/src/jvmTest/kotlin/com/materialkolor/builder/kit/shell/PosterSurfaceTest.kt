package com.materialkolor.builder.kit.shell

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val ShellTextRatio = 4.5

/**
 * Ten hues at five tones, mid tones included, where ink sits closest to the floor.
 */
private val ShellSeeds: List<Argb> = buildList {
    for (hue in 0 until 360 step 36) {
        for (tone in listOf(20.0, 35.0, 50.0, 62.0, 80.0)) {
            add(Argb(Hct.from(hue.toDouble(), 48.0, tone).toInt()))
        }
    }
}

/**
 * One ink a skin draws on the poster and the ground it stands on.
 */
private class ShellInk(
    val name: String,
    val ink: Color,
    val ground: Color,
)

@OptIn(ExperimentalTestApi::class)
class PosterSurfaceTest {
    @Test
    fun posterSurface_fiftySeeds_inkHoldsOnTheSeedInEverySkin() {
        for ((name, expressive) in ShellFlavours) {
            withClue(name) {
                runComposeUiTest {
                    var seed by mutableStateOf(ShellSeeds.first())
                    var inks: List<ShellInk> = emptyList()
                    setContent {
                        ShellHarness(expressive) {
                            val poster = remember(seed) { PosterColors.of(seed) }
                            PosterSurface(poster) { inks = shellInks(poster) }
                        }
                    }

                    val misses = mutableListOf<String>()
                    for (next in ShellSeeds) {
                        seed = next
                        waitForIdle()
                        withClue(next.toHex()) {
                            inks.shouldNotBeEmpty()
                            inks.map { pair -> pair.ground }.distinct() shouldBe listOf(next.toColor())
                        }
                        misses += inks.mapNotNull { pair ->
                            val ratio = pair.ink.contrastRatio(pair.ground)
                            if (ratio < ShellTextRatio) "${next.toHex()} ${pair.name} ${"%.2f".format(ratio)}" else null
                        }
                    }
                    misses.shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun posterSurface_everySkin_standsOnTheExactSeed() {
        for ((name, expressive) in ShellFlavours) {
            withClue(name) {
                runComposeUiTest {
                    var grounds: List<Color> = emptyList()
                    setContent {
                        ShellHarness(expressive) {
                            PosterSurface(ShellPosterColors) {
                                grounds =
                                    shellInks(ShellPosterColors).map { pair -> pair.ground }
                            }
                        }
                    }
                    waitForIdle()

                    grounds.distinct() shouldBe listOf(ShellPosterColors.seed.toColor())
                }
            }
        }
    }

    @Test
    fun posterSurface_newPosterOnTheSameSeed_recomposesNothingUnderIt() {
        for ((name, expressive) in ShellFlavours) {
            withClue(name) {
                runComposeUiTest {
                    var poster by mutableStateOf(ShellPosterColors)
                    val count = CompositionCount()
                    setContent {
                        ShellHarness(expressive) { PosterSurface(poster) { Counted(count) } }
                    }
                    waitForIdle()
                    val before = count.value

                    // Each theme result works its poster out again, so a drag hands over a new one every frame.
                    poster = PosterColors.of(ShellPosterColors.seed)
                    waitForIdle()

                    count.value shouldBe before
                }
            }
        }
    }
}

/**
 * How many times [Counted] ran.
 */
private class CompositionCount {
    var value: Int = 0
}

/**
 * Counts its own runs. Its one argument never changes, so it only runs again when a theme above it forces it.
 */
@Composable
private fun Counted(count: CompositionCount) {
    count.value++
}

/**
 * Every ink on ground pair the shell's skin draws on the poster, from its own theme as well as the
 * builder tokens.
 */
@Composable
private fun shellInks(poster: PosterColors): List<ShellInk> {
    val seed = poster.seed.toColor()
    val tokens = LocalBuilderTokens.current
    val scheme = MaterialTheme.colorScheme
    return listOf(
        ShellInk("tokens text on seed", tokens.textStrong, seed),
        ShellInk("tokens text on panel", tokens.textStrong, tokens.panel),
        ShellInk("tokens text on canvas", tokens.textStrong, tokens.canvas),
        ShellInk("tokens muted text on panel", tokens.textMuted, tokens.panel),
        ShellInk("onSurface on surface", scheme.onSurface, scheme.surface),
        ShellInk("primary on surface", scheme.primary, scheme.surface),
        ShellInk("content colour on surface", LocalContentColor.current, scheme.surface),
        ShellInk("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
    )
}
