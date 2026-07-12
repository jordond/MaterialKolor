package com.materialkolor.builder.ui.ktx

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import com.materialkolor.builder.MainApp

@Composable
actual fun windowSizeClass(): WindowSizeClass {
    return calculateWindowSizeClass(MainApp.activity())
}