package com.materialkolor.builder.feature.workspace

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The rail's button, which opens the poster.
 */
private const val OPEN = "Open the poster"

/**
 * The open poster's button, which folds it back to the rail.
 */
private const val COLLAPSE = "Collapse the poster"

/**
 * Below 840 dp at Medium the poster starts as the rail every session and opens over the canvas when
 * asked. The stored collapse only rules the docked poster.
 */
@OptIn(ExperimentalTestApi::class)
class PosterRailSessionTest {
    private val app = AppHarness()

    @AfterTest
    fun tearDown() {
        app.close()
    }

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
            waitUntil(timeoutMillis = WAIT_MILLIS) { workspace.state.value.preferences.posterCollapsed }
            waitForIdle()

            shows(OPEN) shouldBe true
            workspace.state.value.posterOverCanvas shouldBe false
        }
        runDesktopComposeUiTest(width = 900, height = HEIGHT) {
            showRoot(stores)
            waitUntil(timeoutMillis = WAIT_MILLIS) { workspace.state.value.preferences.posterCollapsed }
            waitForIdle()

            shows(OPEN) shouldBe true
            shows(COLLAPSE) shouldBe false
        }
    }

    private fun button(name: String): SemanticsMatcher =
        (hasText(name) or hasContentDescription(name)) and hasClickAction()

    private fun ComposeUiTest.shows(name: String): Boolean = onAllNodes(button(name)).fetchSemanticsNodes().isNotEmpty()

    /**
     * The whole builder on fakes kept in [stores], booted, one session of it.
     */
    private fun ComposeUiTest.showRoot(stores: InMemoryStoreFactory): AppGraph {
        val platform = FakePlatform(stores = stores)
        val graph = with(app) {
            bootRoot(platform = platform) { graph ->
                workspace = metroViewModel()
                BuilderRoot(graph, workspaceModel = workspace)
            }
        }
        return graph
    }
}
