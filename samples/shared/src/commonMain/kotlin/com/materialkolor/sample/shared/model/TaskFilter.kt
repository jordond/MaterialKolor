package com.materialkolor.sample.shared.model

/**
 * Which tasks the list shows.
 */
public enum class TaskFilter {
    /** Every task. */
    All,

    /** Tasks that are not finished yet. */
    Active,

    /** Finished tasks. */
    Done,
    ;

    /**
     * Whether this filter lets [task] through.
     */
    public fun matches(task: Task): Boolean =
        when (this) {
            All -> true
            Active -> !task.isDone
            Done -> task.isDone
        }
}
