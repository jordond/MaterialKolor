package com.materialkolor.builder.domain.history

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryTest {
    @Test
    fun history_dragSequence_makesOneEntry() {
        val session = Session()

        (1..20).forEach { step -> session.edit(contrast(step * 5), EditPhase.Dragging, at = step * 16L) }
        session.edit(contrast(100), EditPhase.Released, at = 400)

        assertEquals(1, session.history.persisted().size)
        assertEquals(ContrastLevel.High, session.document.contrast)
        assertEquals(ThemeDocument.Default, session.history.undo())
        assertFalse(session.history.canUndo)
        assertEquals(ContrastLevel.High, session.history.redo()?.contrast)
    }

    @Test
    fun history_longDrag_ignoresTheMergeWindow() {
        val session = Session()

        session.edit(contrast(10), EditPhase.Dragging, at = 0)
        session.edit(contrast(20), EditPhase.Dragging, at = 5_000)
        session.edit(contrast(30), EditPhase.Released, at = 10_000)

        assertEquals(1, session.history.persisted().size)
    }

    @Test
    fun history_dragAfterRelease_startsANewEntry() {
        val session = Session()

        session.edit(contrast(10), EditPhase.Dragging, at = 0)
        session.edit(contrast(20), EditPhase.Released, at = 100)
        session.edit(contrast(30), EditPhase.Dragging, at = 200)
        session.edit(contrast(40), EditPhase.Released, at = 300)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_discreteSameKeyAt599ms_merges() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("A"), at = 1_000)
        session.edit(DocumentChange.SetThemeName("Ab"), at = 1_599)

        assertEquals(1, session.history.persisted().size)
        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Ab"), session.history.undoLabel)
        assertEquals(ThemeDocument.Default, session.history.undo())
    }

    @Test
    fun history_discreteSameKeyAt601ms_doesNotMerge() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("A"), at = 1_000)
        session.edit(DocumentChange.SetThemeName("Ab"), at = 1_601)

        assertEquals(2, session.history.persisted().size)
        assertEquals("A", session.history.undo()?.themeName)
    }

    @Test
    fun history_steadyTyping_keepsMergingFromTheLastKeystroke() {
        val session = Session()

        listOf("A", "Ap", "App", "AppT").forEachIndexed { index, name ->
            session.edit(DocumentChange.SetThemeName(name), at = index * 500L)
        }

        assertEquals(1, session.history.persisted().size)
    }

    @Test
    fun history_discreteDifferentKey_doesNotMerge() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("A"), at = 0)
        session.edit(DocumentChange.SetAmoled(true), at = 10)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_discreteAfterADrag_doesNotMergeIntoIt() {
        val session = Session()

        session.edit(contrast(10), EditPhase.Dragging, at = 0)
        session.edit(contrast(20), EditPhase.Released, at = 10)
        session.edit(contrast(30), at = 20)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_styleWithinTheWindow_neverMerges() {
        val session = Session()

        session.edit(DocumentChange.SetStyle(Style.Vibrant), at = 0)
        session.edit(DocumentChange.SetStyle(Style.Rainbow), at = 1)

        assertEquals(2, session.history.persisted().size)
        assertEquals(ChangeLabel(ChangeKind.Style, detail = "Rainbow"), session.history.undoLabel)
    }

    @Test
    fun history_libraryWithinTheWindow_neverMerges() {
        val session = Session()

        session.edit(DocumentChange.SetLibrary(Library.Fluent, expressive = false), at = 0)
        session.edit(DocumentChange.SetLibrary(Library.Fluent, expressive = true), at = 1)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_presetWithinTheWindow_neverMerges() {
        val session = Session()

        session.edit(DocumentChange.SetSeed(red, SeedSource.Preset(id = "plum")), at = 0)
        session.edit(DocumentChange.SetSeed(blue, SeedSource.Preset(id = "sage")), at = 1)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Typed), at = 2)

        assertEquals(3, session.history.persisted().size)
    }

    @Test
    fun history_styleWhileDragging_stillNeverMerges() {
        val session = Session()

        session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Dragging, at = 0)
        session.edit(DocumentChange.SetStyle(Style.Rainbow), EditPhase.Dragging, at = 1)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_replace_isAlwaysItsOwnEntry() {
        val session = Session()
        val other = ThemeDocument.Default.copy(themeName = "Other")

        session.edit(DocumentChange.SetThemeName("Other"), at = 0)
        session.edit(DocumentChange.Replace(ThemeDocument.Default.copy(themeName = "Imported")), at = 1)
        session.edit(DocumentChange.Replace(other), at = 2)

        assertEquals(3, session.history.persisted().size)
        assertEquals(ChangeLabel(ChangeKind.Replace), session.history.undoLabel)
    }

    @Test
    fun history_noOpChange_leavesNoEntry() {
        val session = Session()

        session.edit(DocumentChange.SetStyle(ThemeDocument.Default.style), at = 0)
        session.edit(DocumentChange.Replace(ThemeDocument.Default), at = 1)

        assertFalse(session.history.canUndo)
        assertNull(session.history.undoLabel)
    }

    @Test
    fun history_cancelledDrag_leavesNoEntry() {
        val session = Session()

        session.edit(contrast(30), EditPhase.Dragging, at = 0)
        session.edit(contrast(60), EditPhase.Dragging, at = 16)
        session.edit(contrast(0), EditPhase.Released, at = 32)

        assertFalse(session.history.canUndo)
    }

    // b-307
    @Test
    fun history_presetSeedPutBackAfterADrag_leavesNoEntry() {
        val session = Session()
        session.edit(DocumentChange.SetSeed(red, SeedSource.Preset(id = "plum")), at = 0)

        session.edit(DocumentChange.SetSeed(blue, SeedSource.Picked), EditPhase.Dragging, at = 10_000)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Picked), EditPhase.Dragging, at = 10_016)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Preset(id = "plum")), EditPhase.Released, at = 10_032)

        assertEquals(1, session.history.persisted().size)
        assertEquals(ChangeLabel(ChangeKind.Preset, detail = "plum"), session.history.undoLabel)
    }

    // b-311c
    @Test
    fun history_imageSeedPutBackAfterADrag_leavesNoEntry() {
        val session = Session()
        val image = SeedSource.Image("photo.png", listOf(red, blue))
        session.edit(DocumentChange.SetSeed(red, image), at = 0)

        session.edit(DocumentChange.SetSeed(blue, SeedSource.Picked), EditPhase.Dragging, at = 10_000)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Picked), EditPhase.Dragging, at = 10_016)
        session.edit(DocumentChange.SetSeed(red, image), EditPhase.Released, at = 10_032)

        assertEquals(1, session.history.persisted().size)
        assertEquals(image, session.document.seedSource)
        assertEquals(ThemeDocument.Default, session.history.undo())
    }

    @Test
    fun history_releaseThatNeverMerges_closesTheDragItEnds() {
        val session = Session()

        session.edit(DocumentChange.SetSeed(blue, SeedSource.Picked), EditPhase.Dragging, at = 0)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Preset(id = "plum")), EditPhase.Released, at = 16)
        session.edit(DocumentChange.SetSeed(blue, SeedSource.Picked), EditPhase.Dragging, at = 32)

        assertEquals(2, session.history.persisted().size)
    }

    @Test
    fun history_dragBackToTheStart_keepsWhatCanBeRedone() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(contrast(30), EditPhase.Dragging, at = 10_000)
        assertFalse(session.history.canRedo)
        session.edit(contrast(0), EditPhase.Released, at = 10_016)

        assertTrue(session.history.canRedo)
        assertEquals(ChangeLabel(ChangeKind.Amoled), session.history.redoLabel)
        assertFalse(session.history.canUndo)
    }

    @Test
    fun history_dragThatLands_throwsAwayRedo() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(contrast(30), EditPhase.Dragging, at = 10_000)
        session.edit(contrast(40), EditPhase.Released, at = 10_016)

        assertFalse(session.history.canRedo)
    }

    // b-307a
    @Test
    fun history_dragThroughTheStartThatLandsElsewhere_throwsAwayRedo() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(contrast(30), EditPhase.Dragging, at = 10_000)
        session.edit(contrast(0), EditPhase.Dragging, at = 10_016)
        session.edit(contrast(20), EditPhase.Dragging, at = 10_032)
        session.edit(contrast(40), EditPhase.Released, at = 10_048)

        assertFalse(session.history.canRedo)
        assertNull(session.history.redoLabel)
        assertEquals(1, session.history.persisted().size)
        assertEquals(ThemeDocument.Default, session.history.undo())
    }

    @Test
    fun history_undoMidDrag_dropsTheRedoStepsTheDragSetAside() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(contrast(30), EditPhase.Dragging, at = 10_000)
        session.undo()
        session.edit(contrast(50), EditPhase.Dragging, at = 10_016)
        session.edit(contrast(0), EditPhase.Released, at = 10_032)

        assertEquals(ContrastLevel(30), session.history.redo()?.contrast)
        assertFalse(session.history.canRedo)
    }

    @Test
    fun history_mergedEditsBackToTheStart_leaveNoEntry() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("AppThemeX"), at = 0)
        session.edit(DocumentChange.SetThemeName("AppTheme"), at = 100)

        assertFalse(session.history.canUndo)
    }

    @Test
    fun history_noOpChange_keepsWhatCanBeRedone() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(DocumentChange.SetAmoled(false), at = 10_000)

        assertTrue(session.history.canRedo)
    }

    @Test
    fun history_newEdit_throwsAwayRedo() {
        val session = Session()
        session.edit(DocumentChange.SetAmoled(true), at = 0)
        session.undo()

        session.edit(DocumentChange.SetThemeName("Plum"), at = 10_000)

        assertFalse(session.history.canRedo)
        assertNull(session.history.redoLabel)
    }

    @Test
    fun history_editRightAfterUndo_doesNotMergeIntoTheUndoneStep() {
        val session = Session()
        session.edit(DocumentChange.SetThemeName("A"), at = 0)
        session.edit(DocumentChange.SetThemeName("B"), at = 10_000)
        session.undo()

        session.edit(DocumentChange.SetThemeName("C"), at = 10_001)

        assertEquals(2, session.history.persisted().size)
        assertEquals("A", session.history.undo()?.themeName)
    }

    @Test
    fun history_undoAndRedo_walkTheSnapshotsWithTheirLabels() {
        val session = Session()
        session.edit(DocumentChange.SetStyle(Style.Vibrant), at = 0)
        session.edit(DocumentChange.SetThemeName("Plum"), at = 10_000)

        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Plum"), session.history.undoLabel)
        assertEquals(Style.Vibrant, session.history.undo()?.style)
        assertEquals(ChangeLabel(ChangeKind.Style, detail = "Vibrant"), session.history.undoLabel)
        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Plum"), session.history.redoLabel)
        assertEquals(ThemeDocument.Default, session.history.undo())
        assertNull(session.history.undo())
        assertTrue(session.history.canRedo)
        assertEquals(Style.Vibrant, session.history.redo()?.style)
        assertEquals("Plum", session.history.redo()?.themeName)
        assertNull(session.history.redo())
    }

    @Test
    fun history_emptyHistory_hasNothingToUndoOrRedo() {
        val history = History()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undoLabel)
        assertNull(history.redoLabel)
        assertNull(history.undo())
        assertNull(history.redo())
        assertEquals(emptyList(), history.persisted())
    }

    @Test
    fun history_pastCapacity_keepsTheNewest100() {
        val session = Session()

        (1..150).forEach { step -> session.edit(DocumentChange.SetThemeName("Theme$step"), at = step * 10_000L) }

        var undone = 0
        var shown: ThemeDocument? = null
        while (session.history.canUndo) {
            shown = session.history.undo()
            undone++
        }
        assertEquals(History.CAPACITY, undone)
        assertEquals("Theme50", shown?.themeName)
    }

    @Test
    fun history_persisted_handsOutTheNewest50OldestFirst() {
        val session = Session()

        (1..80).forEach { step -> session.edit(DocumentChange.SetThemeName("Theme$step"), at = step * 10_000L) }
        val persisted = session.history.persisted()

        assertEquals(History.PERSISTED, persisted.size)
        assertEquals("Theme31", persisted.first().after.themeName)
        assertEquals("Theme80", persisted.last().after.themeName)
    }

    @Test
    fun history_persisted_leavesOutWhatWasUndone() {
        val session = Session()
        (1..3).forEach { step -> session.edit(DocumentChange.SetThemeName("Theme$step"), at = step * 10_000L) }

        session.undo()

        assertEquals(listOf("Theme1", "Theme2"), session.history.persisted().map { entry -> entry.after.themeName })
    }

    @Test
    fun history_restoredEntries_canBeUndone() {
        val first = Session()
        (1..3).forEach { step -> first.edit(DocumentChange.SetThemeName("Theme$step"), at = step * 10_000L) }

        val restored = History(first.history.persisted())

        assertEquals(ChangeLabel(ChangeKind.ThemeName, detail = "Theme3"), restored.undoLabel)
        assertEquals("Theme2", restored.undo()?.themeName)
        assertEquals("Theme1", restored.undo()?.themeName)
        assertEquals(ThemeDocument.Default, restored.undo())
    }

    @Test
    fun history_negativeElapsedTime_doesNotMerge() {
        val session = Session()

        session.edit(DocumentChange.SetThemeName("A"), at = 1_000)
        session.edit(DocumentChange.SetThemeName("Ab"), at = 900)

        assertEquals(2, session.history.persisted().size)
    }

    // b-311a

    @Test
    fun history_newImageSeedRightAfterASeedEdit_isItsOwnStep() {
        val session = Session()
        val image = SeedSource.Image("photo.png", listOf(blue, red))

        session.edit(DocumentChange.SetSeed(red, SeedSource.Typed), at = 0)
        val typed = session.document
        session.edit(DocumentChange.SetSeed(blue, image), at = 100)
        session.edit(DocumentChange.SetSeed(red, SeedSource.Image("other.png", listOf(red))), at = 200)

        assertEquals(3, session.history.persisted().size)
        assertEquals(image, session.history.undo()?.seedSource)
        assertEquals(typed, session.history.undo())
    }

    @Test
    fun history_chipSwapWithinOneImage_stillFoldsIntoTheImageSeed() {
        val session = Session()
        val image = SeedSource.Image("photo.png", listOf(blue, red))

        session.edit(DocumentChange.SetSeed(blue, image), at = 0)
        session.edit(DocumentChange.SetSeed(red, image), at = 100)

        assertEquals(1, session.history.persisted().size)
        assertEquals(ThemeDocument.Default, session.history.undo())
    }

    // b-311c
    @Test
    fun history_seedEditRightAfterAnImageSeed_isItsOwnStep() {
        listOf(SeedSource.Typed, SeedSource.Shuffled, SeedSource.Eyedropper).forEach { next ->
            val session = Session()
            val image = SeedSource.Image("photo.png", listOf(blue, red))

            session.edit(DocumentChange.SetSeed(blue, image), at = 0)
            val seeded = session.document
            session.edit(DocumentChange.SetSeed(red, next), at = 100)
            session.edit(DocumentChange.SetSeed(blue, next), at = 200)

            assertEquals(2, session.history.persisted().size, "after $next")
            assertEquals(seeded, session.history.undo(), "after $next")
            assertEquals(ThemeDocument.Default, session.history.undo(), "after $next")
        }
    }

    private fun contrast(hundredths: Int): DocumentChange = DocumentChange.SetContrast(ContrastLevel(hundredths))

    /**
     * A document being edited, the way the builder drives the history, so each test only has to say
     * which edits happened and when.
     */
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
    }

    private companion object {
        val red = Argb(0xFFFF0000.toInt())
        val blue = Argb(0xFF0000FF.toInt())
    }
}
