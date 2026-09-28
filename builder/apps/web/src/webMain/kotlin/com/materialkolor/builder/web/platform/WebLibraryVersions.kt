@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.LibraryVersionSource
import com.materialkolor.builder.web.interop.toKotlinString
import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

/**
 * The library versions the Worker serves at `/api/versions` on the page's own origin.
 *
 * Only a JSON answer reaches Kotlin. A static server without the Worker, the local dev server or the
 * one the browser tests use, answers the path with its HTML page, which comes back as null here so
 * the export keeps the versions it was built with.
 */
internal object WebLibraryVersions : LibraryVersionSource {
    override suspend fun fetch(): String? = startVersionsFetch().await<JsString?>()?.toKotlinString()
}

// Every way a fetch can go wrong, a thrown error included, resolves to null rather than rejecting.
private fun startVersionsFetch(): Promise<JsString?> =
    js(
        """
        Promise.resolve()
            .then(() => fetch('/api/versions'))
            .then((response) => {
                const type = response.headers.get('content-type') || '';
                if (!response.ok || !type.startsWith('application/json')) return null;
                return response.text();
            })
            .catch(() => null)
        """,
    )
