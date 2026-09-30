package com.materialkolor.builder.core.resources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.ResourceReader

/**
 * A [ResourceReader] that reads each strings file whole, once, and hands out the parts of it from
 * memory.
 *
 * Compose resources reads every string on its own, and on the web each of those reads takes its own
 * turn through the browser's cache. A screen full of strings then fills in over seconds on a slow
 * machine, and the sections that compose last show empty text meanwhile. With the file read once,
 * every string in it arrives together.
 *
 * Whole reads, such as fonts, go straight to [base].
 */
@OptIn(ExperimentalResourceApi::class)
internal class WholeFileResourceReader(
    private val base: ResourceReader,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : ResourceReader {
    private val lock = Mutex()
    private val files = mutableMapOf<String, Deferred<ByteArray>>()
    private val reading = MutableStateFlow(0)

    /**
     * Whether a part is being read now.
     */
    val isReading: Boolean
        get() = reading.value > 0

    override suspend fun read(path: String): ByteArray = base.read(path)

    override suspend fun readPart(
        path: String,
        offset: Long,
        size: Long,
    ): ByteArray {
        reading.update { count -> count + 1 }
        try {
            val file = lock.withLock {
                files[path]?.takeUnless { file -> file.isCancelled }
                    ?: scope.async { base.read(path) }.also { file -> files[path] = file }
            }
            return file.await().copyOfRange(offset.toInt(), (offset + size).toInt())
        } finally {
            reading.update { count -> count - 1 }
        }
    }

    override fun getUri(path: String): String = base.getUri(path)

    /**
     * Waits until no part is being read.
     */
    suspend fun awaitReads() {
        reading.first { count -> count == 0 }
    }
}
