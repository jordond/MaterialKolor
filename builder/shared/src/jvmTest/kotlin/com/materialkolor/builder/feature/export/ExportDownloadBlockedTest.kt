package com.materialkolor.builder.feature.export

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.ShellExpressive
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
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A download the browser blocks says so in a toast and never counts as an export.
 */
@OptIn(ExperimentalTestApi::class)
class ExportDownloadBlockedTest {
    @Test
    fun download_whenTheBrowserBlocksIt_saysSoAndNeverCountsAsAnExport() =
        runDesktopComposeUiTest(width = 1280, height = 800) {
            val files = FakeFileSaver()
            files.failure = IllegalStateException("Download blocked")
            val exported = mutableListOf<ExportAction>()
            val toasts = mutableListOf<String>()
            val document = ThemeDocument.Default
            val state = ExportModel.State(document = document, preferences = Preferences())
            val ready = ExportOutcome.Ready(
                files = listOf(
                    GeneratedFile(
                        path = "src/commonMain/kotlin/com/example/theme/Theme.kt",
                        language = Language.Kotlin,
                        lines = listOf(listOf(Token(TokenKind.Plain, "val theme = 1"))),
                    ),
                ),
                allText = "val theme = 1",
                zip = OutgoingFile(name = "AppTheme.zip", bytes = byteArrayOf(1, 2, 3), mime = ZIP_MIME),
            )
            val result = ThemeResolver().resolve(document)
            setContent {
                BuilderTheme(
                    expressive = ShellExpressive,
                    result = result,
                    isDark = false,
                    reducedMotion = true,
                ) {
                    ProvideBuilderLayout(coarsePointer = false, modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalAnnouncer provides Announcer {}) {
                            ExportSheet(
                                visible = true,
                                state = state,
                                capabilities = capabilitiesOf(document),
                                outcomeOf = { ready },
                                clipboard = FakeClipboard(),
                                files = files,
                                dispatcher = rememberDispatcher<ExportAction> { action -> exported += action },
                                workspace = rememberDispatcher<WorkspaceAction> { action ->
                                    if (action is WorkspaceAction.ShowToast) toasts += action.message
                                },
                            )
                        }
                    }
                }
            }
            waitForIdle()

            onNodeWithText("Download zip").performClick()
            waitUntil { toasts.isNotEmpty() }

            toasts shouldBe listOf("Could not save the zip")
            files.saved shouldBe emptyList()
            exported.none { action -> action == ExportAction.Exported } shouldBe true
        }
}
