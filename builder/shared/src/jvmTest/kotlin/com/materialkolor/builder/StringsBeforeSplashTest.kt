package com.materialkolor.builder

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.LocalResourceReader
import org.jetbrains.compose.resources.ResourceReader
import kotlin.test.AfterTest
import kotlin.test.Test

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
            var reading = false
            with(app) {
                val graph = createGraph()
                setContent {
                    Provide {
                        val real = LocalResourceReader.current
                        val gated = remember(real) { GatedReader(real, gate) }
                        CompositionLocalProvider(LocalResourceReader provides gated) {
                            BuilderRoot(graph) { _ ->
                                val reader = LocalResourceReader.current
                                LaunchedEffect(reader) {
                                    reading = true
                                    reader.readPart(GATED_PATH, offset = 0, size = 1)
                                }
                            }
                        }
                    }
                }
            }
            waitUntil(timeoutMillis = WAIT_MILLIS) { reading }
            mainClock.advanceTimeBy(500)

            platform.environment.splashHidden shouldBe false

            gate.complete(Unit)
            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
        }

    /**
     * The app's own reader, but a read of [GATED_PATH] answers only once [gate] opens.
     */
    private class GatedReader(
        private val real: ResourceReader,
        private val gate: CompletableDeferred<Unit>,
    ) : ResourceReader by real {
        override suspend fun read(path: String): ByteArray {
            if (path != GATED_PATH) return real.read(path)
            gate.await()
            return byteArrayOf(0)
        }
    }

    private companion object {
        const val GATED_PATH = "values/strings_late.cvr"
    }
}
