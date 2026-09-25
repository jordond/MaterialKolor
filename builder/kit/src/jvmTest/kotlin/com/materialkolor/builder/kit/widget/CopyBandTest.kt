package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.LocalLayout
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import kotlin.test.Test

/**
 * The narrowest a swatch gets in the app, the Roles tab's least column width.
 */
private val NarrowestSwatch: Dp = 152.dp

/**
 * A phone in the hand, where the copy button always shows at the 48 dp target.
 */
private val Phone: LayoutInfo = LayoutInfo.of(360.dp, 800.dp, coarsePointer = true)

/**
 * A desktop under a mouse, where a skin's own icon button outgrows the 24 dp target.
 */
private val Desktop: LayoutInfo = LayoutInfo.of(1280.dp, 800.dp)

/**
 * The swatch's name, which a test focuses it by.
 */
private const val NarrowSwatchName = "primary, #6750A4, tone 40"

/**
 * A line long enough to scroll sideways in [CodeViewSize].
 */
private const val LongLine = "val primaryContainer = Color(0xFFEADDFF) // the first line runs past the corner"

/**
 * A code view narrower than [LongLine].
 */
private val CodeViewSize: Dp = 320.dp

/**
 * Enough presses of the right arrow to scroll [LongLine] to its end.
 */
private const val SidewaysPresses = 80

/**
 * The copy band never covers what it sits beside. A swatch keeps the band's room free of its lines,
 * and a code view lets its lines run on past the band, in every skin.
 */
@OptIn(ExperimentalTestApi::class)
class CopyBandTest {
    @Test
    fun swatchTile_atItsNarrowest_keepsItsLinesClearOfTheCopyBand() {
        for ((name, layout) in listOf("phone" to Phone, "desktop" to Desktop)) {
            withClue(name) {
                forEachWidgetSkin { _, skin ->
                    setContent {
                        WidgetHarness(skin) {
                            CompositionLocalProvider(LocalLayout provides layout) {
                                SwatchTile(
                                    name = "primary",
                                    color = Color(0xFF6750A4),
                                    onColor = Color(0xFFFFFFFF),
                                    tone = 40.0,
                                    contrast = 21.0,
                                    onCopy = {},
                                    onClick = {},
                                    modifier = Modifier.width(NarrowestSwatch),
                                )
                            }
                        }
                    }
                    onNodeWithContentDescription(NarrowSwatchName).requestFocus()
                    waitForIdle()
                    val band = onNodeWithTag(CopyBandTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    val readout = onNodeWithTag(SwatchReadoutTag, useUnmergedTree = true).fetchSemanticsNode()
                    withClue("room for the lines ${readout.boundsInRoot}, band $band") {
                        readout.boundsInRoot.right shouldBeLessThanOrEqual band.left
                    }
                    val lines = onAllNodes(
                        SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and
                            hasAnyAncestor(hasTestTag(SwatchReadoutTag)),
                        useUnmergedTree = true,
                    ).fetchSemanticsNodes()
                    lines.shouldNotBeEmpty()
                    for (line in lines) {
                        withClue("${line.config[SemanticsProperties.Text]} at ${line.boundsInRoot}, band $band") {
                            line.boundsInRoot.right shouldBeLessThanOrEqual band.left
                        }
                    }
                }
            }
        }
    }

    @Test
    fun codeView_scrolledToTheEnd_bringsATopLineOutFromUnderTheCopyBand() =
        forEachWidgetSkin { _, skin ->
            val lines = listOf(listOf(Token(TokenKind.Plain, LongLine)))
            setContent {
                WidgetHarness(skin) { CodeView(lines, onCopy = {}, Modifier.size(CodeViewSize, 160.dp)) }
            }
            val code = onNodeWithTag(CodeScrollTag)
            code.requestFocus()
            waitForIdle()
            code.performKeyInput { repeat(SidewaysPresses) { pressKey(Key.DirectionRight) } }
            waitForIdle()
            val band = onNodeWithTag(CopyBandTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val text = onNode(hasText(LongLine), useUnmergedTree = true).fetchSemanticsNode()
            val end = text.positionInRoot.x + text.size.width
            withClue("line from ${text.positionInRoot} to $end, band $band") {
                text.positionInRoot.y shouldBeLessThan band.bottom
                end shouldBeLessThanOrEqual band.left
            }
        }
}
