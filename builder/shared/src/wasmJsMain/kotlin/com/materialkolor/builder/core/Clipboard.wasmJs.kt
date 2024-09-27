package com.materialkolor.builder.core

import kotlinx.browser.window

internal actual fun copyTextToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}