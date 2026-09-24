package com.materialkolor.sample.shared.state

import com.materialkolor.sample.shared.model.TaskFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleStoreTest {
    @Test
    fun store_startsFromInitial() {
        assertEquals(SampleState.Initial, SampleStore().state)
    }

    @Test
    fun dispatch_runsTheActionThroughTheReducer() {
        val store = SampleStore()

        store.dispatch(SampleAction.ToggleTask(2))
        store.dispatch(SampleAction.SelectFilter(TaskFilter.Active))

        val expected = SampleState.Initial
            .reduce(SampleAction.ToggleTask(2))
            .reduce(SampleAction.SelectFilter(TaskFilter.Active))
        assertEquals(expected, store.state)
    }
}
