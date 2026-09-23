@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// An overlay entry's history state holds `mkOverlay`, the overlay id, and `mkDepth`, how many
// overlay entries deep it sits counting itself. Any other state reads as depth 0.
//
// Safari throws a SecurityError when a page calls the history API too often in a short time, so
// every call here catches and reports whether it went through.

/** The path of the address the page is on, `/t/abc` for example. */
internal fun locationPath(): String = js("window.location.pathname")

/** The query of the address the page is on, with its leading `?`, or empty. */
internal fun locationQuery(): String = js("window.location.search")

/** Swap the address for [url] without adding an entry. False when the browser refused. */
internal fun historyReplaceUrl(url: String): Boolean =
    js(
        """{
        try {
            window.history.replaceState(window.history.state, '', url);
            return true;
        } catch (e) {
            return false;
        }
    }""",
    )

/** Add an entry on the same address for the overlay [id], [depth] deep. False when the browser refused. */
internal fun historyPushOverlay(
    id: String,
    depth: Int,
): Boolean =
    js(
        """{
        try {
            window.history.pushState({ mkOverlay: id, mkDepth: depth }, '');
            return true;
        } catch (e) {
            return false;
        }
    }""",
    )

/** Go back one entry. The browser does it later and reports it through [onHistoryPop]. */
internal fun historyBack(): Unit =
    js(
        """{
        try { window.history.back(); } catch (e) {}
    }""",
    )

/** How many overlay entries deep the current entry is. */
internal fun historyOverlayDepth(): Int =
    js(
        """{
        const state = window.history.state;
        return state && typeof state.mkDepth === 'number' ? state.mkDepth : 0;
    }""",
    )

/** Call [listener] with the overlay depth of the entry the browser moved to, back or forward. */
internal fun onHistoryPop(listener: (Int) -> Unit): Unit =
    js(
        """{
        window.addEventListener('popstate', (event) => {
            const state = event.state;
            listener(state && typeof state.mkDepth === 'number' ? state.mkDepth : 0);
        });
    }""",
    )
