package com.materialkolor.sample.shared.ui

import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.theme.SampleSeed
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleCopyTest {
    @Test
    fun counts_readAsTheReadmeShowsThem() {
        assertEquals("2 of 5 done", SampleCopy.summary(done = 2, total = 5))
        assertEquals("3 left", SampleCopy.remaining(3))
    }

    @Test
    fun dialogBody_isSingularForOneTask() {
        assertEquals("This removes 1 finished task for good.", SampleCopy.dialogBody(1))
        assertEquals("This removes 2 finished tasks for good.", SampleCopy.dialogBody(2))
    }

    @Test
    fun emptyState_dependsOnTheFilter() {
        assertEquals("No tasks yet. Add one above.", SampleCopy.empty(TaskFilter.All))
        assertEquals("Nothing left to do.", SampleCopy.empty(TaskFilter.Active))
        assertEquals("Nothing finished yet.", SampleCopy.empty(TaskFilter.Done))
    }

    @Test
    fun seedLabel_isTheSeedName() {
        assertEquals("Teal", SampleCopy.label(SampleSeed.Teal))
    }
}
