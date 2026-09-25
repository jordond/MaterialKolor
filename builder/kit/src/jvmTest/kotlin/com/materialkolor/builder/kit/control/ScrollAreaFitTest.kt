package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.Skin
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ScrollAreaFitTest {
    @Test
    fun fitContent_standsAsTallAsWhatItHolds_whileTheDefaultTakesAllItMay() =
        runComposeUiTest {
            setContent {
                ControlsHarness(Skin(Library.Material3, expressive = false)) {
                    Column {
                        BuilderScrollArea(Modifier.testTag("fills").heightIn(max = 300.dp)) {
                            Box(Modifier.height(40.dp))
                        }
                        BuilderScrollArea(Modifier.testTag("fits").heightIn(max = 300.dp), fitContent = true) {
                            Box(Modifier.height(40.dp))
                        }
                    }
                }
            }
            waitForIdle()

            onNodeWithTag("fills").assertHeightIsEqualTo(300.dp)
            onNodeWithTag("fits").assertHeightIsEqualTo(40.dp)
        }
}
