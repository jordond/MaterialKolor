package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.contrast
import com.materialkolor.builder.kit.control.shouldClearTheTrack
import com.materialkolor.builder.kit.control.shouldRingAllTheWayRound
import com.materialkolor.builder.kit.control.shouldRingEverySide
import com.materialkolor.builder.kit.control.shouldShowRing
import com.materialkolor.builder.kit.control.tabOntoRing
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test

/** The least contrast a focus line needs against what it sits on and against its halo. */
private const val LineContrast = 3.0

/** The key held with C to copy, Command on a Mac and Control everywhere else, as the desktop reads it. */
private val CopyModifierKey: Key =
    if (System.getProperty("os.name").orEmpty().startsWith("Mac")) Key.MetaLeft else Key.CtrlLeft

/**
 * The widgets ring when Tab lands on them, in every skin, with the ring standing 3 to 1 from what
 * it sits on all the way round (AR-01, S5 rows 30, 32 and 33, and the partial rings of the rerun).
 */
@OptIn(ExperimentalTestApi::class)
class WidgetFocusRingTest {
    @Test
    fun swatchTile_ringsOnThePanel_andSoDoesItsCopyButton() {
        for ((name, skin) in WidgetSkins) {
            withClue(name) {
                runComposeUiTest {
                    val tile = tabOntoRing(skin) { RingSwatch() }
                    onNodeWithContentDescription(RingSwatchName).assertIsFocused()
                    tile.shouldShowRing()
                    tile.shouldRingEverySide()
                }
                runComposeUiTest {
                    val copy = tabOntoRing(skin, presses = 2) { RingSwatch() }.nearFocused(CopyRingReach)
                    onNodeWithContentDescription("Copy").assertIsFocused()
                    copy.shouldShowRing()
                    copy.shouldRingEverySide()
                    copy.shouldRingAllTheWayRound()
                }
            }
        }
    }

