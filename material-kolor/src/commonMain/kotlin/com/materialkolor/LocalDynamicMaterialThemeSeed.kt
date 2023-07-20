package com.materialkolor

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

public val LocalDynamicMaterialThemeSeed: ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { error("DynamicMaterialTheme not initialized") }