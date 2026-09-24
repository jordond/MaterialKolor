package com.materialkolor.builder.feature.image

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.fakes.FakeImageHandle
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800
private const val WAIT_MILLIS = 5_000L

@OptIn(ExperimentalTestApi::class)
class ImagePickTest {
    private val platform = FakePlatform()

    @Test
    fun imageButton_opensThePickerBeforeTheClickReturns() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            // No frame and no task runs after the click, so only a pick started inside it counts.
            mainClock.autoAdvance = false

            onNodeWithText("Image").performClick()

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

    /** The whole builder on fakes, booted. */
    private fun ComposeUiTest.showRoot(): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                BuilderRoot(graph)
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    private companion object {
        const val UNSUPPORTED = "That file couldn’t be read as an image, so the theme stays as it was"
    }
}
