package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// b-508
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionTimelineTest : SessionTestBase() {
    @Test
    fun jumpTo_olderStep_showsAndSavesItsDocumentAndOnlyThePast() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            session.edit(DocumentChange.SetThemeName("Harbor"), EditPhase.Discrete)
            settle()

            session.jumpTo(1)
            settle()

            val amoled = ThemeDocument.Default.copy(amoled = true)
            session.document.value shouldBe amoled
            session.history.value.canRedo shouldBe true
            projects.load(id).shouldNotBeNull().document shouldBe amoled
            projects.loadHistory(id).entries.map { entry -> entry.after } shouldBe listOf(amoled)
            session.timeline().steps.size shouldBe 3
        }

    @Test
    fun jumpTo_staleCursor_isIgnored() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()
            val revision = projects.load(id).shouldNotBeNull().revision

            session.jumpTo(2)
            session.jumpTo(-1)
            session.jumpTo(1)
            settle()

            session.document.value shouldBe ThemeDocument.Default.copy(amoled = true)
            session.timeline().cursor shouldBe 1
            projects.load(id).shouldNotBeNull().revision shouldBe revision
        }

    @Test
    fun timeline_afterEdits_listsStepsOldestFirstWithTheirTimes() =
        runTest {
            val (session) = session()
            booted(session)
            session.timeline() shouldBe Timeline(cursor = 0, now = 0, start = ThemeDocument.Default, steps = emptyList())

            advanceTimeBy(1_000)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            advanceTimeBy(2_000)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            advanceTimeBy(500)
            session.undo()
            val timeline = session.timeline()

            timeline.start shouldBe ThemeDocument.Default
            timeline.cursor shouldBe 1
            timeline.now shouldBe 3_500L
            timeline.steps.map { step -> step.label.kind } shouldBe listOf(ChangeKind.Amoled, ChangeKind.Style)
            timeline.steps.map { step -> step.at } shouldBe listOf(1_000L, 3_000L)
        }

    @Test
    fun adopt_afterAJumpBack_dropsTheUndoneSteps() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            session.jumpTo(1)
            advanceTimeBy(CONFLICT_WINDOW_MILLIS + AUTOSAVE_DELAY_MILLIS)
            runCurrent()

            saveFromAnotherTab(id, FOREST)

            session.document.value shouldBe FOREST
            val timeline = session.timeline()
            timeline.cursor shouldBe 2
            timeline.steps.map { step -> step.after } shouldBe listOf(ThemeDocument.Default.copy(amoled = true), FOREST)
            session.history.value.canRedo shouldBe false
        }

    @Test
    fun open_otherProjectAndBack_keepsOnlyTheSavedPast() =
        runTest {
            val (session) = session()
            val first = booted(session)
            advanceTimeBy(1_000)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            session.jumpTo(1)
            settle()
            session.newProject(copyCurrent = false)
            session.timeline().steps shouldBe emptyList()

            session.open(first)

            val timeline = session.timeline()
            timeline.start shouldBe ThemeDocument.Default
            timeline.cursor shouldBe 1
            timeline.steps.map { step -> step.label.kind } shouldBe listOf(ChangeKind.Amoled)
            timeline.steps.single().at shouldBe 1_000L
            session.document.value shouldBe ThemeDocument.Default.copy(amoled = true)
        }
}
