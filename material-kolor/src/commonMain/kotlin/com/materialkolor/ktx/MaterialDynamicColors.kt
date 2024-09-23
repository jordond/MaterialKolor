package com.materialkolor.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.dynamiccolor.MaterialDynamicColors

@Composable
public fun DynamicMaterialThemeState.m3Colors(): MaterialDynamicColors {
    return remember(isExtendedFidelity, dynamicScheme) {
        MaterialDynamicColors()
    }
}