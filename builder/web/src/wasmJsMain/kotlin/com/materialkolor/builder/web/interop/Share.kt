@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.w3c.files.File
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsArray

/** Whether the share sheet takes files here, asked with a small text file. */
internal fun pageCanShareFiles(): Boolean =
    js(
        """{
        if (typeof navigator.canShare !== 'function' || typeof navigator.share !== 'function') return false;
        try {
            return navigator.canShare({ files: [new File(['MaterialKolor'], 'probe.txt', { type: 'text/plain' })] });
        } catch (error) {
            return false;
        }
    }""",
    )

/** A browser file called [name] of type [mime] holding [bytes]. */
internal fun browserFile(
    bytes: Int8Array,
    name: String,
    mime: String,
): File = js("new File([bytes], name, { type: mime })")

/**
 * Hand [files] to the share sheet. Returns why it did not go, or null when it did.
 *
 * The sheet opens before the first suspension, so a caller that starts this undispatched from a
 * click opens it inside the click, which the browser insists on. Closing the sheet is the user
 * changing their mind rather than a failure, so it comes back as null too.
 */
internal suspend fun shareBrowserFiles(files: List<File>): String? =
    startShare(files.toJsArray()).await<JsString?>()?.toString()

private fun startShare(files: JsArray<File>): Promise<JsString?> =
    js(
        """{
        const reason = (error) => String((error && (error.name || error.message)) || error || 'Refused');
        try {
            if (typeof navigator.canShare !== 'function' || !navigator.canShare({ files })) {
                return Promise.resolve('NotSupportedError');
            }
            return navigator.share({ files }).then(
                () => null,
                (error) => (error && error.name === 'AbortError' ? null : reason(error)),
            );
        } catch (error) {
            return Promise.resolve(reason(error));
        }
    }""",
    )
