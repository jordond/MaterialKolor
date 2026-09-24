package com.materialkolor.sample.shared.ui

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

/**
 * Every string the Tasks app shows. Samples read their labels from here instead of writing their own, and the
 * contract checks the UI against the same values.
 */
public object SampleCopy {
    /** The title at the top of the window. */
    public val appTitle: String = "Tasks"

    /** The heading over the seed swatches. */
    public val seedGroup: String = "Seed"

    /** The heading over the mode picker. */
    public val modeGroup: String = "Mode"

    /** The placeholder in the empty task field. */
    public val inputPlaceholder: String = "Add a task"

    /** The button that adds the typed task. */
    public val add: String = "Add"

    /** The button that asks before removing finished tasks. */
    public val clearDone: String = "Clear done"

    /** The title of the dialog that asks before clearing. */
    public val dialogTitle: String = "Clear finished tasks?"

    /** The dialog button that clears. */
    public val dialogConfirm: String = "Clear"

    /** The dialog button that keeps every task. */
    public val dialogCancel: String = "Keep"

    /**
     * The summary line over the progress bar, for example "2 of 5 done".
     */
    public fun summary(
        done: Int,
        total: Int,
    ): String = "$done of $total done"

    /**
     * The footer count, for example "3 left".
     */
    public fun remaining(count: Int): String = "$count left"

    /**
     * What the list says when [filter] matches nothing.
     */
    public fun empty(filter: TaskFilter): String =
        when (filter) {
            TaskFilter.All -> "No tasks yet. Add one above."
            TaskFilter.Active -> "Nothing left to do."
            TaskFilter.Done -> "Nothing finished yet."
        }

    /**
     * The dialog body, for example "This removes 2 finished tasks for good."
     */
    public fun dialogBody(count: Int): String {
        val tasks = if (count == 1) "task" else "tasks"
        return "This removes $count finished $tasks for good."
    }

    /**
     * The content description of the delete button on the task called [title].
     */
    public fun deleteTask(title: String): String = "Delete $title"

    /**
     * The name of [section] on its tab.
     */
    public fun label(section: AppSection): String =
        when (section) {
            AppSection.Tasks -> "Tasks"
            AppSection.Palette -> "Palette"
        }

    /**
     * The name of [filter] on its option.
     */
    public fun label(filter: TaskFilter): String =
        when (filter) {
            TaskFilter.All -> "All"
            TaskFilter.Active -> "Active"
            TaskFilter.Done -> "Done"
        }

    /**
     * The name of [mode] on its option.
     */
    public fun label(mode: ThemeMode): String =
        when (mode) {
            ThemeMode.System -> "System"
            ThemeMode.Light -> "Light"
            ThemeMode.Dark -> "Dark"
        }

    /**
     * The name of [tag], on the composer option and on the chip of every task that has it.
     */
    public fun label(tag: TaskTag): String =
        when (tag) {
            TaskTag.Personal -> "Personal"
            TaskTag.Work -> "Work"
            TaskTag.Errand -> "Errand"
        }

    /**
     * The name of [seed], which is also the content description of its swatch.
     */
    public fun label(seed: SampleSeed): String = seed.name
}
