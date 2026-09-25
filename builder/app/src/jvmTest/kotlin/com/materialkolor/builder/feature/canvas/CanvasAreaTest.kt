package com.materialkolor.builder.feature.canvas

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A Medium window, where a Desktop screen shrinks past 0.6 and a Tablet one does not.
 */
private const val MEDIUM = 700

@OptIn(ExperimentalTestApi::class)
class CanvasAreaTest {
    @Test
    fun tabs_eachPicked_composeOnlyTheVisibleTab() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            val composed = mutableListOf<PreviewTab>()
            setContent { Canvas(host, probe = { tab -> composed += tab }) }
            waitForIdle()
            composed.distinct() shouldContainExactly listOf(PreviewTab.App)

            for (tab in listOf(PreviewTab.Components, PreviewTab.Roles, PreviewTab.App)) {
                composed.clear()
                onNodeWithText(tab.name).performClick()
                waitForIdle()

                host.state.view.tab shouldBe tab
                composed.distinct() shouldContainExactly listOf(tab)
            }
            onNode(SplitHandle).assertExists()
        }

    @Test
    fun tabs_arrowKeys_moveAlongTheCanvasTabs() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            val composed = mutableListOf<PreviewTab>()
            setContent { Canvas(host, probe = { tab -> composed += tab }) }
            waitForIdle()

            onNodeWithText("App").requestFocus()
            onNodeWithText("App").performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            host.state.view.tab shouldBe PreviewTab.Components

            composed.clear()
            onNodeWithText("Components").performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            host.state.view.tab shouldBe PreviewTab.Roles
            composed.distinct() shouldContainExactly listOf(PreviewTab.Roles)

            onNodeWithText("Roles").performKeyInput { pressKey(Key.DirectionLeft) }
            onNodeWithText("Components").performKeyInput { pressKey(Key.DirectionLeft) }
            waitForIdle()
            host.state.view.tab shouldBe PreviewTab.App
        }

    @Test
    fun appState_isSharedByTheLightAndDarkCopiesAndOutlivesATabSwitch() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertDoesNotExist()

            // The light copy of the split, the one assistive tech reads.
            onNodeWithText("Past").performClick()
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertExists()

            // Dark shows the dark copy alone, and it has the pick too.
            onNodeWithText("Dark").performClick()
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertExists()

            onNodeWithText("Components").performClick()
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertDoesNotExist()
            onNodeWithText("App").performClick()
            waitForIdle()
            onNodeWithText(NO_PAST_TRIPS).assertExists()
        }

    @Test
    fun deviceScreen_tooWideForTheCanvas_scalesDownTo60PercentAndThenScrolls() =
        runDesktopComposeUiTest(width = MEDIUM, height = HEIGHT) {
            val host = CanvasHost(view = ProjectViewState(mode = PreviewMode.Light, deviceWidth = DeviceWidth.Desktop))
            setContent { Canvas(host) }
            waitForIdle()

            val desktop = onNodeWithTag(DEVICE_SCREEN_TAG).fetchSemanticsNode()
            val range = desktop.config[SemanticsProperties.HorizontalScrollAxisRange]
            val shown = with(density) { (1280.dp * MIN_SCREEN_SCALE).toPx() }
            range.maxValue() shouldBeGreaterThan 0f
            (desktop.size.width + range.maxValue()) shouldBe (shown plusOrMinus 2f)

            host.state = host.state.copy(view = host.state.view.copy(deviceWidth = DeviceWidth.Tablet))
            waitForIdle()
            val tablet = onNodeWithTag(DEVICE_SCREEN_TAG).fetchSemanticsNode()
            tablet.config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() shouldBe 0f
            tablet.size.width shouldBeGreaterThan with(density) { (840.dp * MIN_SCREEN_SCALE).roundToPx() }
        }

    @Test
    fun deviceWidth_onAWideWindow_isOfferedAndPicked() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            onNodeWithContentDescription("Device width, Tablet").performClick()
            waitForIdle()
            onNodeWithText("Desktop").performClick()
            waitForIdle()

            host.actions.last() shouldBe WorkspaceAction.SetDeviceWidth(DeviceWidth.Desktop)
            onNodeWithContentDescription("Device width, Desktop").assertExists()
        }

    @Test
    fun deviceWidth_atCompact_isHidden() =
        runDesktopComposeUiTest(width = PHONE, height = HEIGHT) {
            setContent { Canvas(CanvasHost()) }
            waitForIdle()

            onNodeWithContentDescription("Device width, Tablet").assertDoesNotExist()
            onNode(SplitHandle).assertExists()
        }

    @Test
    fun vision_picked_isDispatchedAndLabelledOnTheDock() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()
            onNodeWithTag(VISION_LABEL_TAG, useUnmergedTree = true).assertDoesNotExist()

            onNodeWithContentDescription("Color vision, None").performClick()
            waitForIdle()
            onNodeWithText("Protanopia").performClick()
            waitForIdle()

            host.actions.last() shouldBe WorkspaceAction.SetVision(VisionSimulation.Protanopia)
            onNodeWithContentDescription("Color vision, Protanopia").assertExists()
            onNodeWithTag(VISION_LABEL_TAG, useUnmergedTree = true).assertExists()
        }

    @Test
    fun inspect_toggled_dispatchesSetInspect() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            onNodeWithText("Inspect").performClick()
            waitForIdle()

            host.actions.last() shouldBe WorkspaceAction.SetInspect(on = true)
            host.state.inspect shouldBe true
        }
}

/**
 * What the Trips app says once the Past filter is on, there being no past trips.
 */
private const val NO_PAST_TRIPS = "No past trips yet"
