package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectRecord
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * When an edit saves and what another tab's save does, told apart with no session around them.
 */
class SessionRulesTest {
    @Test
    fun editOutcome_dragStep_onlyShows() {
        editOutcome(EditPhase.Dragging, before = OCEAN, after = FOREST, committed = OCEAN) shouldBe EditOutcome.Drag
    }

    @Test
    fun editOutcome_releaseBackOntoTheCommittedDocument_savesNothing() {
        editOutcome(EditPhase.Released, before = FOREST, after = OCEAN, committed = OCEAN) shouldBe
            EditOutcome.ReleasedOntoCommitted
    }

    @Test
    fun editOutcome_releaseSomewhereNew_commits() {
        editOutcome(EditPhase.Released, before = FOREST, after = FOREST, committed = OCEAN) shouldBe EditOutcome.Commit
    }

    @Test
    fun editOutcome_discreteChangeThatChangesNothing_isUnchanged() {
        editOutcome(EditPhase.Discrete, before = FOREST, after = FOREST, committed = OCEAN) shouldBe
            EditOutcome.Unchanged
    }

    @Test
    fun editOutcome_discreteChange_commits() {
        editOutcome(EditPhase.Discrete, before = OCEAN, after = FOREST, committed = OCEAN) shouldBe EditOutcome.Commit
    }

    @Test
    fun incomingSave_fromThisTab_isIgnored() {
        incoming(theirs = record(FOREST, revision = 3, writerTab = TAB)) shouldBe IncomingSave.Ignore
    }

    @Test
    fun incomingSave_staleRevision_isIgnored() {
        incoming(theirs = record(FOREST, revision = 2)) shouldBe IncomingSave.Ignore
    }

    @Test
    fun incomingSave_sameDocumentAsShowing_matches() {
        incoming(theirs = record(OCEAN, revision = 3), conflicted = true) shouldBe IncomingSave.Matches
    }

    @Test
    fun incomingSave_withNothingHeld_isTakenAtAnyRevision() {
        incoming(held = null, theirs = record(FOREST, revision = 0)) shouldBe IncomingSave.Adopt
    }

    @Test
    fun incomingSave_withAConflictUp_raisesItAgain() {
        incoming(theirs = record(FOREST, revision = 3), conflicted = true) shouldBe IncomingSave.RaiseConflict
    }

    @Test
    fun incomingSave_editedInsideTheWindow_raisesAConflict() {
        incoming(theirs = record(FOREST, revision = 3), lastEditAt = NOW - CONFLICT_WINDOW_MILLIS + 1) shouldBe
            IncomingSave.RaiseConflict
    }

    @Test
    fun incomingSave_editedExactlyAWindowAgo_adopts() {
        incoming(theirs = record(FOREST, revision = 3), lastEditAt = NOW - CONFLICT_WINDOW_MILLIS) shouldBe
            IncomingSave.Adopt
    }

    @Test
    fun incomingSave_neverEdited_adopts() {
        incoming(theirs = record(FOREST, revision = 3), lastEditAt = null) shouldBe IncomingSave.Adopt
    }

    private fun incoming(
        theirs: ProjectRecord,
        held: ProjectRecord? = record(OCEAN, revision = 2),
        conflicted: Boolean = false,
        lastEditAt: Long? = null,
    ): IncomingSave =
        incomingSave(
            held = held,
            incoming = theirs,
            tabId = TAB,
            document = OCEAN,
            conflicted = conflicted,
            lastEditAt = lastEditAt,
            now = NOW,
        )

    private fun record(
        document: ThemeDocument,
        revision: Long,
        writerTab: String = "other",
    ): ProjectRecord = ProjectRecord("p1", "Ocean", document, revision, writerTab)

    private companion object {
        const val TAB = "this-tab"
        const val NOW = 10_000L
        val OCEAN = ThemeDocument(seed = Argb(0xFF1565C0.toInt()))
        val FOREST = ThemeDocument(seed = Argb(0xFF2E7D32.toInt()))
    }
}
