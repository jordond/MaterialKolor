@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// The browser tests in `builder/e2e` set `window.__mkE2e` before the page loads. Only then does the
// shell hang its test hooks on `window.__mk`, so a real visit never carries them.

/** Whether the page was opened by the browser tests. */
internal fun e2eHooksWanted(): Boolean = js("window.__mkE2e === true")

/** Put [hook] on `window.__mk` under [name]. Every hook takes one string and returns one. */
internal fun exposeE2eHook(
    name: String,
    hook: (String) -> String,
): Unit =
    js(
        """{
        window.__mk = window.__mk || {};
        window.__mk[name] = (argument) => hook(argument === undefined ? '' : String(argument));
    }""",
    )
