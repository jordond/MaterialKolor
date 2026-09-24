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
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.custom.LocalBuilderIdentity
import com.materialkolor.builder.kit.skin.fluent.fluentButtonColors
import com.materialkolor.builder.kit.skin.fluent.fluentCheckboxColors
import com.materialkolor.builder.kit.skin.fluent.fluentSwitchStyles
import com.materialkolor.builder.kit.skin.fluent.fluentTabColors
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.fluent.toFluentShades
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.contrastRatio
import com.materialkolor.unstyled.MaterialKolorTokens
import io.github.composefluent.Colors
import io.github.composefluent.FluentTheme
import io.github.composefluent.scheme.VisualState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import io.github.composefluent.LocalContentColor as FluentContentColor

private const val ShellTextRatio = 4.5

/** What a glyph or a stroke that is not text has to reach on its ground, WCAG's non-text contrast. */
private const val ShellGlyphRatio = 3.0
private const val ShellClickTag = "shell-click"

/** Ten hues at five tones, mid tones included, where ink sits closest to the floor. */
private val ShellSeeds: List<Argb> = buildList {
    for (hue in 0 until 360 step 36) {
        for (tone in listOf(20.0, 35.0, 50.0, 62.0, 80.0)) {
            add(Argb(Hct.from(hue.toDouble(), 48.0, tone).toInt()))
        }
    }
}

/** One ink a skin draws on the poster, the ground it stands on and the ratio it has to reach there. */
private class ShellInk(
    val name: String,
    val ink: Color,
    val ground: Color,
    val floor: Double = ShellTextRatio,
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
     * Fluent on the poster takes its shades from the seed's own ramp and its dark flag from the
     * ink, and every Fluent control there draws its labels, glyphs and strokes in the ink. Fluent's
     * own black or white text reaches only about 4.35 to 1 on a mid tone seed, so each control's
     * resting label has to hold 4.5 on the ground it stands on, in the light chrome and the dark.
     * A glyph that is not text, such as the switch's thumb on Fluent's tinted track, holds 3.
     */
    @Test
    fun posterSurface_fluent_drawsEveryControlInTheInkInBothModes() {
        for (isDark in listOf(false, true)) {
            withClue(if (isDark) "dark chrome" else "light chrome") {
                runComposeUiTest {
                    var seed by mutableStateOf(ShellSeeds.first())
                    var colors: Colors? = null
                    var inks: List<ShellInk> = emptyList()
                    setContent {
                        ShellHarness(Skin(Library.Fluent, expressive = false), isDark = isDark) {
                            val poster = remember(seed) { PosterColors.of(seed) }
                            PosterSurface(poster) {
                                colors = FluentTheme.colors
                                inks = fluentControlInks(poster.seed.toColor())
                            }
                        }
                    }

                    val misses = mutableListOf<String>()
                    for (next in ShellSeeds) {
                        seed = next
                        waitForIdle()
                        val poster = PosterColors.of(next)
                        withClue(next.toHex()) {
                            val fluent = checkNotNull(colors)
                            fluent.darkMode shouldBe !poster.isLight
                            fluent.shades.base shouldBe poster.ramp.toFluentShades().base
                            inks.shouldNotBeEmpty()
                        }
                        misses += inks.mapNotNull { pair ->
                            val ratio = pair.ink.contrastRatio(pair.ground)
                            if (ratio < pair.floor) "${next.toHex()} ${pair.name} ${"%.2f".format(ratio)}" else null
                        }
                    }
                    withClue(misses.joinToString("\n")) { misses.shouldBeEmpty() }
                }
            }
        }
    }

    @Test
    fun fluentControls_offThePoster_keepFluentsOwnColours() =
        runComposeUiTest {
            var label: Color? = null
            var fluentLabel: Color? = null
            setContent {
                ShellHarness(Skin(Library.Fluent, expressive = false)) {
                    label = fluentSwitchStyles(checked = false).schemeFor(VisualState.Default).labelColor
                    fluentLabel = FluentTheme.colors.text.text.primary
                }
            }
            waitForIdle()

            label shouldBe fluentLabel
        }

    // pf-1
    @Test
    fun posterSurface_newPosterOnTheSameSeed_recomposesNothingUnderIt() {
        for ((name, skin) in ShellSkins) {
            withClue(name) {
                runComposeUiTest {
                    var poster by mutableStateOf(ShellPosterColors)
                    val count = CompositionCount()
                    setContent {
                        ShellHarness(skin) { PosterSurface(poster) { Counted(count) } }
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

// pf-1

/** How many times [Counted] ran. */
private class CompositionCount {
    var value: Int = 0
}

/** Counts its own runs. Its one argument never changes, so it only runs again when a theme above it forces it. */
@Composable
private fun Counted(count: CompositionCount) {
    count.value++
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
            listOf(ShellInk("Fluent content colour on seed", FluentContentColor.current, seed))
        }
    }
    return shared + own
}

/**
 * The resting ink of every Fluent control on the poster, each on the ground it stands on. A fill
 * Fluent lays on the seed is laid over it first, since Fluent's fills are translucent. Labels are
 * held to the text ratio and the glyphs, fills and strokes to the non-text one.
 */
@Composable
private fun fluentControlInks(seed: Color): List<ShellInk> {
    val rest = VisualState.Default

    fun Color.onSeed(): Color = compositeOver(seed)
    return buildList {
        for (emphasis in Emphasis.entries) {
            val look = fluentButtonColors(emphasis).schemeFor(rest)
            add(ShellInk("$emphasis button label", look.contentColor, look.fillColor.onSeed()))
            if (emphasis == Emphasis.Primary || emphasis == Emphasis.Danger) {
                add(ShellInk("$emphasis button fill", look.fillColor.onSeed(), seed, ShellGlyphRatio))
            }
        }
        for (on in listOf(false, true)) {
            val switch = fluentSwitchStyles(on).schemeFor(rest)
            add(ShellInk("switch label, on $on", switch.labelColor, seed))
            add(ShellInk("switch thumb, on $on", switch.controlColor, switch.fillColor.onSeed(), ShellGlyphRatio))
            val box = fluentCheckboxColors(on).schemeFor(rest)
            add(ShellInk("checkbox label, on $on", box.labelTextColor, seed))
            if (on) {
                add(ShellInk("checkbox check", box.contentColor, box.fillColor.onSeed(), ShellGlyphRatio))
            } else {
                add(ShellInk("checkbox box", box.borderColor, seed, ShellGlyphRatio))
            }
            val tab = fluentTabColors(on).schemeFor(rest)
            add(ShellInk("tab label, selected $on", tab.contentColor, tab.fillColor.onSeed()))
        }
    }
}
