package com.materialkolor.sample.shared.model

import androidx.compose.runtime.Immutable

/**
 * @property[title] Already trimmed.
 */
@Immutable
public data class Task(
    public val id: Long,
    public val title: String,
    public val tag: TaskTag,
    public val isDone: Boolean,
)

public fun canAddTask(title: String): Boolean = title.isNotBlank()
