package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeEnvironment
import com.materialkolor.builder.fakes.FakeFileSaver
import com.materialkolor.builder.fakes.FakeLinkCardSource
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.workspaceStateOf
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.Dispatcher
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val NEW_NAME = "Lighthouse"

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ShareHostTest : SessionTestBase() {
    @Test
    fun copy_afterANameTypedButNotCommitted_renamesTheProjectToIt() {
        val (before, after) = namesAround {
            onNode(hasSetTextAction()).performTextReplacement(NEW_NAME)
            onNode(hasText("Copy link") and hasClickAction()).performClick()
            waitForIdle()
        }

        before shouldBe "Harbour"
        after shouldBe NEW_NAME
    }

    @Test
    fun close_afterANameTypedButNotCommitted_renamesTheProjectToIt() {
        val (_, after) = namesAround {
            onNode(hasSetTextAction()).performTextReplacement(NEW_NAME)
            onNode(hasContentDescription("Close")).performClick()
            waitForIdle()
        }

        after shouldBe NEW_NAME
    }

    @Test
    fun close_afterTheNameIsClearedToBlank_leavesTheNameAlone() {
        val (before, after) = namesAround {
            onNode(hasSetTextAction()).performTextReplacement("  ")
            onNode(hasContentDescription("Close")).performClick()
            waitForIdle()
        }

        after shouldBe before
    }

    @Test
    fun nameToRename_aNewGoodName_isTheDraftTrimmed() {
        nameToRename("  Lighthouse ", current = "Harbour") shouldBe "Lighthouse"
    }

    @Test
    fun nameToRename_theNameTheProjectHas_renamesNothing() {
        nameToRename("Harbour ", current = "Harbour").shouldBeNull()
    }

    @Test
    fun nameToRename_aBlankOrTooLongDraft_renamesNothing() {
        nameToRename("   ", current = "Harbour").shouldBeNull()
        nameToRename("x".repeat(49), current = "Harbour").shouldBeNull()
    }

    /**
     * The saved name of a project called Harbour before and after [act] runs on its open share
     * dialog, hosted over a real session with fake platform parts.
     */
    private fun namesAround(act: ComposeUiTest.() -> Unit): Pair<String, String> {
        var names: Pair<String, String>? = null
        runTest {
            val (session, _) = session()
            val id = booted(session)
            session.rename(id, "Harbour").shouldBeNull()
            val controller =
                ShareController(session, FakeClipboard(), FakeFileSaver(), FakeEnvironment(), FakeLinkCardSource())

            suspend fun savedName() =
                projects.index
                    .first()
                    .projects
                    .single { meta -> meta.id == id }
                    .name
            val before = savedName()

            runComposeUiTest {
                setContent {
                    BuilderTheme(
                        expressive = false,
                        result = ThemeResolver().resolve(ThemeDocument.Default),
                        isDark = false,
                        reducedMotion = true,
                    ) {
                        ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                            ShareHost(
                                state = workspaceStateOf(projectName = before).copy(panel = Panel.Share),
                                dispatcher = Dispatcher<WorkspaceAction> {},
                                controller = controller,
                            )
                        }
                    }
                }
                waitForIdle()
                act()
            }
            runCurrent()

            names = before to savedName()
        }
        return checkNotNull(names)
    }
}
