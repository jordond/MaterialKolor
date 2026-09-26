package com.materialkolor.builder.feature.share

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeEnvironment
import com.materialkolor.builder.fakes.FakeFileSaver
import com.materialkolor.builder.fakes.FakeLinkCardSource
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val LINK = "https://materialkolor.com/t/AQAAAAAAAAA"

/**
 * A share link as long as a theme with a few custom colors makes it.
 */
private const val LONG_LINK =
    "https://materialkolor.com/t/AdllOwAAABALQnVybnQgT3JhbmdlIHdpdGggYSBsb25nIG5hbWUgdGhhdCBnb2VzIG9uIHBhc3QgdGhlIGVkZ2U"

/**
 * A phone held upright.
 */
private const val PHONE_WIDTH = 360

private const val PHONE_HEIGHT = 780

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
    fun copy_thatFailsOnATouchScreen_withoutASheet_sendsAFingerBackToCopy() =
        runComposeUiTest {
            showDialog(copyOutcome = ShareOutcome.CopyFailed, coarsePointer = true)

            onNodeWithText("Copy link").performClick()
            waitForIdle()

            onNode(hasText("Try Copy link again", substring = true)).assertExists()
            onAllNodes(hasText("copy it yourself", substring = true)).assertCountEquals(0)
            done.shouldBeEmpty()
        }

    @Test
    fun copy_thatFailsOnATouchScreen_withASheet_sendsAFingerToShare() =
        runComposeUiTest {
            showDialog(copyOutcome = ShareOutcome.CopyFailed, sharesToSheet = true, coarsePointer = true)

            onNodeWithText("Copy link").performClick()
            waitForIdle()

            onNode(hasText("Try Share instead", substring = true)).assertExists()
            onAllNodes(hasText("copy it yourself", substring = true)).assertCountEquals(0)
        }

    @Test
    fun share_thatFailsOnATouchScreen_sendsAFingerToCopy() =
        runComposeUiTest {
            showDialog(sharesToSheet = true, shareOutcome = ShareOutcome.ShareFailed, coarsePointer = true)

            onNodeWithText("Share").performClick()
            waitForIdle()

            onNode(hasText("Try Copy link instead", substring = true)).assertExists()
            done.shouldBeEmpty()
        }

    @Test
    fun copyAndShare_thatBothFailOnATouchScreen_sendAFingerBackToCopy() {
        for (copyFirst in listOf(true, false)) {
            withClue(if (copyFirst) "Copy link first" else "Share first") {
                runComposeUiTest {
                    showDialog(
                        copyOutcome = ShareOutcome.CopyFailed,
                        sharesToSheet = true,
                        shareOutcome = ShareOutcome.ShareFailed,
                        coarsePointer = true,
                    )

                    val order = if (copyFirst) listOf("Copy link", "Share") else listOf("Share", "Copy link")
                    for (button in order) {
                        onNodeWithText(button).performClick()
                        waitForIdle()
                    }

                    onNode(hasText("Try Copy link again", substring = true)).assertExists()
                    onAllNodes(hasText("instead", substring = true)).assertCountEquals(0)
                    done.shouldBeEmpty()
                }
            }
        }
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

    // One line in a field of its own that scrolls inside, with Copy link beside it, so a long link
    // never runs past the dialog's padding. A finger still copies it all with Copy link.
    @Test
    fun link_onAPhone_staysOnOneLineInsideItsField() =
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
            showDialog(link = LONG_LINK)

            val link = onNode(hasText(LONG_LINK))
            link.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            link.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
            layouts.single().lineCount shouldBe 1
            val field = link.fetchSemanticsNode().boundsInRoot
            val copy = onNode(hasText("Copy link")).fetchSemanticsNode().boundsInRoot
            field.right shouldBeLessThanOrEqual copy.left
            copy.right shouldBeLessThanOrEqual PHONE_WIDTH.toFloat()
            onNode(hasContentDescription("Share link")).assertExists()
            onNode(hasText("Copy link")).performClick()
            waitForIdle()
            copied shouldBe listOf(LONG_LINK)
        }

    @Test
    fun copyClick_writesTheClipboardBeforeTheClickReturns() =
        runTest {
            val clipboard = FakeClipboard()
            val controller =
                ShareController(session().first, clipboard, FakeFileSaver(), FakeEnvironment(), FakeLinkCardSource())
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
            val controller = ShareController(session().first, clipboard, files, FakeEnvironment(), FakeLinkCardSource())

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
        shareOutcome: ShareOutcome = ShareOutcome.Shared,
        coarsePointer: Boolean = false,
    ) {
        setContent {
            BuilderTheme(
                skin = Skin(library = SkinLibrary.Material3, expressive = false),
                result = ThemeResolver().resolve(ThemeDocument.Default),
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                    ShareDialog(
                        visible = true,
                        link = link,
                        sharesToSheet = sharesToSheet,
                        copy = { url ->
                            copied += url
                            copyOutcome
                        },
                        share = { shareOutcome },
                        onDone = { outcome -> done += outcome },
                        onDismissRequest = {},
                    )
                }
            }
        }
        waitForIdle()
    }
}
