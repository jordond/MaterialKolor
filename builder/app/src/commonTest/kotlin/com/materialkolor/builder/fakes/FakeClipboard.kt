package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Clipboard

/**
 * A [Clipboard] that keeps every text written to it.
 */
internal class FakeClipboard : Clipboard {
    /** Every text written, oldest first. */
    val texts: MutableList<String> = mutableListOf()

    /** When set, every write fails with it and nothing is kept. */
    var failure: Throwable? = null

    /** What the clipboard holds now, or null when nothing was written. */
    val text: String?
        get() = texts.lastOrNull()

    override suspend fun writeText(text: String): Result<Unit> {
        failure?.let { error -> return Result.failure(error) }
        texts += text
        return Result.success(Unit)
    }
}
