package com.materialkolor.builder.feature.image

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.AppHarness
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.HEIGHT
import com.materialkolor.builder.WAIT_MILLIS
import com.materialkolor.builder.WIDTH
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.fakes.FakeImageHandle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ImagePickTest {
    private val app = AppHarness()
    private val platform = app.platform

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun uploadImage_opensThePickerBeforeTheClickReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            onNodeWithText("Image").performClick()
            waitForIdle()
            // No frame and no task runs after the click, so only a pick started inside it counts.
            mainClock.autoAdvance = false

            onNodeWithText(UPLOAD).performClick()

            platform.images.picks shouldBe 1
        }

    @Test
    fun pickedImage_seedsTheThemeAsOneEntry_withAnUndoToast() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val photo = FakeImageHandle("photo.png")
            platform.images.picked = photo
            platform.images.decoded[photo] = decodedOf(QuadrantColors)
            val graph = showRoot()
            val before = graph.session.document.value

            onNodeWithText("Image").performClick()
            onNodeWithText(UPLOAD).performClick()
            waitUntil(timeoutMillis = WAIT_MILLIS) { graph.session.document.value.seedSource is SeedSource.Image }
            waitForIdle()

            val source = graph.session.document.value.seedSource
                .shouldBeInstanceOf<SeedSource.Image>()
            source.name shouldBe "photo.png"
            graph.session.document.value.seed shouldBe source.candidates.first()
            onNodeWithContentDescription(chipLabel(source.candidates.first()), substring = true).assertExists()
            onNodeWithText("Seed taken from photo.png").assertExists()

            onNodeWithText("Undo").performClick()
            waitForIdle()
            graph.session.document.value shouldBe before
        }

    @Test
    fun unreadableFile_saysSoAndLeavesTheThemeAlone() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val graph = showRoot()
            val before = graph.session.document.value

            platform.images.drop(FakeImageHandle("notes.txt"))
            waitUntil(timeoutMillis = WAIT_MILLIS) {
                onAllNodes(hasText(UNSUPPORTED)).fetchSemanticsNodes().isNotEmpty()
            }

            graph.session.document.value shouldBe before
        }

    /**
     * The whole builder on fakes, booted.
     */
    private fun ComposeUiTest.showRoot(): AppGraph = with(app) { bootRoot() }

    private companion object {
        const val UNSUPPORTED = "That file couldn’t be read as an image, so the theme stays as it was"
        const val UPLOAD = "Upload image"
    }
}
