package com.materialkolor.sample.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import kotlin.test.Test

/**
 * The behaviour every Tasks sample shares, written once and run against each UI.
 *
 * Subclass it in a sample's `jvmTest` and render the whole app from [Content]. JUnit picks up the inherited tests.
 *
 * ```kotlin
 * class UnstyledSampleAppTest : SampleAppContract() {
 *     @Composable
 *     override fun Content() {
 *         UnstyledSampleApp()
 *     }
 * }
 * ```
 *
 * Every test starts from `SampleState.Initial`, finds nodes only through [SampleTags] and checks them only against
 * [SampleCopy] and standard semantics. [SampleTags] says what each tagged node has to expose.
 */
@OptIn(ExperimentalTestApi::class)
public abstract class SampleAppContract {
    /**
     * The whole sample app, the way its `Main.kt` shows it.
     */
    @Composable
    protected abstract fun Content()

    @Test
    public fun startingState_showsTheReadmeTasks() {
        runSampleTest {
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 5))
            assertText(SampleTags.Remaining, SampleCopy.remaining(3))
            assertProgress(0.4f)
            for ((id, title) in StartingTitles) {
                assertPresent(SampleTags.taskRow(id))
                assertText(SampleTags.taskTitle(id), title)
            }
            assertText(SampleTags.taskTag(1), SampleCopy.label(TaskTag.Personal))
            assertText(SampleTags.taskTag(3), SampleCopy.label(TaskTag.Work))
            assertText(SampleTags.taskTag(5), SampleCopy.label(TaskTag.Errand))
            assertAbsent(SampleTags.taskRow(6))
            assertAbsent(SampleTags.EmptyState)
            assertAbsent(SampleTags.ClearDialog)
        }
    }

    @Test
    public fun addTask_isDisabledUntilTheFieldHasText() {
        runSampleTest {
            val input = onNodeWithTag(SampleTags.TaskInput)
            val add = onNodeWithTag(SampleTags.AddTask)
            add.assert(hasClickAction())
            add.assertIsNotEnabled()

            input.performTextInput("   ")
            waitForIdle()
            add.assertIsNotEnabled()

            input.performImeAction()
            waitForIdle()
            assertAbsent(SampleTags.taskRow(6))

            input.performTextClearance()
            input.performTextInput("Water the plants")
            waitForIdle()
            add.assertIsEnabled()
        }
    }

    @Test
    public fun addTask_withTheButtonAppendsAndClearsTheField() {
        runSampleTest {
            onNodeWithTag(SampleTags.TaskInput).performTextInput("Water the plants")
            onNodeWithTag(SampleTags.AddTask).performClick()
            waitForIdle()

            assertPresent(SampleTags.taskRow(6))
            assertText(SampleTags.taskTitle(6), "Water the plants")
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 6))
            onNodeWithTag(SampleTags.TaskInput).assert(hasEditableText(""))
            onNodeWithTag(SampleTags.AddTask).assertIsNotEnabled()
        }
    }

    @Test
    public fun addTask_withTheImeActionAppendsAndClearsTheField() {
        runSampleTest {
            val input = onNodeWithTag(SampleTags.TaskInput)
            input.assert(hasImeAction(ImeAction.Done))

            input.performTextInput("Water the plants")
            input.performImeAction()
            waitForIdle()

            assertPresent(SampleTags.taskRow(6))
            assertText(SampleTags.taskTitle(6), "Water the plants")
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 6))
            input.assert(hasEditableText(""))
        }
    }

    @Test
    public fun addTask_usesTheSelectedComposerTag() {
        runSampleTest {
            onNodeWithTag(SampleTags.composerTag(TaskTag.Personal)).assertIsSelected()
            onNodeWithTag(SampleTags.composerTag(TaskTag.Work)).assertIsNotSelected()

            onNodeWithTag(SampleTags.composerTag(TaskTag.Work)).performClick()
            waitForIdle()
            onNodeWithTag(SampleTags.composerTag(TaskTag.Work)).assertIsSelected()
            onNodeWithTag(SampleTags.composerTag(TaskTag.Personal)).assertIsNotSelected()

            onNodeWithTag(SampleTags.TaskInput).performTextInput("Book a table")
            onNodeWithTag(SampleTags.AddTask).performClick()
            waitForIdle()

            assertText(SampleTags.taskTag(6), SampleCopy.label(TaskTag.Work))
        }
    }

    @Test
    public fun toggleTask_flipsTheCheckAndTheCounts() {
        runSampleTest {
            val check = onNodeWithTag(SampleTags.taskCheck(2))
            onNodeWithTag(SampleTags.taskCheck(1)).assertIsOn()
            check.assertIsOff()

            check.performClick()
            waitForIdle()
            check.assertIsOn()
            assertText(SampleTags.Summary, SampleCopy.summary(done = 3, total = 5))
            assertText(SampleTags.Remaining, SampleCopy.remaining(2))
            assertProgress(0.6f)

            check.performClick()
            waitForIdle()
            check.assertIsOff()
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 5))
            assertText(SampleTags.Remaining, SampleCopy.remaining(3))
        }
    }

    @Test
    public fun filters_showOnlyTheMatchingRows() {
        runSampleTest {
            onNodeWithTag(SampleTags.filter(TaskFilter.All)).assertIsSelected()

            selectFilter(TaskFilter.Active)
            assertRows(present = listOf(2, 3, 5), absent = listOf(1, 4))

            selectFilter(TaskFilter.Done)
            assertRows(present = listOf(1, 4), absent = listOf(2, 3, 5))

            selectFilter(TaskFilter.All)
            assertRows(present = listOf(1, 2, 3, 4, 5), absent = emptyList())
        }
    }

    @Test
    public fun emptyState_showsWhenTheFilterMatchesNothing() {
        runSampleTest {
            onNodeWithTag(SampleTags.taskCheck(1)).performClick()
            onNodeWithTag(SampleTags.taskCheck(4)).performClick()
            waitForIdle()
            assertAbsent(SampleTags.EmptyState)

            selectFilter(TaskFilter.Done)
            assertText(SampleTags.EmptyState, SampleCopy.empty(TaskFilter.Done))
            assertRows(present = emptyList(), absent = listOf(1, 2, 3, 4, 5))

            selectFilter(TaskFilter.All)
            assertAbsent(SampleTags.EmptyState)
        }
    }

    @Test
    public fun deleteTask_removesTheRow() {
        runSampleTest {
            val delete = onNodeWithTag(SampleTags.taskDelete(3))
            delete.assert(hasClickAction())
            delete.assert(hasContentDescription(SampleCopy.deleteTask("Review the pull request")))

            delete.performClick()
            waitForIdle()

            assertAbsent(SampleTags.taskRow(3))
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 4))
            assertText(SampleTags.Remaining, SampleCopy.remaining(2))
        }
    }

    @Test
    public fun clearDone_asksFirstAndThenClears() {
        runSampleTest {
            val clearDone = onNodeWithTag(SampleTags.ClearDone)
            clearDone.assert(hasClickAction())
            clearDone.assertIsEnabled()

            clearDone.performClick()
            waitForIdle()
            assertPresent(SampleTags.ClearDialog)
            onNodeWithText(SampleCopy.dialogTitle, useUnmergedTree = true).assertExists()
            onNodeWithText(SampleCopy.dialogBody(2), useUnmergedTree = true).assertExists()

            onNodeWithTag(SampleTags.ClearCancel).performClick()
            waitForIdle()
            assertAbsent(SampleTags.ClearDialog)
            assertRows(present = listOf(1, 2, 3, 4, 5), absent = emptyList())
            assertText(SampleTags.Summary, SampleCopy.summary(done = 2, total = 5))

            clearDone.performClick()
            waitForIdle()
            assertPresent(SampleTags.ClearDialog)

            onNodeWithTag(SampleTags.ClearConfirm).performClick()
            waitForIdle()
            assertAbsent(SampleTags.ClearDialog)
            assertRows(present = listOf(2, 3, 5), absent = listOf(1, 4))
            assertText(SampleTags.Summary, SampleCopy.summary(done = 0, total = 3))
            clearDone.assertIsNotEnabled()
        }
    }

    @Test
    public fun seed_movesTheSelection() {
        runSampleTest {
            for (seed in SampleSeed.entries) {
                val node = onNodeWithTag(SampleTags.seed(seed))
                node.assert(hasClickAction())
                node.assert(hasContentDescription(SampleCopy.label(seed)))
            }
            assertOnlySelected(SampleSeed.entries, SampleSeed.Violet, SampleTags::seed)

            onNodeWithTag(SampleTags.seed(SampleSeed.Teal)).performClick()
            waitForIdle()
            assertOnlySelected(SampleSeed.entries, SampleSeed.Teal, SampleTags::seed)
        }
    }

    @Test
    public fun mode_movesTheSelection() {
        runSampleTest {
            assertOnlySelected(ThemeMode.entries, ThemeMode.System, SampleTags::mode)

            onNodeWithTag(SampleTags.mode(ThemeMode.Dark)).performClick()
            waitForIdle()
            assertOnlySelected(ThemeMode.entries, ThemeMode.Dark, SampleTags::mode)

            onNodeWithTag(SampleTags.mode(ThemeMode.Light)).performClick()
            waitForIdle()
            assertOnlySelected(ThemeMode.entries, ThemeMode.Light, SampleTags::mode)
        }
    }

    @Test
    public fun section_switchesBetweenTasksAndPalette() {
        runSampleTest {
            assertOnlySelected(AppSection.entries, AppSection.Tasks, SampleTags::section)
            assertPresent(SampleTags.TaskList)
            assertAbsent(SampleTags.Palette)

            onNodeWithTag(SampleTags.section(AppSection.Palette)).performClick()
            waitForIdle()
            assertOnlySelected(AppSection.entries, AppSection.Palette, SampleTags::section)
            assertPresent(SampleTags.Palette)
            assertAbsent(SampleTags.TaskList)

            onNodeWithTag(SampleTags.section(AppSection.Tasks)).performClick()
            waitForIdle()
            assertOnlySelected(AppSection.entries, AppSection.Tasks, SampleTags::section)
            assertPresent(SampleTags.TaskList)
            assertAbsent(SampleTags.Palette)
        }
    }

    private fun runSampleTest(block: ComposeUiTest.() -> Unit) {
        runComposeUiTest {
            setContent { Content() }
            waitForIdle()
            block()
        }
    }

    private fun ComposeUiTest.selectFilter(filter: TaskFilter) {
        onNodeWithTag(SampleTags.filter(filter)).performClick()
        waitForIdle()
        assertOnlySelected(TaskFilter.entries, filter, SampleTags::filter)
    }

    private fun ComposeUiTest.assertRows(
        present: List<Long>,
        absent: List<Long>,
    ) {
        for (id in present) assertPresent(SampleTags.taskRow(id))
        for (id in absent) assertAbsent(SampleTags.taskRow(id))
    }
}

private val StartingTitles = mapOf(
    1L to "Pick a seed color",
    2L to "Try the dark theme",
    3L to "Review the pull request",
    4L to "Write the release notes",
    5L to "Buy oat milk",
)
