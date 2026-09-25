package com.materialkolor.builder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.core.platform.TimingMarks
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.canvas.TestOwner
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlin.test.Test

private const val WIDTH = 1280
private const val HEIGHT = 800
private const val WAIT_MILLIS = 10_000L

/**
 * One pause before the sample composes, one before its steps and one before the live frame's.
 */
private const val PAUSES = 3

// pf-3
@OptIn(ExperimentalTestApi::class)
class FluentWarmUpTest {
    private val platform = FakePlatform()

    @Test
    fun idleHook_warmsFluentUpOnceAfterTheFirstFrame_outOfTheSemanticsTree() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val gate = Channel<Unit>(Channel.UNLIMITED)
            var pauses = 0
            var firstFrameBeforeWarmUp = false
            showRoot {
                if (pauses == 0) firstFrameBeforeWarmUp = TimingMarks.FIRST_FRAME in platform.environment.marks
                pauses++
                gate.receive()
            }
            waitUntil(timeoutMillis = WAIT_MILLIS) { pauses == 1 }
            val clickable = onAllNodes(hasClickAction()).fetchSemanticsNodes().size

            gate.trySend(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { pauses == 2 }
            waitForIdle()
            onAllNodes(hasClickAction()).fetchSemanticsNodes().size shouldBe clickable

            repeat(PAUSES - 1) { gate.trySend(Unit) }
            waitUntil(timeoutMillis = WAIT_MILLIS) { pauses == PAUSES }
            waitForIdle()
            mainClock.advanceTimeBy(1_000)
            waitForIdle()

            pauses shouldBe PAUSES
            firstFrameBeforeWarmUp shouldBe true
            onAllNodes(hasClickAction()).fetchSemanticsNodes().size shouldBe clickable
        }

    /**
     * The whole builder on fakes, booted, warming up through [awaitIdle].
     */
    private fun ComposeUiTest.showRoot(awaitIdle: suspend () -> Unit): AppGraph {
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                BuilderRoot(graph, awaitIdle = awaitIdle)
            }
        }
        waitUntil { platform.environment.splashHidden }
        return graph
    }
}
