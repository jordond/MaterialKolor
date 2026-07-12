package com.materialkolor.builder

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.materialkolor.builder.ui.App
import materialkolorbuilder.app.generated.resources.Res
import materialkolorbuilder.app.generated.resources.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit

fun main() = application {
    // Get the current width and height of the screen
    val dimensions = Toolkit.getDefaultToolkit().screenSize
    val screenWidth = dimensions.width.dp
    val screenHeight = dimensions.height.dp

    // Define thresholds for small screens (adjust as needed)
    val smallScreenThreshold = 1800.dp

    // Calculate window size as a fraction of screen size for larger screens
    val windowWidthFraction = 0.8f
    val windowHeightFraction = 0.8f
    val windowWidth = (screenWidth * windowWidthFraction).coerceAtMost(1920.dp)
    val windowHeight = (screenHeight * windowHeightFraction).coerceAtMost(1080.dp)

    // Determine window placement based on screen size
    val windowPlacement = if (screenWidth <= smallScreenThreshold) {
        WindowPlacement.Maximized
    } else {
        WindowPlacement.Floating
    }

    Logger.d { "Screen size: $screenWidth x $screenHeight" }
    Logger.d { "Window size: $windowWidth x $windowHeight" }
    Logger.d { "Window placement: $windowPlacement" }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MaterialKolor Builder",
        icon = painterResource(Res.drawable.icon),
        state = rememberWindowState(
            placement = windowPlacement,
            width = if (windowPlacement == WindowPlacement.Floating) windowWidth else screenWidth,
            height = if (windowPlacement == WindowPlacement.Floating) windowHeight else screenHeight,
            position = WindowPosition(Alignment.Center),
        ),
    ) {
        App()
    }
}
