package com.materialkolor.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.DynamicMaterialThemeState
import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.MaterialDynamicColors

/**
 * Returns the Material 3 color scheme based on the current dynamic color scheme.
 */
public val DynamicMaterialThemeState.m3Colors: MaterialDynamicColors
    @Composable
    get() = remember(isExtendedFidelity, dynamicScheme) {
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
            MaterialKolors(scheme, isAmoled, isExtendedFidelity)
        }
    }