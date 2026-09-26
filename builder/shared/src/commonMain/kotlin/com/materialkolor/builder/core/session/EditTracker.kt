package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.HistoryRecord

/**
 * The undo steps of the project [ProjectSession] shows, and when this tab last edited it.
 *
 * It decides with [editOutcome] whether an edit saves, and remembers the document last handed to
 * autosave so a release back onto it saves nothing. The time of the last edit is what opens the
 * conflict window for another tab's save. It holds plain values and publishes nothing, and only the
 * UI thread calls it.
 *
 * @param[now] The time in epoch milliseconds, for merging undo steps, the time each step keeps and
 * the time of the last edit.
 */
internal class EditTracker(
    private val now: () -> Long,
) {
    private var steps = History()

    /**
     * When this tab last edited the project, or null when it has not since the project opened.
     */
    var lastEditAt: Long? = null
        private set

    // The document last handed to autosave, or the one the project opened with, and when the edit
    // that made it happened.
    private var committed = ThemeDocument.Default
    private var committedEditAt: Long? = null

    /**
     * What undo and redo can do right now.
     */
    val historyState: HistoryState
        get() = HistoryState(steps.canUndo, steps.canRedo, steps.undoLabel, steps.redoLabel)

    /**
     * Start over with [steps] for a project that opened on [document].
     */
    fun reset(
        steps: History,
        document: ThemeDocument,
    ) {
        this.steps = steps
        lastEditAt = null
        committed = document
        committedEditAt = null
    }

    /**
     * Record [change] from [before] to [after] for undo.
     *
     * @return Whether [after] should be saved.
     */
    fun edit(
        change: DocumentChange,
        phase: EditPhase,
        before: ThemeDocument,
        after: ThemeDocument,
    ): Boolean {
        val time = now()
        steps.record(before, after, change, phase, time)
        return when (editOutcome(phase, before, after, committed)) {
            EditOutcome.ReleasedOntoCommitted -> {
                lastEditAt = committedEditAt
                false
            }
            // A discrete change that left the document as it was is not an edit.
            EditOutcome.Unchanged -> {
                false
            }
            EditOutcome.Drag -> {
                lastEditAt = time
                false
            }
            EditOutcome.Commit -> {
                lastEditAt = time
                true
            }
        }
    }

    /**
     * Step back once and count it as an edit.
     *
     * @return The document to show, or null when there is nothing to undo.
     */
    fun undo(): ThemeDocument? = steps.undo()?.also { stamp() }

    /**
     * Step forward once and count it as an edit.
     *
     * @return The document to show, or null when there is nothing to redo.
     */
    fun redo(): ThemeDocument? = steps.redo()?.also { stamp() }

    /**
     * Move to [cursor] at once and count it as an edit.
     *
     * @return The document to show, or null for a cursor past the steps or the one the history is at.
     */
    fun jumpTo(cursor: Int): ThemeDocument? {
        if (cursor !in 0..steps.size) return null
        return steps.jumpTo(cursor)?.also { stamp() }
    }

    /**
     * Record another tab's [theirs] as a step from [mine], without counting it as an edit here.
     */
    fun takeTheirs(
        mine: ThemeDocument,
        theirs: ThemeDocument,
    ) {
        steps.record(mine, theirs, DocumentChange.Replace(theirs), EditPhase.Discrete, now())
    }

    /**
     * Mark [document] as the one handed to autosave.
     *
     * @return The steps to save along with it.
     */
    fun commit(document: ThemeDocument): HistoryRecord {
        committed = document
        committedEditAt = lastEditAt
        return HistoryRecord(steps.persisted())
    }

    /**
     * The steps as they stand now, starting from [showing] when there are none.
     */
    fun timeline(showing: ThemeDocument): Timeline {
        val entries = steps.entries
        return Timeline(
            cursor = steps.cursor,
            now = now(),
            start = entries.firstOrNull()?.before ?: showing,
            steps = entries,
        )
    }

    private fun stamp() {
        lastEditAt = now()
    }
}
