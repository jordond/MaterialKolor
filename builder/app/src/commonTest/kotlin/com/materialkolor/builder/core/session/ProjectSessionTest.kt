package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.fakes.FakeEnvironment
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionTest {
    private var ids = 0
    private val stores = InMemoryStoreFactory(now = { 0 })
    private val environment = FakeEnvironment(tabId = TAB)
    private val projects = ProjectRepository(stores, tabId = TAB, now = { 0 }, newId = { "p${++ids}" })

    @Test
    fun boot_firstVisit_createsTheDefaultProject() =
        runTest {
            val (session, preferences) = session()

            session.boot(Route.Home, tabProjectId = null) shouldBe null

            val ref = session.project.value.shouldBeInstanceOf<ProjectRef.Persisted>()
            val meta = projects.index
                .first()
                .projects
                .single()
            meta.name shouldBe ColorNames.nameOf(DEFAULT_SEED)
            meta.library shouldBe Library.Material3
            meta.expressive shouldBe false
            session.document.value shouldBe ThemeDocument.Default
            session.viewState.value.mode shouldBe PreviewMode.Split
            preferences.current().lastProjectId shouldBe ref.id
        }

    @Test
    fun boot_legacyLink_opensItWithDarkPreviewAndRemembersThePackage() =
        runTest {
            val (session, preferences) = session()

            session.boot(Route.Legacy("color_seed=FF1565C0&dark_mode=true&package_name=com.ocean"), tabProjectId = null)

            session.project.value.shouldBeInstanceOf<ProjectRef.Transient>()
            session.document.value.seed shouldBe OCEAN.seed
            session.viewState.value.mode shouldBe PreviewMode.Dark
            preferences.exportPrefs(ExportTarget.Material3).packageName shouldBe "com.ocean"
        }

    @Test
    fun boot_badLink_startsANewProjectAndSaysTheLinkWasBad() =
        runTest {
            val (session) = session()

            session.boot(Route.Theme("not-a-code"), tabProjectId = null) shouldBe BootNotice.InvalidLink

            session.project.value.shouldBeInstanceOf<ProjectRef.Persisted>()
        }

    @Test
    fun openShared_sameLinkTwice_staysOneUnsavedProject() =
        runTest {
            val (session) = session()
            val code = ShareCodec.encode(OCEAN, projectName = "Ocean")

            session.openShared(code) shouldBe null
            session.openShared(code) shouldBe null
            advanceUntilIdle()

            session.project.value shouldBe ProjectRef.Transient(code)
            session.document.value shouldBe OCEAN
            projects.index.first().projects shouldBe emptyList()
        }

    @Test
    fun edit_onASharedTheme_savesItUnderTheLinksName() =
        runTest {
            val (session) = session()
            session.openShared(ShareCodec.encode(OCEAN, projectName = "Ocean"))

            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            session.project.value.shouldBeInstanceOf<ProjectRef.Transient>()
            settle()

            val ref = session.project.value.shouldBeInstanceOf<ProjectRef.Persisted>()
            val saved = projects.load(ref.id).shouldNotBeNull()
            saved.name shouldBe "Ocean"
            saved.document shouldBe OCEAN.copy(amoled = true)
            projects.loadHistory(ref.id).entries.size shouldBe 1
        }

    @Test
    fun edit_onASharedThemeWithoutAName_savesItAsSharedTheme() =
        runTest {
            val (session) = session()
            session.openShared(ShareCodec.encode(OCEAN))

            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()

            projects.index
                .first()
                .projects
                .single()
                .name shouldBe SHARED_THEME_NAME
        }

    @Test
    fun openShared_linkWhoseThemeWasSaved_opensTheSavedProject() =
        runTest {
            val (session) = session()
            val code = ShareCodec.encode(OCEAN)
            session.openShared(code)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()
            val saved = session.project.value
            session.undo()
            settle()
            session.newProject(copyCurrent = false)

            session.openShared(code) shouldBe null

            session.project.value shouldBe saved
            session.document.value shouldBe OCEAN
        }

    @Test
    fun edit_twoChangesInABurst_savesOnceHalfASecondAfterTheLast() =
        runTest {
            val (session) = session()
            val id = booted(session)

            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            advanceTimeBy(300)
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            advanceTimeBy(AUTOSAVE_DELAY_MILLIS - 1)
            runCurrent()
            projects.load(id).shouldNotBeNull().revision shouldBe 1
            session.saveStatus.value shouldBe SaveStatus.Pending

            advanceTimeBy(1)
            runCurrent()
            val saved = projects.load(id).shouldNotBeNull()
            saved.revision shouldBe 2
            saved.document shouldBe ThemeDocument.Default.copy(amoled = true, style = Style.Vibrant)
            session.saveStatus.value shouldBe SaveStatus.Idle
            advanceUntilIdle()
            projects.load(id).shouldNotBeNull().revision shouldBe 2
        }

    @Test
    fun edit_whileDragging_savesOnceAfterTheRelease() =
        runTest {
            val (session) = session()
            val id = booted(session)

            listOf(0xFF102030, 0xFF203040, 0xFF304050).forEach { color ->
                session.edit(DocumentChange.SetSeed(Argb(color.toInt()), SeedSource.Picked), EditPhase.Dragging)
                advanceTimeBy(AUTOSAVE_DELAY_MILLIS * 2)
            }
            projects.load(id).shouldNotBeNull().revision shouldBe 1
            environment.splashColors.size shouldBe 1

            session.edit(DocumentChange.SetSeed(OCEAN.seed, SeedSource.Picked), EditPhase.Released)
            settle()

            projects.load(id).shouldNotBeNull().revision shouldBe 2
            projects.loadHistory(id).entries.size shouldBe 1
            environment.splashColors.last() shouldBe (OCEAN.seed to DARK_SPLASH)
        }

    @Test
    fun flush_withAnEditWaiting_writesItRightAway() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)

            session.flush().join()

            testScheduler.currentTime shouldBe 0
            projects
                .load(id)
                .shouldNotBeNull()
                .document.amoled shouldBe true
        }

    @Test
    fun savedElsewhere_afterAQuietSpell_takesTheirsAndUndoBringsMineBack() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            advanceTimeBy(CONFLICT_WINDOW_MILLIS + AUTOSAVE_DELAY_MILLIS)
            runCurrent()
            val mine = session.document.value

            saveFromAnotherTab(id, FOREST)

            session.conflict.value shouldBe null
            session.document.value shouldBe FOREST
            session.undo()
            session.document.value shouldBe mine
        }

    @Test
    fun savedElsewhere_rightAfterAnEdit_raisesAConflictAndKeepMineSavesMine() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)

            val theirs = saveFromAnotherTab(id, FOREST)
            settle()

            session.conflict.value shouldBe Conflict(theirs)
            projects.load(id).shouldNotBeNull().document shouldBe FOREST
            session.resolveConflict(keepMine = true)
            settle()
            session.conflict.value shouldBe null
            projects.load(id).shouldNotBeNull().document shouldBe ThemeDocument.Default.copy(amoled = true)
        }

    @Test
    fun resolveConflict_loadTheirs_takesTheirsAsAnUndoStep() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            saveFromAnotherTab(id, FOREST)

            session.resolveConflict(keepMine = false)
            settle()

            session.document.value shouldBe FOREST
            projects.load(id).shouldNotBeNull().document shouldBe FOREST
            session.undo()
            session.document.value.amoled shouldBe true
        }

    @Test
    fun rename_openProjectWithASaveWaiting_keepsTheNewName() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)

            session.rename(id, "Renamed") shouldBe null
            settle()

            val saved = projects.load(id).shouldNotBeNull()
            saved.name shouldBe "Renamed"
            saved.document.amoled shouldBe true
            projects.index
                .first()
                .projects
                .single()
                .name shouldBe "Renamed"
        }

    @Test
    fun saveStatus_storageRefusesTheSave_reportsTheFailure() =
        runTest {
            val (session) = session()
            booted(session)
            stores.failNextUpdates(count = 1, StoreError.Unavailable)

            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()

            session.saveStatus.value shouldBe SaveStatus.Failed(StoreError.Unavailable)
            session.flush().join()
            session.saveStatus.value shouldBe SaveStatus.Idle
        }

    @Test
    fun updateView_twoChangesInABurst_savesTheViewOnce() =
        runTest {
            val (session) = session()
            val id = booted(session)

            session.updateView { view -> view.copy(tab = PreviewTab.Roles) }
            session.updateView { view -> view.copy(mode = PreviewMode.Dark) }
            settle()

            val view = projects.viewState(id)
            view.tab shouldBe PreviewTab.Roles
            view.mode shouldBe PreviewMode.Dark
        }

    @Test
    fun open_projectWithSavedHistory_canUndoRightAway() =
        runTest {
            val (session) = session()
            val id = booted(session)
            session.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            settle()
            session.newProject(copyCurrent = true)
            session.history.value.canUndo shouldBe false

            session.open(id) shouldBe true

            session.history.value.canUndo shouldBe true
            session.undo()
            session.document.value shouldBe ThemeDocument.Default
        }

    private fun TestScope.session(): Pair<ProjectSession, PreferencesRepository> {
        val preferences = PreferencesRepository(stores, backgroundScope)
        val session = ProjectSession(
            projects = projects,
            preferences = preferences,
            environment = environment,
            colorsOf = { document -> colorsOf(document) },
            scope = backgroundScope,
            now = { testScheduler.currentTime },
        )
        return session to preferences
    }

    /** Boot on a first visit and let the session start watching other tabs. */
    private suspend fun TestScope.booted(session: ProjectSession): String {
        session.boot(Route.Home, tabProjectId = null)
        runCurrent()
        return session.project.value
            .shouldBeInstanceOf<ProjectRef.Persisted>()
            .id
    }

    private fun TestScope.settle() {
        advanceTimeBy(AUTOSAVE_DELAY_MILLIS)
        runCurrent()
    }

    private suspend fun TestScope.saveFromAnotherTab(
        id: String,
        document: ThemeDocument,
    ): ProjectRecord {
        val theirs = projects.load(id).shouldNotBeNull().copy(document = document, revision = 10, writerTab = "other")
        stores.writeFromAnotherTab(StorageKeys.project(id), ProjectRecord.Codec.encode(theirs))
        runCurrent()
        return theirs
    }

    private fun colorsOf(document: ThemeDocument): SessionColors =
        SessionColors(List(4) { document.seed }, splashLight = document.seed, splashDark = DARK_SPLASH)

    private companion object {
        const val TAB = "this-tab"
        val OCEAN = ThemeDocument(seed = Argb(0xFF1565C0.toInt()))
        val FOREST = ThemeDocument(seed = Argb(0xFF2E7D32.toInt()))
        val DARK_SPLASH = Argb(0xFF101010.toInt())
    }
}
