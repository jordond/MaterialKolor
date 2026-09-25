package com.materialkolor.builder.feature.command

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.fakes.FakePasteInput
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Start = Argb(0xFFD9653B.toInt())
private const val WAIT_MILLIS = 5_000L

/**
 * The Undo on a pasted color's toast only ever undoes that color.
 */
@OptIn(ExperimentalTestApi::class)
class PasteUndoTest {
    private val pastes = FakePasteInput()
    private val model = PasteRouter(pastes)
    private val store = ViewModelStore().apply { put("pastes", model) }
    private val workspace = PasteWorkspaceFake(ThemeDocument(seed = Start))

    @Test
    fun undo_whileTheColorIsStillOnTop_undoesIt() =
        runComposeUiTest {
            showHost()
            val toast = paste("#6750A4")

            workspace.document.seed shouldBe Argb(0xFF6750A4.toInt())
            toast.onAction?.invoke()

            workspace.undos shouldBe 1
            store.clear()
        }

    @Test
    fun anotherEdit_takesTheToastBack_andItsUndoDoesNothing() =
        runComposeUiTest {
            showHost()
            val toast = paste("#6750A4")

            workspace.document = workspace.document.copy(contrast = ContrastLevel.High)
            waitForIdle()

            workspace.withdrawn shouldBe 1
            toast.onAction?.invoke()
            workspace.undos shouldBe 0
            store.clear()
        }

    private fun ComposeUiTest.showHost() {
        setContent {
            val dispatcher = rememberDispatcher<WorkspaceAction> { action -> workspace.dispatch(action) }
            PasteHostContent(
                model = model,
                dispatcher = dispatcher,
                panelOpen = false,
                project = 0,
                document = workspace.document,
                openShared = { null },
            )
        }
    }

    /**
     * Pastes [text] and waits for the toast its seed puts up.
     */
    private fun ComposeUiTest.paste(text: String): WorkspaceAction.ShowToast {
        pastes.paste(Paste.Text(text))
        waitUntil(timeoutMillis = WAIT_MILLIS) { workspace.toasts.isNotEmpty() }
        waitForIdle()
        return workspace.toasts.last()
    }
}

/**
 * Stands in for the workspace. Edits land at once, an Undo is only counted, and toasts are kept.
 */
private class PasteWorkspaceFake(
    document: ThemeDocument,
) {
    var document by mutableStateOf(document)
    var undos = 0
    var withdrawn = 0
    val toasts = mutableListOf<WorkspaceAction.ShowToast>()

    fun dispatch(action: WorkspaceAction) {
        when (action) {
            is WorkspaceAction.EditWithReveal -> {
                document = action.change.apply(document)
            }
            WorkspaceAction.Undo -> {
                undos++
            }
            is WorkspaceAction.ShowWithdrawableToast -> {
                toasts += action.toast
                action.onShown { withdrawn++ }
            }
            else -> {
                Unit
            }
        }
    }
}
