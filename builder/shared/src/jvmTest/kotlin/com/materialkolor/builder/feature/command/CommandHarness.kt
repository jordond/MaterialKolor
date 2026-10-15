package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.DEVICE_SCREEN_TAG
import com.materialkolor.builder.feature.history.LocalSwatchReadProbe
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideOverlaysInTreeForTest
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * The whole builder on fakes, booted, with the command registry of every composition in [commands].
 * Each test class that uses one calls [close] from its `@AfterTest`.
 */
@OptIn(ExperimentalTestApi::class, KitTestApi::class)
internal class CommandHarness(
    platform: FakePlatform = FakePlatform(),
) : AppHarness(platform) {
    lateinit var workspace: WorkspaceModel
    var commands: List<Command> = emptyList()

    /**
     * The command [id], as the registry has it now.
     */
    fun command(id: String): Command = commands.first { command -> command.id == id }

    /**
     * Boots the builder. [onTextInput] hears each text input session a field starts, after the
     * shortcuts have counted it, so a test can drive the session as an input method would.
     * [registryBuilds] hears each build of the builder's own registries, the page's and the
     * palette's, and not the one this harness keeps in [commands]. With [inTree] its overlays draw
     * in the page, the way the web draws them, so Esc and the focus hand back work as they do there.
     * [swatchReads] hears each time a History swatch reads its scheme.
     */
    fun ComposeUiTest.show(
        onTextInput: (PlatformTextInputMethodRequest) -> Unit = {},
        probe: @Composable (state: WorkspaceModel.State) -> Unit = {},
        registryBuilds: (() -> Unit)? = null,
        inTree: Boolean = false,
        swatchReads: (() -> Unit)? = null,
    ) {
        val watcher = PlatformTextInputInterceptor { request, nextHandler ->
            onTextInput(request)
            nextHandler.startInputMethod(request)
        }
        bootRoot { graph ->
            CompositionLocalProvider(
                LocalRegistryBuilds provides registryBuilds,
                LocalSwatchReadProbe provides swatchReads,
            ) {
                workspace = metroViewModel()
                InterceptPlatformTextInput(watcher) {
                    val root = @Composable {
                        BuilderRoot(graph, workspaceModel = workspace) { state ->
                            CompositionLocalProvider(LocalRegistryBuilds provides null) {
                                commands = actionRegistry(state, rememberDispatcher { })
                            }
                            probe(state)
                        }
                    }
                    if (inTree) ProvideOverlaysInTreeForTest(root) else root()
                }
            }
        }
    }
}

/**
 * Presses keys on whatever holds focus, the way a keyboard does.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.keys(block: androidx.compose.ui.test.KeyInjectionScope.() -> Unit) {
    onAllNodes(isRoot()).onFirst().performKeyInput(block)
    waitForIdle()
}

/**
 * Whether some workspace node, in any window, reads [name] as its text or content description. The
 * sample app in the preview does not count.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.named(name: String): Boolean =
    onAllNodes((hasText(name) or hasContentDescription(name)) and InWorkspace, useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

/**
 * Anything outside the sample app in the preview, which has controls and words of its own, a "More
 * options" and a "Saved" among them.
 */
internal val InWorkspace: SemanticsMatcher = !hasAnyAncestor(hasTestTag(DEVICE_SCREEN_TAG))
