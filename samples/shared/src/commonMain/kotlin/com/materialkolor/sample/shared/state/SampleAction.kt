package com.materialkolor.sample.shared.state

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

/**
 * Everything a sample can ask of the state. [reduce] says what each one does.
 */
public sealed interface SampleAction {
    /**
     * Adds a task with the composer tag at the end of the list. The title is trimmed first, and a blank one is
     * ignored.
     */
    public data class AddTask(
        public val title: String,
    ) : SampleAction

    /** Flips the task with [id] between done and not done. */
    public data class ToggleTask(
        public val id: Long,
    ) : SampleAction

    /** Removes the task with [id]. */
    public data class DeleteTask(
        public val id: Long,
    ) : SampleAction

    /** Shows only the tasks [filter] lets through. */
    public data class SelectFilter(
        public val filter: TaskFilter,
    ) : SampleAction

    /** Picks the tag the next added task gets. */
    public data class SelectComposerTag(
        public val tag: TaskTag,
    ) : SampleAction

    /** Opens the dialog that asks before clearing finished tasks, if there are any. */
    public data object RequestClearDone : SampleAction

    /** Removes every finished task and closes the dialog. */
    public data object ConfirmClearDone : SampleAction

    /** Closes the dialog and keeps every task. */
    public data object DismissClearDone : SampleAction

    /** Regenerates the theme from [seed]. */
    public data class SelectSeed(
        public val seed: SampleSeed,
    ) : SampleAction

    /** Switches between light, dark and system. */
    public data class SelectMode(
        public val mode: ThemeMode,
    ) : SampleAction

    /** Shows [section] under the header. */
    public data class SelectSection(
        public val section: AppSection,
    ) : SampleAction
}
