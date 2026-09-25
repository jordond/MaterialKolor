package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val EXPRESSIVE_MESSAGE = "Expressive themes usually use the Expressive style on the 2025 spec"
private const val UNDO_EXPRESSIVE = "Undo library change to Expressive"
private const val REDO_EXPRESSIVE = "Redo library change to Expressive"

/**
 * A desktop window wide enough for the segmented switcher in every skin, beside the full actions.
 */
private const val WIDTH = 1600 // b-231
private const val HEIGHT = 800

/**
 * The frames a reveal may take from the click to the change. The host records the old frame in the
 * first and the change lands at the start of the next, with one to spare. The 100 ms capture
 * timeout is six frames, so a change that skipped the reveal would miss this.
 */
private const val REVEAL_FRAMES = 3

@OptIn(ExperimentalTestApi::class)
class TopBarContentTest {
    @Test
    fun undoButton_afterAStyleChange_namesTheChangeInItsTooltipWithoutTheRawStyleName() =
        runComposeUiTest {
            val history = HistoryState(canUndo = true, undoLabel = ChangeLabel(ChangeKind.Style, detail = "Vibrant"))
            val document = ThemeDocument.Default.copy(library = Library.Custom)
            setContent {
                val dispatcher = rememberDispatcher<WorkspaceAction> {}
                BuilderTheme(
                    skin = Skin(library = Library.Custom, expressive = false),
                    result = ThemeResolver().resolve(document),
                    isDark = false,
                    reducedMotion = false,
                ) {
                    ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                        TopBarContent(state = workspaceState(document, history), dispatcher = dispatcher)
                    }
                }
            }

            onNodeWithContentDescription("Undo style change").requestFocus()
            waitForIdle()

