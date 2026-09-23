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
