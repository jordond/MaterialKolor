package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderToastHost
import com.materialkolor.builder.kit.control.BuilderToastHostState
import com.materialkolor.builder.kit.control.ControlsHarness
import com.materialkolor.builder.kit.control.ToastDuration
import com.materialkolor.builder.kit.shell.DockRegion
import com.materialkolor.builder.kit.shell.TopBarRegion
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Fluent = Skin(Library.Fluent, expressive = false)
private const val BarTag = "fluent-bar"
private const val DockTag = "fluent-dock"
private const val DockTools = 12

@OptIn(ExperimentalTestApi::class)
class FluentRegionsTest {
    /**
     * Fluent's own `CommandBar` keeps a nameless overflow button composed off screen, which a
     * screen reader would still find. The header is laid out as the bar is without it, so the only
     * controls in it are the ones it was given, and a weighted spacer pushes the last to the far end.
     */
    @Test
    fun topBar_holdsItsControlsOnlyAndLetsAWeightedChildFillTheRest() =
        runSkikoComposeUiTest(size = Size(800f, 200f)) {
            setContent {
                ControlsHarness(Fluent) {
                    TopBarRegion(Modifier.testTag(BarTag)) {
                        BuilderButton(onClick = {}, label = "Share")
                        Spacer(Modifier.weight(1f))
                        BuilderButton(onClick = {}, label = "Export")
                    }
                }
            }
            waitForIdle()

            onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes() shouldHaveSize 2
            val bar = onNodeWithTag(BarTag).getBoundsInRoot()
            (bar.right - onNodeWithText("Export").getBoundsInRoot().right) shouldBeLessThanOrEqualTo 24.dp
            (onNodeWithText("Share").getBoundsInRoot().left - bar.left) shouldBeLessThanOrEqualTo 24.dp
        }

    /**
     * A dock too wide for its window squeezes its tools and never hands them to an overflow.
     */
    @Test
    fun dock_widerThanItsWindow_keepsEveryToolInTheBar() =
        runSkikoComposeUiTest(size = Size(390f, 200f)) {
            setContent {
                ControlsHarness(Fluent) {
                    DockRegion(Modifier.testTag(DockTag)) {
                        repeat(DockTools) { index -> DockTool(index) }
                    }
                }
            }
            waitForIdle()

            onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes() shouldHaveSize DockTools
            onNodeWithText("Tool 0").assertIsDisplayed()
            onNodeWithTag(DockTag).getBoundsInRoot().right shouldBeLessThanOrEqualTo 390.dp
        }

    /**
     * The toast is Fluent's `InfoBar`, its message in Selawik, and its action still closes it.
     */
    @Test
    fun toast_setsItsMessageInSelawikAndItsActionClosesIt() =
        runComposeUiTest {
            val toasts = BuilderToastHostState()
            var face: FontFamily? = null
            var undone = false
            setContent {
                ControlsHarness(Fluent) {
                    face = fluentFontFamily()
                    BuilderToastHost(toasts)
                }
            }
            toasts.show("Pin removed", actionLabel = "Undo", duration = ToastDuration.Indefinite) { undone = true }
            waitForIdle()

            val layout = onNodeWithText("Pin removed", useUnmergedTree = true).fetchTextLayout()
            layout.layoutInput.style.fontFamily shouldBe face
            onNodeWithText("Undo").performClick()
            waitForIdle()

            undone shouldBe true
            toasts.toasts shouldHaveSize 0
        }
}

@Composable
private fun DockTool(index: Int) {
    BuilderButton(onClick = {}, label = "Tool $index", modifier = Modifier.width(64.dp))
}

/**
 * The text layout of this node, which has to hold a single piece of text.
 */
private fun SemanticsNodeInteraction.fetchTextLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.single()
}
