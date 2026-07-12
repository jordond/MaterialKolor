package com.materialkolor.builder.core

import kotlinx.browser.window

internal actual fun launchUrl(url: String) {
    window.open(url, target = "_blank")
}
