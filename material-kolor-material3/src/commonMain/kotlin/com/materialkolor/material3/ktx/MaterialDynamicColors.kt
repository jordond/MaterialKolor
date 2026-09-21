package com.materialkolor.material3.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.material3.DynamicMaterialThemeState

/**
 * Returns the Material 3 color scheme based on the current dynamic color scheme.
 */
public val DynamicMaterialThemeState.m3Colors: MaterialDynamicColors
    @Composable
    get() = remember(dynamicScheme) {
        MaterialDynamicColors()
    }

/**
 * A [MaterialKolors] class that holds the generated colors based on the current state.
 */
public val DynamicMaterialThemeState.colors: MaterialKolors
    @Composable
    get() {
        val scheme = dynamicScheme
        return remember(scheme) {
            MaterialKolors(scheme, isAmoled)
        }
    }
