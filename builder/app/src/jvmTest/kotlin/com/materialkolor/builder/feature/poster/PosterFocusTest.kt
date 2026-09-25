package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * A seed whose primary comes out calmer, so the poster shows the explainer line and its Why.
 */
private val CalmerSeed = Argb(0xE53935)

private const val EXPLAINER_TITLE = "Why primary differs from your seed"

private const val SHARE_TITLE = "Share this theme"

/**
 * The panels the poster opens hand focus back to the button that opened them.
 */
@OptIn(ExperimentalTestApi::class)
class PosterFocusTest {
    private val platform = FakePlatform()
    private lateinit var workspace: WorkspaceModel

    @Test
    fun explainer_openedOverACollapsedPoster_shows() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            collapsePoster()
            onNodeWithText("Why?").assertDoesNotExist()

            runOnUiThread { workspace.openPanel(Panel.Explainer) }
            waitForIdle()

            onNodeWithText(EXPLAINER_TITLE).assertExists()
        }

    @Test
    fun explainer_close_handsFocusBackToWhy() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            runOnUiThread {
                workspace.edit(DocumentChange.SetSeed(CalmerSeed, SeedSource.Typed), EditPhase.Discrete)
            }
            waitForIdle()

            onNodeWithText("Why?").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText(EXPLAINER_TITLE).assertExists()
            onNodeWithText("Close").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            onNodeWithText(EXPLAINER_TITLE).assertDoesNotExist()
            onNodeWithText("Why?").assertIsFocused()
        }

    @Test
    fun projectsDrawer_openedFromTheHeader_escHandsFocusBackToIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            waitUntil { projectName().isNotBlank() }
            val projects = onNodeWithContentDescription("Projects, ${projectName()}")

            projects.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            escapeFromTheOverlay()

            projects.assertIsFocused()
        }

    @Test
    fun projectsDrawer_openedFromTheRail_escHandsFocusBackToIt() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            collapsePoster()
            val projects = onNodeWithContentDescription("Projects")

            projects.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            escapeFromTheOverlay()

            projects.assertIsFocused()
        }

    @Test
    fun manualCopy_fromThePoster_doneHandsFocusBackToTheCopyButton() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.clipboard.failure = IllegalStateException("No user activation")
            showRoot()

            onNodeWithContentDescription("Copy Kotlin").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText("Copy it yourself").assertExists()
            onNodeWithText("Done").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            onNodeWithText("Copy it yourself").assertDoesNotExist()
            onNodeWithContentDescription("Copy Kotlin").assertIsFocused()
        }

    @Test
    fun manualCopy_posterCollapsedWhileOpen_doneStillCloses() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.clipboard.failure = IllegalStateException("No user activation")
            showRoot()

            onNodeWithContentDescription("Copy Kotlin").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            collapsePoster()
            onNodeWithContentDescription("Copy Kotlin").assertDoesNotExist()
            onNodeWithText("Done").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            onNodeWithText("Copy it yourself").assertDoesNotExist()
        }

    @Test
    fun share_openedFromTheDrawersGetALink_handsFocusBackToProjects() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.storageAvailable = false
            showRoot()
            waitUntil { projectName().isNotBlank() }
            val projects = onNodeWithContentDescription("Projects, ${projectName()}")

            projects.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText("Get a link").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText(SHARE_TITLE).assertExists()
            escapeFromTheOverlay()

            onNodeWithText(SHARE_TITLE).assertDoesNotExist()
            projects.assertIsFocused()
        }

    @Test
    fun share_openedFromTheTopBarAfterTheDrawer_handsFocusBackToTheTopBar() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.storageAvailable = false
            showRoot()
            waitUntil { projectName().isNotBlank() }
            onNodeWithContentDescription("Projects, ${projectName()}").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText("Get a link").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            escapeFromTheOverlay()
            val share = onNodeWithContentDescription("Share")

            share.performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNodeWithText(SHARE_TITLE).assertExists()
            escapeFromTheOverlay()

            share.assertIsFocused()
        }

    private fun projectName(): String = workspace.state.value.projectName

    private fun ComposeUiTest.collapsePoster() {
        runOnUiThread { workspace.setPosterCollapsed(true) }
        waitUntil { workspace.state.value.preferences.posterCollapsed }
        waitForIdle()
    }

    private fun ComposeUiTest.escapeFromTheOverlay() {
        onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
    }

    /**
     * The whole builder on fakes, booted.
     */
    private fun ComposeUiTest.showRoot(): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                workspace = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }
}
