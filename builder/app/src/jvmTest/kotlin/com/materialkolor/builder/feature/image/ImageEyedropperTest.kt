package com.materialkolor.builder.feature.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.poster.PosterHarness
import com.materialkolor.builder.feature.poster.showSection
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Four pixels, none of them a candidate, so only an exact read lands on one.
 */
private val Pixels = listOf(0xFF123456, 0xFF654321, 0xFF0A0B0C, 0xFFFEDCBA).map { argb -> Argb(argb.toInt()) }

private val Candidates = listOf(0xFFD32F2F, 0xFF388E3C).map { argb -> Argb(argb.toInt()) }

@OptIn(ExperimentalTestApi::class)
class ImageEyedropperTest {
    private val source = SeedSource.Image("photo.png", Candidates)
    private val before = ThemeDocument(seed = Candidates.first(), seedSource = source)

    @Test
    fun click_takesTheExactPixelUnderIt_keepsTheSource_andCloses() =
        runComposeUiTest {
            val harness = PosterHarness(before, openPanel = Panel.ImageEyedropper)
            showEyedropper(harness)

            // The bottom right pixel of the two by two picture.
            onNodeWithTag(EYEDROPPER_PICTURE_TAG).performTouchInput { click(Offset(width * 0.75f, height * 0.75f)) }
            waitForIdle()

            harness.actions
                .filterIsInstance<WorkspaceAction.EditWithReveal>()
                .single()
                .change shouldBe
                DocumentChange.SetSeed(Pixels[3], source)
            harness.document shouldBe before.copy(seed = Pixels[3])
            harness.openPanel shouldBe null
        }

    @Test
    fun esc_closesTheEyedropper_andChangesNothing() =
        runComposeUiTest {
            val harness = PosterHarness(before, openPanel = Panel.ImageEyedropper)
            showEyedropper(harness)
            onNodeWithText("Pick any spot", substring = true).assertExists()

            onAllNodes(isFocused()).onLast().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.ClosePanel)
            harness.document shouldBe before
            onNodeWithText("Pick any spot", substring = true).assertDoesNotExist()
        }

    // b-311d

    @Test
    fun rowLeaving_closesTheEyedropper_soNoPanelIsLeftOpen() =
        runComposeUiTest {
            val preset = Presets.images[0]
            val onPreset = ThemeDocument(seed = preset.seed, seedSource = SeedSource.Preset(preset.id))
            val harness = PosterHarness(onPreset, openPanel = Panel.ImageEyedropper)
            showSection(harness) { context, dispatcher -> ImageCandidateRow(context, dispatcher) }
            waitForIdle()
            onNodeWithText("Pick any spot", substring = true).assertExists()

            // A typed seed leaves the row out, and the eyedropper with it.
            runOnUiThread { harness.document = onPreset.copy(seedSource = SeedSource.Typed) }
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.ClosePanel)
            harness.openPanel shouldBe null
            onNodeWithText("Pick any spot", substring = true).assertDoesNotExist()
        }

    @Test
    fun pixelUnder_readsEachSpotAsThePixelItFallsIn_andKeepsTheEdgesInside() {
        val picture = pictureOf(Pixels)
        val size = Size(200f, 200f)

        pixelUnder(Offset(0f, 0f), size, picture).let { at -> (at.x to at.y) shouldBe (0 to 0) }
        pixelUnder(Offset(99f, 101f), size, picture).let { at -> (at.x to at.y) shouldBe (0 to 1) }
        pixelUnder(Offset(200f, 200f), size, picture).let { at -> (at.x to at.y) shouldBe (1 to 1) }
    }

    private fun ComposeUiTest.showEyedropper(harness: PosterHarness) {
        val picture = pictureOf(Pixels)
        showSection(harness) { context, dispatcher ->
            ImageEyedropper(
                visible = context.openPanel == Panel.ImageEyedropper,
                picture = picture,
                source = context.document.seedSource,
                dispatcher = dispatcher,
            )
        }
        waitForIdle()
    }

    /**
     * A two by two picture of [pixels], row by row from the top left.
     */
    private fun pictureOf(pixels: List<Argb>): ImageBitmap {
        val picture = ImageBitmap(2, 2)
        val canvas = Canvas(picture)
        pixels.forEachIndexed { index, pixel ->
            val x = (index % 2).toFloat()
            val y = (index / 2).toFloat()
            canvas.drawRect(Rect(x, y, x + 1f, y + 1f), Paint().apply { color = Color(pixel.value) })
        }
        return picture
    }
}
