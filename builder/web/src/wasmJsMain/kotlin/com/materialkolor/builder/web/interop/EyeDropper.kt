@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

/** Whether the browser has the screen eyedropper, Chromium on desktop so far. */
internal fun pageHasEyeDropper(): Boolean = js("typeof window.EyeDropper === 'function'")

/**
 * Let the user pick a color anywhere on the screen. Returns it as `#rrggbb`, or null when they
 * press Esc or the browser refuses.
 *
 * The eyedropper opens before the first suspension, so a caller that starts this undispatched from
 * a click opens it inside the click, which the browser insists on.
 */
internal suspend fun pickColorOnScreen(): String? = openEyeDropper().await<JsString?>()?.toString()

private fun openEyeDropper(): Promise<JsString?> =
    js(
        """{
        try {
            return new window.EyeDropper().open().then(
                (result) => (result && typeof result.sRGBHex === 'string' ? result.sRGBHex : null),
                () => null,
            );
        } catch (error) {
            return Promise.resolve(null);
        }
    }""",
    )
