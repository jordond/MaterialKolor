@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// What the browser API hooks need from the page. Only called once `e2eHooksWanted` said yes.

/**
 * Put a small button in the top left corner, above the builder, that calls [onClick] inside its
 * click handler. Playwright clicks it for a real user gesture.
 */
internal fun addE2eGestureButton(onClick: () -> Unit): Unit =
    js(
        """{
        const button = document.createElement('button');
        button.id = 'mk-e2e-gesture';
        button.textContent = 'Gesture';
        button.style.cssText =
            'position:fixed;top:0;left:0;z-index:2147483647;width:24px;height:24px;opacity:0.01;margin:0;padding:0';
        button.addEventListener('click', () => onClick());
        document.body.appendChild(button);
    }""",
    )

/** Milliseconds since the page loaded, for timing a decode. */
internal fun pageMillis(): Double = js("performance.now()")
