package com.materialkolor.builder.core.resources

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.ResourceReader
import kotlin.test.Test

@OptIn(ExperimentalResourceApi::class, ExperimentalCoroutinesApi::class)
class WholeFileResourceReaderTest {
    @Test
    fun partsOfOneFile_readTheFileOnce() =
        runTest {
            val base = FakeReader()
            val reader = readerOn(base)

            val parts = listOf(0L to 3L, 3L to 2L, 5L to 4L).map { (offset, size) ->
                async { reader.readPart(STRINGS, offset, size).decodeToString() }
            }

            parts.map { part -> part.await() } shouldBe listOf("abc", "de", "fghi")
            base.reads shouldBe listOf(STRINGS)
        }

    @Test
    fun wholeReads_goStraightToTheBase() =
        runTest {
            val base = FakeReader()
            val reader = readerOn(base)

            reader.read(STRINGS)
            reader.read(STRINGS)

            base.reads shouldBe listOf(STRINGS, STRINGS)
        }

    @Test
    fun awaitReads_waitsUntilThePartInFlightIsIn() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val reader = readerOn(FakeReader(gate))
            launch { reader.readPart(STRINGS, offset = 0, size = 3) }
            runCurrent()

            val settled = async { reader.awaitReads() }
            runCurrent()
            settled.isCompleted shouldBe false

            gate.complete(Unit)
            runCurrent()
            settled.isCompleted shouldBe true
        }

    @Test
    fun failedRead_isTriedAgainOnTheNextPart() =
        runTest {
            val base = FakeReader(failures = 1)
            val reader = readerOn(base)

            shouldThrow<IllegalStateException> { reader.readPart(STRINGS, offset = 0, size = 3) }
            reader.readPart(STRINGS, offset = 0, size = 3).decodeToString() shouldBe "abc"

            base.reads shouldBe listOf(STRINGS, STRINGS)
            reader.awaitReads()
        }

    @Test
    fun partPastTheEndOfTheFile_failsNamingTheFile() =
        runTest {
            val reader = readerOn(FakeReader())

            val failure = shouldThrow<IllegalArgumentException> { reader.readPart(STRINGS, offset = 7, size = 3) }

            failure.message shouldBe "$STRINGS has 9 bytes, not the 3 at 7 asked for"
            reader.readPart(STRINGS, offset = 7, size = 2).decodeToString() shouldBe "hi"
        }

    @Test
    fun close_cancelsThePartInFlight() =
        runTest {
            val reader = readerOn(FakeReader(CompletableDeferred()))
            val part = launch { reader.readPart(STRINGS, offset = 0, size = 3) }
            runCurrent()

            reader.close()
            runCurrent()

            part.isCancelled shouldBe true
            reader.isReading shouldBe false
        }

    private fun TestScope.readerOn(base: ResourceReader): WholeFileResourceReader =
        WholeFileResourceReader(base, scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))

    /**
     * A base reader holding one file, which answers once [gate] opens and fails its first [failures]
     * reads.
     */
    private class FakeReader(
        private val gate: CompletableDeferred<Unit>? = null,
        private var failures: Int = 0,
    ) : ResourceReader {
        val reads = mutableListOf<String>()

        override suspend fun read(path: String): ByteArray {
            reads += path
            gate?.await()
            if (failures > 0) {
                failures--
                error("The read failed")
            }
            return FILE.encodeToByteArray()
        }

        override suspend fun readPart(
            path: String,
            offset: Long,
            size: Long,
        ): ByteArray = error("The reader under test reads parts from the whole file")

        override fun getUri(path: String): String = path
    }

    private companion object {
        const val STRINGS = "values/strings.cvr"
        const val FILE = "abcdefghi"
    }
}
