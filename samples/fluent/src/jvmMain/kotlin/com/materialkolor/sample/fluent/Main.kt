package com.materialkolor.sample.fluent

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.materialkolor.sample.fluent.ui.FluentSampleApp

/**
 * Run it with `./gradlew :samples:fluent:run`.
 */
public fun main() {
    application {
        val state = rememberWindowState(
            size = DpSize(960.dp, 760.dp),
            position = WindowPosition(Alignment.Center),
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = state,
            title = "MaterialKolor Fluent sample",
        ) {
            FluentSampleApp()
        }
    }
}
