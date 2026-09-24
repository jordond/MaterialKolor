package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The whole builder on fakes, booted, with the command registry of every composition in [commands]. */
@OptIn(ExperimentalTestApi::class)
internal class CommandHarness(
    val platform: FakePlatform = FakePlatform(),
) {
    lateinit var workspace: WorkspaceModel
    lateinit var graph: AppGraph
    var commands: List<Command> = emptyList()

    /** The command [id], as the registry has it now. */
    fun command(id: String): Command = commands.first { command -> command.id == id }

    fun ComposeUiTest.show(probe: @Composable (state: WorkspaceModel.State) -> Unit = {}) {
        graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                workspace = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace) { state ->
                    commands = actionRegistry(state, rememberDispatcher { })
                    probe(state)
                }
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
    }
}

/** Presses keys on whatever holds focus, the way a keyboard does. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.keys(block: androidx.compose.ui.test.KeyInjectionScope.() -> Unit) {
    onAllNodes(isRoot()).onFirst().performKeyInput(block)
    waitForIdle()
}

/** Whether some node, in any window, reads [name] as its text or content description. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.named(name: String): Boolean =
    onAllNodes(hasText(name) or hasContentDescription(name), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
