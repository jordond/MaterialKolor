package com.materialkolor.builder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.LocalResourceReader
import org.jetbrains.compose.resources.ResourceReader
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalResourceApi::class)
class StringsBeforeSplashTest {
    private val app = AppHarness()
    private val platform = app.platform

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun splash_staysUpUntilTheStringsTheFirstScreenReadsAreIn() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val gate = CompletableDeferred<Unit>()
            var started = 0
            showRootReading(listOf(gate), stringsWait = Duration.INFINITE) { started++ }
            waitUntil(timeoutMillis = WAIT_MILLIS) { started == 1 }
            mainClock.advanceTimeBy(1_000)

            platform.environment.splashHidden shouldBe false

            gate.complete(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
        }

    @Test
    fun splash_whenAStringArrivingComposesAnotherRead_waitsForThatOneToo() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val first = CompletableDeferred<Unit>()
            val second = CompletableDeferred<Unit>()
            var started = 0
            showRootReading(listOf(first, second), stringsWait = Duration.INFINITE) { started++ }
            waitUntil(timeoutMillis = WAIT_MILLIS) { started == 1 }

            first.complete(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { started == 2 }
            mainClock.advanceTimeBy(1_000)

            platform.environment.splashHidden shouldBe false

            second.complete(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
        }

    @Test
    fun splash_whenAStringsReadNeverAnswers_stillGoesAfterTheWait() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            showRootReading(listOf(CompletableDeferred()), stringsWait = 1.seconds) {}

            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
        }

    /**
     * The whole builder on fakes, with gated reads started under the splash one after another. Each
     * read answers once its gate in [gates] opens, and its answer composes the next read, the way a
     * string that arrives can show a section with strings of its own. [onRead] runs as each starts.
     */
    private fun ComposeUiTest.showRootReading(
        gates: List<CompletableDeferred<Unit>>,
        stringsWait: Duration,
        onRead: () -> Unit,
    ) {
        with(app) {
            val graph = createGraph()
            setContent {
                Provide {
                    val real = LocalResourceReader.current
                    val gated = remember(real) { GatedReader(real, gates) }
                    CompositionLocalProvider(LocalResourceReader provides gated) {
                        BuilderRoot(graph, stringsWait = stringsWait) { _ ->
                            val reader = LocalResourceReader.current
                            var answered by remember { mutableIntStateOf(0) }
                            if (answered < gates.size) {
                                LaunchedEffect(reader, answered) {
                                    onRead()
                                    reader.readPart(gatedPath(answered), offset = 0, size = 1)
                                    answered++
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The app's own reader, but the read of each gated path answers only once its gate in [gates]
     * opens.
     */
    private class GatedReader(
        private val real: ResourceReader,
        private val gates: List<CompletableDeferred<Unit>>,
    ) : ResourceReader by real {
        override suspend fun read(path: String): ByteArray {
            val gate = gates.indices.firstOrNull { index -> gatedPath(index) == path } ?: return real.read(path)
            gates[gate].await()
            return byteArrayOf(0)
        }
    }

    private companion object {
        fun gatedPath(index: Int): String = "values/strings_late_$index.cvr"
    }
}
