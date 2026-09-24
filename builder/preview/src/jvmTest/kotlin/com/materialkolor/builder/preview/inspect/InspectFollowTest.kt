package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import kotlin.test.Test

/** How many rows the scrolling list has, far more than fit. */
private const val ROWS = 100

/** The row the card is pinned to. */
private const val PINNED_ROW = 2

/** The element in the light copy of a split. */
private const val LIGHT = "light"

@OptIn(ExperimentalTestApi::class)
class InspectFollowTest {
    @Test
    fun scrollUnderAPinnedCard_outlineAndCardFollow_andGoWhenTheElementLeaves() =
        runComposeUiTest {
            val list = LazyListState()
            setContent {
                Inspecting(PreviewMode.Light, remember { SplitState() }) {
                    LazyColumn(Modifier.fillMaxSize().background(Color.White), state = list) {
                        items(ROWS) { row ->
                            Box(
                                Modifier
                                    .padding(
                                        start = 40.dp,
                                    ).size(40.dp)
                                    .testTag(rowTag(row))
                                    .previewRoles(*PrimaryPair),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            onNodeWithTag(rowTag(PINNED_ROW)).performClick()
            waitForIdle()
            val element = onNodeWithTag(rowTag(PINNED_ROW)).getBoundsInRoot()
            val card = onNodeWithTag(INSPECT_CARD_TAG).getBoundsInRoot()
            outlined(element) shouldBe true

            runOnIdle { list.dispatchRawDelta(with(density) { 30.dp.toPx() }) }
            waitForIdle()
            val moved = onNodeWithTag(rowTag(PINNED_ROW)).getBoundsInRoot()
            moved.top shouldBe element.top - 30.dp
            onNodeWithTag(INSPECT_CARD_TAG).getBoundsInRoot().top shouldBe card.top - 30.dp
            outlined(moved) shouldBe true
            outlined(element) shouldBe false

            runOnIdle { list.dispatchRawDelta(with(density) { 2000.dp.toPx() }) }
            waitForIdle()
            onNodeWithTag(rowTag(PINNED_ROW)).assertDoesNotExist()
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()
        }

    @Test
    fun handleCrossingAPinnedElement_movesTheCardToTheOtherCopy() =
        handleCrossing(darkCopyHasIt = true) {
            onNode(OnCard and hasText("Dark mode")).assertExists()
            onNode(OnCard and hasText("Pin this role")).assertExists()
        }

    @Test
    fun handleCrossingAPinnedElement_withNothingOnTheOtherCopy_dropsTheCard() =
        handleCrossing(darkCopyHasIt = false) {
            onNodeWithTag(INSPECT_CARD_TAG).assertDoesNotExist()
        }

    /**
     * Pin the element in the light copy of a split, move the handle without reaching it, then past
     * it to the start edge, and run [check].
     */
    private fun handleCrossing(
        darkCopyHasIt: Boolean,
        check: ComposeUiTest.() -> Unit,
    ) = runComposeUiTest {
        val split = SplitState()
        setContent {
            Inspecting(PreviewMode.Split, split) {
                SplitPreview(
                    start = LightSpec,
                    end = DarkSpec,
                    split = split,
                    modifier = Modifier.fillMaxSize(),
                ) { spec ->
                    // The pane stretches what it holds, so a box of its own keeps the element small.
                    Box(Modifier.fillMaxSize()) {
                        if (!spec.isDark || darkCopyHasIt) {
                            Box(
                                Modifier
                                    .padding(40.dp)
                                    .size(40.dp)
                                    .testTag(LIGHT)
                                    .previewRoles(*PrimaryPair),
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag(LIGHT).performClick()
        waitForIdle()
        onNode(OnCard and hasText("Light mode")).assertExists()

        runOnIdle { split.fraction = 0.3f }
        waitForIdle()
        onNode(OnCard and hasText("Light mode")).assertExists()

        runOnIdle { split.fraction = 0f }
        waitForIdle()
        check()
    }

    /** Whether the Inspect outline runs down the start edge of [element], checked halfway down. */
    private fun ComposeUiTest.outlined(element: DpRect): Boolean {
        val preview = onNodeWithTag(PREVIEW_TAG).getBoundsInRoot()
        val image = onNodeWithTag(PREVIEW_TAG).captureToImage().toPixelMap()
        val x = with(density) { (element.left - preview.left).toPx() }.roundToInt() - 1
        val y = with(density) { ((element.top + element.bottom) / 2 - preview.top).toPx() }.roundToInt()
        return image[x, y] != Color.White
    }

    private fun rowTag(row: Int): String = "row $row"
}
