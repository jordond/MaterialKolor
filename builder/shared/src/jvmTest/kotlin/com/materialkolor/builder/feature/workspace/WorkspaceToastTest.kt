package com.materialkolor.builder.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.feature.projects.ProjectsAction
import com.materialkolor.builder.feature.projects.ProjectsModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Past the kit's four second Short toast and well inside its ten second Long one, so a toast still
 * up by then was shown for Long.
 */
private const val PAST_SHORT_MILLIS = 6_000L

@OptIn(ExperimentalTestApi::class)
class WorkspaceToastTest {
    private val app = AppHarness()

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun showToast_withAnActionAndALongDuration_reachesTheToastsAndItsActionRunsOnClick() =
        runComposeUiTest {
            lateinit var projects: ProjectsModel
            with(app) {
                bootRoot { graph ->
                    BuilderRoot(graph)
                    projects = metroViewModel()
                }
            }
            waitUntil(timeoutMillis = WAIT_MILLIS) {
                projects.state.value.projects
                    .isNotEmpty()
            }
            val deleted = projects.state.value.projects
                .first()
            val message = "Deleted ${deleted.name}"

            // The drawer's undo toast is a ShowToast with Undo, ToastDuration.Long and the undo to run.
            runOnIdle { projects.handle(ProjectsAction.Delete(deleted.id)) }
            waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(PAST_SHORT_MILLIS)
            onNodeWithText(message).assertExists()
            mainClock.autoAdvance = true

            onNodeWithText("Undo").performClick()
            waitUntil(timeoutMillis = WAIT_MILLIS) {
                projects.state.value.projects
                    .any { meta -> meta.id == deleted.id }
            }

            onNodeWithText(message).assertDoesNotExist()
            projects.state.value.lastDeletion shouldBe null
        }
}
