package com.materialkolor.sample.shared.ui

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

public object SampleCopy {
    public val appTitle: String = "Tasks"
    public val seedGroup: String = "Seed"
    public val modeGroup: String = "Mode"
    public val inputPlaceholder: String = "Add a task"
    public val add: String = "Add"
    public val clearDone: String = "Clear done"
    public val dialogTitle: String = "Clear finished tasks?"
    public val dialogConfirm: String = "Clear"
    public val dialogCancel: String = "Keep"

    public fun summary(
        done: Int,
        total: Int,
    ): String = "$done of $total done"

    public fun remaining(count: Int): String = "$count left"

    public fun empty(filter: TaskFilter): String =
        when (filter) {
            TaskFilter.All -> "No tasks yet. Add one above."
            TaskFilter.Active -> "Nothing left to do."
            TaskFilter.Done -> "Nothing finished yet."
        }

    public fun dialogBody(count: Int): String {
        val tasks = if (count == 1) "task" else "tasks"
        return "This removes $count finished $tasks for good."
    }

    public fun label(section: AppSection): String =
        when (section) {
            AppSection.Tasks -> "Tasks"
            AppSection.Palette -> "Palette"
        }

    public fun label(filter: TaskFilter): String =
        when (filter) {
            TaskFilter.All -> "All"
            TaskFilter.Active -> "Active"
            TaskFilter.Done -> "Done"
        }

    public fun label(mode: ThemeMode): String =
        when (mode) {
            ThemeMode.System -> "System"
            ThemeMode.Light -> "Light"
            ThemeMode.Dark -> "Dark"
        }

    public fun label(tag: TaskTag): String =
        when (tag) {
            TaskTag.Personal -> "Personal"
            TaskTag.Work -> "Work"
            TaskTag.Errand -> "Errand"
        }

    public fun label(seed: SampleSeed): String = seed.name
}
