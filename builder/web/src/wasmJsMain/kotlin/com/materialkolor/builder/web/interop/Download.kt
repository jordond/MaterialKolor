@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import org.khronos.webgl.Int8Array
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

/**
 * Hand [bytes] to the browser as a download called [name] of type [mime]. Returns why it failed, or
 * null when the browser took it.
 *
 * The object URL is revoked [revokeAfterMillis] later rather than right away, since Safari drops a
 * download whose URL goes before it has started reading.
 */
internal fun downloadBytes(
    bytes: Int8Array,
    name: String,
    mime: String,
    revokeAfterMillis: Int,
): String? = startDownload(bytes, name, mime, revokeAfterMillis)?.toString()

private fun startDownload(
    bytes: Int8Array,
    name: String,
    mime: String,
    revokeAfterMillis: Int,
): JsString? =
    js(
        """{
        try {
            const url = URL.createObjectURL(new Blob([bytes], { type: mime }));
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = name;
            anchor.rel = 'noopener';
            anchor.style.display = 'none';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            setTimeout(() => URL.revokeObjectURL(url), revokeAfterMillis);
            return null;
        } catch (error) {
            return String((error && (error.name || error.message)) || error);
        }
    }""",
    )
