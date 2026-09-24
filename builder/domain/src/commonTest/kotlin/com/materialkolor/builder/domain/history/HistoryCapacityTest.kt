package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryCapacityTest {
    @Test
    fun history_pastCapacity_keepsTheNewest100() {
        val session = Session()

        (1..150).forEach { step -> session.edit(DocumentChange.SetThemeName("Theme$step"), at = step * STEP_MILLIS) }

        val (undone, shown) = session.undoAll()
        assertEquals(History.CAPACITY, undone)
        assertEquals("Theme50", shown.themeName)
    }

    @Test
    fun history_cancelledDragAtCapacity_keepsTheOldestStep() {
        val session = Session.full()
        val start = session.document.contrast

        session.edit(DocumentChange.SetContrast(ContrastLevel(50)), EditPhase.Dragging, at = END)
        session.edit(DocumentChange.SetContrast(ContrastLevel(80)), EditPhase.Dragging, at = END + 16)
        session.edit(DocumentChange.SetContrast(start), EditPhase.Released, at = END + 32)

        val (undone, shown) = session.undoAll()
        assertEquals(History.CAPACITY, undone)
        assertEquals(ThemeDocument.Default, shown)
    }

    @Test
    fun history_typingBackToTheStartAtCapacity_keepsTheOldestStep() {
        val session = Session.full()
        val start = session.document.themeName

        session.edit(DocumentChange.SetThemeName("${start}x"), at = END)
        session.edit(DocumentChange.SetThemeName(start), at = END + 100)

        val (undone, shown) = session.undoAll()
        assertEquals(History.CAPACITY, undone)
        assertEquals(ThemeDocument.Default, shown)
    }

    @Test
    fun history_dragThatLandsAtCapacity_stillPushesTheOldestOut() {
        val session = Session.full()

        session.edit(DocumentChange.SetContrast(ContrastLevel(50)), EditPhase.Dragging, at = END)
        session.edit(DocumentChange.SetContrast(ContrastLevel(80)), EditPhase.Released, at = END + 16)

        val (undone, shown) = session.undoAll()
        assertEquals(History.CAPACITY, undone)
        assertEquals("Theme1", shown.themeName)
    }

    /**
     * A regression guard. It passes with or without the `trimmed = null` in undo and redo, since both
     * clear the open step as well, so the next record always pushes and replaces the step it trimmed.
     * The clears are there in case undo or redo ever leave a step open to fold into.
     */
    @Test
    fun regressionGuard_undoAfterAPushAtCapacity_doesNotBringTheOldestBack() {
        val session = Session.full()
        session.edit(DocumentChange.SetThemeName("Newest"), at = END)

        session.history.undo()
        session.history.redo()

        val (undone, shown) = session.undoAll()
        assertEquals(History.CAPACITY, undone)
        assertEquals("Theme1", shown.themeName)
    }

    /** A document being edited, the way the builder drives the history. */
    private class Session {
        val history = History()
        var document: ThemeDocument = ThemeDocument.Default
            private set

        fun edit(
            change: DocumentChange,
            phase: EditPhase = EditPhase.Discrete,
            at: Long,
        ) {
            val after = change.apply(document)
            history.record(before = document, after = after, change = change, phase = phase, now = at)
            document = after
        }

        /** Undo until nothing is left, and say how many steps that took and what showed last. */
        fun undoAll(): Pair<Int, ThemeDocument> {
            var undone = 0
            while (history.canUndo) {
                document = history.undo() ?: document
                undone++
            }
            return undone to document
        }

        companion object {
            /** A history holding exactly [History.CAPACITY] steps, from the defaults to "Theme100". */
            fun full(): Session =
                Session().apply {
                    (1..History.CAPACITY).forEach { step ->
                        edit(DocumentChange.SetThemeName("Theme$step"), at = step * STEP_MILLIS)
                    }
                }
        }
    }

    private companion object {
        /** Far enough apart that no two steps fold. */
        const val STEP_MILLIS = 10_000L

        /** Well after the last step [Session.full] records. */
        const val END = (History.CAPACITY + 1) * STEP_MILLIS
    }
}
