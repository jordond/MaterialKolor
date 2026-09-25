package com.materialkolor.builder.feature.image

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeImageHandle
import com.materialkolor.builder.fakes.FakeImageInput
import com.materialkolor.builder.fakes.FakePasteInput
import com.materialkolor.builder.fakes.FakeRouter
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageSeedModelTest : SessionTestBase() {
    private val harness = ViewModelHarness()
    private val images = FakeImageInput()
    private val pastes = FakePasteInput()
    private val photo = FakeImageHandle("photo.png")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun drop_showsTheSkeletonAtOnce_thenTheThumbnail_thenSeedsTheTopCandidate() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            images.decodeGate = gate
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, results) = model()

            images.drop(photo)
            runCurrent()
            val waiting = model.state.value.arriving
                .shouldNotBeNull()
            waiting.name shouldBe "photo.png"
            waiting.thumbnail.shouldBeNull()
            results.shouldBeEmpty()

            gate.complete(Unit)
            runCurrent()
            val seeded = results.single().shouldBeInstanceOf<ImageSeedResult.Seeded>()
            seeded.source.name shouldBe "photo.png"
            seeded.top shouldBe seeded.source.candidates.first()
            seeded.change shouldBe DocumentChange.SetSeed(seeded.top, seeded.source)
            val landing = model.state.value.arriving
                .shouldNotBeNull()
            landing.thumbnail.shouldNotBeNull()
            landing.lands shouldBe seeded.source
            model.state.value.newest
                ?.source shouldBe seeded.source

            // The workspace says when the seed shows, and the skeleton goes.
            model.landed(landing)
            model.state.value.arriving
                .shouldBeNull()
            harness.clearAndJoin()
        }

    @Test
    fun seed_whenTheWorkspaceNeverSaysItLanded_stillLetsTheSkeletonGo() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, _) = model()

            images.drop(photo)
            runCurrent()
            model.state.value.arriving
                .shouldNotBeNull()
            advanceTimeBy(LANDING_TIMEOUT_MILLIS + 1)

            model.state.value.arriving
                .shouldBeNull()
            model.state.value.newest
                .shouldNotBeNull()
            harness.clearAndJoin()
        }

    // b-311a
    // Through the workspace model, where the host's reveal lands each seed, on the session's clock.

    @Test
    fun imageSeed_rightAfterATypedSeed_isStillItsOwnUndoEntry() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val workspace = workspace()
            val (_, results) = model()
            workspace.edit(DocumentChange.SetSeed(TYPED, SeedSource.Typed), EditPhase.Discrete)
            val typed = workspace.state.value.document

            images.drop(photo)
            runCurrent()
            val seeded = results.single().shouldBeInstanceOf<ImageSeedResult.Seeded>()
            workspace.land(seeded)

            workspace.state.value.document.seed shouldBe seeded.top
            workspace.undo()
            workspace.state.value.document shouldBe typed
            harness.clearAndJoin()
        }

    @Test
    fun chipSwap_insideTheMergeWindow_foldsIntoTheImageSeed_andPastItIsOneMore() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val workspace = workspace()
            val start = workspace.state.value.document
            val (_, results) = model()
            images.drop(photo)
            runCurrent()
            val seeded = results.single().shouldBeInstanceOf<ImageSeedResult.Seeded>()
            workspace.land(seeded)
            val top = workspace.state.value.document
            val (second, third) = seeded.source.candidates.drop(1)

            // A moment later, so it folds, then a full merge window on, so it does not.
            advanceTimeBy(MERGE_WINDOW_MILLIS - 1)
            workspace.edit(DocumentChange.SetSeed(second, seeded.source), EditPhase.Discrete)
            advanceTimeBy(MERGE_WINDOW_MILLIS + 1)
            workspace.edit(DocumentChange.SetSeed(third, seeded.source), EditPhase.Discrete)

            workspace.state.value.document.seedSource shouldBe seeded.source
            workspace.undo()
            workspace.state.value.document.seed shouldBe second
            workspace.undo()
            workspace.state.value.document shouldBe start
            top.seed shouldBe seeded.top
            harness.clearAndJoin()
        }

    @Test
    fun newerImage_cancelsTheOlderJobAndItsSeed() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            images.decodeGate = gate
            val older = FakeImageHandle("older.png")
            val newer = FakeImageHandle("newer.png")
            images.decoded[older] = decodedOf(QuadrantColors)
            images.decoded[newer] = decodedOf(QuadrantColors.reversed())
            val (model, results) = model()

            images.drop(older)
            runCurrent()
            images.drop(newer)
            runCurrent()
            gate.complete(Unit)
            runCurrent()
            advanceTimeBy(LANDING_TIMEOUT_MILLIS + 1)

            results
                .single()
                .shouldBeInstanceOf<ImageSeedResult.Seeded>()
                .source.name shouldBe "newer.png"
            model.state.value.newest
                ?.source
                ?.name shouldBe "newer.png"
            harness.clearAndJoin()
        }

    @Test
    fun unsupportedFile_saysSoOnce_andKeepsTheImageBeforeIt() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, results) = model()
            images.drop(photo)
            runCurrent()
            val before = model.state.value.newest

            images.drop(FakeImageHandle("notes.txt"))
            runCurrent()

            results shouldHaveSize 2
            results[1] shouldBe ImageSeedResult.Unsupported
            model.state.value.arriving
                .shouldBeNull()
            model.state.value.newest shouldBe before
            harness.clearAndJoin()
        }

    @Test
    fun mostlyGrayImage_saysSo_andStillSeedsItsTopCandidate() =
        runTest {
            images.decoded[photo] = decodedOf(GrayColors)
            val (model, results) = model()

            images.drop(photo)
            runCurrent()

            val seeded = results.single().shouldBeInstanceOf<ImageSeedResult.Seeded>()
            seeded.top shouldBe seeded.source.candidates.first()
            val newest = model.state.value.newest
                .shouldNotBeNull()
            newest.mostlyGray shouldBe true
            harness.clearAndJoin()
        }

    @Test
    fun imageWithNoName_keepsAnEmptyName() =
        runTest {
            val unnamed = FakeImageHandle(name = null)
            images.decoded[unnamed] = decodedOf(QuadrantColors)
            val (_, results) = model()

            images.drop(unnamed)
            runCurrent()

            results
                .single()
                .shouldBeInstanceOf<ImageSeedResult.Seeded>()
                .source.name shouldBe ""
            harness.clearAndJoin()
        }

    @Test
    fun pastedFiles_seed_andPastedTextIsLeftAlone() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (_, results) = model()

            pastes.paste(Paste.Text("#6750A4"))
            runCurrent()
            results.shouldBeEmpty()
            pastes.paste(Paste.Files(listOf(photo)))
            runCurrent()

            results
                .single()
                .shouldBeInstanceOf<ImageSeedResult.Seeded>()
                .source.name shouldBe "photo.png"
            harness.clearAndJoin()
        }

    @Test
    fun whileThePickerIsOpen_dropsAndPastesAreLeftAlone() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, results) = model()
            model.follow(picking = true, project = 0)

            images.drop(photo)
            pastes.paste(Paste.Files(listOf(photo)))
            runCurrent()
            results.shouldBeEmpty()
            model.state.value.arriving
                .shouldBeNull()

            model.follow(picking = false, project = 0)
            images.drop(photo)
            runCurrent()
            results shouldHaveSize 1
            harness.clearAndJoin()
        }

    @Test
    fun newProject_dropsTheImageInMemory() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, _) = model()
            images.drop(photo)
            runCurrent()
            val remembered = model.state.value
            remembered.newest
                .shouldNotBeNull()
            remembered
                .forProject(1)
                .newest
                .shouldBeNull()

            model.follow(picking = false, project = 1)

            model.state.value.newest
                .shouldBeNull()
            harness.clearAndJoin()
        }

    // b-311a

    @Test
    fun pickerOpening_stopsTheImageOnItsWay() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            images.decodeGate = gate
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, results) = model()
            images.drop(photo)
            runCurrent()

            model.follow(picking = true, project = 0)
            model.state.value.arriving
                .shouldBeNull()
            gate.complete(Unit)
            runCurrent()
            advanceTimeBy(LANDING_TIMEOUT_MILLIS + 1)

            results.shouldBeEmpty()
            model.state.value.newest
                .shouldBeNull()
            harness.clearAndJoin()
        }

    @Test
    fun projectSwitch_stopsTheImageOnItsWay() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            images.decodeGate = gate
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (model, results) = model()
            images.drop(photo)
            runCurrent()

            model.follow(picking = false, project = 1)
            gate.complete(Unit)
            runCurrent()
            advanceTimeBy(LANDING_TIMEOUT_MILLIS + 1)

            results.shouldBeEmpty()
            model.state.value shouldBe ImageSeedModel.State()
            harness.clearAndJoin()
        }

    @Test
    fun severalPastedFiles_seedFromTheLastThatReadsAsAnImage() =
        runTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val (_, results) = model()

            pastes.paste(Paste.Files(listOf(FakeImageHandle("first.png"), photo, FakeImageHandle("notes.txt"))))
            runCurrent()

            results
                .single()
                .shouldBeInstanceOf<ImageSeedResult.Seeded>()
                .source.name shouldBe "photo.png"
            harness.clearAndJoin()
        }

    /**
     * A model on the fakes, with every result it sends collected as it comes.
     */
    private fun TestScope.model(): Pair<ImageSeedModel, List<ImageSeedResult>> {
        val model = harness.own(ImageSeedModel(images, pastes))
        val results = mutableListOf<ImageSeedResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.results.collect { result -> results += result }
        }
        return model to results
    }

    /**
     * A workspace on a booted session, which edits on the test's clock.
     */
    private suspend fun TestScope.workspace(): WorkspaceModel {
        val (session, preferences) = session()
        booted(session)
        return harness.own(WorkspaceModel(session, preferences, FakeClipboard(), FakeRouter(), ThemeResolver()))
    }

    /**
     * Lands [seeded] the way the host's reveal does once its crossfade is under way.
     */
    private fun WorkspaceModel.land(seeded: ImageSeedResult.Seeded) {
        edit(seeded.change, EditPhase.Discrete)
    }

    private companion object {
        val TYPED = Argb(0xFF6750A4.toInt())
        const val MERGE_WINDOW_MILLIS = History.MERGE_WINDOW_MILLIS

        /**
         * How long the model waits for the workspace to say the seed landed.
         */
        const val LANDING_TIMEOUT_MILLIS = 1_000L
    }
}
