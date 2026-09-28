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

/**
 * Whether the share sheet takes the kinds of file an export makes here, asked with an empty zip and
 * an empty Kotlin file. Chromium only shares images, text and a few media types, so it says no.
 */
internal fun pageCanShareFiles(): Boolean =
    pageCanShare(listOf(shareProbe("theme.zip", "application/zip"), shareProbe("Theme.kt", "text/x-kotlin")))

/**
 * Whether the share sheet takes [files], asked without waiting so a click can choose between sharing
 * and saving before anything suspends.
 */
internal fun pageCanShare(files: List<File>): Boolean = askCanShare(files.toJsArray())

/**
 * An empty file called [name] of type [mime]. `navigator.canShare` only looks at those two.
 */
internal fun shareProbe(
    name: String,
    mime: String,
): File = browserFile(Int8Array(0), name, mime)

private fun askCanShare(files: JsArray<File>): Boolean =
    js(
        """{
        if (typeof navigator.canShare !== 'function' || typeof navigator.share !== 'function') return false;
        try {
            return navigator.canShare({ files });
        } catch (error) {
            return false;
        }
    }""",
    )

/**
 * A browser file called [name] of type [mime] holding [bytes].
 */
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
    startShare(files.toJsArray()).await<JsString?>()?.toKotlinString()

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

/**
 * Whether a link is worth handing to the share sheet rather than the clipboard, which is when the
 * page has `navigator.share` and the main pointer is a finger. Desktop browsers with Web Share get
 * the clipboard, since their share sheet is a detour there.
 */
internal fun pageCanShareLink(): Boolean =
    js(
        """{
        if (typeof navigator.share !== 'function') return false;
        try {
            return window.matchMedia('(pointer: coarse)').matches;
        } catch (error) {
            return false;
        }
    }""",
    )

/**
 * Hand the link [url] called [title] to the share sheet. Returns why it did not go, or null when it
 * did or when the user closed the sheet.
 *
 * The sheet opens before the first suspension, like [shareBrowserFiles], so a caller that starts
 * this undispatched from a click opens it inside the click.
 */
internal suspend fun shareBrowserLink(
    url: String,
    title: String,
): String? = startLinkShare(url, title).await<JsString?>()?.toKotlinString()

private fun startLinkShare(
    url: String,
    title: String,
): Promise<JsString?> =
    js(
        """{
        const reason = (error) => String((error && (error.name || error.message)) || error || 'Refused');
        try {
            if (typeof navigator.share !== 'function') return Promise.resolve('NotSupportedError');
            return navigator.share({ url, title }).then(
                () => null,
                (error) => (error && error.name === 'AbortError' ? null : reason(error)),
            );
        } catch (error) {
            return Promise.resolve(reason(error));
        }
    }""",
    )
