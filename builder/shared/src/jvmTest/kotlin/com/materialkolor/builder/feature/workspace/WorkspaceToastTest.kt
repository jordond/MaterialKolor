package com.materialkolor.builder.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.projects.ProjectsAction
import com.materialkolor.builder.feature.projects.ProjectsModel
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Past the kit's four second Short toast and well inside its ten second Long one, so a toast still
 * up by then was shown for Long.
 */
private const val PAST_SHORT_MILLIS = 6_000L

@OptIn(ExperimentalTestApi::class)
class WorkspaceToastTest {
    @Test
    fun showToast_withAnActionAndALongDuration_reachesTheToastsAndItsActionRunsOnClick() =
        runComposeUiTest {
            val platform = FakePlatform()
            val graph = createGraphFactory<AppGraph.Factory>().create(platform)
            val owner = TestOwner()
            lateinit var projects: ProjectsModel
            setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner,
                    LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                ) {
                    BuilderRoot(graph)
                    projects = metroViewModel()
                }
            }
            waitUntil { platform.environment.splashHidden }
            waitUntil {
                projects.state.value.projects
                    .isNotEmpty()
            }
            val deleted = projects.state.value.projects
                .first()
            val message = "Deleted ${deleted.name}"

            // The drawer's undo toast is a ShowToast with Undo, ToastDuration.Long and the undo to run.
            runOnIdle { projects.handle(ProjectsAction.Delete(deleted.id)) }
            waitUntil { onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(PAST_SHORT_MILLIS)
            onNodeWithText(message).assertExists()
            mainClock.autoAdvance = true

            onNodeWithText("Undo").performClick()
            waitUntil {
                projects.state.value.projects
                    .any { meta -> meta.id == deleted.id }
            }

            onNodeWithText(message).assertDoesNotExist()
            projects.state.value.lastDeletion shouldBe null
        }

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