            onNodeWithText("Undo style change").assertExists()
        }

    @Test
    fun librarySwitcher_pickingExpressive_landsThroughTheRevealAsOneUndoEntry() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            mainClock.autoAdvance = false

            onNodeWithText("Expressive").performClick()
            mainClock.advanceTimeBy(0)
            // A plain edit would have landed inside the click. The reveal holds it until it has drawn the old frame.
            graph.session.document.value.expressive shouldBe false
            var frames = 0
            while (!graph.session.document.value.expressive && frames < REVEAL_FRAMES) {
                mainClock.advanceTimeByFrame()
                frames++
            }
            graph.session.document.value.expressive shouldBe true

            mainClock.autoAdvance = true
            onNodeWithText("Keep mine").performClick()
            waitForIdle()
            graph.session.history.value.undoLabel shouldBe ChangeLabel(ChangeKind.Library, detail = "Material3")
            onNodeWithContentDescription(UNDO_EXPRESSIVE).performClick()
            waitForIdle()

            graph.session.document.value.expressive shouldBe false
            graph.session.history.value.canUndo shouldBe false
            onNodeWithText("M3").assertIsSelected()
            onNodeWithContentDescription(REDO_EXPRESSIVE).assertExists()
        }

    @Test
    fun expressiveSuggestion_afterASwitchFromThe2021Spec_changesNothingWithoutAClick() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()

            onNodeWithText("Expressive").performClick()
            waitForIdle()
            onNodeWithText(EXPRESSIVE_MESSAGE).assertExists()
            mainClock.advanceTimeBy(10_000)

            onNodeWithText(EXPRESSIVE_MESSAGE).assertExists()
            graph.session.document.value.style shouldBe Style.TonalSpot
            graph.session.document.value.spec shouldBe SpecVersion.Spec2021
            onNodeWithText("Keep mine").performClick()
            waitForIdle()

            onNodeWithText(EXPRESSIVE_MESSAGE).assertDoesNotExist()
            graph.session.document.value.style shouldBe Style.TonalSpot
            graph.session.history.value.undoLabel shouldBe ChangeLabel(ChangeKind.Library, detail = "Material3")
        }

    @Test
    fun expressiveSuggestion_afterASwitchFromFluent_showsOnlyOnceTheMaterialSkinIsIn() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            runOnIdle {
                val fluent = graph.session.document.value.copy(
                    library = Library.Fluent,
                    expressive = false,
                    style = Style.Rainbow,
                    spec = SpecVersion.Spec2025,
                )
                graph.session.edit(DocumentChange.Replace(fluent), EditPhase.Discrete)
            }
            waitForIdle()
            mainClock.autoAdvance = false

            onNodeWithText("Expressive").performClick()
            mainClock.advanceTimeBy(0)
            var frames = 0
            while (!graph.session.document.value.expressive && frames < REVEAL_FRAMES) {
                // Still Fluent, so the suggestion must not have opened in the skin being left.
                onAllNodesWithText(EXPRESSIVE_MESSAGE).fetchSemanticsNodes().isEmpty() shouldBe true
                mainClock.advanceTimeByFrame()
                frames++
            }
            skinOf(graph.session.document.value) shouldBe Skin(library = Library.Material3, expressive = true)
            mainClock.autoAdvance = true
            waitForIdle()

            onNodeWithText(EXPRESSIVE_MESSAGE).assertExists()
            skinOf(graph.session.document.value) shouldBe Skin(library = Library.Material3, expressive = true)
            graph.session.document.value.style shouldBe Style.Rainbow
        }

    @Test
    fun expressiveSuggestion_apply_movesStyleAndSpecAsOneUndoEntry() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()

            onNodeWithText("Expressive").performClick()
            waitForIdle()
            onNodeWithText("Apply").performClick()
            waitForIdle()

            onNodeWithText(EXPRESSIVE_MESSAGE).assertDoesNotExist()
            graph.session.document.value.style shouldBe Style.Expressive
            graph.session.document.value.spec shouldBe SpecVersion.Spec2025
            onNodeWithContentDescription("Undo theme change").performClick()
            waitForIdle()
            graph.session.document.value.style shouldBe Style.TonalSpot
            graph.session.document.value.spec shouldBe SpecVersion.Spec2021
            graph.session.document.value.expressive shouldBe true
        }

    @Test
    fun topBar_focusedButtonAcrossASwitchToFluent_keepsFocus() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            // A button without a tooltip, since a tooltip popup open across the switch trips the kit.
            onNodeWithText("Export code").requestFocus()
            waitForIdle()
            onNodeWithText("Export code").assertIsFocused()

            onNodeWithText("Fluent").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            graph.session.document.value.library shouldBe Library.Fluent
            onNodeWithText("Export code").assertIsFocused()
        }

    // b-221f
    @Test
    fun appearanceRows_inTheMoreMenu_onlyTheChosenOneIsSelected() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val document = ThemeDocument.Default
            val state = workspaceState(document, HistoryState())
                .copy(preferences = Preferences(appearance = Appearance.Dark))
            setContent {
                val dispatcher = rememberDispatcher<WorkspaceAction> {}
                BuilderTheme(
                    skin = skinOf(document),
                    result = ThemeResolver().resolve(document),
                    isDark = false,
                    reducedMotion = true,
                ) {
                    ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                        TopBarContent(state = state, dispatcher = dispatcher)
                    }
                }
            }

            onNodeWithContentDescription("More options").performClick()
            waitForIdle()

            onNodeWithText("Use the dark appearance").assertIsSelected()
            onNodeWithText("Use the light appearance").assertIsNotSelected()
            onNodeWithText("Use the system appearance").assertIsNotSelected()
        }

    /**
     * The whole builder on fakes, booted, with its graph so a test can read the session.
     */
    private fun ComposeUiTest.showRoot(): AppGraph {
        val platform = FakePlatform()
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                BuilderRoot(graph)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    private fun workspaceState(
        document: ThemeDocument,
        history: HistoryState,
    ): WorkspaceModel.State =
        WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = history,
            view = ProjectViewState(),
            preferences = Preferences(),
        )

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