    /**
     * The copy button sits on a band of the panel, so its ring lands on the panel and not on the code
     * ground, which the focus color does not clear in every skin (S5 gap 5).
     */
    @Test
    fun codeView_copyButton_ringsOnItsBandAllTheWayRound() {
        val file = widgetGoldenColorFile()
        forEachWidgetSkin { _, skin ->
            val copy = tabOntoRing(skin, presses = 2) {
                CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp))
            }.nearFocused(CopyRingReach)
            onNodeWithContentDescription("Copy").assertIsFocused()
            copy.shouldShowRing()
            copy.shouldRingEverySide()
            copy.shouldRingAllTheWayRound()
        }
    }

    /**
     * The thumb sits in a panel halo, so its ring never crosses the track's colors. It is small and
     * round, so it is drawn at two pixels per dp like the slider's thumb.
     */
    @Test
    fun gamutTrack_everyChannel_ringsTheThumbAllTheWayRoundClearOfTheTrack() {
        for (channel in HctChannel.entries) {
            withClue(channel.name) {
                forEachWidgetSkin { _, skin ->
                    val capture = tabOntoRing(skin, density = 2f) { RingTrack(channel) }
                    capture.shouldShowRing()
                    capture.shouldRingEverySide(around = capture.ringBounds)
                    capture.shouldRingAllTheWayRound()
                    capture.shouldClearTheTrack()
                }
            }
        }
    }

    /** The ring stands outside the chip's hairline rather than on it, chosen or not. */
    @Test
    fun schemeChip_chosenOrNot_ringsOutsideItsEdgeAllTheWayRound() {
        for (selected in listOf(false, true)) {
            withClue("selected $selected") {
                forEachWidgetSkin { _, skin ->
                    val capture = tabOntoRing(skin, density = 2f) { RingChip(selected) }
                    capture.shouldShowRing()
                    capture.shouldRingEverySide()
                    capture.shouldRingAllTheWayRound()
                }
            }
        }
    }

    @Test
    fun rampStrip_everyFocusedStop_drawsALineThatClearsTheStopAndItsHalo() =
        forEachWidgetSkin { _, skin ->
            val ramp = ThemeResolver().resolve(WidgetDocument).ramps[KeyColor.Primary, false]
            var tokens: BuilderTokens? = null
            setContent {
                WidgetHarness(skin) {
                    tokens = LocalBuilderTokens.current
                    RampStrip(ramp, onCopyTone = {}, Modifier.width(720.dp))
                }
            }
            val colors = tokens.shouldNotBeNull()
            val halo = with(density) { (WidgetFocusWidth / 2).toPx() }
            val line = with(density) { WidgetFocusWidth.toPx() }
            ramp.steps.size shouldBe RampSet.Tones.size
            for (step in ramp.steps) {
                withClue("tone ${step.tone}") {
                    val stop = onNodeWithContentDescription("tone ${step.tone}, ${step.argb.toHex()}")
                    stop.requestFocus()
                    waitForIdle()
                    val pixels = stop.captureToImage().toPixelMap()
                    val y = pixels.height / 2
                    val drawn = pixels[(halo + line / 2).toInt(), y]
                    val color = step.argb.toColor()
                    contrast(drawn, color) shouldBeGreaterThanOrEqual LineContrast
                    contrast(drawn, pixels[(halo / 2).toInt(), y]) shouldBeGreaterThanOrEqual LineContrast
                    contrast(drawn, pixels[(halo * 1.5f + line).toInt(), y]) shouldBeGreaterThanOrEqual LineContrast
                    val focusClears = contrast(colors.focus, color) >= LineContrast &&
                        contrast(colors.focus, colors.panel) >= LineContrast
                    if (focusClears) widgetShouldMatch(drawn, colors.focus)
                }
            }
        }

    @Test
    fun codeView_tabFromAButton_landsOnOneRingedStopThenTheCopyButton() {
        val file = widgetGoldenColorFile()
        for ((name, skin) in WidgetSkins) {
            withClue(name) {
                runComposeUiTest {
                    val capture = tabOntoRing(skin) { CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp)) }
                    onNodeWithTag(CodeScrollTag).assertIsFocused()
                    capture.shouldShowRing()
                    capture.shouldRingEverySide()
                    onNode(isFocused()).performKeyInput { pressKey(Key.Tab) }
                    waitForIdle()
                    onNodeWithContentDescription("Copy").assertIsFocused()
                }
            }
        }
    }

    @Test
    fun codeView_dragSelectionThenTheCopyKeys_copiesTheSelection() =
        forEachWidgetSkin { _, skin ->
            val file = widgetGoldenColorFile()
            val text = file.lines.joinToString("\n") { line -> line.joinToString("") { token -> token.text } }
            val clipboard = CopiedText()
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    WidgetHarness(skin) { CodeView(file.lines, onCopy = {}, Modifier.size(480.dp, 200.dp)) }
                }
            }
            val run = onAllNodes(hasTextOfAtLeast(RunLength), useUnmergedTree = true)[0]
            run.performMouseInput { dragAndDrop(centerLeft + Offset(1f, 0f), centerRight - Offset(1f, 0f)) }
            waitForIdle()
            val code = onNodeWithTag(CodeScrollTag)
            code.assertIsFocused()
            code.performKeyInput { withKeyDown(CopyModifierKey) { pressKey(Key.C) } }
            waitForIdle()
            val copied = clipboard.text.shouldNotBeNull()
            copied.shouldNotBeBlank()
            text.filterNot(Char::isWhitespace) shouldContain copied.filterNot(Char::isWhitespace)
        }
}

private const val RingSwatchName = "primary, #6750A4, tone 40"

/** How far past a copy button its ring and the blend at its edge reach. */
private val CopyRingReach: Dp = FocusRingOffset + FocusRingWidth + 1.dp

/** How long a run of code has to be for the drag test to select a good part of it. */
private const val RunLength = 12

@Composable
private fun RingSwatch() {
    SwatchTile(
        name = "primary",
        color = Color(0xFF6750A4),
        onColor = Color(0xFFFFFFFF),
        tone = 40.0,
        contrast = 6.4,
        onCopy = {},
        onClick = {},
        modifier = Modifier.width(200.dp),
    )
}

/** A picker track for [channel] at the ring document's seed. */
@Composable
private fun RingTrack(channel: HctChannel) {
    val report = rememberUpdatedState<(Argb, EditPhase) -> Unit> { _, _ -> }
    val picker = remember { PickerState(Argb(0x6750A4), report) }
    GamutTrack(picker, channel, channel.name, Modifier.width(320.dp))
}

@Composable
private fun RingChip(selected: Boolean) {
    SchemeChip(
        primary = Color(0xFF6750A4),
        secondaryContainer = Color(0xFFE8DEF8),
        tertiaryContainer = Color(0xFFFFD8E4),
        selected = selected,
        onClick = {},
        label = "Tonal spot",
    )
}

/** Matches a text node with at least [length] characters. */
private fun hasTextOfAtLeast(length: Int): SemanticsMatcher =
    SemanticsMatcher("text of at least $length characters") { node ->
        val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
        text.sumOf { part -> part.length } >= length
    }

/** A clipboard that keeps the last text written to it. */
private class CopiedText : Clipboard {
    var text: String? = null

    override suspend fun getClipEntry(): ClipEntry? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        text = clipEntry?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? String
    }
}
