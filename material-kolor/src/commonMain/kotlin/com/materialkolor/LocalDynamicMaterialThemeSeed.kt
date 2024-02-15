package com.materialkolor

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * LocalDynamicMaterialThemeSeed is a [CompositionLocal] that provides the seed color for the
 * dynamic material theme.
 */
public val LocalDynamicMaterialThemeSeed: ProvidableCompositionLocal<Color> =
    staticCompositionLocalOf { error("DynamicMaterialTheme not initialized") }