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
 */
@Serializable
public data class HistoryEntry(
    @SerialName("before")
    public val before: ThemeDocument,
    @SerialName("after")
    public val after: ThemeDocument,
    @SerialName("label")
    public val label: ChangeLabel,
)
