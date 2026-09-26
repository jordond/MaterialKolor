package com.materialkolor.builder.feature.command

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.Panel
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800
private val Purple = Argb(0xFF6750A4.toInt())
private val Blue = Argb(0xFF1A73E8.toInt())

/**
 * Text pasted with nothing editable focused, on the whole builder.
 */
@OptIn(ExperimentalTestApi::class)
class PasteRouterTest {
    private val harness = CommandHarness()
    private val platform = harness.platform

    @Test
    fun pastedHex_setsTheSeedAsOneUndoEntry_withAnUndoToast() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = document()

            platform.pastes.paste(Paste.Text("  #6750A4\n"))
            waitUntil { document().seed == Purple }

            document().seedSource shouldBe SeedSource.Typed
            waitUntil { named("Seed set to #6750A4") }
            workspaceButton("Undo").performClick()
            waitForIdle()
            document() shouldBe start
            harness.graph.session.history.value.canUndo shouldBe false
        }

    @Test
    fun pastedLink_offersOpen_andOpensTheTheme() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val link = shareLink(ThemeDocument.Default.copy(seed = Blue), projectName = "Blue sky").shouldNotBeNull()

            platform.pastes.paste(Paste.Text(link))
            waitUntil { named("Open the shared theme?") }
            document().seed shouldBe ThemeDocument.Default.seed

            workspaceButton("Open").performClick()

            waitUntil { document().seed == Blue }
            waitUntil { harness.workspace.state.value.projectName == "Blue sky" }
        }

    @Test
    fun pasteWithAPanelOpen_orPlainWords_changesNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = document()

            platform.pastes.paste(Paste.Text("some notes about the theme"))
            platform.pastes.paste(Paste.Text("Vibrant"))
            runOnUiThread { harness.workspace.openPanel(Panel.CheatSheet) }
            waitForIdle()
            platform.pastes.paste(Paste.Text("#6750A4"))
            waitForIdle()
            runOnUiThread { harness.workspace.closePanel() }
            waitForIdle()

            document() shouldBe start
            harness.graph.session.history.value.canUndo shouldBe false
            named("Open the shared theme?") shouldBe false
        }

    @Test
    fun pasteWithTheVisionMenuOpen_changesNothing() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            val start = document()
            runOnUiThread { harness.workspace.setVisionMenuOpen(true) }
            waitForIdle()

            platform.pastes.paste(Paste.Text("#6750A4"))
            waitForIdle()
            runOnUiThread { harness.workspace.setVisionMenuOpen(false) }
            waitForIdle()
            // A paste once the menu has closed lands, and undoing it goes straight back to the start.
            platform.pastes.paste(Paste.Text("#1A73E8"))
            waitUntil { document().seed == Blue }

            runOnUiThread { harness.workspace.undo() }
            waitForIdle()
            document() shouldBe start
        }

    private fun ComposeUiTest.boot() {
        with(harness) { show() }
    }

    private fun document() = harness.graph.session.document.value

    /**
     * The toast's action [label], which no other workspace control shows as text.
     */
    private fun ComposeUiTest.workspaceButton(label: String) =
        onNode(hasText(label) and hasClickAction() and InWorkspace)
}
