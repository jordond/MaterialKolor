package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Conflicts with other tabs and what happens to waiting saves when the open project changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionConflictTest : SessionTestBase() {
    private val mine = ThemeDocument.Default.copy(amoled = true)

    @Test
    fun savedElsewhere_justInsideTheConflictWindow_raisesAConflict() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            advanceTimeBy(CONFLICT_WINDOW_MILLIS - 1)
            runCurrent()

            val theirs = saveFromAnotherTab(id, FOREST)

            session.conflict.value shouldBe Conflict(theirs)
            session.document.value shouldBe mine
        }

    @Test
    fun savedElsewhere_rightAfterAnEditThatChangesNothing_takesTheirsAsAnUndoStep() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(false), EditPhase.Discrete)

            saveFromAnotherTab(id, FOREST)

            session.conflict.value shouldBe null
            session.document.value shouldBe FOREST
            session.undo()
            session.document.value shouldBe ThemeDocument.Default
        }

    @Test
    fun savedElsewhere_againWhileAConflictIsUp_replacesTheirsAndKeepMineSavesMine() =
        runTest {
            val (session) = session()
            val id = conflicted(session)

            val newer = saveFromAnotherTab(id, OCEAN, revision = 11)

            session.conflict.value shouldBe Conflict(newer)
            session.document.value shouldBe mine
            session.resolveConflict(keepMine = true)
            settle()
            session.conflict.value shouldBe null
            projects.load(id).shouldNotBeNull().document shouldBe mine
        }

    @Test
    fun savedElsewhere_againWhileAConflictIsUp_loadTheirsTakesTheNewerSave() =
        runTest {
            val (session) = session()
            val id = conflicted(session)
            saveFromAnotherTab(id, OCEAN, revision = 11)

            session.resolveConflict(keepMine = false)
            settle()

            session.document.value shouldBe OCEAN
            projects.load(id).shouldNotBeNull().document shouldBe OCEAN
            session.undo()
            session.document.value shouldBe mine
        }

    @Test
    fun flush_withAConflictUp_keepsMineWithTheEditsMadeUnderIt() =
        runTest {
            val (session) = session()
            val id = conflicted(session)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)

            session.flush().join()

            session.conflict.value shouldBe null
            projects.load(id).shouldNotBeNull().document shouldBe mine.copy(style = Style.Vibrant)
        }

    @Test
    fun newProject_withAConflictUp_savesMineBeforeItSwitches() =
        runTest {
            val (session) = session()
            val id = conflicted(session)

            session.newProject(copyCurrent = false)

            session.conflict.value shouldBe null
            session.project.value shouldNotBe ProjectRef.Persisted(id)
            projects.load(id).shouldNotBeNull().document shouldBe mine
        }

    @Test
    fun newProject_afterASaveThatDidNotLand_retriesItOnTheNextFlush() =
        runTest {
            val (session) = session()
            val first = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            stores.failNextUpdates(count = 1, StoreError.Unavailable)

            session.newProject(copyCurrent = false)
            session.saveStatus.value shouldBe SaveStatus.Idle
            projects.load(first).shouldNotBeNull().document shouldBe ThemeDocument.Default
            val second = session.project.value
                .shouldBeInstanceOf<ProjectRef.Persisted>()
                .id
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            session.flush().join()

            projects.load(first).shouldNotBeNull().document shouldBe mine
            projects
                .load(second)
                .shouldNotBeNull()
                .document.style shouldBe Style.Vibrant
        }

    /**
     * Boot, edit, and have another tab save [FOREST] straight after, so a conflict is up.
     */
    private suspend fun TestScope.conflicted(session: ProjectSession): String {
        val id = booted(session)
        session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
        val theirs = saveFromAnotherTab(id, FOREST)
        session.conflict.value shouldBe Conflict(theirs)
        // Well past the window, so only the open conflict can stop the next save being taken in.
        advanceTimeBy(CONFLICT_WINDOW_MILLIS + AUTOSAVE_DELAY_MILLIS)
        runCurrent()
        return id
    }
}
