package com.materialkolor.sample.shared.ui

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

/**
 * The test tags every sample puts on its nodes, so the `SampleAppContract` in `:samples:testing` can drive each UI
 * the same way.
 *
 * The contract finds nodes only by these tags and checks them only against [SampleCopy] and standard semantics, so
 * the semantics each tag asks for below are part of the contract. Every tag but [Progress] is marked as a control,
 * text or presence tag, and that decides where it goes.
 *
 * - A control is read in the merged tree and clicked in its middle. Put the tag on the node that owns the click, the
 *   selection or the toggle, which is usually what the component's `modifier` parameter lands on.
 * - A text tag is read in the unmerged tree. It passes when the tagged node or any node inside it shows the text.
 * - A presence tag is read in the unmerged tree, and the contract only checks whether it exists.
 *
 * The contract runs in a 1024 by 768 window and expects every visible row to be composed, so a lazy list is fine as
 * long as six rows fit.
 */
public object SampleTags {
    /** Text. The summary line, showing [SampleCopy.summary]. */
    public val Summary: String = "summary"

    /**
     * The progress bar. Exposes `ProgressBarRangeInfo` over 0 to 1 with `current` equal to `SampleState.progress`, on
     * the tagged node or one inside it. `Modifier.progressSemantics` gives you that.
     */
    public val Progress: String = "progress"

    /**
     * Control. The editable text field itself, the node that takes text input. Its IME action is `ImeAction.Done` and
     * adds the task the same way [AddTask] does. The field is empty again once a task is added.
     */
    public val TaskInput: String = "task-input"

    /** Control. The Add button. Clickable, and disabled while the field is blank. */
    public val AddTask: String = "add-task"

    /** Presence. The task list. Exists only on the Tasks section. */
    public val TaskList: String = "task-list"

    /** Text. Shows [SampleCopy.empty] for the current filter. Exists only while the filter matches no task. */
    public val EmptyState: String = "empty-state"

    /** Text. The footer count, showing [SampleCopy.remaining]. */
    public val Remaining: String = "remaining"

    /** Control. The Clear done button. Clickable, and disabled while no task is done. */
    public val ClearDone: String = "clear-done"

    /** Presence. The dialog that asks before clearing. Exists only while it is up. */
    public val ClearDialog: String = "clear-dialog"

    /** Control. The Clear button in the dialog. Clickable. */
    public val ClearConfirm: String = "clear-confirm"

    /** Control. The Keep button in the dialog. Clickable. */
    public val ClearCancel: String = "clear-cancel"

    /** Presence. The Palette section. Exists only while that section is selected. */
    public val Palette: String = "palette"

    /**
     * Control. The composer option for [tag]. Clickable, and exposes `selected`, true only on the composer tag.
     * `Modifier.selectable` gives both.
     */
    public fun composerTag(tag: TaskTag): String = "composer-tag-${tag.name}"

    /**
     * Control. The option for [filter]. Clickable, and exposes `selected`, true only on the current filter.
     */
    public fun filter(filter: TaskFilter): String = "filter-${filter.name}"

    /**
     * Control. The swatch for [seed]. Clickable, exposes `selected`, true only on the current seed, and has
     * [SampleCopy.label] of the seed as its content description.
     */
    public fun seed(seed: SampleSeed): String = "seed-${seed.name}"

    /**
     * Control. The option for [mode]. Clickable, and exposes `selected`, true only on the current mode.
     */
    public fun mode(mode: ThemeMode): String = "mode-${mode.name}"

    /**
     * Control. The tab for [section]. Clickable, and exposes `selected`, true only on the current section.
     */
    public fun section(section: AppSection): String = "section-${section.name}"

    /** Presence. The row of the task with [id]. Exists only while the filter lets that task through. */
    public fun taskRow(id: Long): String = "task-row-$id"

    /** Control. The checkbox of the task with [id]. Toggleable itself, and on while the task is done. */
    public fun taskCheck(id: Long): String = "task-check-$id"

    /** Text. The title of the task with [id]. */
    public fun taskTitle(id: Long): String = "task-title-$id"

    /** Text. The tag chip of the task with [id], showing [SampleCopy.label] of its tag. */
    public fun taskTag(id: Long): String = "task-tag-$id"

    /**
     * Control. The delete button of the task with [id]. Clickable, with [SampleCopy.deleteTask] of the title as content
     * description, set on the button or on the icon inside it.
     */
    public fun taskDelete(id: Long): String = "task-delete-$id"
}
