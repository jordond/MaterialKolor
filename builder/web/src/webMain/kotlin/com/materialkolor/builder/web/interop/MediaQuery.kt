@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Whether the media [query] matches, now and every time that changes.
 *
 * The listener stays for the life of the page, which is as long as the shell lives.
 */
internal fun mediaQueryState(query: String): StateFlow<Boolean> {
    val state = MutableStateFlow(false)
    state.value = watchMedia(query) { matches -> state.value = matches }
    return state.asStateFlow()
}

// Safari before 14 only has addListener on a MediaQueryList.
private fun watchMedia(
    query: String,
    onChange: (Boolean) -> Unit,
): Boolean =
    js(
        """{
        if (!window.matchMedia) return false;
        const list = window.matchMedia(query);
        const handler = (event) => onChange(event.matches);
        if (list.addEventListener) list.addEventListener('change', handler);
        else list.addListener(handler);
        return list.matches;
    }""",
    )
