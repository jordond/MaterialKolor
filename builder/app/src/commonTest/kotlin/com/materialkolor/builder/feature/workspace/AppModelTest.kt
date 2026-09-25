package com.materialkolor.builder.feature.workspace

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.RoutePath
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.fakes.RouterCall
import com.materialkolor.builder.feature.projects.ProjectsModel
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

// b-314b

/**
 * The boot notices and the storage banners the app model keeps for the session (F-38), booted the
 * way the root boots it, from the route a [FakeRouter] read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModelTest : SessionTestBase() {
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
    fun boot_corruptLinkOnAFirstVisit_saysTheDefaultsAreOpen() =
        runTest {
            val router = FakeRouter(RoutePath.parse(CORRUPT_LINK, query = ""))
            val (session, preferences) = session()
            val app = appModel(session, preferences, router)

            app.boot()

            app.state.value.bootNotice shouldBe BootNotice.InvalidLink
            app.state.value.openedDefaults shouldBe true
            session.document.value shouldBe ThemeDocument.Default
            router.calls shouldBe listOf(RouterCall.ReplaceHome)
            bannersOf(app) shouldBe listOf(WorkspaceBanner.InvalidLinkDefaults)
            harness.clearAndJoin()
        }

    @Test
    fun boot_corruptLinkWithASavedTheme_opensItAndOffersTheDefaults() =
        runTest {
            val (first) = session()
            booted(first)
            first.edit(DocumentChange.SetThemeName("Harbour"), EditPhase.Discrete)
            settle()
            val router = FakeRouter(RoutePath.parse(CORRUPT_LINK, query = ""))
            val (session, preferences) = session()
            val app = appModel(session, preferences, router)

            app.boot()

            app.state.value.bootNotice shouldBe BootNotice.InvalidLink
            app.state.value.openedDefaults shouldBe false
            session.document.value.themeName shouldBe "Harbour"
            bannersOf(app) shouldBe listOf(WorkspaceBanner.InvalidLink)
            harness.clearAndJoin()
        }

    @Test
    fun boot_linkFromANewerBuild_reloadsAtThatLink() =
        runTest {
            val router = FakeRouter(RoutePath.parse(NEWER_LINK, query = ""))
            val (session, preferences) = session()
            val app = appModel(session, preferences, router)

            app.boot()
            app.reloadLink()

            app.state.value.bootNotice shouldBe BootNotice.NewerVersion
            bannersOf(app) shouldBe listOf(WorkspaceBanner.NewerVersion)
            environment.reloads shouldBe listOf(NEWER_LINK)
            harness.clearAndJoin()
        }

    @Test
    fun boot_unknownPath_saysSoUntilDismissed() =
        runTest {
            val router = FakeRouter(RoutePath.parse("/nope", query = ""))
            val (session, preferences) = session()
            val app = appModel(session, preferences, router)

            app.boot()
            bannersOf(app) shouldBe listOf(WorkspaceBanner.UnknownPath)
            app.dismissBootNotice()

            app.state.value.bootNotice
                .shouldBeNull()
            bannersOf(app).shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun boot_home_raisesNoNotice() =
        runTest {
            val router = FakeRouter(Route.Home)
            val (session, preferences) = session()
            val app = appModel(session, preferences, router)

            app.boot()

            app.state.value.bootNotice
                .shouldBeNull()
            bannersOf(app).shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun reloadHome_loadsTheBuilderAtHome() =
        runTest {
            val (session, preferences) = session()
            val app = appModel(session, preferences, FakeRouter(RoutePath.parse(NEWER_LINK, query = "")))

            app.reloadHome()

            environment.reloads shouldBe listOf("/")
            harness.clearAndJoin()
        }

    @Test
    fun saveStatus_storageStillFullAfterPruning_raisesTheBannerUntilASaveLands() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val app = appModel(session, preferences, FakeRouter())
            stores.failNextUpdates(count = 2, StoreError.QuotaExceeded)

            session.edit(DocumentChange.SetThemeName("Full"), EditPhase.Discrete)
            settle()
            app.state.value.storageFull shouldBe true
            bannersOf(app) shouldBe listOf(WorkspaceBanner.StorageFull)

            // The save that is waiting says nothing yet, so the banner stays up through it.
            session.edit(DocumentChange.SetThemeName("Fits"), EditPhase.Discrete)
            app.state.value.storageFull shouldBe true
            settle()

            app.state.value.storageFull shouldBe false
            bannersOf(app).shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun storageUnavailable_closed_staysClosedForTheSession() =
        runTest {
            environment.storageAvailable = false
            val (session, preferences) = session()
            val app = appModel(session, preferences, FakeRouter())
            app.boot()
            bannersOf(app, ProjectsModel.State(storageAvailable = false)) shouldBe
                listOf(WorkspaceBanner.StorageUnavailable)

            // The drawer says so itself, so the stack leaves it to the drawer while it is open.
            workspaceBanners(app.state.value, ProjectsModel.State(storageAvailable = false), drawerOpen = true)
                .shouldBeEmpty()
            app.dismissStorageUnavailable()

            app.state.value.storageUnavailable shouldBe true
            bannersOf(app, ProjectsModel.State(storageAvailable = false)).shouldBeEmpty()
            harness.clearAndJoin()
        }

    @Test
    fun workspaceBanners_everythingAtOnce_stackInTheirOrder() {
        val app = AppModel.State(
            appearance = Appearance.System,
            motion = MotionOverride.System,
            systemDark = false,
            systemReducedMotion = false,
            coarsePointer = false,
            bootNotice = BootNotice.NewerVersion,
            storageFull = true,
            storageUnavailable = true,
        )
        val projects = ProjectsModel.State(open = ProjectRef.Transient("code"), conflict = true, newerData = true)

        // b-314ba
        // The storage full banner keeps the unsaved theme's away, so the rest stack without it.
        workspaceBanners(app, projects) shouldBe listOf(
            WorkspaceBanner.Conflict,
            WorkspaceBanner.NewerVersion,
            WorkspaceBanner.StorageFull,
            WorkspaceBanner.StorageUnavailable,
            WorkspaceBanner.NewerData,
        )
        workspaceBanners(app.copy(storageFull = false), projects) shouldBe listOf(
            WorkspaceBanner.Conflict,
            WorkspaceBanner.NewerVersion,
            WorkspaceBanner.StorageUnavailable,
            WorkspaceBanner.UnsavedTheme,
            WorkspaceBanner.NewerData,
        )
    }

    // b-314ba

    @Test
    fun storageFull_hidesTheUnsavedThemeUntilASaveLands() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val app = appModel(session, preferences, FakeRouter())
            val transient = ProjectsModel.State(open = ProjectRef.Transient("code"))
            bannersOf(app, transient) shouldBe listOf(WorkspaceBanner.UnsavedTheme)
            stores.failNextUpdates(count = 2, StoreError.QuotaExceeded)

            session.edit(DocumentChange.SetThemeName("Full"), EditPhase.Discrete)
            settle()
            bannersOf(app, transient) shouldBe listOf(WorkspaceBanner.StorageFull)

            session.edit(DocumentChange.SetThemeName("Fits"), EditPhase.Discrete)
            settle()

            bannersOf(app, transient) shouldBe listOf(WorkspaceBanner.UnsavedTheme)
            harness.clearAndJoin()
        }

    @Test
    fun canReload_followsThePlatform() =
        runTest {
            val (session, preferences) = session()
            appModel(session, preferences, FakeRouter()).state.value.canReload shouldBe true

            environment.canReload = false

            appModel(session, preferences, FakeRouter()).state.value.canReload shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun newerData_dismissedWhereAReloadDoesNothing_staysAwayForTheSession() =
        runTest {
            environment.canReload = false
            val (session, preferences) = session()
            val app = appModel(session, preferences, FakeRouter())
            val newer = ProjectsModel.State(open = ProjectRef.Persisted("p1"), newerData = true)
            bannersOf(app, newer) shouldBe listOf(WorkspaceBanner.NewerData)

            app.dismissNewerData()

            bannersOf(app, newer).shouldBeEmpty()
            harness.clearAndJoin()
        }

    /**
     * What the stack shows for [app] beside a projects model with nothing to say by default.
     */
    private fun bannersOf(
        app: AppModel,
        projects: ProjectsModel.State = ProjectsModel.State(open = ProjectRef.Persisted("p1")),
    ): List<WorkspaceBanner> = workspaceBanners(app.state.value, projects)

    private fun appModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
        router: FakeRouter,
    ): AppModel = harness.own(AppModel(session, router, preferences, environment))

    private companion object {
        /**
         * No build wrote this, so the code reads as corrupt.
         */
        const val CORRUPT_LINK = "/t/abc123"

        /**
         * One byte, a version past this build's, so the code reads as a newer build's.
         */
        const val NEWER_LINK = "/t/Ag"
    }
}
