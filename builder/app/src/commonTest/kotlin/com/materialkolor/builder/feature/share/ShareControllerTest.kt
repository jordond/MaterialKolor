package com.materialkolor.builder.feature.share

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeFileSaver
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareControllerTest : SessionTestBase() {
    private val harness = ViewModelHarness()
    private val clipboard = FakeClipboard()
    private val files = FakeFileSaver()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun link_forTheOpenTheme_isTheExactUrlAndNeverThePackageName() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            preferences.updateExportPrefs(
                ExportTarget.Material3,
            ) { prefs -> prefs.copy(packageName = "com.example.brand") }
            val controller = controller(session)

            val link = controller.link(OCEAN, "Harbour").shouldNotBeNull()

            link shouldBe "https://materialkolor.com/t/" + ShareCodec.encode(OCEAN, "Harbour")
            link shouldNotContain "example"
            val decoded = ShareCodec.decode(link.removePrefix(SHARE_LINK_BASE)).shouldBeInstanceOf<DecodeResult.Ok>()
            decoded.projectName shouldBe "Harbour"
            decoded.document.seed shouldBe OCEAN.seed
            harness.clearAndJoin()
        }

    @Test
    fun copy_whenTheClipboardTakesIt_isCopiedWithTheExactUrl() =
        runTest {
            val (session, _) = session()
            val controller = controller(session)
            val link = controller.link(OCEAN, "Harbour").shouldNotBeNull()

            controller.copy(link) shouldBe ShareOutcome.Copied

            clipboard.text shouldBe link
            harness.clearAndJoin()
        }

    @Test
    fun copy_whenTheClipboardRefuses_isCopyFailed() =
        runTest {
            val (session, _) = session()
            clipboard.failure = IllegalStateException("NotAllowedError")
            val controller = controller(session)

            controller.copy("https://materialkolor.com/t/abc") shouldBe ShareOutcome.CopyFailed
            harness.clearAndJoin()
        }

    @Test
    fun share_onATouchScreenWithASheet_handsTheLinkToTheSheetAndNotTheClipboard() =
        runTest {
            val (session, _) = session()
            files.canShareLink = true
            val controller = controller(session)

            controller.share("https://materialkolor.com/t/abc", "Harbour") shouldBe ShareOutcome.Shared

            files.sharedLinks shouldBe listOf("https://materialkolor.com/t/abc" to "Harbour")
            clipboard.texts.shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun share_whenTheSheetFails_isShareFailedWithoutFallingBackToTheClipboard() =
        runTest {
            val (session, _) = session()
            files.canShareLink = true
            files.failure = IllegalStateException("NotAllowedError")
            val controller = controller(session)

            controller.share("https://materialkolor.com/t/abc", "Harbour") shouldBe ShareOutcome.ShareFailed

            clipboard.texts.shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun share_withoutASheet_copiesTheLink() =
        runTest {
            val (session, _) = session()
            val controller = controller(session)

            controller.share("https://materialkolor.com/t/abc", "Harbour") shouldBe ShareOutcome.Copied

            clipboard.text shouldBe "https://materialkolor.com/t/abc"
            files.sharedLinks.shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun openShared_theSameLinkTwice_opensOneProject() =
        runTest {
            val (session, _) = session()
            val first = booted(session)
            val controller = controller(session)
            val code = ShareCodec.encode(OCEAN, "Harbour")

            controller.openShared(code).shouldBeNull()
            session.project.value shouldBe ProjectRef.Transient(code)
            session.saveTransient().join()
            runCurrent()
            val saved = session.project.value
                .shouldBeInstanceOf<ProjectRef.Persisted>()
                .id
            session.open(first) shouldBe true

            controller.openShared(code).shouldBeNull()

            session.project.value shouldBe ProjectRef.Persisted(saved)
            projects.index.first().projects shouldHaveSize 2
            harness.clearAndJoin()
        }

    @Test
    fun openShared_aCodeFromANewerBuilder_isTheNewerVersionNotice() =
        runTest {
            val (session, _) = session()
            val first = booted(session)
            val controller = controller(session)

            // Version 2 in the first byte, which this builder cannot read yet.
            controller.openShared("AgAAAAAAAAAA") shouldBe BootNotice.NewerVersion

            session.project.value shouldBe ProjectRef.Persisted(first)
            harness.clearAndJoin()
        }

    @Test
    fun openShared_aCorruptCode_isTheInvalidLinkNotice() =
        runTest {
            val (session, _) = session()
            val first = booted(session)
            val controller = controller(session)

            controller.openShared("not a code") shouldBe BootNotice.InvalidLink

            session.project.value shouldBe ProjectRef.Persisted(first)
            harness.clearAndJoin()
        }

    private fun controller(session: ProjectSession): ShareController =
        harness.own(ShareController(session, clipboard, files))
}
