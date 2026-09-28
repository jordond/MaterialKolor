package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.Deletion
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.StorageKeys
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * What the session's writes leave in storage, and what they leave out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionWritesTest : SessionTestBase() {
    @Test
    fun edit_pickerCancelOnASharedTheme_leavesItUnsaved() =
        runTest {
            val (session, preferences) = session()
            val code = ShareCodec.encode(OCEAN, projectName = "Ocean")
            session.openShared(code)

            session.cancelPicker(from = OCEAN)
            advanceUntilIdle()

            session.project.value shouldBe ProjectRef.Transient(code)
            session.document.value shouldBe OCEAN
            session.saveStatus.value shouldBe SaveStatus.Idle
            projects.index.first().projects shouldBe emptyList()
            environment.tabProject shouldBe null
            preferences.current().lastProjectId shouldBe null
        }

    @Test
    fun edit_realEditAfterAPickerCancel_stillSavesTheSharedTheme() =
        runTest {
            val (session) = session()
            session.openShared(ShareCodec.encode(OCEAN, projectName = "Ocean"))
            session.cancelPicker(from = OCEAN)

            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()

            val ref = session.project.value.shouldBeInstanceOf<ProjectRef.Persisted>()
            val saved = projects.load(ref.id).shouldNotBeNull()
            saved.name shouldBe "Ocean"
            saved.document shouldBe OCEAN.copy(amoled = true)
            session.saveStatus.value shouldBe SaveStatus.Idle
        }

    @Test
    fun edit_pickerCancel_savesNothingAndDoesNotCountAsAnEdit() =
        runTest {
            val (session) = session()
            val id = booted(session)

            session.cancelPicker(from = ThemeDocument.Default)
            session.saveStatus.value shouldBe SaveStatus.Idle
            settle()
            projects.load(id).shouldNotBeNull().revision shouldBe 1

            // Nothing here was edited lately, so another tab's save comes in as an undo step.
            saveFromAnotherTab(id, FOREST)
            session.conflict.value shouldBe null
            session.document.value shouldBe FOREST
            session.undo()
            session.document.value shouldBe ThemeDocument.Default
        }

    @Test
    fun edit_dragBackToTheStartAfterAnEdit_keepsTheEditAsTheOneToSave() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            val edited = session.document.value

            session.cancelPicker(from = edited)
            settle()

            projects.load(id).shouldNotBeNull().document shouldBe edited
            session.saveStatus.value shouldBe SaveStatus.Idle
            session.undo()
            session.document.value shouldBe ThemeDocument.Default
        }

    @Test
    fun write_heldBackByAConflict_stopsReadingAsSavingUntilItSettles() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            saveFromAnotherTab(id, FOREST)
            settle()

            session.conflict.value.shouldNotBeNull()
            session.saveStatus.value shouldBe SaveStatus.Held
            // An edit under the conflict is held straight away, so it never reads as saving.
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            session.saveStatus.value shouldBe SaveStatus.Held

            session.resolveConflict(keepMine = true)
            session.saveStatus.value shouldBe SaveStatus.Pending
            settle()
            session.saveStatus.value shouldBe SaveStatus.Idle
        }

    @Test
    fun updateView_betweenThePreDeleteFlushAndTheDelete_leavesNothingOfTheProject() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.flush().join()

            session.updateView { view -> view.copy(tab = PreviewTab.Roles) }
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            projects.delete(id).shouldBeInstanceOf<Deletion.Deleted>()
            settle()

            stores.textAt(StorageKeys.project(id)) shouldBe null
            stores.textAt(StorageKeys.history(id)) shouldBe null
            stores.textAt(StorageKeys.view(id)) shouldBe null
        }

    /**
     * Drag the seed around in the picker and cancel, which puts [from]'s seed back as a release.
     */
    private fun ProjectSession.cancelPicker(from: ThemeDocument) {
        edit(DocumentChange.SetSeed(FOREST.seed, SeedSource.Picked), EditPhase.Dragging)
        edit(DocumentChange.SetSeed(MEADOW.seed, SeedSource.Picked), EditPhase.Dragging)
        edit(DocumentChange.SetSeed(from.seed, from.seedSource), EditPhase.Released)
    }
}
