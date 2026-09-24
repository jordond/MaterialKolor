package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.Language
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeFileSaver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800
private const val FILE_TEXT = "val theme = 1"
private const val ALL_TEXT = "every file joined"
private val ZIP = OutgoingFile(name = "AppTheme.zip", bytes = byteArrayOf(1, 2, 3), mime = ZIP_MIME)

@OptIn(ExperimentalTestApi::class)
class ExportSheetTest {
    private val clipboard = FakeClipboard()
    private val exported = mutableListOf<ExportAction>()

    @Test
    fun copyAll_whenTheClipboardRefuses_opensTheManualDialogAndNeverSaysCopied() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            clipboard.failure = IllegalStateException("No user activation")
            showSheet(FakeFileSaver(), coarsePointer = false)

            onNodeWithText("Copy all").performClick()
            waitForIdle()

            onNodeWithText("Copy it yourself").assertExists()
            onNodeWithText(ALL_TEXT).assertExists()
            onNodeWithText("Copied").assertDoesNotExist()
            clipboard.texts shouldBe emptyList()
            exported shouldBe emptyList()
        }

    @Test
    fun copyFile_afterItWorked_showsCopiedForAMomentThenCopyAgain() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)
            mainClock.autoAdvance = false

            onNodeWithText("Copy file").performClick()
            mainClock.advanceTimeByFrame()

            onNodeWithText("Copied").assertExists()
            clipboard.text shouldBe "$FILE_TEXT\n"
            exported shouldBe listOf(ExportAction.Exported)
            mainClock.advanceTimeBy(COPIED_MILLIS)
            mainClock.advanceTimeByFrame()
            onNodeWithText("Copied").assertDoesNotExist()
            onNodeWithText("Copy file").assertExists()
        }

    @Test
    fun zip_onATouchScreenWhoseShareSheetTakesIt_goesToTheShareSheet() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val files = FakeFileSaver(canShareFiles = true)
            showSheet(files, coarsePointer = true)

            onNodeWithText("Share files").performClick()
            waitForIdle()

            files.shared.map { batch -> batch.map { file -> file.name } } shouldBe listOf(listOf("AppTheme.zip"))
            files.saved shouldBe emptyList()
        }

    @Test
    fun zip_whenTheShareSheetTurnsItDown_downloads() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val files = FakeFileSaver(canShareFiles = false)
            showSheet(files, coarsePointer = true)

            onNodeWithText("Download zip").performClick()
            waitForIdle()

            files.saved.map { file -> file.name } shouldBe listOf("AppTheme.zip")
            files.shared shouldBe emptyList()
        }

    @Test
    fun zip_withAMouse_downloadsEvenWhereSharingWorks() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val files = FakeFileSaver(canShareFiles = true)
            showSheet(files, coarsePointer = false)

            onNodeWithText("Download zip").performClick()
            waitForIdle()

            files.saved.map { file -> file.name } shouldBe listOf("AppTheme.zip")
            files.shared shouldBe emptyList()
        }

    @Test
    fun idleScope_runsNothingUntilAsked() {
        var ran = false

        idleScope().launch { ran = true }

        ran shouldBe false
    }

    @Test
    fun copyFileAndCopyAll_writeTheClipboardBeforeTheClickReturns() {
        val scope = idleScope()

        scope.launchCopy(clipboard, FILE_TEXT) {}
        clipboard.texts shouldBe listOf(FILE_TEXT)
        scope.launchCopy(clipboard, ALL_TEXT) {}

        clipboard.texts shouldBe listOf(FILE_TEXT, ALL_TEXT)
    }

    @Test
    fun download_savesBeforeTheClickReturns() {
        val files = FakeFileSaver()

        idleScope().launchZip(files, ZIP, share = false) {}

        files.saved.map { file -> file.name } shouldBe listOf(ZIP.name)
        files.shared shouldBe emptyList()
    }

    @Test
    fun share_handsTheZipToTheShareSheetBeforeTheClickReturns() {
        val files = FakeFileSaver(canShareFiles = true)

        idleScope().launchZip(files, ZIP, share = true) {}

        files.shared.map { batch -> batch.map { file -> file.name } } shouldBe listOf(listOf(ZIP.name))
        files.saved shouldBe emptyList()
    }

    /**
     * A scope that runs nothing until its scheduler is asked to, so only a launch that starts
     * undispatched reaches the platform before the click returns.
     */
    private fun idleScope(): CoroutineScope = CoroutineScope(StandardTestDispatcher())

    private fun ComposeUiTest.showSheet(
        files: FakeFileSaver,
        coarsePointer: Boolean,
    ) {
        val document = ThemeDocument.Default
        val state = ExportModel.State(document = document, preferences = Preferences())
        val ready = ExportOutcome.Ready(
            files = listOf(
                GeneratedFile(
                    path = "src/commonMain/kotlin/com/example/theme/Theme.kt",
                    language = Language.Kotlin,
                    lines = listOf(listOf(Token(TokenKind.Plain, FILE_TEXT))),
                ),
            ),
            allText = ALL_TEXT,
            zip = ZIP,
        )
        val result = ThemeResolver().resolve(document)
        setContent {
            BuilderTheme(
                skin = Skin(library = document.library, expressive = document.expressive),
                result = result,
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                    ExportSheet(
                        visible = true,
                        state = state,
                        capabilities = capabilitiesOf(document),
                        outcomeOf = { ready },
                        clipboard = clipboard,
                        files = files,
                        dispatcher = rememberDispatcher<ExportAction> { action -> exported += action },
                        workspace = rememberDispatcher<WorkspaceAction> {},
                    )
                }
            }
        }
        waitForIdle()
    }
}
