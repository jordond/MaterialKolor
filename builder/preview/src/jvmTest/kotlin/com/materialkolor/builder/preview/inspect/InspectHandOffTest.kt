package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-315b

/**
 * The element in the light copy of a split, with nothing in the dark copy.
 */
private const val LIGHT = "light"

/**
 * The first action of a pinned card, so its presence means a card is pinned.
 */
private const val PIN_ROLE = "Pin this role"

/**
 * How many rows the scrolling list has, far more than fit.
 */
private const val ROWS = 100

/**
 * How many Tab presses it may take to reach the card from the preview.
 */
private const val MAX_TABS = 8

@OptIn(ExperimentalTestApi::class)
class InspectHandOffTest {
    @Test
    fun handleCrossingThePinnedElement_whileACardActionHoldsFocus_leavesEscWorking() =
        runComposeUiTest {
            val split = SplitState()
            var leaves = 0
            setContent {
                Inspecting(PreviewMode.Split, split, onLeave = { leaves++ }) {
                    SplitPreview(
                        start = LightSpec,
                        end = DarkSpec,
                        split = split,
                        modifier = Modifier.fillMaxSize(),
                    ) { spec ->
                        // The pane stretches what it holds, so a box of its own keeps the element small.
                        Box(Modifier.fillMaxSize()) {
                            if (!spec.isDark) Element(Modifier.padding(40.dp).testTag(LIGHT))
                        }
                    }
                }
            }
            waitForIdle()
            pinAndTabIntoTheCard(LIGHT)

            runOnIdle { split.fraction = 0f }
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            leaves shouldBe 1
        }

    @Test
    fun pinnedRowScrollingAway_whileACardActionHoldsFocus_leavesEscWorking() =
        runComposeUiTest {
            val list = LazyListState()
            var leaves = 0
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }, onLeave = { leaves++ }) {
                    LazyColumn(Modifier.fillMaxSize(), state = list) {
                        items(ROWS) { row -> Element(Modifier.padding(start = 40.dp).testTag("row $row")) }
                    }
                }
            }
            waitForIdle()
            pinAndTabIntoTheCard("row 2")

            runOnIdle { list.dispatchRawDelta(with(density) { 2000.dp.toPx() }) }
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()

            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            leaves shouldBe 1
        }

    /**
     * Pin the element tagged [tag] with a click, then Tab until an action on its card holds focus.
     */
    private fun ComposeUiTest.pinAndTabIntoTheCard(tag: String) {
        onNodeWithTag(tag).performClick()
        waitForIdle()
        onNode(OnCard and hasText(PIN_ROLE)).assertExists()
        var presses = 0
        while (onAllNodes(OnCard and isFocused()).fetchSemanticsNodes().isEmpty()) {
            check(presses++ < MAX_TABS) { "Tab never reached the Inspect card" }
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
        }
    }
}

/**
 * A small element declaring a role pair.
 */
@Composable
private fun Element(modifier: Modifier) {
    Box(modifier.size(40.dp).previewRoles(*PrimaryPair))
}
