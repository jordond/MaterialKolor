@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

/**
 * Fade `#splash` out over [fadeMillis] and remove it. The canvas takes focus and presses as the fade
 * starts, so a click or a key while the splash fades reaches the builder. Does nothing when there is
 * no splash, or when it is already on its way out.
 *
 * Compose puts its canvas in the shadow root of an element inside `#app`, so that is where it is
 * looked for.
 */
internal fun fadeOutSplash(fadeMillis: Int): Unit =
    js(
        """{
        const splash = document.getElementById('splash');
        if (!splash || splash.dataset.leaving) return;
        splash.dataset.leaving = 'true';
        splash.style.transition = 'opacity ' + fadeMillis + 'ms ease-out';
        splash.style.opacity = '0';
        splash.style.pointerEvents = 'none';
        setTimeout(() => splash.remove(), fadeMillis);
        const app = document.getElementById('app');
        if (!app) return;
        let canvas = app.querySelector('canvas');
        for (const host of app.querySelectorAll('*')) {
            if (canvas) break;
            if (host.shadowRoot) canvas = host.shadowRoot.querySelector('canvas');
        }
        if (canvas) canvas.focus({ preventScroll: true });
    }""",
    )
