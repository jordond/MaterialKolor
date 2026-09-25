package com.materialkolor.builder.feature.picker

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.feature.poster.usable
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import io.kotest.matchers.collections.shouldBeEmpty
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

// b-307
@OptIn(ExperimentalCoroutinesApi::class)
class PickerSessionTest : SessionTestBase() {
    private val harness = ViewModelHarness()
    private val router = FakeRouter()
    private val picker = PickerSession()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cancel_afterDrags_restoresEveryTargetExactlyWithHistoryUnchanged() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)
            val before = workspace.state.value
            before.history.canRedo shouldBe true

            Targets.forEach { target ->
                workspace.open(target)
                workspace.send(picker.pick(Red))
                workspace.send(picker.pick(Blue, fromScreen = true))
                workspace.state.value.document shouldNotBe before.document
                workspace.send(picker.cancel())
                workspace.sync()

                workspace.state.value.document shouldBe before.document
                workspace.state.value.history shouldBe before.history
                workspace.state.value.panel shouldBe null
            }
            harness.clearAndJoin()
        }

    @Test
    fun dragsThenDone_makeOneEntryForEachTarget() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)

            Targets.forEach { target ->
                val before = workspace.state.value.document
                workspace.open(target)
                workspace.send(picker.pick(Red))
                workspace.send(picker.pick(Blue))
                workspace.send(picker.done())
                workspace.sync()

                target.storedIn(workspace.state.value.document) shouldBe Blue
                workspace.undo()
                workspace.state.value.document shouldBe before
                workspace.redo()
            }
            harness.clearAndJoin()
        }

    @Test
    fun done_withNothingSent_sendsNoEdit() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)
            val before = workspace.state.value

            workspace.open(PickerTarget.Seed)
            picker.done() shouldBe listOf(WorkspaceAction.ClosePanel)
            picker.cancel() shouldBe listOf(WorkspaceAction.ClosePanel)

            workspace.state.value.history shouldBe before.history
            harness.clearAndJoin()
        }

    @Test
    fun backAndAnotherPanel_restoreTheValue() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)
            val before = workspace.state.value

            workspace.open(PickerTarget.KeyColorOverride(KeyColor.Secondary))
            workspace.send(picker.pick(Red))
            router.back()
            runCurrent()
            workspace.sync()
            workspace.state.value.document shouldBe before.document
            workspace.state.value.history shouldBe before.history

            workspace.open(PickerTarget.CmfSeed)
            workspace.send(picker.pick(Red))
            workspace.openPanel(Panel.About)
            workspace.sync()
            workspace.state.value.document shouldBe before.document
            workspace.state.value.history shouldBe before.history
            harness.clearAndJoin()
        }

    @Test
    fun targetSwap_restoresTheFirstAndOpensOnTheSecond() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)
            val before = workspace.state.value

            workspace.open(PickerTarget.Seed)
            workspace.send(picker.pick(Red))
            workspace.open(PickerTarget.Pin(Role.Primary, PinMode.Dark))
            workspace.state.value.document shouldBe before.document

            workspace.send(picker.pick(Blue))
            workspace.send(picker.done())
            workspace.state.value.document.seed shouldBe Stored.seed
            workspace.state.value.document.pins[Role.Primary] shouldBe RolePin(light = Green, dark = Blue)
            harness.clearAndJoin()
        }

    @Test
    fun projectSwitch_dropsTheSessionWithNoEdit() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)
            workspace.open(PickerTarget.Seed)
            workspace.send(picker.pick(Red))
            val dragged = workspace.state.value

            val sent = picker.sync(PickerTarget.Seed, OCEAN, dragged.projectGeneration + 1, dragged.capabilities)

            sent.shouldBeEmpty()
            picker.target shouldBe PickerTarget.Seed
            picker.cancel() shouldBe listOf(WorkspaceAction.ClosePanel)
            harness.clearAndJoin()
        }

    @Test
    fun presetSeed_restoredAfterADrag_leavesNoEntry() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored.copy(seedSource = SeedSource.Preset(id = "plum")))
            val before = workspace.state.value

            workspace.open(PickerTarget.Seed)
            workspace.send(picker.pick(Red))
            workspace.send(picker.cancel())

            workspace.state.value.document shouldBe before.document
            workspace.state.value.history shouldBe before.history
            harness.clearAndJoin()
        }

    @Test
    fun keyColorPinAndAccentPicks_neverMoveTheSeed() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)

            Targets.filter { target -> target != PickerTarget.Seed }.forEach { target ->
                workspace.open(target)
                workspace.send(picker.pick(Red, fromScreen = true))
                workspace.send(picker.done())
                workspace.sync()

                workspace.state.value.document.seed shouldBe Stored.seed
                workspace.state.value.document.seedSource shouldBe Stored.seedSource
            }
            harness.clearAndJoin()
        }

    @Test
    fun seedPicks_carryWhereTheyCameFrom() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)

            workspace.open(PickerTarget.Seed)
            workspace.send(picker.pick(Red))
            workspace.state.value.document.seedSource shouldBe SeedSource.Picked
            workspace.send(picker.pick(Blue, fromScreen = true))
            workspace.send(picker.done())

            workspace.state.value.document.seedSource shouldBe SeedSource.Eyedropper
            harness.clearAndJoin()
        }

    @Test
    fun unusableTargetOrMissingAccent_closesWithNoEdit() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored.copy(style = Style.TonalSpot))
            val before = workspace.state.value

            workspace.open(PickerTarget.CmfSeed)
            workspace.state.value.panel shouldBe null
            workspace.open(PickerTarget.Accent(5))
            workspace.state.value.panel shouldBe null

            workspace.state.value.document shouldBe before.document
            workspace.state.value.history shouldBe before.history
            harness.clearAndJoin()
        }

    // b-307a
    @Test
    fun controlTurningUnusableMidSession_restoresTheValueAndCloses() =
        runTest {
            val workspace = workspace()
            workspace.startFrom(Stored)

            workspace.open(PickerTarget.CmfSeed)
            workspace.send(picker.pick(Red))
            workspace.state.value.document.cmfTertiarySeed shouldBe Red
            workspace.edit(DocumentChange.SetStyle(Style.TonalSpot), EditPhase.Discrete)
            val cmfSeed = workspace.state.value.capabilities[Control.CmfSecondSeed]
            cmfSeed.usable shouldBe false
            workspace.sync()

            workspace.state.value.document.cmfTertiarySeed shouldBe Stored.cmfTertiarySeed
            workspace.state.value.document.style shouldBe Style.TonalSpot
            workspace.state.value.panel shouldBe null
            workspace.state.value.pickerTarget shouldBe null
            picker.target shouldBe null
            harness.clearAndJoin()
        }

    private suspend fun TestScope.workspace(): WorkspaceModel {
        val (session, preferences) = session()
        booted(session)
        return harness.own(WorkspaceModel(session, preferences, FakeClipboard(), router, ThemeResolver()))
    }

    /**
     * Starts on [document] with one step to undo and one to redo.
     */
    private fun WorkspaceModel.startFrom(document: ThemeDocument) {
        edit(DocumentChange.Replace(document), EditPhase.Discrete)
        edit(DocumentChange.SetThemeName("Undone"), EditPhase.Discrete)
        undo()
    }

    private fun WorkspaceModel.open(target: PickerTarget) {
        openPicker(target)
        sync()
    }

    private fun WorkspaceModel.sync() {
        val state = state.value
        picker
            .sync(state.pickerTarget, state.document, state.projectGeneration, state.capabilities)
            .forEach { action -> send(action) }
    }

    private fun WorkspaceModel.send(action: WorkspaceAction?) {
        when (action) {
            is WorkspaceAction.Edit -> edit(action.change, action.phase)
            WorkspaceAction.ClosePanel -> closePanel()
            null -> Unit
            else -> error("The picker sends only edits and ClosePanel, got $action")
        }
    }

    private fun WorkspaceModel.send(actions: List<WorkspaceAction>) {
        actions.forEach { action -> send(action) }
        sync()
    }

    private companion object {
        val Red = Argb(0xFFD32F2F.toInt())
        val Blue = Argb(0xFF1976D2.toInt())
        val Green = Argb(0xFF388E3C.toInt())

        /**
         * Every target stores something other than what the tests pick, or nothing at all.
         */
        val Stored = ThemeDocument(
            seed = Argb(0xFF6750A4.toInt()),
            seedSource = SeedSource.Image(name = "photo.jpg"),
            keyColors = KeyColors(),
            style = Style.Cmf,
            cmfTertiarySeed = null,
            accents = listOf(Accent(name = "brand", seed = Argb(0xFFFFA000.toInt()))),
            pins = mapOf(Role.Primary to RolePin(light = Green, dark = null)),
        )

        val Targets = listOf(
            PickerTarget.Seed,
            PickerTarget.KeyColorOverride(KeyColor.Secondary),
            PickerTarget.Pin(Role.Primary, PinMode.Dark),
            PickerTarget.Accent(0),
            PickerTarget.CmfSeed,
        )
    }
}
