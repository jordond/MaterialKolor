package com.materialkolor.builder.feature.projects

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsModelTest : SessionTestBase() {
    private val harness = ViewModelHarness()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun delete_thenUndoWithinEightSeconds_restoresTheProject() =
        runTest {
            val (session, preferences) = session()
            val first = booted(session)
            session.newProject(copyCurrent = false)
            val model = model(session, preferences)
            runCurrent()

            model.handle(ProjectsAction.Delete(first))
            runCurrent()
            listedIds() shouldNotContain first
            model.state.value.pendingDeletion
                .shouldNotBeNull()
                .meta.id shouldBe first

            advanceTimeBy(UNDO_WINDOW_MILLIS - 1)
            model.handle(ProjectsAction.UndoDelete)
            runCurrent()

            listedIds() shouldContain first
            projects.load(first).shouldNotBeNull()
            model.state.value.pendingDeletion
                .shouldBeNull()
            harness.clearAndJoin()
        }

    @Test
    fun delete_undoAfterEightSeconds_leavesTheProjectDeleted() =
        runTest {
            val (session, preferences) = session()
            val first = booted(session)
            session.newProject(copyCurrent = false)
            val model = model(session, preferences)
            runCurrent()

            model.handle(ProjectsAction.Delete(first))
            runCurrent()
            advanceTimeBy(UNDO_WINDOW_MILLIS + 1)
            model.state.value.pendingDeletion
                .shouldBeNull()
            model.handle(ProjectsAction.UndoDelete)
            runCurrent()

            listedIds() shouldNotContain first
            harness.clearAndJoin()
        }

    @Test
    fun delete_theOpenProject_opensTheNewestOtherOneFirst() =
        runTest {
            val (session, preferences) = session()
            val first = booted(session)
            session.newProject(copyCurrent = false)
            val second = session.project.value
                .shouldBeInstanceOf<ProjectRef.Persisted>()
                .id
            val model = model(session, preferences)
            runCurrent()

            model.handle(ProjectsAction.Delete(second))
            runCurrent()

            session.project.value shouldBe ProjectRef.Persisted(first)
            listedIds() shouldBe listOf(first)
            harness.clearAndJoin()
        }

    @Test
    fun search_pastEightProjects_showsAndNarrowsTheList() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            repeat(SEARCH_THRESHOLD - 1) { index -> projects.create("Theme $index", OCEAN, List(4) { OCEAN.seed }) }
            val model = model(session, preferences)
            runCurrent()
            model.state.value.projects shouldHaveSize SEARCH_THRESHOLD
            model.state.value.searchable shouldBe false

            projects.create("Forest floor", FOREST, List(4) { FOREST.seed })
            runCurrent()
            model.state.value.searchable shouldBe true

            model.handle(ProjectsAction.Search("forest"))
            model.state.value.shown
                .map { meta -> meta.name }
                .shouldBe(listOf("Forest floor"))
            harness.clearAndJoin()
        }

    @Test
    fun new_fromTheDefaults_takesTheSeedsAutoName() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = model(session, preferences)
            runCurrent()

            model.handle(ProjectsAction.New(copyCurrent = false))
            runCurrent()

            val id = session.project.value
                .shouldBeInstanceOf<ProjectRef.Persisted>()
                .id
            projects.load(id).shouldNotBeNull().name shouldBe ColorNames.nameOf(ThemeDocument.Default.seed)
            model.state.value.openId shouldBe id
            harness.clearAndJoin()
        }

    @Test
    fun rename_throughTheModel_renamesTheOpenProjectInTheSession() =
        runTest {
            val (session, preferences) = session()
            val first = booted(session)
            val model = model(session, preferences)
            runCurrent()

            model.handle(ProjectsAction.Rename(first, "  Harbour  "))
            runCurrent()

            session.projectName.first() shouldBe "Harbour"
            model.state.value.projects
                .single()
                .name shouldBe "Harbour"
            harness.clearAndJoin()
        }

    @Test
    fun persist_onTheSecondProject_isRequestedOnceForGood() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = model(session, preferences)
            runCurrent()
            environment.persistRequests shouldBe 0

            model.handle(ProjectsAction.New(copyCurrent = false))
            runCurrent()
            environment.persistRequests shouldBe 1

            model.handle(ProjectsAction.New(copyCurrent = true))
            runCurrent()
            model(session, preferences)
            runCurrent()

            environment.persistRequests shouldBe 1
            preferences.current().persistRequested shouldBe true
            harness.clearAndJoin()
        }

    @Test
    fun persist_withoutStorage_isNeverRequested() =
        runTest {
            environment.storageAvailable = false
            val (session, preferences) = session()
            booted(session)
            session.newProject(copyCurrent = false)
            val model = model(session, preferences)
            runCurrent()

            environment.persistRequests shouldBe 0
            model.state.value.storageAvailable shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun saveShared_aThemeFromALink_savesItToTheDrawer() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            session.openShared(ShareCodec.encode(OCEAN, "Harbour")).shouldBeNull()
            val model = model(session, preferences)
            runCurrent()
            model.state.value.transient shouldBe true

            model.handle(ProjectsAction.SaveShared)
            runCurrent()

            model.state.value.transient shouldBe false
            model.state.value.projects
                .map { meta -> meta.name }
                .shouldContain("Harbour")
            harness.clearAndJoin()
        }

    @Test
    fun projectAge_acrossUnits_picksTheLargestWholeUnit() {
        val minute = 60_000L
        val day = 24 * 60 * minute
        ProjectAge.of(savedAt = 0, now = 59_000) shouldBe ProjectAge.JustNow
        ProjectAge.of(savedAt = 10 * minute, now = 0) shouldBe ProjectAge.JustNow
        ProjectAge.of(savedAt = 0, now = 5 * minute) shouldBe ProjectAge.Minutes(5)
        ProjectAge.of(savedAt = 0, now = 3 * 60 * minute) shouldBe ProjectAge.Hours(3)
        ProjectAge.of(savedAt = 0, now = 2 * day) shouldBe ProjectAge.Days(2)
        ProjectAge.of(savedAt = 0, now = 15 * day) shouldBe ProjectAge.Weeks(2)
        ProjectAge.of(savedAt = 0, now = 90 * day) shouldBe ProjectAge.Months(3)
        ProjectAge.of(savedAt = 0, now = 800 * day) shouldBe ProjectAge.Years(2)
    }

    private fun model(
        session: ProjectSession,
        preferences: PreferencesRepository,
    ): ProjectsModel = harness.own(ProjectsModel(session, projects, preferences, environment, StoppedClock))

    private suspend fun listedIds(): List<String> =
        projects.index
            .first()
            .projects
            .map { meta -> meta.id }

    private object StoppedClock : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(0)
    }
}
