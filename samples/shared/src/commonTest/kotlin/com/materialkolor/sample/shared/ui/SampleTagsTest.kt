package com.materialkolor.sample.shared.ui

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleTagsTest {
    @Test
    fun tags_areStableStrings() {
        assertEquals("task-check-2", SampleTags.taskCheck(2))
        assertEquals("seed-Teal", SampleTags.seed(SampleSeed.Teal))
        assertEquals("composer-tag-Work", SampleTags.composerTag(TaskTag.Work))
        assertEquals("filter-Done", SampleTags.filter(TaskFilter.Done))
        assertEquals("mode-Dark", SampleTags.mode(ThemeMode.Dark))
        assertEquals("section-Palette", SampleTags.section(AppSection.Palette))
    }

    @Test
    fun tags_neverCollide() {
        val tags = buildList {
            add(SampleTags.Summary)
            add(SampleTags.Progress)
            add(SampleTags.TaskInput)
            add(SampleTags.AddTask)
            add(SampleTags.TaskList)
            add(SampleTags.EmptyState)
            add(SampleTags.Remaining)
            add(SampleTags.ClearDone)
            add(SampleTags.ClearDialog)
            add(SampleTags.ClearConfirm)
            add(SampleTags.ClearCancel)
            add(SampleTags.Palette)
            TaskTag.entries.mapTo(this, SampleTags::composerTag)
            TaskFilter.entries.mapTo(this, SampleTags::filter)
            SampleSeed.entries.mapTo(this, SampleTags::seed)
            ThemeMode.entries.mapTo(this, SampleTags::mode)
            AppSection.entries.mapTo(this, SampleTags::section)
            for (id in 1L..6L) {
                add(SampleTags.taskRow(id))
                add(SampleTags.taskCheck(id))
                add(SampleTags.taskTitle(id))
                add(SampleTags.taskTag(id))
                add(SampleTags.taskDelete(id))
            }
        }

        assertEquals(tags.size, tags.toSet().size)
    }
}
