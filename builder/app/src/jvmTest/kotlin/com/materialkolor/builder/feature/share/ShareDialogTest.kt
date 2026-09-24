package com.materialkolor.builder.feature.share

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val LINK = "https://materialkolor.com/t/AQAAAAAAAAA"

@OptIn(ExperimentalTestApi::class)
class ShareDialogTest {
    private val done = mutableListOf<ShareOutcome>()
    private val copied = mutableListOf<String>()

    @Test
    fun copy_thatLands_isReportedAsCopied() =
        runComposeUiTest {
            showDialog(copyOutcome = ShareOutcome.Copied)

            onNodeWithText("Copy link").performClick()
            waitForIdle()

            copied shouldBe listOf(LINK)
            done shouldBe listOf(ShareOutcome.Copied)
        }

    @Test
    fun copy_thatFails_showsTheManualCopyTextAndNeverCopied() =
        runComposeUiTest {
            showDialog(copyOutcome = ShareOutcome.CopyFailed)

            onNodeWithText("Copy link").performClick()
            waitForIdle()

            onNode(hasText("copy it yourself", substring = true)).assertExists()
            onNode(hasText(LINK)).assertExists()
            done.shouldBeEmpty()
        }

    @Test
    fun dialog_withoutAShareSheet_offersOnlyCopy() =
        runComposeUiTest {
            showDialog(sharesToSheet = false)

            onAllNodes(hasText("Share")).assertCountEquals(0)
            onNodeWithText("Copy link").assertExists()
        }

    @Test
    fun share_onATouchScreen_goesToTheSheet() =
        runComposeUiTest {
            showDialog(sharesToSheet = true)

            onNodeWithText("Share").performClick()
            waitForIdle()

            done shouldBe listOf(ShareOutcome.Shared)
            copied.shouldBeEmpty()
        }

    private fun ComposeUiTest.showDialog(
        copyOutcome: ShareOutcome = ShareOutcome.Copied,
        sharesToSheet: Boolean = false,
    ) {
        setContent {
            BuilderTheme(
                skin = Skin(library = Library.Material3, expressive = false),
                result = ThemeResolver().resolve(ThemeDocument.Default),
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                    ShareDialog(
                        visible = true,
                        link = LINK,
                        sharesToSheet = sharesToSheet,
                        copy = { url ->
                            copied += url
                            copyOutcome
                        },
                        share = { ShareOutcome.Shared },
                        onDone = { outcome -> done += outcome },
                        onDismissRequest = {},
                    )
                }
            }
        }
        waitForIdle()
    }
}
