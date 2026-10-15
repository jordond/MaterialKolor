package com.materialkolor.builder.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.feature.command.CommandHarness
import com.materialkolor.builder.kit.layout.PosterMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The Fine-tune sheet is session state on the workspace, which nothing saves. Collapsing the poster,
 * opening the Projects drawer or showing another project shuts it.
 */
@OptIn(ExperimentalTestApi::class)
class FineTuneSessionTest {
    private val harness = CommandHarness()

    @AfterTest
    fun tearDown() {
        harness.close()
    }

    @Test
    fun openFineTune_opensAtTheSection_orAtTheLocksWithNone_andLeavesTheProjectAlone() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            val view = harness.workspace.state.value.view

            runOnUiThread { harness.workspace.openFineTune(FineTuneSection.Pins) }
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.Pins

            runOnUiThread { harness.workspace.openFineTune() }
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.Locks
            harness.workspace.state.value.view shouldBe view

            runOnUiThread { harness.workspace.closeFineTune() }
            harness.workspace.state.value.fineTune shouldBe null
        }

    @Test
    fun collapsingThePoster_orOpeningProjects_shutsTheSheet() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }

            runOnUiThread {
                harness.workspace.openFineTune(FineTuneSection.Spec)
                harness.workspace.setPosterCollapsed(collapsed = true, mode = PosterMode.Rail72)
            }
            harness.workspace.state.value.fineTune shouldBe null

            runOnUiThread {
                harness.workspace.openFineTune(FineTuneSection.Spec)
                harness.workspace.openPanel(Panel.Palette)
            }
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.Spec

            runOnUiThread {
                harness.workspace.closePanel()
                harness.workspace.openPanel(Panel.Projects)
            }
            harness.workspace.state.value.fineTune shouldBe null

            runOnUiThread {
                harness.workspace.closePanel()
                harness.workspace.openFineTune(FineTuneSection.Spec)
                harness.workspace.setPosterCollapsed(collapsed = true)
            }
            harness.workspace.state.value.fineTune shouldBe null
        }

    @Test
    fun anotherProject_shutsTheSheet_andTheSameOneKeepsItOpen() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            with(harness) { show() }
            val start = harness.workspace.state.value.projectGeneration

            runOnUiThread {
                harness.workspace.openFineTune(FineTuneSection.KeyColors)
                harness.workspace.edit(DocumentChange.SetThemeName("EditedTheme"), EditPhase.Discrete)
            }
            harness.workspace.state.value.projectGeneration shouldBe start
            harness.workspace.state.value.fineTune shouldBe FineTuneSection.KeyColors

            runOnUiThread {
                harness.workspace.viewModelScope.launch { harness.graph.session.newProject(copyCurrent = false) }
            }
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.workspace.state.value.projectGeneration != start }
            harness.workspace.state.value.fineTune shouldBe null
        }
}
