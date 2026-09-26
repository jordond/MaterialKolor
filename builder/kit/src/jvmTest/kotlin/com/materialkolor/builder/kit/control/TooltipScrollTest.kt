package com.materialkolor.builder.kit.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.LocalOverlayHost
import com.materialkolor.builder.kit.headless.OverlayHostState
import com.materialkolor.builder.kit.headless.OverlayKind
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val AreaTag = "area"
private const val AnchorTag = "anchor"
private const val Label = "Copy the hex"

@OptIn(ExperimentalTestApi::class)
class TooltipScrollTest {
    @Test
    fun wheel_pointerRestingOverTooltipAnchors_everyStepScrollsTheArea() =
        forEachSkin { _, skin ->
            val scroll = ScrollState(initial = 600)
            setContent {
                HostOverlays(skin, inTree = true) {
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

    @Test
    fun wheel_overAShownLabel_scrollsThePageUnderItAndHidesTheLabel() =
        forEachSkin { _, skin ->
            lateinit var host: OverlayHostState
            val scroll = ScrollState(initial = 0)
            setContent {
                HostOverlays(skin, inTree = true) {
                    host = checkNotNull(LocalOverlayHost.current)
                    BuilderScrollArea(Modifier.testTag(AreaTag).size(240.dp, 320.dp), state = scroll) {
                        Spacer(Modifier.height(160.dp))
                        BuilderTooltip(Label) { Box(Modifier.testTag(AnchorTag).size(40.dp)) }
                        Spacer(Modifier.height(800.dp))
                    }
                }
            }
            val anchor = onNodeWithTag(AnchorTag, useUnmergedTree = true).getBoundsInRoot()
            val onLabel = with(density) { Offset(anchor.left.toPx() + 20.dp.toPx(), anchor.top.toPx() - 24.dp.toPx()) }
            onNodeWithTag(AnchorTag, useUnmergedTree = true).performMouseInput { moveTo(center) }
            mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis * 2)
            waitForIdle()
            onRoot().performMouseInput { moveTo(onLabel) }
            mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis * 2)
            waitForIdle()
            withClue("the label stays while the pointer is on it") { host.labelShown() shouldBe true }

            onRoot().performMouseInput { scroll(1f) }
            waitForIdle()

            (scroll.value > 0) shouldBe true
            host.labelShown() shouldBe false
        }

    @Test
    fun hover_showsTheLabelOnlyAfterARest() {
        for (inTree in listOf(false, true)) {
            withClue(if (inTree) "in tree" else "in windows") {
                runComposeUiTest {
                    var host: OverlayHostState? = null
                    setContent {
                        HostOverlays(Skin(SkinLibrary.Custom, expressive = false), inTree) {
                            host = LocalOverlayHost.current
                            Column {
                                Spacer(Modifier.height(80.dp))
                                BuilderTooltip(Label) { Box(Modifier.testTag(AnchorTag).size(40.dp)) }
                            }
                        }
                    }
                    waitForIdle()
                    mainClock.autoAdvance = false
                    onNodeWithTag(AnchorTag, useUnmergedTree = true).performMouseInput { moveTo(center) }
                    mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis / 2)
                    labelShown(host) shouldBe false
                    mainClock.advanceTimeBy(OverlayMetrics.tooltipDelayMillis)
                    labelShown(host) shouldBe true
                }
            }
        }
    }
}

private fun OverlayHostState.labelShown(): Boolean =
    layers.any { layer ->
        layer.kind == OverlayKind.Passive &&
            layer.open
    }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.labelShown(host: OverlayHostState?): Boolean =
    host?.labelShown() ?: onAllNodesWithText(Label).fetchSemanticsNodes().isNotEmpty()
