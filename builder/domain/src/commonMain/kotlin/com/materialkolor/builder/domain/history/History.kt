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
 * The steps form one line with a [cursor] in it. The steps before the cursor are applied and
 * can be undone, and the ones after it were undone and can be redone. Undo and redo move the cursor
 * by one, and [jumpTo] moves it straight to any step, as that many undos or redos in one go. A jump
 * is not a step of its own. Nothing after the cursor is lost until the next real edit, which drops
 * it the same way it drops what could be redone.
 *
 * The history never reads a clock. Whoever records an edit says when it happened, which keeps the
 * merge window testable and the class free of anything platform specific. Each step keeps that time
 * as [HistoryEntry.at].
 *
 * @param[entries] Steps to start from, oldest first, usually what [persisted] handed out last time.
 */
public class History(
    entries: List<HistoryEntry> = emptyList(),
) {
    private val done = ArrayDeque(entries.takeLast(CAPACITY))
    private val undone = ArrayDeque<HistoryEntry>()
    private var last: LastRecord? = null

    // What could be redone before the open drag started, handed back if the drag ends where it began.
    private var parked: List<HistoryEntry>? = null

    // The oldest step the open one pushed out at capacity, handed back if the open step comes to nothing.
    // Undo, redo and a jump clear it only as a guard. They clear last too, so the next record pushes and sets it anew.
    private var trimmed: HistoryEntry? = null

    /**
     * Whether there is a step to undo.
     */
    public val canUndo: Boolean
        get() = done.isNotEmpty()

    /**
     * Whether there is a step to redo.
     */
    public val canRedo: Boolean
        get() = undone.isNotEmpty()

    /**
     * What the undo button says, or null when there is nothing to undo.
     */
    public val undoLabel: ChangeLabel?
        get() = done.lastOrNull()?.label

    /**
     * What the redo button says, or null when there is nothing to redo.
     */
    public val redoLabel: ChangeLabel?
        get() = undone.lastOrNull()?.label

    /**
     * Every step, oldest first. The applied ones come first and the undone ones follow in the order
     * they were made, so the step at index `i` is the one [jumpTo] of `i + 1` lands after.
     */
    public val entries: List<HistoryEntry>
        get() = done + undone.asReversed()

    /**
     * How many of [entries] are applied. Zero is the document before the oldest step.
     */
    public val cursor: Int
        get() = done.size

    /**
     * How many steps there are, applied and undone together. It never passes [CAPACITY].
     */
    public val size: Int
        get() = done.size + undone.size

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
     * A new step takes [now] as its [HistoryEntry.at], and a step an edit folds into moves up to it.
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
        if (canFold(change, phase, now, before)) {
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
        parked = null
        trimmed = null
        return entry.before
    }

    /**
     * Steps forward once and returns the document to show, or null when there was nothing to redo.
     */
    public fun redo(): ThemeDocument? {
        val entry = undone.removeLastOrNull() ?: return null
        done.addLast(entry)
        last = null
        parked = null
        trimmed = null
        return entry.after
    }

    /**
     * Puts the cursor [cursor] steps in and returns the document to show there, or null when it sits
     * there already, and then nothing changes.
     *
     * It walks the steps over one by one, the way that many undos or redos would, and like them it
     * closes the open step, so the next edit starts a new one however soon it comes. Zero lands on
     * the document before the oldest step and [size] on the newest step.
     *
     * @throws[IllegalArgumentException] when [cursor] is not between zero and [size].
     */
    public fun jumpTo(cursor: Int): ThemeDocument? {
        require(cursor in 0..size) { "Cursor is 0 to $size, got $cursor" }
        if (cursor == this.cursor) return null
        while (done.size > cursor) undone.addLast(done.removeLast())
        while (done.size < cursor) done.addLast(undone.removeLast())
        last = null
        parked = null
        trimmed = null
        return done.lastOrNull()?.after ?: undone.last().before
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
        before: ThemeDocument,
    ): Boolean {
        val previous = last ?: return false
        if (previous.coalesceKey != change.coalesceKey) return false
        // A release closes its drag even when the change never merges, so a preset seed put back
        // after a drag leaves no second step.
        if (phase == EditPhase.Released && previous.phase == EditPhase.Dragging) return true
        if (!change.merges || !previous.merges || change.crossesAnImage(before)) return false
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
        val entry = done.removeLast().copy(after = after, label = change.label, at = now)
        if (entry.after == entry.before) {
            last = null
            parked?.let(undone::addAll)
            parked = null
            trimmed?.let(done::addFirst)
            trimmed = null
            return
        }
        done.addLast(entry)
        last = LastRecord(change.coalesceKey, change.merges, phase, now)
        if (phase != EditPhase.Dragging) parked = null
    }

    private fun push(
        before: ThemeDocument,
        after: ThemeDocument,
        change: DocumentChange,
        phase: EditPhase,
        now: Long,
    ) {
        if (after == before) return
        // A drag keeps what could be redone aside until it lands somewhere new.
        parked = undone.toList().takeIf { phase == EditPhase.Dragging && it.isNotEmpty() }
        undone.clear()
        done.addLast(HistoryEntry(before = before, after = after, label = change.label, at = now))
        // The done steps never pass capacity before a push, so at most one goes.
        trimmed = if (done.size > CAPACITY) done.removeFirst() else null
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
        /**
         * How many steps the history holds in memory. A new step past it pushes the oldest out, and a
         * step that comes to nothing, such as a cancelled picker, brings it back.
         */
        public const val CAPACITY: Int = 100

        /**
         * How many of the newest steps [persisted] hands out.
         */
        public const val PERSISTED: Int = 50

        /**
         * How close together two discrete edits have to land to fold into one step.
         */
        public const val MERGE_WINDOW_MILLIS: Long = 600
    }
}

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
