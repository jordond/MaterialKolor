package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.WorkspaceScreen
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.control.rememberBuilderToastHostState
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.Dispatcher
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val WIDE = 1280
private const val PHONE = 400
private const val HEIGHT = 800

/** The split handle, a slider named Split. The Trips app has a progress bar of its own. */
private val SplitHandle = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and
    hasContentDescription("Split")

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
    fun modeSwitch_underFrozenMotion_composesOneCopyAndComesBackToTheSavedHandle() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost(view = ProjectViewState(splitFraction = 0.3f))
            setContent { Canvas(host) }
            waitForIdle()
            onNode(SplitHandle).assertExists()

            onNodeWithText("Dark").performClick()
            waitForIdle()
            host.state.view.mode shouldBe PreviewMode.Dark
            onNode(SplitHandle).assertDoesNotExist()

            onNodeWithText("Split").performClick()
            waitForIdle()
            handleFraction() shouldBe (0.3f plusOrMinus 0.001f)
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
        }

    @Test
    fun modeSwitch_withMotion_slidesTheHandleToTheEdgeBeforeDroppingACopy() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host, frozen = false) }
            waitForIdle()
            mainClock.autoAdvance = false

            onNodeWithText("Dark").performClick()
            mainClock.advanceTimeBy(150)
            handleFraction() shouldBeGreaterThan 0f
            handleFraction() shouldBeLessThan 0.5f

            mainClock.advanceTimeBy(1_000)
            onNode(SplitHandle).assertDoesNotExist()
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
        }

    @Test
    fun splitHandle_moved_savesTheFractionOnceAndStaysThere() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            onNode(SplitHandle).performSemanticsAction(SemanticsActions.SetProgress) { move -> move(0.25f) }
            waitForIdle()

            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe
                listOf(WorkspaceAction.SetSplitFraction(0.25f))
            host.state.view.splitFraction shouldBe 0.25f
            handleFraction() shouldBe (0.25f plusOrMinus 0.001f)
        }

    @Test
    fun splitHandle_savedElsewhere_movesToTheSavedFraction() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent { Canvas(host) }
            waitForIdle()

            host.state = host.state.copy(view = host.state.view.copy(splitFraction = 0.8f))
            waitForIdle()

            handleFraction() shouldBe (0.8f plusOrMinus 0.001f)
            host.actions.filterIsInstance<WorkspaceAction.SetSplitFraction>() shouldBe emptyList()
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

    @Test
    fun fullscreen_fromTheDock_hidesTheTopBarUntilTheExitPillBringsItBack() =
        runDesktopComposeUiTest(width = WIDE, height = HEIGHT) {
            val host = CanvasHost()
            setContent {
                Themed(host, frozen = true, probe = null) {
                    WorkspaceScreen(
                        state = host.state,
                        posterColors = LocalThemeResult.current.poster,
                        toasts = rememberBuilderToastHostState(),
                        dispatcher = host.dispatcher,
                    )
                }
            }
            waitForIdle()
            onNodeWithText("Export code").assertExists()

            onNodeWithContentDescription("Fullscreen").performClick()
            waitForIdle()
            host.state.fullscreen shouldBe true
            onNodeWithText("Export code").assertDoesNotExist()
            onNodeWithContentDescription("Fullscreen").assertDoesNotExist()

            onNodeWithText("Exit fullscreen").performClick()
            waitForIdle()
            host.state.fullscreen shouldBe false
            onNodeWithText("Export code").assertExists()
        }

    /** Where the split handle sits, from its slider semantics. */
    private fun ComposeUiTest.handleFraction(): Float =
        onNode(SplitHandle).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
}

/**
 * A workspace state that the canvas's own actions update, standing in for the model, with every
 * action it was sent.
 */
private class CanvasHost(
    view: ProjectViewState = ProjectViewState(),
) {
    var state: WorkspaceModel.State by mutableStateOf(workspaceState(view))
    val actions = mutableListOf<WorkspaceAction>()
    val dispatcher = Dispatcher<WorkspaceAction> { action ->
        actions += action
        state = reduce(state, action)
    }

    private fun reduce(
        state: WorkspaceModel.State,
        action: WorkspaceAction,
    ): WorkspaceModel.State =
        when (action) {
            is WorkspaceAction.SetPreviewTab -> state.copy(view = state.view.copy(tab = action.tab))
            is WorkspaceAction.SetPreviewMode -> state.copy(view = state.view.copy(mode = action.mode))
            is WorkspaceAction.SetSplitFraction -> state.copy(view = state.view.copy(splitFraction = action.fraction))
            is WorkspaceAction.SetDeviceWidth -> state.copy(view = state.view.copy(deviceWidth = action.width))
            is WorkspaceAction.SetVision -> state.copy(vision = action.vision)
            is WorkspaceAction.SetInspect -> state.copy(inspect = action.on)
            WorkspaceAction.ToggleFullscreen -> state.copy(fullscreen = !state.fullscreen)
            else -> state
        }
}

private fun workspaceState(view: ProjectViewState): WorkspaceModel.State {
    val document = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)
    return WorkspaceModel.State(
        document = document,
        capabilities = capabilitiesOf(document),
        history = HistoryState(),
        view = view,
        preferences = Preferences(),
    )
}

/** The canvas and its dock over the host's state, the way the shell lays them out. */
@Composable
private fun Canvas(
    host: CanvasHost,
    frozen: Boolean = true,
    probe: ((PreviewTab) -> Unit)? = null,
) {
    Themed(host, frozen, probe) {
        Box(Modifier.fillMaxSize()) {
            CanvasArea(host.state, PaddingValues(), host.dispatcher)
            CanvasDock(host.state, host.dispatcher, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** The Material skin and the resolved document, laid out for the window. */
@Composable
private fun Themed(
    host: CanvasHost,
    frozen: Boolean,
    probe: ((PreviewTab) -> Unit)?,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(host.state.document) }
    CompositionLocalProvider(
        LocalMotionFrozen provides frozen,
        LocalThemeResult provides result,
        LocalCanvasProbe provides probe,
    ) {
        BuilderTheme(
            skin = Skin(Library.Material3, expressive = false),
            result = result,
            isDark = false,
            reducedMotion = false,
        ) {
            ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
