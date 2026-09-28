package com.materialkolor.builder.feature.workspace

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.fakes.RouterCall
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.topbar.LibraryChoice
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceModelTest : SessionTestBase() {
    private val harness = ViewModelHarness()
    private val router = FakeRouter()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setAppearance_dark_leavesThePreviewModeAlone() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val app = appModel(session, preferences)
            val workspace = workspaceModel(session, preferences)

            workspace.setAppearance(Appearance.Dark)
            runCurrent()

            app.state.value.isDark shouldBe true
            session.viewState.value.mode shouldBe PreviewMode.Split
            workspace.state.value.view.mode shouldBe PreviewMode.Split
            harness.clearAndJoin()
        }

    @Test
    fun setPreviewMode_dark_leavesTheChromeAppearanceAlone() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val app = appModel(session, preferences)
            val workspace = workspaceModel(session, preferences)

            workspace.setPreviewMode(PreviewMode.Dark)
            runCurrent()

            workspace.state.value.view.mode shouldBe PreviewMode.Dark
            app.state.value.isDark shouldBe false
            preferences.preferences.value.appearance shouldBe Appearance.System
            harness.clearAndJoin()
        }

    @Test
    fun edit_libraryToFluent_disablesItsControlsBeforeReturning() =
        runTest {
            // Nothing the model launches runs until the test lets it, so only a synchronous update counts.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            runCurrent()
            val seen = mutableListOf<WorkspaceModel.State>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { workspace.state.collect(seen::add) }

            workspace.edit(DocumentChange.SetLibrary(Library.Fluent, expressive = false), EditPhase.Discrete)

            val state = workspace.state.value
            state.document.library shouldBe Library.Fluent
            state.capabilities[Control.Contrast].shouldBeInstanceOf<ControlState.Disabled>()
            state.history.canUndo shouldBe true
            runCurrent()
            seen.forEach { each -> each.capabilities shouldBe capabilitiesOf(each.document) }
            harness.clearAndJoin()
        }

    @Test
    fun edit_librarySwitch_isOneUndoEntry() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)

            workspace.edit(DocumentChange.SetLibrary(Library.Material3, expressive = true), EditPhase.Discrete)
            workspace.state.value.history.undoLabel shouldBe ChangeLabel(ChangeKind.Library, detail = "Material3")
            workspace.undo()

            val state = workspace.state.value
            state.document.expressive shouldBe false
            state.history.canUndo shouldBe false
            state.history.canRedo shouldBe true
            harness.clearAndJoin()
        }

    @Test
    fun pickLibrary_ontoM3Expressive_movesLibraryStyleAndSpecAsOneUndoStep() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            val start = fluentOn2021(workspace)
            workspace.edit(DocumentChange.Replace(start), EditPhase.Discrete)

            workspace.pickLibrary(LibraryChoice.M3Expressive)
            val entered = workspace.state.value
            entered.document shouldBe
                start.copy(
                    library = Library.Material3,
                    expressive = true,
                    style = Style.Expressive,
                    spec = SpecVersion.Spec2025,
                )
            entered.history.undoLabel shouldBe ChangeLabel(ChangeKind.Library, detail = "Material3")
            workspace.undo()

            workspace.state.value.document shouldBe start
            workspace.redo()
            workspace.state.value.document shouldBe entered.document
            harness.clearAndJoin()
        }

    @Test
    fun pickLibrary_leavingM3Expressive_restoresTheStyleAndSpecItEnteredFrom() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            val start = fluentOn2021(workspace).copy(style = Style.Vibrant)
            workspace.edit(DocumentChange.Replace(start), EditPhase.Discrete)
            workspace.pickLibrary(LibraryChoice.M3Expressive)
            workspace.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)

            workspace.pickLibrary(LibraryChoice.Unstyled)

            val left = workspace.state.value.document
            left.library shouldBe Library.Unstyled
            left.expressive shouldBe false
            left.style shouldBe Style.Vibrant
            left.spec shouldBe SpecVersion.Spec2021
            harness.clearAndJoin()
        }

    @Test
    fun pickLibrary_leavingAgainAfterUndoingALeave_restoresTheSameStyle() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            workspace.edit(DocumentChange.SetStyle(Style.Content), EditPhase.Discrete)
            workspace.pickLibrary(LibraryChoice.M3Expressive)
            workspace.pickLibrary(LibraryChoice.M3)
            workspace.state.value.document.style shouldBe Style.Content
            workspace.undo()
            workspace.state.value.document.style shouldBe Style.Expressive

            workspace.pickLibrary(LibraryChoice.Custom)

            workspace.state.value.document.style shouldBe Style.Content
            workspace.state.value.document.library shouldBe Library.Custom
            harness.clearAndJoin()
        }

    @Test
    fun pickLibrary_theChoiceTheDocumentIsOn_changesNothing() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            val document = workspace.state.value.document

            workspace.pickLibrary(LibraryChoice.of(document))

            workspace.state.value.document shouldBe document
            workspace.state.value.history.canUndo shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun edit_thatChangesNothing_leavesNoUndoEntry() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)

            workspace.edit(DocumentChange.SetLibrary(Library.Material3, expressive = false), EditPhase.Discrete)
            workspace.edit(DocumentChange.SetContrast(ContrastLevel.Standard), EditPhase.Released)

            workspace.state.value.history.canUndo shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun openPanel_thenBack_closesItAfterOneHistoryEntry() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)

            workspace.openPanel(Panel.Export)
            workspace.openPanel(Panel.Palette)
            router.back()
            runCurrent()

            router.calls shouldBe listOf(RouterCall.PushOverlay("Export"))
            workspace.state.value.panel shouldBe null
            harness.clearAndJoin()
        }

    @Test
    fun closePanel_fromTheUi_popsItsEntryOnceAndALaterBackDoesNothingMore() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)

            workspace.openPanel(Panel.Export)
            workspace.closePanel()
            workspace.closePanel()
            router.back()
            runCurrent()

            router.calls shouldBe listOf(RouterCall.PushOverlay("Export"), RouterCall.PopOverlay)
            workspace.state.value.panel shouldBe null
            harness.clearAndJoin()
        }

    @Test
    fun openPicker_overAnOpenPanel_pushesNothing() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)

            workspace.openPanel(Panel.Export)
            workspace.openPicker(PickerTarget.Seed)

            router.calls shouldBe listOf(RouterCall.PushOverlay("Export"))
            workspace.state.value.panel shouldBe Panel.Picker
            workspace.state.value.pickerTarget shouldBe PickerTarget.Seed
            harness.clearAndJoin()
        }

    @Test
    fun openPanel_history_carriesTheTimelineAndLeavesFullscreen() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            workspace.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            workspace.toggleFullscreen()
            workspace.state.value.timeline shouldBe null

            workspace.openPanel(Panel.History)

            val opened = workspace.state.value
            opened.fullscreen shouldBe false
            val timeline = opened.timeline.shouldNotBeNull()
            timeline.cursor shouldBe 1
            timeline.steps.single().after shouldBe ThemeDocument.Default.copy(amoled = true)
            router.calls shouldBe listOf(RouterCall.PushOverlay("History"))
            workspace.undo()
            workspace.state.value.timeline
                .shouldNotBeNull()
                .cursor shouldBe 0
            harness.clearAndJoin()
        }

    @Test
    fun jumpTo_movesTheCursor() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            workspace.edit(DocumentChange.Replace(fluentOn2021(workspace)), EditPhase.Discrete)
            workspace.pickLibrary(LibraryChoice.M3Expressive)
            workspace.openPanel(Panel.History)

            workspace.jumpTo(1)

            val state = workspace.state.value
            state.document.library shouldBe Library.Fluent
            state.document.spec shouldBe SpecVersion.Spec2021
            state.history.canRedo shouldBe true
            state.panel shouldBe Panel.History
            val timeline = state.timeline.shouldNotBeNull()
            timeline.cursor shouldBe 1
            timeline.steps.size shouldBe 2
            harness.clearAndJoin()
        }

    @Test
    fun closePanel_fromHistory_dropsTheTimeline() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            workspace.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            workspace.openPanel(Panel.History)
            workspace.state.value.timeline
                .shouldNotBeNull()

            workspace.closePanel()
            workspace.edit(DocumentChange.SetContrast(ContrastLevel.High), EditPhase.Discrete)
            workspace.state.value.timeline shouldBe null
            workspace.openPanel(Panel.History)
            router.back()
            runCurrent()

            workspace.state.value.panel shouldBe null
            workspace.state.value.timeline shouldBe null
            harness.clearAndJoin()
        }

    @Test
    fun setPreviewTab_movesOnlyTheView_andKeepsTheCapabilitiesAndTheTimeline() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            workspace.edit(DocumentChange.SetAmoled(true), EditPhase.Discrete)
            workspace.openPanel(Panel.History)
            val before = workspace.state.value
            val timeline = before.timeline.shouldNotBeNull()

            workspace.setPreviewTab(PreviewTab.Palettes)

            val after = workspace.state.value
            after.view.tab shouldBe PreviewTab.Palettes
            after.capabilities shouldBeSameInstanceAs before.capabilities
            after.timeline shouldBeSameInstanceAs timeline
            harness.clearAndJoin()
        }

    @Test
    fun state_afterBoot_carriesTheProjectNameAndSaveStatus() =
        runTest {
            val (session, preferences) = session()
            val id = booted(session)
            val workspace = workspaceModel(session, preferences)

            session.rename(id, "Harbor")
            workspace.edit(DocumentChange.SetContrast(ContrastLevel.High), EditPhase.Discrete)
            runCurrent()

            workspace.state.value.projectName shouldBe "Harbor"
            workspace.state.value.saveStatus shouldBe SaveStatus.Pending
            settle()
            workspace.state.value.saveStatus shouldBe SaveStatus.Idle
            harness.clearAndJoin()
        }

    @Test
    fun boot_calledTwice_opensOnceAndPutsHomeBack() =
        runTest {
            val (session, preferences) = session()
            val app = appModel(session, preferences)

            app.boot()
            app.boot()

            router.calls shouldBe listOf(RouterCall.ReplaceHome)
            app.state.value.bootNotice shouldBe null
            harness.clearAndJoin()
        }

    @Test
    fun isDark_followingTheSystem_changesWithIt() =
        runTest {
            val (session, preferences) = session()
            val app = appModel(session, preferences)

            environment.prefersDark.value = true

            app.state.value.isDark shouldBe true
            harness.clearAndJoin()
        }

    @Test
    fun pageHide_afterAnEdit_writesTheRecordBeforeAnythingElseRuns() =
        runTest {
            // Main only runs when asked, so a collector dispatched to it would sit until runCurrent.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val (session, preferences) = session()
            val id = booted(session)
            appModel(session, preferences)
            runCurrent()
            session.edit(DocumentChange.SetThemeName("HiddenTheme"), EditPhase.Discrete)
            runCurrent()

            // No advance and no runCurrent from here, so only an undispatched collector gets the write out.
            environment.pageHides.tryEmit(Unit) shouldBe true

            projects.load(id)?.document?.themeName shouldBe "HiddenTheme"
            harness.clearAndJoin()
        }

    @Test
    fun projectGeneration_movesOnceWithEachProjectShownAndNeverForAnEditARenameOrAFirstSave() =
        runTest {
            val (session, preferences) = session()
            val first = booted(session)
            val workspace = workspaceModel(session, preferences)
            val seen = mutableListOf<WorkspaceModel.State>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                workspace.state.collect { state -> seen.add(state) }
            }
            val start = workspace.state.value.projectGeneration

            // Only the state carrying the new project's document may carry the new number, and it must.
            suspend fun shows(
                generation: Int,
                document: () -> ThemeDocument,
                open: suspend () -> Unit,
            ) {
                val from = seen.size
                open()
                settle()
                val arrived = seen.drop(from).first { state -> state.document == document() }
                arrived.projectGeneration shouldBe generation
                workspace.state.value.projectGeneration shouldBe generation
            }

            workspace.edit(DocumentChange.SetThemeName("EditedTheme"), EditPhase.Discrete)
            session.rename(first, "Renamed")
            settle()
            workspace.state.value.projectGeneration shouldBe start
            val edited = session.document.value

            shows(start + 1, { firstNewDocument("Renamed") }) { session.newProject(copyCurrent = false) }
            shows(start + 2, { edited }) { session.open(first) }
            val shared = ThemeDocument.Default.copy(themeName = "SharedTheme")
            shows(start + 3, { shared }) { session.openShared(ShareCodec.encode(shared)) }

            // Its first save makes the shared theme a saved project, which is still the same project.
            workspace.edit(DocumentChange.SetThemeName("SavedTheme"), EditPhase.Discrete)
            settle()
            session.project.value.shouldBeInstanceOf<ProjectRef.Persisted>()
            workspace.state.value.projectGeneration shouldBe start + 3
            harness.clearAndJoin()
        }

    @Test
    fun projectGeneration_standardMain_neverPairsADocumentWithAnotherProjectsNumber() =
        runTest {
            // The model's collectors run once the session lets go of the thread, as they do on the page.
            projectsNeverMix(StandardTestDispatcher(testScheduler))
        }

    @Test
    fun projectGeneration_unconfinedMain_neverPairsADocumentWithAnotherProjectsNumber() =
        runTest {
            // The model's collectors run inside the session's own update, before show() returns.
            projectsNeverMix(UnconfinedTestDispatcher(testScheduler))
        }

    /**
     * Show three more projects with [main] as the model's dispatcher and check that every state the
     * model emits pairs a document only with the number of the project it belongs to, in order.
     */
    private suspend fun TestScope.projectsNeverMix(main: TestDispatcher) {
        Dispatchers.setMain(main)
        val (session, preferences) = session()
        val first = booted(session)
        val workspace = workspaceModel(session, preferences)
        runCurrent()
        workspace.edit(DocumentChange.SetThemeName("EditedTheme"), EditPhase.Discrete)
        settle()
        val edited = session.document.value
        val start = workspace.state.value.projectGeneration
        val seen = mutableListOf<Pair<Int, ThemeDocument>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            workspace.state.collect { state ->
                val pair = state.projectGeneration to state.document
                if (seen.lastOrNull() != pair) seen.add(pair)
            }
        }
        val shared = ThemeDocument.Default.copy(themeName = "SharedTheme")

        session.newProject(copyCurrent = false)
        settle()
        session.open(first)
        settle()
        session.openShared(ShareCodec.encode(shared))
        settle()

        seen shouldBe listOf(
            start to edited,
            start + 1 to firstNewDocument(ColorNames.nameOf(DEFAULT_SEED)),
            start + 2 to edited,
            start + 3 to shared,
        )
        harness.clearAndJoin()
    }

    /**
     * The workspace's theme on Fluent and the 2021 spec, so a pick of M3 Expressive moves both the
     * style and the spec.
     */
    private fun fluentOn2021(workspace: WorkspaceModel): ThemeDocument =
        workspace.state.value.document
            .copy(library = Library.Fluent, spec = SpecVersion.Spec2021)

    private fun appModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
    ): AppModel = harness.own(AppModel(session, router, preferences, environment))

    private fun workspaceModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
    ): WorkspaceModel = harness.own(WorkspaceModel(session, preferences, FakeClipboard(), router, ThemeResolver()))
}
