package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Clipboard

// stub
// B-301 writes through the async clipboard API.
internal object WebClipboard : Clipboard {
    override suspend fun writeText(text: String): Result<Unit> = notInBrowserYet()
}

internal fun notInBrowserYet(): Result<Unit> =
    Result.failure(UnsupportedOperationException("Not available in the browser yet"))
