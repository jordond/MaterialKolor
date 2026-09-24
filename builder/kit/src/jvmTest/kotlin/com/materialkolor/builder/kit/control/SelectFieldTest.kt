package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import kotlin.test.Test

private const val SelectTag = "select"

@OptIn(ExperimentalTestApi::class)
class SelectFieldTest {
    @Test
    fun select_eachWay_everySkin_opensOnAMouseClickAtTheCentreOfTheWidthItIsGiven() =
        hostEachWay { skin, inTree ->
            setContent {
                HostOverlays(skin, inTree) {
                    BuilderSelect(
                        label = "Style",
                        options = listOf("Tonal spot", "Vibrant"),
                        selected = "Tonal spot",
                        onSelect = {},
                        modifier = Modifier.testTag(SelectTag).width(260.dp),
                    )
                }
            }
            val option = onNode(hasText("Vibrant") and hasRole(Role.RadioButton))
            option.assertDoesNotExist()

            onNodeWithTag(SelectTag).performMouseInput { click(center) }
            waitForIdle()

            option.assertExists()
        }
}
