package com.materialkolor.sample.shared.model

public enum class TaskFilter {
    All,

    Active,

    Done,
    ;

    public fun matches(task: Task): Boolean =
        when (this) {
            All -> true
            Active -> !task.isDone
            Done -> task.isDone
        }
}
