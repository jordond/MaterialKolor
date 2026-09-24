package com.materialkolor.builder.feature.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.fakes.FakeImageHandle
import com.materialkolor.builder.fakes.FakeImageInput
import com.materialkolor.builder.fakes.FakePasteInput
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Start = Argb(0xFF6750A4.toInt())
private const val WAIT_MILLIS = 5_000L

/**
 * The Undo on an image seed's toast only ever undoes that seed, and the toast goes once the
 * document has moved on (R-B-311).
 */
@OptIn(ExperimentalTestApi::class)
class ImageUndoToastTest {
    private val images = FakeImageInput()
    private val model = ImageSeedModel(images, FakePasteInput())
    private val store = ViewModelStore().apply { put("images", model) }
    private val photo = FakeImageHandle("photo.png")
    private val other = FakeImageHandle("other.png")
    private val workspace = ToastWorkspace(ThemeDocument(seed = Start))

    init {
        images.decoded[photo] = decodedOf(QuadrantColors)
        images.decoded[other] = decodedOf(GrayColors)
    }

    @Test
    fun undo_whileTheSeedIsStillOnTop_undoesIt() =
        runComposeUiTest {
            showHost()
            val toast = seed(photo)

            toast.press()

            workspace.undos shouldBe 1
            store.clear()
        }

    @Test
    fun anotherEdit_takesTheToastBack_andItsUndoDoesNothing() =
        runComposeUiTest {
            showHost()
            val toast = seed(photo)

            workspace.document = workspace.document.copy(contrast = ContrastLevel.High)
            waitForIdle()

            toast.withdrawn shouldBe true
            toast.press()
            workspace.undos shouldBe 0
            store.clear()
        }

    @Test
    fun newerImage_takesTheOlderToastBack() =
        runComposeUiTest {
            showHost()
            val older = seed(photo)

            val newer = seed(other)

            older.withdrawn shouldBe true
            newer.withdrawn shouldBe false
            older.press()
            workspace.undos shouldBe 0
            newer.press()
            workspace.undos shouldBe 1
            store.clear()
        }

    @Test
    fun projectSwitch_takesTheToastBack_andItsUndoDoesNothing() =
        runComposeUiTest {
            showHost()
            val toast = seed(photo)

            workspace.project = 1
            waitForIdle()

            toast.withdrawn shouldBe true
            toast.press()
            workspace.undos shouldBe 0
            store.clear()
        }

    @Test
    fun sameImageLandingAgain_changesNothing_andOffersNoUndo() =
        runComposeUiTest {
            showHost()
            seed(photo)
            // Another edit on top, which an Undo from the second landing would otherwise take back.
            workspace.document = workspace.document.copy(contrast = ContrastLevel.High)
            waitForIdle()

            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { workspace.plain.isNotEmpty() }

            val again = workspace.plain.single()
            again.message shouldBe "Seed taken from photo.png"
            again.actionLabel.shouldBeNull()
            again.onAction.shouldBeNull()
            workspace.undos shouldBe 0
            store.clear()
        }

    private fun ComposeUiTest.showHost() {
        setContent {
            val dispatcher = rememberDispatcher<WorkspaceAction> { action -> workspace.dispatch(action) }
            ImageHostContent(
                model = model,
                dispatcher = dispatcher,
                picking = false,
                project = workspace.project,
                document = workspace.document,
            )
        }
    }

    /** Drops [handle] and waits for the toast its seed puts up. */
    private fun ComposeUiTest.seed(handle: FakeImageHandle): ShownToast {
        val before = workspace.withdrawable.size
        images.drop(handle)
        waitUntil(timeoutMillis = WAIT_MILLIS) { workspace.withdrawable.size > before }
        waitForIdle()
        return workspace.withdrawable.last()
    }
}

/**
 * Stands in for the workspace behind the image host. Edits land at once, an Undo is only counted,
 * and every toast is kept with whether it was taken back.
 */
private class ToastWorkspace(
    document: ThemeDocument,
) {
    var document by mutableStateOf(document)
    var project by mutableStateOf(0)
    var undos = 0
    val withdrawable = mutableListOf<ShownToast>()
    val plain = mutableListOf<WorkspaceAction.ShowToast>()

    fun dispatch(action: WorkspaceAction) {
        when (action) {
            is WorkspaceAction.EditWithReveal -> {
                document = action.change.apply(document)
            }
            WorkspaceAction.Undo -> {
                undos++
            }
            is WorkspaceAction.ShowToast -> {
                plain += action
            }
            is WorkspaceAction.ShowWithdrawableToast -> {
                val shown = ShownToast(action.toast)
                withdrawable += shown
                action.onShown { shown.withdrawn = true }
            }
            else -> {
                Unit
            }
        }
    }
}

/** A toast as the workspace put it up. */
private class ShownToast(
    private val toast: WorkspaceAction.ShowToast,
) {
    var withdrawn = false

    /** Presses its action, as the toast host does. */
    fun press() {
        toast.onAction?.invoke()
    }
}
