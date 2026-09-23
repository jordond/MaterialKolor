package com.materialkolor.builder.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes the newest value it was handed once nothing new has come in for [delayMillis], or right
 * away on [flush].
 *
 * Only the last value handed over is written, so a burst of edits costs one write. Writes never
 * overlap, and one that has started is never cancelled halfway. A write that did not land is kept
 * for the next [flush] unless something newer has come in since.
 *
 * [schedule] and [flush] are called from one thread, the UI thread in the app.
 *
 * @param[scope] Where the writes run.
 * @param[delayMillis] How long things have to be quiet before a write.
 * @param[write] Writes a value and says whether it landed.
 */
internal class Autosave<T : Any>(
    private val scope: CoroutineScope,
    private val delayMillis: Long = AUTOSAVE_DELAY_MILLIS,
    private val write: suspend (T) -> Boolean,
) {
    private val pending = MutableStateFlow<T?>(null)
    private val writing = Mutex()
    private var timer: Job? = null

    /** Whether a value is waiting to be written. */
    val hasPending: Boolean
        get() = pending.value != null

    /** Write [value] once [delayMillis] pass without another call, in place of anything still waiting. */
    fun schedule(value: T) {
        pending.value = value
        timer?.cancel()
        timer = scope.launch {
            delay(delayMillis)
            withContext(NonCancellable) { writePending() }
        }
    }

    /** Write whatever is waiting now, and wait for a write already under way to finish first. */
    suspend fun flush() {
        timer?.cancel()
        writePending()
    }

    private suspend fun writePending() {
        writing.withLock {
            val value = pending.getAndUpdate { null } ?: return
            if (!write(value)) pending.compareAndSet(expect = null, update = value)
        }
    }
}

/** How long after the last committed change autosave writes, in milliseconds. */
internal const val AUTOSAVE_DELAY_MILLIS: Long = 500
