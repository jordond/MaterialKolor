package com.materialkolor.builder.feature.image

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.fakes.FakeImageHandle
import com.materialkolor.builder.fakes.FakeImageInput
import com.materialkolor.builder.fakes.FakePasteInput
import com.materialkolor.builder.feature.poster.PosterHarness
import com.materialkolor.builder.feature.poster.showSection
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.control.ToastDuration
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.extensions.collectAsState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test

private val Seed = Argb(0xFF6750A4.toInt())

private const val READING = "Pulling colors from the image"
private const val ADD_AGAIN = "Add the image again"
private const val DROP = "Drop to pull colors from this image"
private const val MOSTLY_GRAY = "This image is mostly gray, so its colors are quiet"
private const val WAIT_MILLIS = 5_000L

@OptIn(ExperimentalTestApi::class)
class ImageCandidateRowTest {
    private val images = FakeImageInput()
    private val model = ImageSeedModel(images, FakePasteInput())
    private val store = ViewModelStore().apply { put("images", model) }
    private val photo = FakeImageHandle("photo.png")

    /** The tone a chip shows until its colors resolve, read from the skin the row is drawn in. */
    private var placeholder = Color.Unspecified

    @Test
    fun droppedImage_showsSkeletonChipsFirst_thenAChipPerCandidate_andSeedsAsOneEntry() =
        runComposeUiTest {
            val gate = CompletableDeferred<Unit>()
            images.decodeGate = gate
            images.decoded[photo] = decodedOf(QuadrantColors)
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)

            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { model.state.value.arriving != null }
            onNodeWithContentDescription(READING).assertExists()
            onAllNodes(hasContentDescription(", ", substring = true)).assertCountEquals(0)

