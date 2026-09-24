package com.materialkolor.sample.shared.state

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleStateTest {
    @Test
    fun initial_matchesTheReadmeTable() {
        val expected = listOf(
            Task(id = 1, title = "Pick a seed color", tag = TaskTag.Personal, isDone = true),
            Task(id = 2, title = "Try the dark theme", tag = TaskTag.Personal, isDone = false),
            Task(id = 3, title = "Review the pull request", tag = TaskTag.Work, isDone = false),
            Task(id = 4, title = "Write the release notes", tag = TaskTag.Work, isDone = true),
            Task(id = 5, title = "Buy oat milk", tag = TaskTag.Errand, isDone = false),
        )

        with(SampleState.Initial) {
            assertEquals(expected, tasks)
            assertEquals(TaskFilter.All, filter)
            assertEquals(TaskTag.Personal, composerTag)
            assertEquals(SampleSeed.Violet, seed)
            assertEquals(ThemeMode.System, mode)
            assertEquals(AppSection.Tasks, section)
            assertFalse(isClearDialogVisible)
            assertEquals(6L, nextId)
        }
    }

    @Test
    fun initial_derivesItsCounts() {
        with(SampleState.Initial) {
            assertEquals(5, totalCount)
            assertEquals(2, doneCount)
            assertEquals(3, activeCount)
            assertEquals(0.4f, progress)
            assertTrue(canClearDone)
            assertEquals(5, count(TaskFilter.All))
            assertEquals(3, count(TaskFilter.Active))
            assertEquals(2, count(TaskFilter.Done))
            assertEquals(tasks, visibleTasks)
        }
    }

    @Test
    fun visibleTasks_followTheFilter() {
        val active = SampleState.Initial.copy(filter = TaskFilter.Active)
        val done = SampleState.Initial.copy(filter = TaskFilter.Done)

        assertEquals(listOf(2L, 3L, 5L), active.visibleTasks.map(Task::id))
        assertEquals(listOf(1L, 4L), done.visibleTasks.map(Task::id))
    }

    @Test
    fun emptyList_hasNoProgressAndNothingToClear() {
        val empty = SampleState.Initial.copy(tasks = emptyList())

        assertEquals(0, empty.totalCount)
        assertEquals(0, empty.doneCount)
        assertEquals(0, empty.activeCount)
        assertEquals(0f, empty.progress)
        assertFalse(empty.canClearDone)
        assertEquals(emptyList(), empty.visibleTasks)
    }

    @Test
    fun allDone_isFullProgress() {
        val allDone = SampleState.Initial.copy(
            tasks = SampleState.Initial.tasks.map { task -> task.copy(isDone = true) },
        )

        assertEquals(1f, allDone.progress)
        assertEquals(0, allDone.activeCount)
        assertEquals(0, allDone.count(TaskFilter.Active))
    }
}
