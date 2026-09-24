package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One step in the undo history, kept as the document on either side of it.
 *
 * The history holds snapshots rather than the edits themselves, so undoing never has to work an
 * edit backwards and a saved history reads back without knowing which edits exist.
 *
 * @property[before] The document as it was before the step.
 * @property[after] The document as the step left it.
 * @property[label] What the undo and redo buttons say about the step.
 * @property[at] When the step last changed, in epoch milliseconds from the session clock. A step
 * that took several edits, a drag or quick typing, carries its latest one. Steps saved before the
 * history kept times read back without one.
 */
@Serializable
public data class HistoryEntry(
    @SerialName("before")
    public val before: ThemeDocument,
    @SerialName("after")
    public val after: ThemeDocument,
    @SerialName("label")
    public val label: ChangeLabel,
    @SerialName("at")
    public val at: Long? = null,
)
