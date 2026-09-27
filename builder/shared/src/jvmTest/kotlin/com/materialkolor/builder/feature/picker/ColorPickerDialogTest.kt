package com.materialkolor.builder.feature.picker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {
    private val platform = FakePlatform()

    @Test
    fun seedPick_opensThePickerTitledByItsTarget() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            onNodeWithText("Pick").performClick()
            waitForIdle()

            onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Seed color")).assertExists()
            onNodeWithText("Done").assertExists()
        }

    @Test
    fun eyedropper_whereThereIsNone_isHidden() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            onNodeWithText("Pick").performClick()
            waitForIdle()

            onNodeWithContentDescription("Pick from screen").assertDoesNotExist()
        }

    @Test
    fun eyedropper_pickThenDone_landsTheSeedAsOneEntry() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.eyeDropperAvailable = true
            platform.environment.screenColor = Screen
            val graph = showRoot()

            onNodeWithText("Pick").performClick()
            waitForIdle()
            onNodeWithContentDescription("Pick from screen").performClick()
            waitForIdle()
            onNodeWithText("Done").performClick()
            waitForIdle()

            graph.session.document.value.seed shouldBe Screen
            graph.session.document.value.seedSource shouldBe SeedSource.Eyedropper
            onNodeWithText("Done").assertDoesNotExist()
        }

    @Test
    fun eyedropper_cancelled_changesNothingAndKeepsThePickerOpen() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.eyeDropperAvailable = true
            val graph = showRoot()
            val before = graph.session.document.value

            onNodeWithText("Pick").performClick()
            waitForIdle()
            onNodeWithContentDescription("Pick from screen").performClick()
            waitForIdle()

            platform.environment.screenPicks shouldBe 1
            graph.session.document.value shouldBe before
            onNodeWithText("Done").assertExists()
        }

    @Test
    fun eyedropper_startsBeforeTheClickReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.eyeDropperAvailable = true
            platform.environment.screenColor = Screen
            showRoot()
            onNodeWithText("Pick").performClick()
            waitForIdle()
            // No frame and no task runs after the click, so only a pick started inside it counts.
            mainClock.autoAdvance = false

            onNodeWithContentDescription("Pick from screen").performClick()

            platform.environment.screenPicks shouldBe 1
        }

    @Test
    fun was_afterAPick_putsTheSeedBackAndKeepsThePickerOpen() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.eyeDropperAvailable = true
            platform.environment.screenColor = Screen
            val graph = showRoot()
            val before = graph.session.document.value
            val history = graph.session.history.value

            onNodeWithText("Pick").performClick()
            waitForIdle()
            onNodeWithContentDescription("Pick from screen").performClick()
            waitForIdle()
            graph.session.document.value.seed shouldBe Screen
            onNodeWithContentDescription("Go back to ${before.seed.toHex()}").performClick()
            waitForIdle()

            graph.session.document.value shouldBe before
            graph.session.history.value shouldBe history
            onNodeWithText("Done").assertExists()
            onNodeWithText("Done").performClick()
            waitForIdle()
            graph.session.document.value shouldBe before
            graph.session.history.value shouldBe history
        }

    @Test
    fun escape_afterAPick_restoresTheSeedAndHandsFocusBackToPick() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            platform.environment.eyeDropperAvailable = true
            platform.environment.screenColor = Screen
            val graph = showRoot()
            val before = graph.session.document.value
            val history = graph.session.history.value

            onNodeWithText("Pick").performClick()
            waitForIdle()
            onNodeWithContentDescription("Pick from screen").performClick()
            waitForIdle()
            graph.session.document.value.seed shouldBe Screen
            onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            graph.session.document.value shouldBe before
            graph.session.history.value shouldBe history
            onNodeWithText("Pick").assertIsFocused()
        }

    private fun ComposeUiTest.showRoot(): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                val workspace: WorkspaceModel = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    private companion object {
        val Screen = Argb(0xFF1A73E8.toInt())
    }
}
