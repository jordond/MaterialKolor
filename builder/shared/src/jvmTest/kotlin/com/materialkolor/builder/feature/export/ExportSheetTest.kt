package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.ShellExpressive
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.Language
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeFileSaver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800

/**
 * A width under 720 dp, where the sheet folds its options.
 */
private const val NARROW_WIDTH = 600
private const val FILE_TEXT = "val theme = 1"
private const val ALL_TEXT = "every file joined"
private val ZIP = OutgoingFile(name = "AppTheme.zip", bytes = byteArrayOf(1, 2, 3), mime = ZIP_MIME)

@OptIn(ExperimentalTestApi::class)
class ExportSheetTest {
    private val clipboard = FakeClipboard()
    private val exported = mutableListOf<ExportAction>()
    private val announced = mutableListOf<String>()

    @Test
    fun copyAll_whenTheClipboardRefuses_opensTheManualDialogAndNeverSaysCopied() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            clipboard.failure = IllegalStateException("No user activation")
            showSheet(FakeFileSaver(), coarsePointer = false)

            onNodeWithText("Copy all").performClick()
            waitForIdle()

            onNodeWithText("Copy it yourself").assertExists()
            onNodeWithText(ALL_TEXT).assertExists()
            action("Copied").assertDoesNotExist()
            clipboard.texts shouldBe emptyList()
            exported shouldBe emptyList()
            announced shouldBe emptyList()
        }

    @Test
    fun copy_eachTimeItWorks_readsCopiedOutOnce() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)

            action("Copy file").performClick()
            waitForIdle()
            onNodeWithText("Copy all").performClick()
            waitForIdle()

            announced shouldBe listOf("Copied", "Copied")
        }

    @Test
    fun invalidPackageDraft_holdsBackEveryExportUntilEsc() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)

            nameField("Package name").performTextReplacement("Not A Package")
            waitForIdle()

            exportButtons().forEach { button -> button.assertIsNotEnabled() }
            onAllNodesWithText("Not A Package is not a package name", substring = true).assertCountEquals(1)
            nameField("Package name").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            exportButtons().forEach { button -> button.assertIsEnabled() }
            onAllNodesWithText("is not a package name", substring = true).assertCountEquals(0)
        }

    @Test
    fun invalidThemeNameDraft_holdsBackEveryExportUntilEsc() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)

            nameField("Theme name").performTextReplacement("1 bad")
            waitForIdle()

            exportButtons().forEach { button -> button.assertIsNotEnabled() }
            onAllNodesWithText("1 bad cannot be a theme name", substring = true).assertCountEquals(1)
            nameField("Theme name").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            exportButtons().forEach { button -> button.assertIsEnabled() }
        }

    /**
     * Only the narrow sheet folds the fields into Options, which starts closed.
     */
    @Test
    fun invalidPackageDraft_collapsingOptions_letsTheExportThrough() =
        runDesktopComposeUiTest(width = NARROW_WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)
            onNodeWithText("Options").performClick()
            waitForIdle()
            nameField("Package name").performTextReplacement("Not A Package")
            waitForIdle()
            exportButtons().forEach { button -> button.assertIsNotEnabled() }

            onNodeWithText("Options").performClick()
            waitForIdle()

            nameField("Package name").assertDoesNotExist()
            exportButtons().forEach { button -> button.assertIsEnabled() }
            onAllNodesWithText("is not a package name", substring = true).assertCountEquals(0)
        }

    @Test
    fun invalidPackageDraft_targetSwitch_followsTheNewTargetsSavedPackage() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val fluent = ExportPrefs(packageName = "com.fluent.theme")
            showSheet(
                files = FakeFileSaver(),
                coarsePointer = false,
                preferences = Preferences(exportPrefs = mapOf(ExportTarget.Fluent to fluent)),
            )
            nameField("Package name").performTextReplacement("Not A Package")
            waitForIdle()
            exportButtons().forEach { button -> button.assertIsNotEnabled() }

            onNodeWithText("Fluent").performClick()
            waitForIdle()

            nameField("Package name").assertTextContains("com.fluent.theme")
            exportButtons().forEach { button -> button.assertIsEnabled() }
            onAllNodesWithText("is not a package name", substring = true).assertCountEquals(0)
        }

    @Test
    fun packageDraft_escBeforeItsEchoes_neverMovesTheText() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val echoes = mutableListOf<() -> Unit>()
            showSheet(FakeFileSaver(), coarsePointer = false, echoes = echoes)
            val start = packageText()

            nameField("Package name").performTextReplacement("com.typed")
            waitForIdle()
            nameField("Package name").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            packageText() shouldBe start

            // The echoes land late and one at a time, the typed package first.
            val shown = echoes.map { echo ->
                runOnIdle { echo() }
                waitForIdle()
                packageText()
            }
            shown shouldBe listOf(start, start)
            exported shouldBe listOf(ExportAction.SetPackageName("com.typed"), ExportAction.SetPackageName(start))
        }

    @Test
    fun validThenInvalidPackageDraft_targetSwitch_followsTheNewTargetsSavedPackage() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val fluent = ExportPrefs(packageName = "com.fluent.theme")
            val echoes = mutableListOf<() -> Unit>()
            showSheet(
                files = FakeFileSaver(),
                coarsePointer = false,
                preferences = Preferences(exportPrefs = mapOf(ExportTarget.Fluent to fluent)),
                echoes = echoes,
            )
            nameField("Package name").performTextReplacement("com.typed")
            waitForIdle()
            runOnIdle { echoes.forEach { echo -> echo() } }
            waitForIdle()
            nameField("Package name").performTextReplacement("Not A Package")
            waitForIdle()
            exportButtons().forEach { button -> button.assertIsNotEnabled() }

            onNodeWithText("Fluent").performClick()
            waitForIdle()

            packageText() shouldBe "com.fluent.theme"
            exportButtons().forEach { button -> button.assertIsEnabled() }
            onAllNodesWithText("is not a package name", substring = true).assertCountEquals(0)
        }

    @Test
    fun validPackageDraft_goesOutAsItIsTyped() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)

            nameField("Package name").performTextReplacement("com.typed")
            waitForIdle()

            exported shouldBe listOf(ExportAction.SetPackageName("com.typed"))
            exportButtons().forEach { button -> button.assertIsEnabled() }
        }

    @Test
    fun copyFile_afterItWorked_showsCopiedForAMomentThenCopyAgain() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showSheet(FakeFileSaver(), coarsePointer = false)
            mainClock.autoAdvance = false

            action("Copy file").performClick()
            mainClock.advanceTimeByFrame()

            action("Copied").assertExists()
            clipboard.text shouldBe "$FILE_TEXT\n"
            exported shouldBe listOf(ExportAction.Exported)
            mainClock.advanceTimeBy(COPIED_MILLIS)
            mainClock.advanceTimeByFrame()
            action("Copied").assertDoesNotExist()
            action("Copy file").assertExists()
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

    private fun ComposeUiTest.packageText(): String =
        nameField("Package name").fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun ComposeUiTest.exportButtons(): List<SemanticsNodeInteraction> =
        listOf("Copy file", "Copy all", "Download zip").map { label -> action(label) }

    /**
     * The name field called [name]. The header sets it inline, named by its description, and the
     * narrow sheet's Options shows it boxed under its label.
     */
    private fun ComposeUiTest.nameField(name: String): SemanticsNodeInteraction =
        onNode(hasSetTextAction() and (hasText(name) or hasContentDescription(name)))

    /**
     * The button called [name], labelled by its text or, drawn on the code ground, by its description.
     */
    private fun ComposeUiTest.action(name: String): SemanticsNodeInteraction =
        onNode(hasClickAction() and (hasText(name) or hasContentDescription(name)))

    /**
     * Shows the sheet on the default document. A pick among its library cards moves the document.
     * With [echoes] each package sent waits there to come back as the saved package until a test
     * runs it, and without it nothing comes back.
     */
    private fun ComposeUiTest.showSheet(
        files: FakeFileSaver,
        coarsePointer: Boolean,
        preferences: Preferences = Preferences(),
        echoes: MutableList<() -> Unit>? = null,
    ) {
        val document = ThemeDocument.Default
        var state by mutableStateOf(ExportModel.State(document = document, preferences = preferences))
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
                expressive = ShellExpressive,
                result = result,
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalAnnouncer provides Announcer { message -> announced += message }) {
                        ExportSheet(
                            visible = true,
                            state = state,
                            capabilities = capabilitiesOf(state.document),
                            outcomeOf = { ready },
                            clipboard = clipboard,
                            files = files,
                            dispatcher = rememberDispatcher<ExportAction> { action ->
                                exported += action
                                if (action is ExportAction.SetPackageName && echoes != null) {
                                    val target = state.target
                                    echoes += {
                                        val prefs = state.preferences.exportPrefsFor(target)
                                        val saved = prefs.copy(packageName = action.packageName)
                                        val preferences = state.preferences.withExportPrefs(target, saved)
                                        state = state.copy(preferences = preferences)
                                    }
                                }
                            },
                            workspace = rememberDispatcher<WorkspaceAction> { action ->
                                if (action is WorkspaceAction.EditWithReveal) {
                                    state = state.copy(document = action.change.apply(state.document))
                                }
                            },
                        )
                    }
                }
            }
        }
        waitForIdle()
    }
}
