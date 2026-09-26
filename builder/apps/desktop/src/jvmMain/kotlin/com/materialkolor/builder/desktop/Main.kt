package com.materialkolor.builder.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.materialkolor.builder.BuilderApp

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MaterialKolor Builder",
            state = rememberWindowState(width = 1440.dp, height = 900.dp),
        ) {
            BuilderApp(DesktopPlatform)
        }
    }
