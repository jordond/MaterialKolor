package com.materialkolor.builder.feature.workspace

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.hct.Hct
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceShuffleTest : SessionTestBase() {
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
    fun shuffle_withTheStyleLocked_movesOnlyTheSeed() =
        runTest {
            val workspace = workspace(ShuffleLock.Style)
            val before = workspace.state.value.document

            workspace.applyShuffle(workspace.drawShuffle(Random(1)).shouldNotBeNull())

            val after = workspace.state.value.document
            after.seed shouldNotBe before.seed
            after.seedSource shouldBe SeedSource.Shuffled
            after.style shouldBe before.style
            harness.clearAndJoin()
        }

    @Test
    fun shuffle_withTheSeedLocked_movesOnlyTheStyle() =
        runTest {
            val workspace = workspace(ShuffleLock.Seed)
            val before = workspace.state.value.document

            workspace.applyShuffle(workspace.drawShuffle(Random(2)).shouldNotBeNull())

            val after = workspace.state.value.document
            after.seed shouldBe before.seed
            after.seedSource shouldBe before.seedSource
            after.style shouldNotBe before.style
            harness.clearAndJoin()
        }

    @Test
    fun shuffle_withTheHueLocked_keepsTheSeedsHue() =
        runTest {
            val workspace = workspace(ShuffleLock.Style, ShuffleLock.Hue)
            val before = workspace.state.value.document

            workspace.applyShuffle(workspace.drawShuffle(Random(3)).shouldNotBeNull())

            val after = workspace.state.value.document
            after.seed shouldNotBe before.seed
            val drift = abs(Hct.fromInt(after.seed.value).hue - Hct.fromInt(before.seed.value).hue)
            (min(drift, 360 - drift) <= HUE_TOLERANCE) shouldBe true
            harness.clearAndJoin()
        }

    @Test
    fun shuffle_withTheSeedAndStyleLocked_drawsNothing() =
        runTest {
            val workspace = workspace(ShuffleLock.Style, ShuffleLock.Seed)

            workspace.drawShuffle(Random(4)).shouldBeNull()

            workspace.state.value.history.canUndo shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun shuffle_twiceInARow_isOneUndoEntryEach() =
        runTest {
            val workspace = workspace(ShuffleLock.Style)
            val first = workspace.state.value.document

            workspace.applyShuffle(workspace.drawShuffle(Random(5)).shouldNotBeNull())
            workspace.applyShuffle(workspace.drawShuffle(Random(6)).shouldNotBeNull())
            workspace.state.value.history.undoLabel
                ?.kind shouldBe ChangeKind.Replace
            workspace.undo()
            workspace.state.value.history.canUndo shouldBe true
            workspace.undo()

            workspace.state.value.document shouldBe first
            workspace.state.value.history.canUndo shouldBe false
            harness.clearAndJoin()
        }

    /** A booted workspace whose shuffle locks are exactly [locks]. */
    private suspend fun TestScope.workspace(vararg locks: ShuffleLock): WorkspaceModel {
        val (session, preferences) = session()
        booted(session)
        val workspace = workspaceModel(session, preferences)
        ShuffleLock.entries.forEach { lock -> workspace.setLock(lock, lock in locks) }
        runCurrent()
        return workspace
    }

    private fun workspaceModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
    ): WorkspaceModel {
        val model = WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver())
        return harness.own(model)
    }

    private companion object {
        /** How far a round trip through sRGB can move a hue, in degrees. */
        const val HUE_TOLERANCE = 3.0
    }
}
