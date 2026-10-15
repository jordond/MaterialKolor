@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.LinkCardSource
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

/**
 * Link cards fetched from the page's own origin, the only one the page may connect to.
 *
 * The browser checks the answer, so only a PNG ever reaches Kotlin. The local dev server answers a
 * card path with its HTML page, which comes back as null here.
 */
internal object WebLinkCards : LinkCardSource {
    override suspend fun fetch(url: String): ByteArray? =
        startCardFetch(url).await<ArrayBuffer?>()?.let { buffer -> Int8Array(buffer).toByteArray() }
}

// Every way a fetch can go wrong, a thrown error included, resolves to null rather than rejecting.
private fun startCardFetch(url: String): Promise<ArrayBuffer?> =
    js(
        """
        Promise.resolve()
            .then(() => fetch(url))
            .then((response) => {
                const type = response.headers.get('content-type') || '';
                if (!response.ok || !type.startsWith('image/png')) return null;
                return response.arrayBuffer();
            })
            .catch(() => null)
        """,
    )
