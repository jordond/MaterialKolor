package com.materialkolor.builder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.builder.core.DI
import com.materialkolor.builder.ui.home.HomeScreen
import com.materialkolor.builder.ui.ktx.windowSizeClass
import com.materialkolor.builder.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { error("Not initialized") }

@Composable
@Preview
fun App(destination: String? = null) {
    val darkModeProvider = remember { DI.darkModeProvider }
    val isDarkMode = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        darkModeProvider.initialize(isDarkMode)
    }

    val model = viewModel { AppModel() }
    val state by model.state.collectAsStateWithLifecycle()

    AppTheme(
        settings = state.settings,
        urlLauncher = model.urlLauncher,
    ) {
        CompositionLocalProvider(
            LocalWindowSizeClass provides windowSizeClass(),
        ) {
            HomeScreen(destination)
        }
    }
}
