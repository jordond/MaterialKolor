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
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SampleReducerTest {
    private val initial = SampleState.Initial

    @Test
    fun addTask_appendsWithTheComposerTagAndBumpsTheId() {
        val state = initial
            .reduce(SampleAction.SelectComposerTag(TaskTag.Errand))
            .reduce(SampleAction.AddTask("Water the plants"))

        assertEquals(
            Task(id = 6, title = "Water the plants", tag = TaskTag.Errand, isDone = false),
            state.tasks.last(),
        )
        assertEquals(6, state.totalCount)
        assertEquals(7L, state.nextId)
    }

    @Test
    fun addTask_trimsTheTitle() {
        val state = initial.reduce(SampleAction.AddTask("  Water the plants \n"))

        assertEquals("Water the plants", state.tasks.last().title)
    }

    @Test
    fun addTask_ignoresBlankTitles() {
        assertSame(initial, initial.reduce(SampleAction.AddTask("")))
        assertSame(initial, initial.reduce(SampleAction.AddTask("   ")))
    }

    @Test
    fun addTask_neverReusesAnId() {
        val state = initial
            .reduce(SampleAction.DeleteTask(5))
            .reduce(SampleAction.AddTask("Water the plants"))

        assertEquals(listOf(1L, 2L, 3L, 4L, 6L), state.tasks.map(Task::id))
    }

    @Test
    fun toggleTask_flipsOnlyThatTask() {
        val once = initial.reduce(SampleAction.ToggleTask(2))
        val twice = once.reduce(SampleAction.ToggleTask(2))

        assertEquals(listOf(true, true, false, true, false), once.tasks.map(Task::isDone))
        assertEquals(3, once.doneCount)
        assertEquals(initial, twice)
    }

    @Test
    fun toggleTask_withAnUnknownIdChangesNothing() {
        assertEquals(initial, initial.reduce(SampleAction.ToggleTask(42)))
    }

    @Test
    fun deleteTask_removesOnlyThatTask() {
        val state = initial.reduce(SampleAction.DeleteTask(3))

        assertEquals(listOf(1L, 2L, 4L, 5L), state.tasks.map(Task::id))
        assertEquals(6L, state.nextId)
    }

    @Test
    fun selectFilter_changesTheVisibleTasks() {
        val state = initial.reduce(SampleAction.SelectFilter(TaskFilter.Done))

        assertEquals(TaskFilter.Done, state.filter)
        assertEquals(listOf(1L, 4L), state.visibleTasks.map(Task::id))
        assertEquals(initial.tasks, state.tasks)
    }

    @Test
    fun selectComposerTag_changesOnlyTheComposerTag() {
        val state = initial.reduce(SampleAction.SelectComposerTag(TaskTag.Work))

        assertEquals(initial.copy(composerTag = TaskTag.Work), state)
    }

    @Test
    fun requestClearDone_opensTheDialog() {
        val state = initial.reduce(SampleAction.RequestClearDone)

        assertTrue(state.isClearDialogVisible)
        assertEquals(initial.tasks, state.tasks)
    }

    @Test
    fun requestClearDone_doesNothingWhenNothingIsDone() {
        val nothingDone = initial
            .reduce(SampleAction.ToggleTask(1))
            .reduce(SampleAction.ToggleTask(4))

        assertSame(nothingDone, nothingDone.reduce(SampleAction.RequestClearDone))
    }

    @Test
    fun confirmClearDone_removesFinishedTasksAndClosesTheDialog() {
        val state = initial
            .reduce(SampleAction.RequestClearDone)
            .reduce(SampleAction.ConfirmClearDone)

        assertEquals(listOf(2L, 3L, 5L), state.tasks.map(Task::id))
        assertFalse(state.isClearDialogVisible)
        assertFalse(state.canClearDone)
    }

    @Test
    fun dismissClearDone_keepsEveryTaskAndClosesTheDialog() {
        val state = initial
            .reduce(SampleAction.RequestClearDone)
            .reduce(SampleAction.DismissClearDone)

        assertEquals(initial, state)
    }

    @Test
    fun selectSeed_changesOnlyTheSeed() {
        val state = initial.reduce(SampleAction.SelectSeed(SampleSeed.Teal))

        assertEquals(initial.copy(seed = SampleSeed.Teal), state)
    }

    @Test
    fun selectMode_changesOnlyTheMode() {
        val state = initial.reduce(SampleAction.SelectMode(ThemeMode.Dark))

        assertEquals(initial.copy(mode = ThemeMode.Dark), state)
    }

    @Test
    fun selectSection_changesOnlyTheSection() {
        val state = initial.reduce(SampleAction.SelectSection(AppSection.Palette))

        assertEquals(initial.copy(section = AppSection.Palette), state)
    }
}
