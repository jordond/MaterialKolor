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
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeFileSaver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val LINK = "https://materialkolor.com/t/AQAAAAAAAAA"

@OptIn(ExperimentalTestApi::class)
class ShareDialogTest : SessionTestBase() {
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

    @Test
    fun dialog_withoutALink_saysTheThemeCannotBePutInOne() =
        runComposeUiTest {
            showDialog(link = null)

            onNodeWithText("This theme can’t be put in a link").assertExists()
            onAllNodes(hasText("Copy link")).assertCountEquals(0)
        }

    @Test
    fun copyClick_writesTheClipboardBeforeTheClickReturns() =
        runTest {
            val clipboard = FakeClipboard()
            val controller = ShareController(session().first, clipboard, FakeFileSaver())
            val outcomes = mutableListOf<ShareOutcome>()

            idleScope().launchSend(LINK, controller::copy) { outcome -> outcomes += outcome }

            clipboard.texts shouldBe listOf(LINK)
            outcomes shouldBe listOf(ShareOutcome.Copied)
        }

    @Test
    fun shareClick_reachesTheShareSheetBeforeTheClickReturns() =
        runTest {
            val clipboard = FakeClipboard()
            val files = FakeFileSaver(canShareLink = true)
            val controller = ShareController(session().first, clipboard, files)

            idleScope().launchSend(LINK, call = { url -> controller.share(url, "Harbour") }, onOutcome = {})

            files.sharedLinks shouldBe listOf(LINK to "Harbour")
            clipboard.texts.shouldBeEmpty()
        }

    @Test
    fun sharedNoticeText_eachNotice_asksForAReloadOrSaysTheLinkDoesNotRead() =
        runTest {
            sharedNoticeText(BootNotice.NewerVersion) shouldBe "Made with a newer version. Reload to update."
            sharedNoticeText(BootNotice.InvalidLink) shouldBe "That link doesn’t open a theme"
            sharedNoticeText(BootNotice.UnknownPath) shouldBe "That link doesn’t open a theme"
        }

    /**
     * A scope that runs nothing until its scheduler is asked to, so only a launch that starts
     * undispatched reaches the platform before the click returns.
     */
    private fun idleScope(): CoroutineScope = CoroutineScope(StandardTestDispatcher())

    private fun ComposeUiTest.showDialog(
        copyOutcome: ShareOutcome = ShareOutcome.Copied,
        sharesToSheet: Boolean = false,
        link: String? = LINK,
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
                        link = link,
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
