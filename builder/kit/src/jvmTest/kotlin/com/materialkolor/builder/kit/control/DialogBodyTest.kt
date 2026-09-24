package com.materialkolor.builder.kit.control

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import kotlin.test.Test

private const val CloseTag = "close"
private const val ListTag = "list"

@OptIn(ExperimentalTestApi::class)
class DialogBodyTest {
    @Test
    fun dialog_eachWay_everySkin_givesALongListWhatIsLeftAndKeepsItsActionsOnScreen() =
        hostEachWay { skin, inTree ->
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(
                        visible = true,
                        onDismissRequest = {},
                        title = "Presets",
                        actions = { BuilderButton({}, "Close", Modifier.testTag(CloseTag)) },
                    ) {
                        BuilderScrollArea(Modifier.testTag(ListTag), tabStop = false) {
                            repeat(80) { line -> BuilderText("Preset $line") }
                        }
                    }
                }
            }
            waitForIdle()

            val window = onAllNodes(isRoot()).fetchSemanticsNodes().maxOf { root -> root.boundsInWindow.bottom }
            val close = onNodeWithTag(CloseTag).assertIsDisplayed().assertHeightIsAtLeast(24.dp)
            close.fetchSemanticsNode().boundsInWindow.bottom shouldBeLessThanOrEqual window
            onNodeWithTag(ListTag).fetchSemanticsNode().boundsInWindow.height shouldBeGreaterThan window / 3
        }
}
