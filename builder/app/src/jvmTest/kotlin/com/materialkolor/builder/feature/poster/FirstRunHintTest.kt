package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.topbar.TopBarContent
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * The first run hint, word for word.
 */
private const val HINT = "Paste a color, drop an image, or press Space to shuffle. Press ? for shortcuts."

/**
 * Well past the longest reveal a skin plays.
 */
private const val PULSE_DONE_MS = 1_000L

/**
 * Part way into the pulse, while the ring still shows.
 */
private const val PULSE_MIDWAY_MS = 150L

/**
 * Preferences a newer build wrote, which every write from this build is turned down over.
 */
private const val NEWER_PREFS = """{"schema":999,"data":{}}"""

@OptIn(ExperimentalTestApi::class)
class FirstRunHintTest {
    private val platform = FakePlatform()
    private lateinit var workspace: WorkspaceModel

    @Test
    fun firstVisit_afterBoot_landsOnTheDefaultThemeInM3WithTheAppTabInSplit() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()

            val document = graph.session.document.value
            document.seed shouldBe DEFAULT_SEED
            document.library shouldBe Library.Material3
            document.expressive shouldBe false
            workspace.state.value.view.tab shouldBe PreviewTab.App
            workspace.state.value.view.mode shouldBe PreviewMode.Split
        }

    @Test
    fun firstVisit_afterBoot_showsTheHintUntilItIsClosed() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            onNodeWithText(HINT).assertExists()

            onNodeWithContentDescription("Close the hint").performClick()
            waitUntil { FIRST_RUN_HINT in workspace.state.value.preferences.dismissedHints }
            waitForIdle()

            onNodeWithText(HINT).assertDoesNotExist()
        }

    @Test
    fun hintClosedOnAnEarlierVisit_neverShowsInAnyFrame() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.stores.seed(
                StorageKeys.PREFS,
                Preferences.Codec.encode(Preferences(dismissedHints = setOf(FIRST_RUN_HINT))),
            )
            val shown = mutableListOf<String>()
            var frames = 0

            showRoot { state ->
                frames++
                if (showsFirstRunHint(state.preferences, state.projectName, state.sessionDismissedHints)) {
                    shown += state.projectName
                }
            }

            frames shouldNotBe 0
            workspace.state.value.projectName shouldNotBe ""
            shown shouldBe emptyList()
            onNodeWithText(HINT).assertDoesNotExist()
        }

    @Test
    fun close_whenStorageTurnsTheWriteDown_keepsTheHintShutForTheSession() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.stores.seed(StorageKeys.PREFS, NEWER_PREFS)
            showRoot()
            onNodeWithText(HINT).assertExists()

            onNodeWithContentDescription("Close the hint").performClick()
            waitForIdle()

            onNodeWithText(HINT).assertDoesNotExist()
            workspace.state.value.sessionDismissedHints shouldContain FIRST_RUN_HINT
            workspace.state.value.preferences.dismissedHints shouldBe emptySet()
            platform.stores.textAt(StorageKeys.PREFS) shouldBe NEWER_PREFS
        }

    @Test
    fun hint_afterTheFirstExport_goesAway() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument.Default).apply { projectName = "Theme" }
            showSection(harness) { context, dispatcher -> FirstRunHint(context, dispatcher) }
            onNodeWithText(HINT).assertExists()

            harness.preferences = harness.preferences.copy(firstExportDone = true)
            waitForIdle()

            onNodeWithText(HINT).assertDoesNotExist()
        }

    @Test
    fun hint_beforeBootOpensAProject_staysHidden() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument.Default)
            showSection(harness) { context, dispatcher -> FirstRunHint(context, dispatcher) }

            onNodeWithText(HINT).assertDoesNotExist()
        }

    @Test
    fun closeFromTheKeyboard_handsFocusToTheNextPosterControl() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument.Default).apply { projectName = "Theme" }
            showSection(harness) { context, dispatcher ->
                FirstRunHint(context, dispatcher)
                BuilderButton(onClick = {}, label = "Next on the poster")
            }

            onNodeWithContentDescription("Close the hint").requestFocus()
            onNode(isFocused()).performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            onNodeWithText(HINT).assertDoesNotExist()
            onNodeWithText("Next on the poster").assertIsFocused()
            harness.actions shouldContain WorkspaceAction.DismissHint(FIRST_RUN_HINT)
        }

    @Test
    fun closeWithAClick_leavesFocusWhereItWas() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument.Default).apply { projectName = "Theme" }
            showSection(harness) { context, dispatcher ->
                BuilderButton(onClick = {}, label = "Before the hint")
                FirstRunHint(context, dispatcher)
                BuilderButton(onClick = {}, label = "Next on the poster")
            }
            onNodeWithText("Before the hint").requestFocus()
            waitForIdle()

            // A mouse click, as on the desktop and the web. A touch would put the test in touch mode,
            // which clears focus by itself.
            onNodeWithContentDescription("Close the hint").performMouseInput { click() }
            waitForIdle()

            onNodeWithText(HINT).assertDoesNotExist()
            onNodeWithText("Before the hint").assertIsFocused()
            onNodeWithText("Next on the poster").assertIsNotFocused()
            harness.actions shouldContain WorkspaceAction.DismissHint(FIRST_RUN_HINT)
        }

    @Test
    fun switcherPulse_whileTheHintShows_playsOnceThenPutsItselfAway() =
        runComposeUiTest {
            val holder = TopBarHolder(workspaceState(projectName = "Theme"))
            showTopBar(holder, reducedMotion = false)

            mainClock.advanceTimeBy(PULSE_DONE_MS)
            waitForIdle()
            mainClock.advanceTimeBy(PULSE_DONE_MS)
            waitForIdle()

            holder.pulseDismissals shouldBe 1
            (SWITCHER_PULSE_HINT in holder.state.preferences.dismissedHints) shouldBe true
        }

    @Test
    fun switcherPulse_beforeBootOpensAProject_waits() =
        runComposeUiTest {
            val holder = TopBarHolder(workspaceState(projectName = ""))
            showTopBar(holder, reducedMotion = false)

            mainClock.advanceTimeBy(PULSE_DONE_MS)
            waitForIdle()

            holder.pulseDismissals shouldBe 0
        }

    @Test
    fun pulseRing_underReducedMotion_drawsNothingAndIsDoneStraightAway() =
        runComposeUiTest {
            var active by mutableStateOf(false)
            var done = 0
            showPulse(reducedMotion = true) { Modifier.pulseRing(active) { done++ } }
            val before = onRoot().captureToImage()

            active = true
            waitForIdle()

            done shouldBe 1
            samePixels(before, onRoot().captureToImage()) shouldBe true
        }

    @Test
    fun pulseRing_withFullMotion_drawsARingPartWayThrough() =
        runComposeUiTest {
            var active by mutableStateOf(false)
            var done = 0
            showPulse(reducedMotion = false) { Modifier.pulseRing(active) { done++ } }
            val before = onRoot().captureToImage()
            mainClock.autoAdvance = false

            active = true
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(PULSE_MIDWAY_MS)

            samePixels(before, onRoot().captureToImage()) shouldBe false
            done shouldBe 0
            mainClock.advanceTimeBy(PULSE_DONE_MS)
            done shouldBe 1
        }

    /**
     * The whole builder on fakes, booted, with [probe] drawn over the workspace every frame.
     */
    private fun ComposeUiTest.showRoot(probe: @Composable (state: WorkspaceModel.State) -> Unit = {}): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                workspace = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace, probe = probe)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    private fun ComposeUiTest.showTopBar(
        holder: TopBarHolder,
        reducedMotion: Boolean,
    ) {
        val document = holder.state.document
        setContent {
            BuilderTheme(
                skin = Skin(library = Library.Material3, expressive = false),
                result = ThemeResolver().resolve(document),
                isDark = false,
                reducedMotion = reducedMotion,
            ) {
                ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                    TopBarContent(state = holder.state, dispatcher = holder.dispatcher)
                }
            }
        }
    }

    private fun ComposeUiTest.showPulse(
        reducedMotion: Boolean,
        pulse: @Composable () -> Modifier,
    ) {
        val document = ThemeDocument.Default
        setContent {
            BuilderTheme(
                skin = Skin(library = Library.Material3, expressive = false),
                result = ThemeResolver().resolve(document),
                isDark = false,
                reducedMotion = reducedMotion,
            ) {
                Box(Modifier.padding(24.dp).size(width = 120.dp, height = 40.dp).then(pulse()))
            }
        }
    }

    private fun samePixels(
        first: ImageBitmap,
        second: ImageBitmap,
    ): Boolean = first.toPixelMap().buffer.contentEquals(second.toPixelMap().buffer)

    private fun workspaceState(projectName: String): WorkspaceModel.State {
        val document = ThemeDocument.Default.copy(library = Library.Material3, expressive = false)
        return WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = HistoryState(),
            view = ProjectViewState(),
            preferences = Preferences(),
            projectName = projectName,
        )
    }

    /**
     * The top bar's state, which keeps the hints it is asked to dismiss the way the workspace does.
     */
    private class TopBarHolder(
        initial: WorkspaceModel.State,
    ) {
        var state by mutableStateOf(initial)
        var pulseDismissals = 0

        val dispatcher = Dispatcher<WorkspaceAction> { action ->
            if (action is WorkspaceAction.DismissHint) {
                if (action.id == SWITCHER_PULSE_HINT) pulseDismissals++
                val hints = state.preferences.dismissedHints + action.id
                state = state.copy(preferences = state.preferences.copy(dismissedHints = hints))
            }
        }
    }
}
