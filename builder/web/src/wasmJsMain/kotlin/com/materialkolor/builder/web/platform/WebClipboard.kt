package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.web.interop.writeClipboardText

/**
 * The system clipboard through the async clipboard API.
 *
 * Safari only lets a page write while it handles a click. The write starts before [writeText]
 * first suspends, so start it undispatched from the click handler, with `Dispatchers.Unconfined`
 * or `CoroutineStart.UNDISPATCHED`. A refused write is a failure, never a quiet success, so the
 * caller can offer the text to copy by hand.
 */
internal object WebClipboard : Clipboard {
    override suspend fun writeText(text: String): Result<Unit> = outcome(writeClipboardText(text))
}

/** Success when the browser gave no [refusal], else a failure that carries it. */
internal fun outcome(refusal: String?): Result<Unit> =
    if (refusal == null) Result.success(Unit) else Result.failure(BrowserRefusal(refusal))

/** The browser said no, with the name of the DOM error when it gave one, `NotAllowedError` say. */
internal class BrowserRefusal(
    reason: String,
) : Exception(reason)
