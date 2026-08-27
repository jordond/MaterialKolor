package com.materialkolor.builder.core

import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun copyTextToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}
