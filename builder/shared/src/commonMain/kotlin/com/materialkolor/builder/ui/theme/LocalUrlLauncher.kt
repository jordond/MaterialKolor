package com.materialkolor.builder.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.Logger
import com.materialkolor.builder.core.UrlLauncher

val LocalUrlLauncher = staticCompositionLocalOf<UrlLauncher> {
    object : UrlLauncher {
        override fun launch(url: String) {
            Logger.d { "Launching $url" }
        }
    }
}