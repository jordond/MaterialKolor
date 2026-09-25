package com.materialkolor.builder.feature.workspace

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.feature.command.CommandHarness
import com.materialkolor.builder.kit.layout.PosterMode
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * The Fine-tune sheet is session state on the workspace, which nothing saves. Collapsing the poster,
 * opening the Projects drawer or showing another project shuts it.
 */
@OptIn(ExperimentalTestApi::class)
class FineTuneSessionTest {
    private val harness = CommandHarness()

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
            val open = harness.workspace.state.value.copy(fineTune = FineTuneSection.KeyColors)

            open.withGeneration(open.projectGeneration).fineTune shouldBe FineTuneSection.KeyColors
            open.withGeneration(open.projectGeneration + 1).fineTune shouldBe null
        }
}
