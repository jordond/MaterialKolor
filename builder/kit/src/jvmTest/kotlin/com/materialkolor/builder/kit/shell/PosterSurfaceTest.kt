package com.materialkolor.builder.kit.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.custom.LocalBuilderIdentity
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.unstyled.MaterialKolorTokens
import io.github.composefluent.Colors
import io.github.composefluent.FluentTheme
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import io.github.composefluent.LocalContentColor as FluentContentColor

private const val ShellTextRatio = 4.5
private const val ShellClickTag = "shell-click"

/** Ten hues at five tones, mid tones included, where ink sits closest to the floor. */
private val ShellSeeds: List<Argb> = buildList {
    for (hue in 0 until 360 step 36) {
        for (tone in listOf(20.0, 35.0, 50.0, 62.0, 80.0)) {
            add(Argb(Hct.from(hue.toDouble(), 48.0, tone).toInt()))
        }
    }
}

/** One ink a skin draws on the poster and the ground it stands on. */
private class ShellInk(
    val name: String,
    val ink: Color,
    val ground: Color,
)

@OptIn(ExperimentalTestApi::class)
class PosterSurfaceTest {
    @Test
    fun seeds_areFiftyDistinctColours() {
        ShellSeeds.toSet().size shouldBe 50
    }

    @Test
    fun posterSurface_fiftySeeds_inkHoldsOnTheSeedInEverySkin() {
        for ((name, skin) in ShellSkins) {
            withClue(name) {
                runComposeUiTest {
                    var seed by mutableStateOf(ShellSeeds.first())
                    var inks: List<ShellInk> = emptyList()
                    setContent {
                        ShellHarness(skin) {
                            val poster = remember(seed) { PosterColors.of(seed) }
                            PosterSurface(poster) { inks = shellInks(skin, poster) }
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
        for ((name, skin) in ShellSkins) {
            withClue(name) {
                runComposeUiTest {
                    var grounds: List<Color> = emptyList()
                    setContent {
                        ShellHarness(skin) {
                            PosterSurface(ShellPosterColors) {
                                grounds =
                                    shellInks(skin, ShellPosterColors).map { pair -> pair.ground }
                            }
                        }
                    }
                    waitForIdle()

                    grounds.distinct() shouldBe listOf(ShellPosterColors.seed.toColor())
                }
            }
        }
    }

    /**
     * Fluent on the poster takes its shades from the seed's own ramp, and its dark flag from the
     * ink, so its fixed black or white text lands on the side that reads on the seed.
     */
    @Test
    fun posterSurface_fluent_takesItsShadesFromTheRampAndItsModeFromTheInk() =
        runComposeUiTest {
            var seed by mutableStateOf(ShellSeeds.first())
            var colors: Colors? = null
            setContent {
                ShellHarness(Skin(Library.Fluent, expressive = false)) {
                    val poster = remember(seed) { PosterColors.of(seed) }
                    PosterSurface(poster) { colors = FluentTheme.colors }
                }
            }

            for (next in ShellSeeds) {
                seed = next
                waitForIdle()
                val poster = PosterColors.of(next)
                withClue(next.toHex()) {
                    val fluent = checkNotNull(colors)
                    fluent.darkMode shouldBe !poster.isLight
                    fluent.shades.base shouldBe poster.ramp.toFluentShades().base
                }
            }
        }

    @Test
    fun posterSurface_unstyled_givesAPlainClickableAnIndication() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                ShellHarness(Skin(Library.Unstyled, expressive = false)) {
                    PosterSurface(ShellPosterColors) {
                        Box(Modifier.size(48.dp).testTag(ShellClickTag).clickable { clicks++ })
                    }
                }
            }
            onNodeWithTag(ShellClickTag).performClick()
            waitForIdle()

            clicks shouldBe 1
        }
}

/** Every ink on ground pair [skin] draws on the poster, from its own theme as well as the builder tokens. */
@Composable
private fun shellInks(
    skin: Skin,
    poster: PosterColors,
): List<ShellInk> {
    val seed = poster.seed.toColor()
    val tokens = LocalBuilderTokens.current
    val shared = listOf(
        ShellInk("tokens text on seed", tokens.textStrong, seed),
        ShellInk("tokens text on panel", tokens.textStrong, tokens.panel),
        ShellInk("tokens text on canvas", tokens.textStrong, tokens.canvas),
        ShellInk("tokens muted text on panel", tokens.textMuted, tokens.panel),
    )
    val own = when (skin.library) {
        Library.Material3 -> {
            val scheme = MaterialTheme.colorScheme
            listOf(
                ShellInk("onSurface on surface", scheme.onSurface, scheme.surface),
                ShellInk("primary on surface", scheme.primary, scheme.surface),
                ShellInk("content colour on surface", LocalContentColor.current, scheme.surface),
                ShellInk("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
            )
        }
        Library.Unstyled -> {
            val colors = Theme[MaterialKolorTokens.colors]
            val surface = colors[MaterialKolorTokens.surface]
            listOf(
                ShellInk("onSurface on surface", colors[MaterialKolorTokens.onSurface], surface),
                ShellInk("primary on surface", colors[MaterialKolorTokens.primary], surface),
                ShellInk("onSurfaceVariant on surface", colors[MaterialKolorTokens.onSurfaceVariant], surface),
            )
        }
        Library.Custom -> {
            val identity = LocalBuilderIdentity.current
            val surface = identity[CustomSlot.Surface]
            listOf(
                ShellInk("TextStrong on Surface", identity[CustomSlot.TextStrong], surface),
                ShellInk("OnSurface on Surface", identity[CustomSlot.OnSurface], surface),
                ShellInk("TextMuted on Surface", identity[CustomSlot.TextMuted], surface),
            )
        }
        Library.Fluent -> {
            val colors = FluentTheme.colors
            listOf(
                ShellInk("Fluent text on seed", colors.text.text.primary.compositeOver(seed), seed),
                ShellInk("Fluent content colour on seed", FluentContentColor.current, seed),
            )
        }
    }
    return shared + own
}
