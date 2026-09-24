package com.materialkolor.builder.feature.history

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.feature.command.CommandHarness
import com.materialkolor.builder.feature.command.InWorkspace
import com.materialkolor.builder.feature.command.keys
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.workspace.Panel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

// b-509

/**
 * The History list on the keyboard, drawn in the page the way the web draws it (D40), where Esc and
 * the focus hand back behave as people meet them.
 */
@OptIn(ExperimentalTestApi::class)
class TimelineTest {
    private val harness = CommandHarness()

    @Test
    fun timeline_keysOpenMoveAndApply_staysOpenAndEscReturnsFocus() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            listOf(Style.Vibrant, Style.Rainbow, Style.Neutral).forEach { style ->
                runOnUiThread { harness.workspace.edit(DocumentChange.SetStyle(style), EditPhase.Discrete) }
                waitForIdle()
            }

            keys { pressKey(Key.H) }
            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected().assert(hasText("Style change"))
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.Enter) }

            harness.graph.session.document.value.style shouldBe Style.Vibrant
            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected()
            keys { pressKey(Key.Escape) }
            harness.workspace.state.value.panel shouldBe null
            // H opened it, so focus went back to the page, where Space shuffles.
            onNode(hasContentDescription("History") and hasClickAction() and InWorkspace).assertIsNotFocused()
            val seed = harness.graph.session.document.value.seed
            keys { pressKey(Key.Spacebar) }
            harness.graph.session.document.value.seed shouldNotBe seed
        }

    @Test
    fun timeline_jumpAcrossALibrarySwitch_keepsFocusOnTheCurrentStep() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            boot()
            runOnUiThread { harness.workspace.edit(LibraryChoice.Unstyled.change, EditPhase.Discrete) }
            waitUntil { harness.workspace.state.value.document.library == Library.Unstyled }
            waitForIdle()

            keys { pressKey(Key.H) }
            focusedRow().assertIsSelected().assert(hasText("Library change to Unstyled"))
            keys { pressKey(Key.DirectionDown) }
            keys { pressKey(Key.Enter) }
            waitUntil { harness.workspace.state.value.document.library == Library.Material3 }
            waitForIdle()

            harness.workspace.state.value.panel shouldBe Panel.History
            focusedRow().assertIsSelected().assert(hasText("Start"))
        }

    private fun ComposeUiTest.boot() {
        with(harness) { show(inTree = true) }
    }

    /** Whatever holds focus, a row of the list while it is open. */
    private fun ComposeUiTest.focusedRow(): SemanticsNodeInteraction = onNode(isFocused() and InWorkspace)
}
