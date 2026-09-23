package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DemoAppStateTest {
    @Test
    fun scroll_inEitherCopy_isMirroredByTheOther() =
        runComposeUiTest {
            val state = DemoAppState()
            val lists = mutableMapOf<String, LazyListState>()
            setContent {
                Chrome {
                    SplitPreview(
                        LightSpec,
                        DarkSpec,
                        SplitState(),
                        Modifier.size(400.dp, 300.dp).testTag("split"),
                    ) { spec ->
                        val list = state.rememberListState("feed")
                        lists[spec.label] = list
                        LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                            items(200) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                        }
                    }
                }
            }
            val light = { lists.getValue("Light") }
            val dark = { lists.getValue("Dark") }

            onNodeWithTag("split").performTouchInput {
                swipe(Offset(width * 0.1f, height * 0.8f), Offset(width * 0.1f, height * 0.2f), durationMillis = 300)
            }
            waitForIdle()
            val afterStart = light().firstVisibleItemIndex
            afterStart shouldBeGreaterThan 0
            dark().position() shouldBe light().position()

            onNodeWithTag("split").performTouchInput {
                swipe(Offset(width * 0.9f, height * 0.8f), Offset(width * 0.9f, height * 0.2f), durationMillis = 300)
            }
            waitForIdle()
            dark().firstVisibleItemIndex shouldBeGreaterThan afterStart
            light().position() shouldBe dark().position()

            onNode(hasScrollToIndexAction()).performScrollToIndex(150)
            waitForIdle()
            light().firstVisibleItemIndex shouldBe 150
            dark().position() shouldBe light().position()
        }

    @Test
    fun scroll_eachCopyMovingWhileTheOtherIsDragged_keepsMirroringAfterwards() =
        runComposeUiTest {
            val state = DemoAppState()
            val lists = mutableMapOf<String, LazyListState>()
            setContent {
                Chrome {
                    SplitPreview(
                        LightSpec,
                        DarkSpec,
                        SplitState(),
                        Modifier.size(400.dp, 300.dp).testTag("split"),
                    ) { spec ->
                        val list = state.rememberListState("feed")
                        lists[spec.label] = list
                        LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                            items(2000) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                        }
                    }
                }
            }
            val light = { lists.getValue("Light") }
            val dark = { lists.getValue("Dark") }

            // One finger drags the dark copy and holds it, while a second drags the light copy and
            // lets it fling. Each copy moves on its own while the other is held by a drag.
            onNodeWithTag("split").performTouchInput {
                down(DARK, Offset(width * 0.9f, height * 0.8f))
                repeat(5) { moveBy(DARK, Offset(0f, -10f)) }
                down(LIGHT, Offset(width * 0.1f, height * 0.9f))
                repeat(8) {
                    updatePointerBy(LIGHT, Offset(0f, -25f))
                    updatePointerBy(DARK, Offset(0f, -2f))
                    move()
                }
                up(LIGHT)
                repeat(5) { moveBy(DARK, Offset(0f, -2f)) }
                up(DARK)
            }
            waitForIdle()
            dark().position() shouldBe light().position()

            val settled = light().firstVisibleItemIndex
            onNodeWithTag("split").performTouchInput {
                swipe(Offset(width * 0.1f, height * 0.8f), Offset(width * 0.1f, height * 0.2f), durationMillis = 300)
            }
            waitForIdle()
            light().firstVisibleItemIndex shouldBeGreaterThan settled
            dark().position() shouldBe light().position()

            onNodeWithTag("split").performTouchInput {
                swipe(Offset(width * 0.9f, height * 0.8f), Offset(width * 0.9f, height * 0.2f), durationMillis = 300)
            }
            waitForIdle()
            light().position() shouldBe dark().position()
        }

    @Test
    fun switchesAndCheckboxes_untouched_readOffAndRememberWhatIsSet() {
        val state = DemoAppState()

        state.isOn("wifi") shouldBe false
        state.isChecked("terms") shouldBe false
        state.setOn("wifi", true)
        state.setChecked("terms", true)

        state.isOn("wifi") shouldBe true
        state.isChecked("terms") shouldBe true
        state.isOn("terms") shouldBe false
    }

    private fun LazyListState.position(): Pair<Int, Int> = firstVisibleItemIndex to firstVisibleItemScrollOffset
}

/** The finger on the dark copy. */
private const val DARK = 0

/** The finger on the light copy. */
private const val LIGHT = 1
