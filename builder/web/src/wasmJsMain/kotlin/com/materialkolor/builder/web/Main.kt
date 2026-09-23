package com.materialkolor.builder.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.materialkolor.builder.BuilderApp
import com.materialkolor.builder.web.interop.A11yLiveRegion
import com.materialkolor.builder.web.interop.FocusRepair
import com.materialkolor.builder.web.interop.MirrorRoot
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(VIEWPORT_ID) { BuilderApp(BrowserPlatform) }

    // b-220
    // The viewport element outlives `ComposeViewport` clearing its children, so both hooks can go
    // on it now even if Skiko is not ready yet.
    val viewport = document.getElementById(VIEWPORT_ID)
    if (viewport != null) {
        MirrorRoot.install(viewport)
        FocusRepair.install(viewport)
    }
    A11yLiveRegion.install()
}

private const val VIEWPORT_ID = "app"
