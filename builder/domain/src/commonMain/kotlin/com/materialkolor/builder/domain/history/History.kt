package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The undo and redo stacks of one editing session.
 *
 * Every edit is recorded as the document before it and the document after it. Edits that belong
 * together end up as one step. A drag becomes a single step however many frames it took, and quick
 * repeats of the same edit, typing a hex or nudging with the arrow keys, fold into the step before
 * them. A step that ends where it started, such as a picker that was opened and then cancelled,
 * leaves nothing behind.
 *
 * The history never reads a clock. Whoever records an edit says when it happened, which keeps the
 * merge window testable and the class free of anything platform specific.
 *
 * @param[entries] Steps to start from, oldest first, usually what [persisted] handed out last time.
 */
public class History(
    entries: List<HistoryEntry> = emptyList(),
) {
    private val done = ArrayDeque(entries.takeLast(CAPACITY))
    private val undone = ArrayDeque<HistoryEntry>()
    private var last: LastRecord? = null

    // b-307
    // What could be redone before the open drag started, handed back if the drag ends where it began.
    private var parked: List<HistoryEntry>? = null

    /** Whether there is a step to undo. */
    public val canUndo: Boolean
        get() = done.isNotEmpty()

    /** Whether there is a step to redo. */
    public val canRedo: Boolean
        get() = undone.isNotEmpty()

    /** What the undo button says, or null when there is nothing to undo. */
    public val undoLabel: ChangeLabel?
        get() = done.lastOrNull()?.label

    /** What the redo button says, or null when there is nothing to redo. */
    public val redoLabel: ChangeLabel?
        get() = undone.lastOrNull()?.label

    /**
     * Records that [change] took the document from [before] to [after].
     *
     * While [phase] is [EditPhase.Dragging] each call moves the end of the same step, and
     * [EditPhase.Released] moves it one last time and closes it. A discrete edit folds into the
     * step before it when both share a [DocumentChange.coalesceKey] and the earlier one landed at
     * most [MERGE_WINDOW_MILLIS] before [now]. Edits that never merge always start their own step.
     *
     * Recording a real change throws away what could have been redone. An edit that changes nothing
     * leaves the redo steps where they are, and so does a drag that ends where it began. A release
     * always closes the drag of its key, even for a change that never merges otherwise.
     *
     * @param[now] When the edit happened, in milliseconds on whatever clock the caller keeps.
     */
    public fun record(
        before: ThemeDocument,
        after: ThemeDocument,
        change: DocumentChange,
        phase: EditPhase,
        now: Long,
    ) {
        if (canFold(change, phase, now, before)) { // b-311a
            fold(after, change, phase, now)
        } else {
            push(before, after, change, phase, now)
        }
    }

    /**
     * Steps back once and returns the document to show, or null when there was nothing to undo.
     */
    public fun undo(): ThemeDocument? {
        val entry = done.removeLastOrNull() ?: return null
        undone.addLast(entry)
        last = null
        parked = null // b-307
        return entry.before
    }

    /**
     * Steps forward once and returns the document to show, or null when there was nothing to redo.
     */
    public fun redo(): ThemeDocument? {
        val entry = undone.removeLastOrNull() ?: return null
        done.addLast(entry)
        last = null
        parked = null // b-307
        return entry.after
    }

    /**
     * The newest steps worth keeping across a reload, oldest first.
     *
     * Only what can be undone is kept. Redo is a thing of the moment and does not survive a reload.
     */
    public fun persisted(): List<HistoryEntry> = done.takeLast(PERSISTED)

    private fun canFold(
        change: DocumentChange,
        phase: EditPhase,
        now: Long,
        before: ThemeDocument, // b-311a
    ): Boolean {
        val previous = last ?: return false
        if (previous.coalesceKey != change.coalesceKey) return false
        // b-307
        // A release closes its drag even when the change never merges, so a preset seed put back
        // after a drag leaves no second step.
        if (phase == EditPhase.Released && previous.phase == EditPhase.Dragging) return true
        if (!change.merges || !previous.merges || change.crossesAnImage(before)) return false // b-311a b-311c
        return when (phase) {
            EditPhase.Dragging -> {
                previous.phase == EditPhase.Dragging
            }
            EditPhase.Released -> {
                previous.phase == EditPhase.Dragging
            }
            EditPhase.Discrete -> {
                previous.phase == EditPhase.Discrete && (now - previous.at) in 0L..MERGE_WINDOW_MILLIS
            }
        }
    }

    private fun fold(
        after: ThemeDocument,
        change: DocumentChange,
        phase: EditPhase,
        now: Long,
    ) {
        val entry = done.removeLast().copy(after = after, label = change.label)
        if (entry.after == entry.before) {
            last = null
            parked?.let(undone::addAll) // b-307
            parked = null
            return
        }
        done.addLast(entry)
        last = LastRecord(change.coalesceKey, change.merges, phase, now)
        if (phase != EditPhase.Dragging) parked = null // b-307
    }

    private fun push(
        before: ThemeDocument,
        after: ThemeDocument,
        change: DocumentChange,
        phase: EditPhase,
        now: Long,
    ) {
        if (after == before) return
        // b-307
        // A drag keeps what could be redone aside until it lands somewhere new.
        parked = undone.toList().takeIf { phase == EditPhase.Dragging && it.isNotEmpty() }
        undone.clear()
        done.addLast(HistoryEntry(before = before, after = after, label = change.label))
        while (done.size > CAPACITY) done.removeFirst()
        last = LastRecord(change.coalesceKey, change.merges, phase, now)
    }

    /**
     * What the history needs to remember about the newest edit to know whether the next one folds
     * into it.
     */
    private data class LastRecord(
        val coalesceKey: String,
        val merges: Boolean,
        val phase: EditPhase,
        val at: Long,
    )

    public companion object {
        /** How many steps the history holds in memory. */
        public const val CAPACITY: Int = 100

        /** How many of the newest steps [persisted] hands out. */
        public const val PERSISTED: Int = 50

        /** How close together two discrete edits have to land to fold into one step. */
        public const val MERGE_WINDOW_MILLIS: Long = 600
    }
}

// b-311a b-311c

/**
 * Whether this sets the seed from a new image, or from anything else over a seed [before] took from
 * an image. An image seed is always its own step both ways, the way a preset is. Undoing it never
 * takes an earlier seed edit with it, and a seed typed, shuffled or lifted right after it never
 * vanishes into it. Swapping to another of the same image's colors is still a seed edit like any
 * other and folds as one.
 */
private fun DocumentChange.crossesAnImage(before: ThemeDocument): Boolean =
    this is DocumentChange.SetSeed &&
        source != before.seedSource &&
        (source is SeedSource.Image || before.seedSource is SeedSource.Image)
