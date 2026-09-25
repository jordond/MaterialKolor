package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    @Test
    fun scrollbar_drawsAThumbOnlyWhileTheContentOverflows() =
        runComposeUiTest {
            setContent {
                ControlsHarness(Skin(Library.Material3, expressive = false)) {
                    Column {
                        ThumbProbe("fits", content = 40.dp)
                        ThumbProbe("overflows", content = 600.dp)
                    }
                }
            }
            waitForIdle()

            thumbInk("fits") shouldBe ProbeGround
            thumbInk("overflows") shouldNotBe ProbeGround
        }
}

@Composable
private fun ThumbProbe(
    tag: String,
    content: Dp,
) {
    Box(Modifier.testTag(tag).size(200.dp, 300.dp).background(ProbeGround)) {
        BuilderScrollArea(Modifier.size(200.dp, 300.dp)) {
            Box(Modifier.height(content))
        }
    }
}

/**
 * The colour in the middle of the thumb's lane near the top of the area, where the thumb sits
 * before any scroll.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.thumbInk(tag: String): Color {
    val pixels = onNodeWithTag(tag).captureToImage().toPixelMap()
    val lane = with(density) { (OverlayMetrics.thumbInset + OverlayMetrics.thumbThickness / 2).roundToPx() }
    val top = with(density) { 20.dp.roundToPx() }
    return pixels[pixels.width - lane, top]
}

private val ProbeGround = Color.White
