package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val Title = "Share"

private const val BODY = "body"

@OptIn(ExperimentalTestApi::class)
class DialogFrameTest {
    @Test
    fun dialog_eachWay_everySkin_closeButtonIsAButtonThatAsksToClose() =
        hostEachWay { skin, inTree ->
            var dismissed = 0
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(
                        visible = true,
                        onDismissRequest = { dismissed++ },
                        title = Title,
                        closeButton = true,
                    ) { BuilderText("Body") }
                }
            }
            waitForIdle()

            val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            onNode(hasContentDescription("Close") and isButton).performClick()
            waitForIdle()

            dismissed shouldBe 1
        }

    @Test
    fun dialog_eachWay_everySkin_closeButtonLeavesTheBodyWhereItIs() =
        hostEachWay { skin, inTree ->
            var closeButton by mutableStateOf(false)
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(
                        visible = true,
                        onDismissRequest = {},
                        title = Title,
                        closeButton = closeButton,
                    ) { Box(Modifier.testTag(BODY).fillMaxWidth().height(40.dp)) }
                }
            }
            waitForIdle()
            val without = onNodeWithTag(BODY).fetchSemanticsNode().boundsInWindow.top

            closeButton = true
            waitForIdle()

            onNodeWithTag(BODY).fetchSemanticsNode().boundsInWindow.top shouldBe without
        }

    @Test
    fun dialog_eachWay_everySkin_closeButtonTakesAClickAboveTheTitle() =
        hostEachWay { skin, inTree ->
            var dismissed = 0
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderDialog(
                        visible = true,
                        onDismissRequest = { dismissed++ },
                        title = Title,
                        closeButton = true,
                    ) { BuilderText("Body") }
                }
            }
            waitForIdle()

            val close = onNode(hasContentDescription("Close"))
            val button = close.fetchSemanticsNode().boundsInWindow
            val title = onNodeWithText(Title).fetchSemanticsNode().boundsInWindow
            // A pixel inside the target's top edge, over the panel's padding above the title.
            val y = 1f
            button.top + y shouldBeLessThan title.top
            close.performMouseInput { click(Offset(centerX, y)) }
            waitForIdle()

            dismissed shouldBe 1
        }

    @Test
    fun dialog_eachWay_everySkin_wideDialogGrowsPastMaterialsWidestUpToItsOwn() {
        for (inTree in listOf(false, true)) {
            for ((name, skin) in ControlSkins) {
                withClue("${if (inTree) "in tree" else "in windows"} $name") {
                    runSkikoComposeUiTest(size = Size(1280f, 800f)) {
                        setContent {
                            HostOverlays(skin, inTree) {
                                BuilderDialog(
                                    visible = true,
                                    onDismissRequest = {},
                                    title = Title,
                                    maxWidth = 920.dp,
                                ) { Box(Modifier.fillMaxWidth().height(40.dp)) }
                            }
                        }
                        waitForIdle()

                        val pane = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, Title)
                        val width = onNode(pane).fetchSemanticsNode().boundsInWindow.width / density.density
                        width shouldBeGreaterThan 560f
                        width shouldBeLessThanOrEqual 920f
                    }
                }
            }
        }
    }
}
