package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectMeta
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val MINUTE = 60_000L

@OptIn(ExperimentalTestApi::class)
class ProjectsDrawerTest {
    private val actions = mutableListOf<ProjectsAction>()

    @Test
    fun drawer_withEightProjects_showsNoSearch() =
        runComposeUiTest {
            showDrawer(ProjectsModel.State(projects = metas(SEARCH_THRESHOLD)))

            onNode(hasContentDescription("Theme 0")).assertExists()
            onAllSearchFields().assertCountEquals(0)
        }

    @Test
    fun drawer_withNineProjects_showsTheSearch() =
        runComposeUiTest {
            showDrawer(ProjectsModel.State(projects = metas(SEARCH_THRESHOLD + 1)))

            onAllSearchFields().assertCountEquals(1)
        }

    @Test
    fun row_showsTheNameAgeAndTarget() =
        runComposeUiTest {
            val meta = meta("Harbour", updatedAt = 0, library = Library.Fluent)
            showDrawer(
                ProjectsModel.State(projects = listOf(meta), open = ProjectRef.Persisted(meta.id)),
                now =
                    5 * MINUTE,
            )

            onNode(hasContentDescription("Harbour")).assertExists()
            onNodeWithText("5 minutes ago").assertExists()
            onNodeWithText("Fluent").assertExists()
        }

    @Test
    fun deleteInTheRowMenu_asksToDeleteThatProject() =
        runComposeUiTest {
            val meta = meta("Harbour")
            showDrawer(ProjectsModel.State(projects = listOf(meta)))

            onNodeWithContentDescription("More for Harbour").performClick()
            waitForIdle()
            onNodeWithText("Delete").performClick()
            waitForIdle()

            actions shouldContain ProjectsAction.Delete(meta.id)
        }

    @Test
    fun renameInTheRowMenu_editsTheNameInPlace() =
        runComposeUiTest {
            val meta = meta("Harbour")
            showDrawer(ProjectsModel.State(projects = listOf(meta)))

            onNodeWithContentDescription("More for Harbour").performClick()
            waitForIdle()
            onNodeWithText("Rename").performClick()
            waitForIdle()
            onNode(hasSetTextAction() and hasText("Harbour")).assertExists()
            val field = onNode(hasSetTextAction())
            field.performTextReplacement("Lighthouse")
            field.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()

            actions shouldContain ProjectsAction.Rename(meta.id, "Lighthouse")
        }

    @Test
    fun newProject_asksForAProjectFromTheDefaults() =
        runComposeUiTest {
            showDrawer(ProjectsModel.State(projects = listOf(meta("Harbour"))))

            onNodeWithText("New project").performClick()
            waitForIdle()

            actions shouldContain ProjectsAction.New(copyCurrent = false)
        }

    @Test
    fun drawer_withoutStorage_saysSoAndOffersALink() =
        runComposeUiTest {
            var linkAsked = false
            showDrawer(ProjectsModel.State(storageAvailable = false), onGetLink = { linkAsked = true })

            onNodeWithText("Get a link").performClick()
            waitForIdle()

            onNode(hasText("won’t be saved", substring = true)).assertExists()
            linkAsked shouldBe true
        }

    /** The search is the only text field until a rename opens one. */
    private fun ComposeUiTest.onAllSearchFields() = onAllNodes(hasSetTextAction())

    private fun ComposeUiTest.showDrawer(
        state: ProjectsModel.State,
        now: Long = 0,
        onGetLink: () -> Unit = {},
    ) {
        setContent {
            Themed {
                ProjectsDrawer(
                    visible = true,
                    state = state,
                    now = now,
                    onAction = { action -> actions += action },
                    onGetLink = onGetLink,
                    onDismissRequest = {},
                )
            }
        }
        waitForIdle()
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) {
    val document = ThemeDocument.Default
    BuilderTheme(
        skin = Skin(library = Library.Material3, expressive = false),
        result = ThemeResolver().resolve(document),
        isDark = false,
        reducedMotion = true,
    ) {
        ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) { content() }
    }
}

private fun metas(count: Int): List<ProjectMeta> = List(count) { index -> meta("Theme $index", id = "p$index") }

private fun meta(
    name: String,
    id: String = "p-$name",
    updatedAt: Long = 0,
    library: Library = Library.Material3,
): ProjectMeta =
    ProjectMeta(
        id = id,
        name = name,
        createdAt = 0,
        updatedAt = updatedAt,
        library = library,
        previewColors = List(ProjectMeta.PREVIEW_COLORS) { Argb(0xFF6750A4.toInt()) },
    )
