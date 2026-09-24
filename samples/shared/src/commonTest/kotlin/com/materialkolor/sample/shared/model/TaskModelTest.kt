package com.materialkolor.sample.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskModelTest {
    private val open = Task(id = 1, title = "Open", tag = TaskTag.Work, isDone = false)
    private val done = Task(id = 2, title = "Done", tag = TaskTag.Work, isDone = true)

    @Test
    fun filters_letTheRightTasksThrough() {
        assertEquals(listOf(true, true), listOf(open, done).map(TaskFilter.All::matches))
        assertEquals(listOf(true, false), listOf(open, done).map(TaskFilter.Active::matches))
        assertEquals(listOf(false, true), listOf(open, done).map(TaskFilter.Done::matches))
    }

    @Test
    fun canAddTask_rejectsBlankTitles() {
        assertFalse(canAddTask(""))
        assertFalse(canAddTask("   "))
        assertFalse(canAddTask("\t\n"))
        assertTrue(canAddTask("Water the plants"))
        assertTrue(canAddTask("  padded  "))
    }
}
