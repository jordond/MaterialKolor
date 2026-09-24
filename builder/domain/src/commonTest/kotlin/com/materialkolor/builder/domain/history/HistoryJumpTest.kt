package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// b-508
class HistoryJumpTest {
    @Test
    fun jumpTo_olderStep_returnsItsAfterAndKeepsLaterStepsToRedo() {
        val session = Session.named(3)
        val before = session.history.entries

        val shown = session.history.jumpTo(1)

        assertEquals("Theme1", shown?.themeName)
        assertEquals(1, session.history.cursor)
        assertEquals(3, session.history.size)
        assertEquals(before, session.history.entries)
        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Theme2"), session.history.redoLabel)
        assertEquals(listOf("Theme1"), session.history.persisted().map { entry -> entry.after.themeName })
        assertEquals("Theme2", session.history.redo()?.themeName)
    }

    @Test
    fun jumpTo_zero_returnsTheStartDocument() {
        val session = Session.named(3)

        val shown = session.history.jumpTo(0)

        assertEquals(ThemeDocument.Default, shown)
        assertEquals(0, session.history.cursor)
        assertFalse(session.history.canUndo)
        assertEquals(emptyList(), session.history.persisted())
    }

    @Test
    fun jumpTo_backThenForward_returnsTheNewestAfter() {
        val session = Session.named(3)
        val before = session.history.entries
        session.history.jumpTo(0)

        val shown = session.history.jumpTo(3)

        assertEquals("Theme3", shown?.themeName)
        assertEquals(before, session.history.entries)
        assertFalse(session.history.canRedo)
        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Theme3"), session.history.undoLabel)
    }

    @Test
    fun jumpTo_currentCursor_returnsNullAndChangesNothing() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()
        // The drag sets the Amoled redo aside, and only a release back where it began hands it back.
        session.edit(tone(30), EditPhase.Dragging, at = 10_000)

        val shown = session.jumpTo(session.history.cursor)
        session.edit(tone(0), EditPhase.Released, at = 10_016)

        assertNull(shown)
        assertEquals(1, session.history.size)
        assertEquals(0, session.history.cursor)
        assertEquals(ChangeLabel(ChangeKind.Amoled), session.history.redoLabel)
    }

    @Test
    fun jumpTo_outOfRange_throws() {
        val session = Session.named(3)

        assertFailsWith<IllegalArgumentException> { session.history.jumpTo(-1) }
        assertFailsWith<IllegalArgumentException> { session.history.jumpTo(4) }
        assertEquals(3, session.history.cursor)
    }

    @Test
    fun record_afterJumpingBack_dropsTheLaterSteps() {
        val session = Session.named(3)
        session.jumpTo(1)

        session.edit(DocumentChange.SetAmoled(true), at = 40_000)

        assertEquals(2, session.history.size)
        assertEquals(2, session.history.cursor)
        assertFalse(session.history.canRedo)
        assertEquals(
            listOf(ChangeKind.ThemeName, ChangeKind.Amoled),
            session.history.entries.map { entry -> entry.label.kind },
        )
    }

    @Test
    fun record_sameKeyWithinTheWindowAfterAJump_startsANewStep() {
        val session = Session()
        session.edit(DocumentChange.SetThemeName("A"), at = 0)
        session.edit(DocumentChange.SetThemeName("AB"), at = 10_000)
        session.jumpTo(1)

        session.edit(DocumentChange.SetThemeName("AC"), at = 10_100)

        assertEquals(listOf("A", "AC"), session.history.entries.map { entry -> entry.after.themeName })
        assertEquals("A", session.history.undo()?.themeName)
    }

    @Test
    fun drag_afterAJumpEndingWhereItBegan_givesTheLaterStepsBack() {
        val session = Session.named(3)
        // A key jumps back while a pointer still holds a slider.
        session.edit(tone(30), EditPhase.Dragging, at = 40_000)
        session.jumpTo(1)

        session.edit(tone(50), EditPhase.Dragging, at = 40_016)
        assertFalse(session.history.canRedo)
        session.edit(tone(0), EditPhase.Released, at = 40_032)

        assertEquals(1, session.history.cursor)
        assertEquals(4, session.history.size)
        assertEquals(
            listOf(ChangeKind.ThemeName, ChangeKind.ThemeName, ChangeKind.ThemeName, ChangeKind.CustomTone),
            session.history.entries.map { entry -> entry.label.kind },
        )
    }

    @Test
    fun record_atCapacityAfterJumpingToStart_keepsOnlyTheNewStep() {
        val session = Session.named(History.CAPACITY)
        session.jumpTo(0)

        session.edit(DocumentChange.SetAmoled(true), at = (History.CAPACITY + 1) * STEP_MILLIS)

        assertEquals(1, session.history.size)
        assertEquals(1, session.history.cursor)
        assertEquals(ThemeDocument.Default, session.history.entries.single().before)
        assertTrue(session.history.entries.single().after.amoled)
    }

    @Test
    fun record_pushAndFold_stampTheLatestTime() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("A"), at = 1_000)
        session.edit(DocumentChange.SetThemeName("Ab"), at = 1_400)
        session.edit(tone(30), EditPhase.Dragging, at = 10_000)
        session.edit(tone(40), EditPhase.Dragging, at = 10_016)
        session.edit(tone(50), EditPhase.Released, at = 10_032)
        session.edit(DocumentChange.SetAmoled(true), at = 20_000)

        assertEquals(listOf(1_400L, 10_032L, 20_000L), session.history.entries.map { entry -> entry.at })
    }

    /**
     * The primary slot's light tone, the change a drag example uses. Zero takes the tone away again,
     * which is where the default document starts.
     */
    private fun tone(light: Int): DocumentChange =
        DocumentChange.SetCustomTone(CustomSlot.Primary, if (light == 0) null else CustomTone(light = light))

    /** A document being edited, the way the builder drives the history, jumps included. */
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

        fun undo() {
            document = history.undo() ?: document
        }

        fun jumpTo(cursor: Int): ThemeDocument? = history.jumpTo(cursor)?.also { shown -> document = shown }

        companion object {
            /** A history of [steps] renames, from the defaults to "Theme1" and on, far enough apart to never fold. */
            fun named(steps: Int): Session =
                Session().apply {
                    (1..steps).forEach { step ->
                        edit(DocumentChange.SetThemeName("Theme$step"), at = step * STEP_MILLIS)
                    }
                }
        }
    }

    private companion object {
        const val STEP_MILLIS = 10_000L
    }
}
