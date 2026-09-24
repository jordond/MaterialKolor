package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.feature.workspace.WorkspaceModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class PosterCollapseTest : SessionTestBase() {
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
    fun setPosterCollapsed_collapsing_persistsForTheNextVisit() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val workspace = harness.own(
                WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver()),
            )

            workspace.setPosterCollapsed(true)
            runCurrent()

            workspace.state.value.preferences.posterCollapsed shouldBe true
            PreferencesRepository(stores, backgroundScope).current().posterCollapsed shouldBe true

            workspace.setPosterCollapsed(false)
            runCurrent()

            PreferencesRepository(stores, backgroundScope).current().posterCollapsed shouldBe false
            harness.clearAndJoin()
        }
}
