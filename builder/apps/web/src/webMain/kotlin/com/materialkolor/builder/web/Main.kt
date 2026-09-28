package com.materialkolor.builder.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.materialkolor.builder.BuilderApp
import com.materialkolor.builder.web.interop.A11yLiveRegion
import com.materialkolor.builder.web.interop.FocusRepair
import com.materialkolor.builder.web.interop.MirrorRoot
import com.materialkolor.builder.web.interop.awaitIdle
import com.materialkolor.builder.web.interop.browserIsAutomated
import com.materialkolor.builder.web.interop.locationQuery
import com.materialkolor.builder.web.interop.motionFrozen
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    // Resources load from the site root, so a page opened on /t/<code> does not look for them under /t/.
    configureWebResources { resourcePathMapping { path -> "/$path" } }

    val frozen = motionFrozen(locationQuery(), browserIsAutomated())
    ComposeViewport(VIEWPORT_ID) {
        BuilderApp(BrowserPlatform, motionFrozen = frozen, awaitIdle = ::awaitIdle)
    }

    val viewport = document.getElementById(VIEWPORT_ID)
    if (viewport != null) {
        MirrorRoot.install(viewport)
        FocusRepair.install(viewport)
    }

    A11yLiveRegion.install()
}

private const val VIEWPORT_ID = "app"
