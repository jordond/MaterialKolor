package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TooltipPlacementTest {
    @Test
    fun materialTooltip_eachWay_placesItsAnchorWhereTheCallersAlignmentSays() {
        for (inTree in listOf(false, true)) {
            withClue(if (inTree) "in tree" else "in windows") {
                runComposeUiTest {
                    setContent {
                        HostOverlays(Skin(Library.Material3, expressive = false), inTree) {
                            Box(Modifier.testTag("frame").size(200.dp)) {
                                BuilderTooltip("Copy", Modifier.align(Alignment.BottomEnd)) {
                                    Box(Modifier.testTag("anchor").size(40.dp))
                                }
                            }
                        }
                    }
                    val frame = onNodeWithTag("frame").getBoundsInRoot()
                    val anchor = onNodeWithTag("anchor", useUnmergedTree = true).getBoundsInRoot()
                    anchor.right.value shouldBe (frame.right.value plusOrMinus 0.5f)
                    anchor.bottom.value shouldBe (frame.bottom.value plusOrMinus 0.5f)
                }
            }
        }
    }
}
