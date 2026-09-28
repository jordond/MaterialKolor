package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val AreaTag = "area"

/**
 * Tooltips in popups of their own, the way the desktop app shows them. A label a scroll carries
 * under a resting pointer takes the next wheel step from the page, since a popup keeps the wheel
 * events over it.
 */
@OptIn(ExperimentalTestApi::class)
class TooltipPopupScrollTest {
    @Test
    fun wheel_pointerRestingOverTooltipAnchors_everyStepScrollsTheArea() =
        forEachSkin { _, skin ->
            val scroll = ScrollState(initial = 600)
            setContent {
                HostOverlays(skin, inTree = false) {
                    BuilderScrollArea(Modifier.testTag(AreaTag).size(240.dp, 320.dp), state = scroll) {
                        repeat(60) { index ->
                            BuilderTooltip("Style $index") { Box(Modifier.fillMaxWidth().height(20.dp)) }
                        }
                    }
                }
            }
            val area = onNodeWithTag(AreaTag)
            area.performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis * 2)
            waitForIdle()
            for (direction in listOf(-1f, 1f)) {
                repeat(8) { step ->
                    val before = scroll.value
                    area.performMouseInput { scroll(direction) }
                    mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis * 2)
                    waitForIdle()
                    withClue("wheel $direction, step $step") { (scroll.value != before) shouldBe true }
                }
            }
        }
}
