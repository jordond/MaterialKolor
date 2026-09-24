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
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800
private const val FILE_TEXT = "val theme = 1"
private const val ALL_TEXT = "every file joined"

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
            zip = OutgoingFile(name = "AppTheme.zip", bytes = byteArrayOf(1, 2, 3), mime = ZIP_MIME),
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
