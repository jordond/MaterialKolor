package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val Title = "Share"

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