            gate.complete(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource is SeedSource.Image }
            waitForIdle()
            val source = harness.document.seedSource.shouldBeInstanceOf<SeedSource.Image>()
            harness.document.seed shouldBe source.candidates.first()
            source.candidates.forEach { candidate ->
                onNodeWithContentDescription(chipLabel(candidate), substring = true).assertExists()
            }
            onNodeWithContentDescription(READING).assertDoesNotExist()
            harness.undoEntries() shouldBe 1
            val toast = harness.actions
                .filterIsInstance<WorkspaceAction.ShowWithdrawableToast>()
                .single()
                .toast
            toast.message shouldBe "Seed taken from photo.png"
            toast.actionLabel shouldBe "Undo"
            toast.duration shouldBe ToastDuration.Long
            store.clear()
        }

    @Test
    fun chip_click_swapsTheSeedAndKeepsTheSource() =
        runComposeUiTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)
            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource is SeedSource.Image }
            waitForIdle()
            val source = harness.document.seedSource as SeedSource.Image
            val chip = source.candidates[1]

            onNodeWithContentDescription(chipLabel(chip), substring = true).performClick()
            waitForIdle()

            val swap = harness.actions.filterIsInstance<WorkspaceAction.EditWithReveal>().last()
            swap.change shouldBe DocumentChange.SetSeed(chip, source)
            (swap.origin != null) shouldBe true
            harness.document.seed shouldBe chip
            harness.document.seedSource shouldBe source
            store.clear()
        }

    @Test
    fun afterAReload_theRowShowsTheChipsAndAsksForTheImageAgain() =
        runComposeUiTest {
            val candidates = listOf(0xFFD32F2F, 0xFF388E3C, 0xFF1976D2).map { argb -> Argb(argb.toInt()) }
            val source = SeedSource.Image("photo.png", candidates)
            val harness = PosterHarness(ThemeDocument(seed = candidates[0], seedSource = source))
            showRow(harness)

            candidates.forEach { candidate ->
                onNodeWithContentDescription(chipLabel(candidate), substring = true).assertExists()
            }
            onNodeWithText(ADD_AGAIN).performClick()

            harness.actions.last() shouldBe WorkspaceAction.OpenImagePicker
            store.clear()
        }

    @Test
    fun mostlyGrayImage_saysSoInTheRow() =
        runComposeUiTest {
            images.decoded[photo] = decodedOf(GrayColors)
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)

            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource is SeedSource.Image }
            waitForIdle()

            onNodeWithText(MOSTLY_GRAY).assertExists()
            onNodeWithText(ADD_AGAIN).assertDoesNotExist()
            store.clear()
        }

    @Test
    fun seedFromElsewhere_leavesTheRowOut() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed, seedSource = SeedSource.Typed))
            showRow(harness)

            onNodeWithText(ADD_AGAIN).assertDoesNotExist()
            onNodeWithContentDescription(READING).assertDoesNotExist()
            store.clear()
        }

    @Test
    fun dropOverlay_showsOnlyWhileFilesAreDragged() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)
            onNodeWithText(DROP).assertDoesNotExist()

            images.dragging.value = true
            waitForIdle()
            onNodeWithText(DROP).assertExists()

            images.dragging.value = false
            waitForIdle()
            onNodeWithText(DROP).assertDoesNotExist()
            store.clear()
        }

    // b-311a

    @Test
    fun dropOverlay_staysAwayWhileThePickerIsOpen() =
        runComposeUiTest {
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness, picking = true)

            images.dragging.value = true
            waitForIdle()

            onNodeWithText(DROP).assertDoesNotExist()
            store.clear()
        }

    @Test
    fun focusOnAChip_comesBackToTheChipsWhenTheNextImageLands() =
        runComposeUiTest {
            val other = FakeImageHandle("other.png")
            images.decoded[photo] = decodedOf(QuadrantColors)
            images.decoded[other] = decodedOf(QuadrantColors)
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)
            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource is SeedSource.Image }
            waitForIdle()
            val first = harness.document.seedSource
            val top = (first as SeedSource.Image).candidates.first()
            onNodeWithContentDescription(chipLabel(top), substring = true).requestFocus()
            waitForIdle()

            images.drop(other)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource != first }
            waitForIdle()

            onNodeWithContentDescription(chipLabel(harness.document.seed), substring = true).assertIsFocused()
            store.clear()
        }

    @Test
    fun focusOnAddTheImageAgain_movesToTheChipsWhenTheImageLands() =
        runComposeUiTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val reloaded = SeedSource.Image("photo.png", listOf(Seed))
            val harness = PosterHarness(ThemeDocument(seed = Seed, seedSource = reloaded))
            showRow(harness)
            onNodeWithText(ADD_AGAIN).requestFocus()
            waitForIdle()

            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource != reloaded }
            waitForIdle()

            onNodeWithContentDescription(chipLabel(harness.document.seed), substring = true).assertIsFocused()
            store.clear()
        }

    @Test
    fun contrastEdit_keepsTheChipsColoredWhileTheyResolveAgain() =
        runComposeUiTest {
            images.decoded[photo] = decodedOf(QuadrantColors)
            val harness = PosterHarness(ThemeDocument(seed = Seed))
            showRow(harness)
            images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { harness.document.seedSource is SeedSource.Image }
            waitForIdle()
            val candidates = (harness.document.seedSource as SeedSource.Image).candidates

            mainClock.autoAdvance = false
            harness.document = harness.document.copy(contrast = ContrastLevel.High)
            mainClock.advanceTimeByFrame()

            // One frame on only the first chip has resolved again, and the rest keep what they had.
            candidates.drop(1).forEach { candidate -> chipTop(candidate) shouldNotBe placeholder }
            mainClock.autoAdvance = true
            store.clear()
        }

    /** The color across the top half of [candidate]'s chip, its primary once resolved. */
    private fun ComposeUiTest.chipTop(candidate: Argb): Color {
        val pixels = onNodeWithContentDescription(chipLabel(candidate), substring = true).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height * 3 / 10]
    }

    /** The candidate row on the poster, fed by the model, with the host that sends its results. */
    private fun ComposeUiTest.showRow(
        harness: PosterHarness,
        picking: Boolean = false,
    ) {
        showSection(harness) { context, dispatcher ->
            placeholder = LocalBuilderTokens.current.border
            val seeds by model.collectAsState()
            // Frozen, so the skeleton's pulse holds still and the test can go idle.
            CompositionLocalProvider(LocalImageSeeds provides seeds, LocalMotionFrozen provides true) {
                ImageCandidateRow(context, dispatcher)
            }
            ImageHostContent(
                model = model,
                dispatcher = dispatcher,
                picking = picking,
                project = 0,
                document = context.document,
            )
        }
    }
}

/** How a chip's name starts, its hex and the comma before its color name. */
internal fun chipLabel(candidate: Argb): String = "${candidate.toHex()}, "
