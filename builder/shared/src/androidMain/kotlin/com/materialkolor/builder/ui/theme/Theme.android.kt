package com.materialkolor.builder.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.ktx.isLight

@Composable
internal actual fun SystemAppearance(isDark: Boolean) {
    if (LocalInspectionMode.current) return

    val view = LocalView.current
    val systemBarColor = Color.TRANSPARENT
    val isLightText = !MaterialTheme.colorScheme.onSurface.isLight()
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightText
        }
    }
}