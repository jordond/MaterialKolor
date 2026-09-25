package com.materialkolor.builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.poster.kotlinLiteralOf
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

// b-221c
@OptIn(ExperimentalTestApi::class)
class BuilderRootTest {
    private val platform = FakePlatform()
    private lateinit var workspace: WorkspaceModel
    private lateinit var scope: CoroutineScope

    @Test
    fun announcer_underTheRoot_readsOutThroughTheEnvironment() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot { _ ->
                val announcer = LocalAnnouncer.current
                LaunchedEffect(announcer) { announcer.announce("Copied Theme.kt") }
            }

            platform.environment.announcements shouldContain "Copied Theme.kt"
        }

    @Test
    fun everyComposition_drawsTheWorkspaceDocumentInItsOwnColors() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val mismatches = mutableListOf<ThemeDocument>()
            var compositions = 0
            val graph = showRoot { state ->
                compositions++
                // The skin still lags one composition on a library switch, see TopBarContent.
                val result = LocalThemeResult.current
                if (result.document != state.document.forTarget(state.target)) {
                    mismatches += state.document
                }
            }
            val before = compositions

            runOnUiThread { workspace.edit(DocumentChange.SetThemeName("EditedTheme"), EditPhase.Discrete) }
            waitForIdle()
            runOnUiThread { workspace.undo() }
            waitForIdle()
            runOnUiThread {
                workspace.edit(
                    DocumentChange.SetLibrary(Library.Fluent, expressive = false),
                    EditPhase.Discrete,
                )
            }
            waitForIdle()
            runOnUiThread { scope.launch { graph.session.newProject(copyCurrent = false) } }
            waitForIdle()

            graph.session.document.value.library shouldBe ThemeDocument.Default.library
            compositions shouldNotBe before
            mismatches.shouldBeEmpty()
        }

    @Test
    fun librarySwitcher_arrowMovesFocusOnlyAndEnterSwitchesAsOneUndoEntry() =
        // b-231
        // Wide enough for the segmented switcher beside the top bar's full actions.
        runDesktopComposeUiTest(width = 1600, height = HEIGHT) {
            val graph = showRoot()
            val start = graph.session.document.value

            onNodeWithText("M3").requestFocus()
            onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            graph.session.document.value shouldBe start
            onNodeWithText("Expressive").assertIsFocused()

            onNode(isFocused()).performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            graph.session.document.value.expressive shouldBe true

            runOnUiThread { workspace.undo() }
            waitForIdle()
            graph.session.document.value shouldBe start
            graph.session.history.value.canUndo shouldBe false
        }

    @Test
    fun exportSheet_closedWithEsc_handsFocusBackToExportCode() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            onNodeWithText("Export code").performClick()
            waitForIdle()
            escapeFromTheOverlay()

            onNodeWithText("Export code").assertIsFocused()
        }

    @Test
    fun shareDialog_closedWithEsc_handsFocusBackToShare() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            onNodeWithContentDescription("Share").performClick()
            waitForIdle()
            escapeFromTheOverlay()

            onNodeWithContentDescription("Share").assertIsFocused()
        }

    // b-221ca
    @Test
    fun posterCopy_writesTheClipboardBeforeTheClickReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            // No frame and no task runs after the click, so only a write started inside it lands.
            mainClock.autoAdvance = false

            onNodeWithContentDescription("Copy Kotlin").performClick()

            platform.clipboard.texts shouldBe listOf(kotlinLiteralOf(graph.session.document.value.seed))
        }

    @Test
    fun posterCopy_whenTheClipboardRefuses_opensTheManualDialogAndNeverSaysCopied() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.clipboard.failure = IllegalStateException("No user activation")
            val graph = showRoot()

            onNodeWithContentDescription("Copy Kotlin").performClick()
            waitForIdle()

            onNodeWithText("Copy it yourself").assertExists()
            onNodeWithText(kotlinLiteralOf(graph.session.document.value.seed)).assertExists()
            onAllNodesWithText("Copied", substring = true).assertCountEquals(0)
            platform.environment.announcements.filter { message -> "Copied" in message } shouldBe emptyList()
            platform.clipboard.texts shouldBe emptyList()
        }

    private fun ComposeUiTest.escapeFromTheOverlay() {
        onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
    }

    /** The whole builder on fakes, booted, with [probe] drawn over the workspace. */
    private fun ComposeUiTest.showRoot(probe: @Composable (state: WorkspaceModel.State) -> Unit = {}): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                workspace = metroViewModel()
                scope = rememberCoroutineScope()
                BuilderRoot(graph, workspaceModel = workspace, probe = probe)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }
}
