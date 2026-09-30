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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.WorkspaceScreen
import com.materialkolor.builder.feature.workspace.withView
import com.materialkolor.builder.feature.workspace.workspaceStateOf
import com.materialkolor.builder.kit.control.rememberBuilderToastHostState
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.Dispatcher

internal const val PHONE = 390

/**
 * The split handle, a slider named Split. The Trips app has a progress bar of its own.
 */
internal val SplitHandle: SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and hasContentDescription("Split")

/**
 * Where the split handle sits, from its slider semantics.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.handleFraction(): Float =
    onNode(SplitHandle).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

/**
 * A workspace state that the canvas's own actions update, standing in for the model, with every
 * action it was sent.
 */
internal class CanvasHost(
    view: ProjectViewState = ProjectViewState(),
) {
    var state: WorkspaceModel.State by mutableStateOf(workspaceState(view))
    val actions = mutableListOf<WorkspaceAction>()
    val dispatcher = Dispatcher<WorkspaceAction> { action ->
        actions += action
        state = reduce(state, action)
    }

    /**
     * The fractions the canvas sent to be saved, oldest first.
     */
    val savedFractions: List<Float>
        get() = actions.filterIsInstance<WorkspaceAction.SetSplitFraction>().map { action -> action.fraction }

    private fun reduce(
        state: WorkspaceModel.State,
        action: WorkspaceAction,
    ): WorkspaceModel.State =
        when (action) {
            is WorkspaceAction.SetPreviewTab -> state.withView(state.view.copy(tab = action.tab))
            is WorkspaceAction.SetPreviewMode -> state.withView(state.view.copy(mode = action.mode))
            is WorkspaceAction.SetSplitFraction -> state.withView(state.view.copy(splitFraction = action.fraction))
            is WorkspaceAction.SetDeviceWidth -> state.withView(state.view.copy(deviceWidth = action.width))
            is WorkspaceAction.SetVision -> state.copy(vision = action.vision)
            is WorkspaceAction.SetInspect -> state.copy(inspect = action.on)
            WorkspaceAction.ToggleFullscreen -> state.copy(fullscreen = !state.fullscreen)
            is WorkspaceAction.SetVisionMenuOpen -> state.copy(visionMenuOpen = action.open)
            else -> state
        }
}

private fun workspaceState(view: ProjectViewState): WorkspaceModel.State {
    val document = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)
    return workspaceStateOf(document = document, view = view)
}

/**
 * The canvas and its dock over the host's state, the way the shell lays them out.
 */
@Composable
internal fun Canvas(
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

/**
 * The whole workspace over the host's state. Its overlays make their own view models, so this
 * gives them [app]'s factory.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showWorkspace(
    host: CanvasHost,
    app: AppHarness,
) {
    with(app) {
        createGraph()
        setContent {
            Provide {
                Themed(host, frozen = true, probe = null) {
                    WorkspaceScreen(
                        state = host.state,
                        posterColors = LocalThemeResult.current.poster,
                        toasts = rememberBuilderToastHostState(),
                        dispatcher = host.dispatcher,
                    )
                }
            }
        }
    }
}

/**
 * The Material skin and the resolved document, laid out for the window.
 */
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
            expressive = false,
            result = result,
            isDark = false,
            reducedMotion = false,
        ) {
            ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
