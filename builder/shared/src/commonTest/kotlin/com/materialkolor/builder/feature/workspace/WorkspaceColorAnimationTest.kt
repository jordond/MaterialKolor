package com.materialkolor.builder.feature.workspace

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The poster's color animation option writes the export options of one target and nothing else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceColorAnimationTest : SessionTestBase() {
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
    fun setColorAnimation_writesOnlyThatTargetWithNoUndoEntry() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = workspaceModel(session, preferences)
            val document = workspace.state.value.document

            workspace.setColorAnimation(ExportTarget.Fluent, on = true)
            workspace.setColorAnimationDuration(ExportTarget.Fluent, durationMs = 500)
            runCurrent()

            val stored = preferences.preferences.value
            stored.exportPrefs.keys shouldBe setOf(ExportTarget.Fluent)
            stored.exportPrefsFor(ExportTarget.Fluent) shouldBe ExportPrefs(animate = true, animationDurationMs = 500)
            stored.exportPrefsFor(ExportTarget.Material3) shouldBe ExportPrefs()
            workspace.state.value.preferences shouldBe stored
            workspace.state.value.document shouldBe document
            workspace.state.value.history.canUndo shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun setColorAnimation_off_keepsTheRestOfThatTarget() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            preferences.updateExportPrefs(ExportTarget.Custom) { prefs -> prefs.copy(packageName = "com.brand") }
            val workspace = workspaceModel(session, preferences)

            workspace.setColorAnimation(ExportTarget.Custom, on = true)
            workspace.setColorAnimation(ExportTarget.Custom, on = false)
            runCurrent()

            preferences.preferences.value.exportPrefsFor(ExportTarget.Custom) shouldBe
                ExportPrefs(packageName = "com.brand", animate = false)
            harness.clearAndJoin()
        }

    private fun workspaceModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
    ): WorkspaceModel =
        harness.own(WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver()))
}
