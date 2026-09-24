package com.materialkolor.sample.shared.state

import androidx.compose.runtime.Immutable
import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

/**
 * Everything the Tasks app shows, in one value. Change it only through [reduce].
 *
 * @property[tasks] In the order they were added.
 * @property[nextId] Only grows, so a deleted task's id is never handed out again.
 */
@Immutable
public data class SampleState(
    public val tasks: List<Task>,
    public val filter: TaskFilter,
    public val composerTag: TaskTag,
    public val seed: SampleSeed,
    public val mode: ThemeMode,
    public val section: AppSection,
    public val isClearDialogVisible: Boolean,
    public val nextId: Long,
) {
    public val visibleTasks: List<Task> = tasks.filter(filter::matches)

    public val totalCount: Int = tasks.size

    public val doneCount: Int = tasks.count { task -> task.isDone }

    public val activeCount: Int = totalCount - doneCount

    /** From 0 to 1. An empty list counts as 0. */
    public val progress: Float = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount

    public val canClearDone: Boolean = doneCount > 0

    /** How many tasks [filter] lets through, whichever filter is selected. */
    public fun count(filter: TaskFilter): Int = tasks.count(filter::matches)

    public companion object {
        /** The samples README lists these tasks, so change them together. */
        public val Initial: SampleState = SampleState(
            tasks = listOf(
                Task(id = 1, title = "Pick a seed color", tag = TaskTag.Personal, isDone = true),
                Task(id = 2, title = "Try the dark theme", tag = TaskTag.Personal, isDone = false),
                Task(id = 3, title = "Review the pull request", tag = TaskTag.Work, isDone = false),
                Task(id = 4, title = "Write the release notes", tag = TaskTag.Work, isDone = true),
                Task(id = 5, title = "Buy oat milk", tag = TaskTag.Errand, isDone = false),
            ),
            filter = TaskFilter.All,
            composerTag = TaskTag.Personal,
            seed = SampleSeed.Violet,
            mode = ThemeMode.System,
            section = AppSection.Tasks,
            isClearDialogVisible = false,
            nextId = 6,
        )
    }
}
