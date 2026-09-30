package com.materialkolor.builder

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.core.platform.TimingMarks
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RevealWarmUpTest {
    private val app = AppHarness()
    private val platform = app.platform

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun idleHook_warmsTheRevealUpOnceAfterTheFirstFrame_outOfTheSemanticsTree() =
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
            waitForIdle()
            mainClock.advanceTimeBy(1_000)
            waitForIdle()

            pauses shouldBe 1
            firstFrameBeforeWarmUp shouldBe true
            onAllNodes(hasClickAction()).fetchSemanticsNodes().size shouldBe clickable
        }

    /**
     * The whole builder on fakes, booted, warming up through [awaitIdle].
     */
    private fun ComposeUiTest.showRoot(awaitIdle: suspend () -> Unit) {
        with(app) {
            val graph = createGraph()
            setContent { Provide { BuilderRoot(graph, awaitIdle = awaitIdle) } }
        }
        waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
    }
}
