package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Clipboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/**
 * A [Clipboard] that keeps every text written to it.
 */
internal class FakeClipboard : Clipboard {
    /**
     * Every text written, oldest first.
     */
    val texts: List<String>
        get() = kept.value

    private val kept = MutableStateFlow<List<String>>(emptyList())

    /**
     * When set, every write fails with it and nothing is kept.
     */
    @Volatile
    var failure: Throwable? = null

    /**
     * What the clipboard holds now, or null when nothing was written.
     */
    val text: String?
        get() = texts.lastOrNull()

    override suspend fun writeText(text: String): Result<Unit> {
        failure?.let { error -> return Result.failure(error) }
        kept.update { texts -> texts + text }
        return Result.success(Unit)
    }
}
