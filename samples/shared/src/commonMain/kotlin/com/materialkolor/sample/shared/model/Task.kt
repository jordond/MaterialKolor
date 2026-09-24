package com.materialkolor.sample.shared.model

import androidx.compose.runtime.Immutable

/**
 * One task on the list.
 *
 * @property[id] Stable identity. Actions and test tags refer to a task by it.
 * @property[title] What the task says, already trimmed.
 * @property[tag] The group the task belongs to.
 * @property[isDone] Whether the task is finished.
 */
@Immutable
public data class Task(
    public val id: Long,
    public val title: String,
    public val tag: TaskTag,
    public val isDone: Boolean,
)

/**
 * Whether [title] is worth adding. Anything but blank is.
 */
public fun canAddTask(title: String): Boolean = title.isNotBlank()
