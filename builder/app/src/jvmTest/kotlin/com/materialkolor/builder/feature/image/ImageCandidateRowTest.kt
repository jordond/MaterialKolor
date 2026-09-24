package com.materialkolor.builder.feature.image

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.materialkolor.builder.domain.color.Argb
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
import dev.stateholder.extensions.collectAsState
import io.kotest.matchers.shouldBe
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
            val toast = harness.actions.filterIsInstance<WorkspaceAction.ShowToast>().single()
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

    /** The candidate row on the poster, fed by the model, with the host that sends its results. */
    private fun ComposeUiTest.showRow(harness: PosterHarness) {
        showSection(harness) { context, dispatcher ->
            val seeds by model.collectAsState()
            // Frozen, so the skeleton's pulse holds still and the test can go idle.
            CompositionLocalProvider(LocalImageSeeds provides seeds, LocalMotionFrozen provides true) {
                ImageCandidateRow(context, dispatcher)
            }
            ImageHostContent(
                model = model,
                dispatcher = dispatcher,
                picking = false,
                project = 0,
                seedSource = context.document.seedSource,
            )
        }
    }
}

/** How a chip's name starts, its hex and the comma before its color name. */
internal fun chipLabel(candidate: Argb): String = "${candidate.toHex()}, "
