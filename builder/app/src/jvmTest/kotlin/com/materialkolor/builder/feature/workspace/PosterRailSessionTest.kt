package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-406g

private const val HEIGHT = 800

/** The rail's button, which opens the poster. */
private const val OPEN = "Open the poster"

/** The open poster's button, which folds it back to the rail. */
private const val COLLAPSE = "Collapse the poster"

/**
 * Below 840 dp at Medium the poster starts as the rail every session and opens over the canvas when
 * asked (spec section 7). The stored collapse only rules the docked poster.
 */
@OptIn(ExperimentalTestApi::class)
class PosterRailSessionTest {
    private lateinit var workspace: WorkspaceModel

    @Test
    fun at720_startsAsTheRail_opensOverTheCanvas_andTheNextSessionStartsAsTheRailAgain() {
        val stores = InMemoryStoreFactory()
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            showRoot(stores)

            workspace.state.value.preferences.posterCollapsed shouldBe false
            shows(OPEN) shouldBe true
            shows(COLLAPSE) shouldBe false

            onAllNodes(button(OPEN)).onFirst().performClick()
            waitForIdle()

            shows(COLLAPSE) shouldBe true
            workspace.state.value.posterOverCanvas shouldBe true
            workspace.state.value.preferences.posterCollapsed shouldBe false
        }
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            showRoot(stores)

            shows(OPEN) shouldBe true
            shows(COLLAPSE) shouldBe false
        }
    }

    @Test
    fun at900_followsTheStoredChoiceAcrossSessions() {
        val stores = InMemoryStoreFactory()
        runDesktopComposeUiTest(width = 900, height = HEIGHT) {
            showRoot(stores)

            shows(COLLAPSE) shouldBe true
            onAllNodes(button(COLLAPSE)).onFirst().performClick()
            waitUntil { workspace.state.value.preferences.posterCollapsed }
            waitForIdle()

            shows(OPEN) shouldBe true
            workspace.state.value.posterOverCanvas shouldBe false
        }
        runDesktopComposeUiTest(width = 900, height = HEIGHT) {
            showRoot(stores)
            waitUntil { workspace.state.value.preferences.posterCollapsed }
            waitForIdle()

            shows(OPEN) shouldBe true
            shows(COLLAPSE) shouldBe false
        }
    }

    private fun button(name: String): SemanticsMatcher =
        (hasText(name) or hasContentDescription(name)) and hasClickAction()

    private fun ComposeUiTest.shows(name: String): Boolean = onAllNodes(button(name)).fetchSemanticsNodes().isNotEmpty()

    /** The whole builder on fakes kept in [stores], booted, one session of it. */
    private fun ComposeUiTest.showRoot(stores: InMemoryStoreFactory): AppGraph {
        val platform = FakePlatform(stores = stores)
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
