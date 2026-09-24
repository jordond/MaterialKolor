@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop

// pf-3
// The builder warms up for its first switch to Fluent in idle time after the first frame, a step at
// a time, and waits here before each step so none lands on someone's input.

/** How long a browser without `requestIdleCallback` waits instead, which is Safari's case. */
private const val FALLBACK_MILLIS = 200

/** Suspends until the browser has an idle moment. */
internal suspend fun awaitIdle(): Unit =
    suspendCancellableCoroutine { continuation ->
        val handle = requestIdle(FALLBACK_MILLIS) { continuation.resume(Unit) }
        continuation.invokeOnCancellation { cancelIdle(handle) }
    }

private fun requestIdle(
    fallbackMillis: Int,
    onIdle: () -> Unit,
): Int =
    js(
        """{
        if (window.requestIdleCallback) return window.requestIdleCallback(() => onIdle());
        return -window.setTimeout(() => onIdle(), fallbackMillis);
    }""",
    )

// A fallback timer's handle comes back negated, so the two kinds cannot be mixed up.
private fun cancelIdle(handle: Int): Unit =
    js(
        """{
        if (handle < 0) window.clearTimeout(-handle);
        else if (window.cancelIdleCallback) window.cancelIdleCallback(handle);
    }""",
    )
