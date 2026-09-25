package com.materialkolor.builder.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes the newest value it was handed for each key once nothing new has come in for
 * [delayMillis], or right away on [flush].
 *
 * Only the last value handed over for a key is written, so a burst of edits costs one write. Values
 * under different keys never replace each other, so what one project still has to save survives
 * another project's edits. Writes never overlap, and one that has started is never cancelled
 * halfway. A write that did not land is kept for the next write unless something newer has come in
 * under its key since.
 *
 * [schedule] and [cancelTimer] touch the timer, so they are called from one thread, the UI thread in
 * the app. [flush] leaves the timer alone, so it can go on after a suspension on any thread. A caller
 * that wants nothing written behind its back calls [cancelTimer] first, before it suspends.
 *
 * @param[scope] Where the writes run.
 * @param[delayMillis] How long things have to be quiet before a write.
 * @param[keyOf] What a value replaces. Values with equal keys replace each other.
 * @param[write] Writes a value and says whether it landed.
 */
internal class Autosave<T : Any>(
    private val scope: CoroutineScope,
    private val delayMillis: Long = AUTOSAVE_DELAY_MILLIS,
    private val keyOf: (T) -> Any = { Unit },
    private val write: suspend (T) -> Boolean,
) {
    private val pending = MutableStateFlow<Map<Any, T>>(emptyMap())
    private val writing = Mutex()
    private var timer: Job? = null

    /**
     * Whether a value is waiting to be written.
     */
    val hasPending: Boolean
        get() = pending.value.isNotEmpty()

    /**
     * Write [value] once [delayMillis] pass without another call, in place of anything still waiting
     * under its key.
     */
    fun schedule(value: T) {
        pending.update { waiting -> waiting + (keyOf(value) to value) }
        timer?.cancel()
        timer = scope.launch {
            delay(delayMillis)
            withContext(NonCancellable) { writePending() }
        }
    }

    /**
     * Stop the timer, so what is waiting stays until the next [schedule] or [flush].
     */
    fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * Write whatever is waiting now, and wait for a write already under way to finish first.
     */
    suspend fun flush() {
        writePending()
    }

    private suspend fun writePending() {
        writing.withLock {
            val values = pending.getAndUpdate { emptyMap() }
            values.forEach { (key, value) ->
                if (write(value)) return@forEach
                pending.update { waiting -> if (key in waiting) waiting else waiting + (key to value) }
            }
        }
    }
}

/**
 * How long after the last committed change autosave writes, in milliseconds.
 */
internal const val AUTOSAVE_DELAY_MILLIS: Long = 500
