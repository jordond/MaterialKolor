@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// What the page tells the browser about itself, and what the browser tells the page back.

/**
 * Set every `<meta name="theme-color">` to [color], a CSS color such as `#1a73e8`, adding one when
 * the page has none.
 */
internal fun writeThemeColor(color: String): Unit =
    js(
        """{
        let tags = document.querySelectorAll('meta[name="theme-color"]');
        if (tags.length === 0) {
            const tag = document.createElement('meta');
            tag.name = 'theme-color';
            document.head.appendChild(tag);
            tags = [tag];
        }
        tags.forEach((tag) => tag.setAttribute('content', color));
    }""",
    )

/**
 * Call [listener] when the page goes out of sight, hidden behind another tab, closed, or put in the
 * back and forward cache. Closing a tab usually fires both, so [listener] may run twice in a row.
 */
internal fun onPageHide(listener: () -> Unit): Unit =
    js(
        """{
        window.addEventListener('pagehide', () => listener());
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'hidden') listener();
        });
    }""",
    )
