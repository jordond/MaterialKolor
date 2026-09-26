package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectRecord

/**
 * What an edit to the document amounts to, once it is showing.
 */
internal sealed interface EditOutcome {
    /**
     * A step of a drag that is still going. It counts as an edit, and the save waits for the release.
     */
    data object Drag : EditOutcome

    /**
     * A release back onto the document last committed, like a cancelled picker or a slider dragged back
     * to its start. It saves nothing and the last edit is the one that made that document.
     */
    data object ReleasedOntoCommitted : EditOutcome

    /**
     * A discrete change that left the document as it was. It saves nothing and is not an edit.
     */
    data object Unchanged : EditOutcome

    /**
     * An edit that counts and is saved.
     */
    data object Commit : EditOutcome
}

/**
 * What another tab's save of the open project does here.
 */
internal sealed interface IncomingSave {
    /**
     * This tab wrote it, or it is no newer than the record held.
     */
    data object Ignore : IncomingSave

    /**
     * It carries the document showing already. Its record is taken and any conflict goes away.
     */
    data object Matches : IncomingSave

    /**
     * This tab edited lately or a conflict is up already, so it comes up as the conflict.
     */
    data object RaiseConflict : IncomingSave

    /**
     * It comes in as an undo step, so Undo brings this tab's document back.
     */
    data object Adopt : IncomingSave
}

/**
 * Tell what an edit in [phase] that turned [before] into [after] amounts to, with [committed] the
 * document last handed to autosave or the one the project opened with.
 */
internal fun editOutcome(
    phase: EditPhase,
    before: ThemeDocument,
    after: ThemeDocument,
    committed: ThemeDocument,
): EditOutcome =
    when {
        phase == EditPhase.Released && after == committed -> EditOutcome.ReleasedOntoCommitted
        phase == EditPhase.Discrete && after == before -> EditOutcome.Unchanged
        phase == EditPhase.Dragging -> EditOutcome.Drag
        else -> EditOutcome.Commit
    }

/**
 * Tell what [incoming], a save of the open project that arrived from storage, does here.
 *
 * @param[held] The record this tab last saved or read, null before the first save.
 * @param[tabId] This tab, so its own saves coming back are left alone.
 * @param[document] The document showing.
 * @param[conflicted] Whether a conflict is up.
 * @param[lastEditAt] When this tab last edited, in epoch milliseconds, or null.
 * @param[now] The time now in epoch milliseconds, for the conflict window.
 */
internal fun incomingSave(
    held: ProjectRecord?,
    incoming: ProjectRecord,
    tabId: String,
    document: ThemeDocument,
    conflicted: Boolean,
    lastEditAt: Long?,
    now: Long,
): IncomingSave {
    if (incoming.writerTab == tabId) return IncomingSave.Ignore
    if (held != null && incoming.revision <= held.revision) return IncomingSave.Ignore
    if (incoming.document == document) return IncomingSave.Matches
    // A conflict that is up takes the newer save, so Keep mine and Load theirs both see it.
    val editedLately = lastEditAt?.let { at -> now - at < CONFLICT_WINDOW_MILLIS } == true
    return if (conflicted || editedLately) IncomingSave.RaiseConflict else IncomingSave.Adopt
}
