package com.materialkolor.sample.shared.state

import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.canAddTask

/**
 * Pure, so the same state and action always give the same result.
 */
public fun SampleState.reduce(action: SampleAction): SampleState =
    when (action) {
        is SampleAction.AddTask -> addTask(action.title)
        is SampleAction.ToggleTask -> copy(
            tasks = tasks.map { task ->
                if (task.id == action.id) task.copy(isDone = !task.isDone) else task
            },
        )
        is SampleAction.DeleteTask -> copy(tasks = tasks.filterNot { task -> task.id == action.id })
        is SampleAction.SelectFilter -> copy(filter = action.filter)
        is SampleAction.SelectComposerTag -> copy(composerTag = action.tag)
        SampleAction.RequestClearDone -> if (canClearDone) copy(isClearDialogVisible = true) else this
        SampleAction.ConfirmClearDone -> copy(
            tasks = tasks.filterNot { task -> task.isDone },
            isClearDialogVisible = false,
        )
        SampleAction.DismissClearDone -> copy(isClearDialogVisible = false)
        is SampleAction.SelectSeed -> copy(seed = action.seed)
        is SampleAction.SelectMode -> copy(mode = action.mode)
        is SampleAction.SelectSection -> copy(section = action.section)
    }

private fun SampleState.addTask(title: String): SampleState {
    val trimmed = title.trim()
    if (!canAddTask(trimmed)) return this

    val task = Task(id = nextId, title = trimmed, tag = composerTag, isDone = false)
    return copy(tasks = tasks + task, nextId = nextId + 1)
}
