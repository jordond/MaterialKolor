@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// sessionStorage lasts as long as the tab, through reloads. It only ever holds small hints, so a
// blocked or full one is shrugged off and the hint is simply gone.

/** The text under [key] for this tab, or null when there is none. */
internal fun sessionStorageRead(key: String): String? =
    js(
        """{
        try { return window.sessionStorage.getItem(key); } catch (e) { return null; }
    }""",
    )

/** Put [value] under [key] for this tab, or remove the key when [value] is null. */
internal fun sessionStorageWrite(
    key: String,
    value: String?,
): Unit =
    js(
        """{
        try {
            if (value === null) window.sessionStorage.removeItem(key);
            else window.sessionStorage.setItem(key, value);
        } catch (e) {}
    }""",
    )
