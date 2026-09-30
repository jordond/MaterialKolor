package com.materialkolor.builder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.core.platform.TimingMarks
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.fakes.FakeImageHandle
import com.materialkolor.builder.feature.image.QuadrantColors
import com.materialkolor.builder.feature.image.decodedOf
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TimingMarksTest {
    private val app = AppHarness()
    private val platform = app.platform

    private lateinit var workspace: WorkspaceModel

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun boot_marksTheFirstFrameOnce_andTheFirstResolve() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            platform.environment.marks.count { mark -> mark == TimingMarks.FIRST_FRAME } shouldBe 1
            platform.environment.marks shouldContain TimingMarks.RESOLVE
        }

    @Test
    fun edit_marksTheStartAndEndOfOneResolve() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()
            val before = resolves()

            runOnUiThread { workspace.edit(DocumentChange.SetThemeName("TimedTheme"), EditPhase.Discrete) }
            waitForIdle()

            resolves() shouldBe before + 1
            platform.environment.marks
                .filter { mark -> mark in RESOLVE_MARKS }
                .chunked(2)
                .distinct() shouldBe listOf(RESOLVE_MARKS)
        }

    @Test
    fun droppedImage_marksItsThumbnailThenItsCandidates() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val photo = FakeImageHandle("photo.png")
            platform.images.decoded[photo] = decodedOf(QuadrantColors)
            val graph = showRoot()
            platform.environment.marks shouldNotContain TimingMarks.EXTRACT

            platform.images.drop(photo)
            waitUntil(timeoutMillis = WAIT_MILLIS) { graph.session.document.value.seedSource is SeedSource.Image }
            waitForIdle()

            platform.environment.marks.filter { mark -> mark in IMAGE_MARKS } shouldBe IMAGE_MARKS
        }

    @Test
    fun unreadableFile_leavesNoImageMarks() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRoot()

            platform.images.drop(FakeImageHandle("notes.txt"))
            waitForIdle()

            platform.environment.marks.filter { mark -> mark in IMAGE_MARKS } shouldBe emptyList()
        }

    private fun resolves(): Int = platform.environment.marks.count { mark -> mark == TimingMarks.RESOLVE }

    /**
     * The whole builder on fakes, booted.
     */
    private fun ComposeUiTest.showRoot(): AppGraph {
        val graph = with(app) {
            bootRoot { graph ->
                CompositionLocalProvider(
                    // The saving glyph turns on the frame clock, which would hold waitForIdle through the save.
                    LocalMotionFrozen provides true,
                ) {
                    workspace = metroViewModel()
                    BuilderRoot(graph, workspaceModel = workspace)
                }
            }
        }
        return graph
    }

    private companion object {
        val IMAGE_MARKS = listOf(TimingMarks.THUMBNAIL, TimingMarks.EXTRACT)
        val RESOLVE_MARKS = listOf(TimingMarks.RESOLVE_START, TimingMarks.RESOLVE)
    }
}
