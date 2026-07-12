package com.materialkolor.builder.ui.ktx

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

@Composable
actual fun windowSizeClass(): WindowSizeClass {
    return calculateWindowSizeClass()
}
