package com.materialkolor.builder.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.materialkolor.builder.BuilderApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("app") { BuilderApp(BrowserPlatform) }
}
