package com.materialkolor.builder.feature.canvas

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

@OptIn(ExperimentalCoroutinesApi::class)
class ShowOnRampTest : SessionTestBase() {
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
    fun showOnRamp_opensPalettesWithTheTarget_andATabSwitchClearsIt() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = harness.own(
                WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver()),
            )
            val target = RampTarget.OfRole(Role.Primary, isDark = true)

            workspace.showOnRamp(target)
            runCurrent()

            workspace.state.value.view.tab shouldBe PreviewTab.Palettes
            workspace.state.value.rampHighlight shouldBe RampHighlight(target, workspace.state.value.projectGeneration)

            workspace.setPreviewTab(PreviewTab.Roles)
            runCurrent()

            workspace.state.value.view.tab shouldBe PreviewTab.Roles
            workspace.state.value.rampHighlight shouldBe null
            harness.clearAndJoin()
        }

    @Test
    fun showOnRamp_thenAnotherProject_leavesTheTargetOnTheOldGeneration() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = harness.own(
                WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver()),
            )
            val picked = workspace.state.value.projectGeneration

            workspace.showOnRamp(RampTarget.OfRole(Role.Primary, isDark = false))
            session.newProject(copyCurrent = false)
            settle()

            val state = workspace.state.value
            state.rampHighlight?.generation shouldBe picked
            state.projectGeneration shouldNotBe picked
            harness.clearAndJoin()
        }
}
