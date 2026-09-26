package com.materialkolor.sample.customtheme.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.rememberDynamicScheme
import com.materialkolor.ktx.rememberTonalPalette

public enum class AppThemeMode {
    Light,
    Dark,
    System,
    ;

    @Composable
    @ReadOnlyComposable
    public fun isDark(): Boolean =
        when (this) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkTheme()
        }
}

public val LocalAppColors: ProvidableCompositionLocal<AppColors> =
    staticCompositionLocalOf { error("No AppColors provided, wrap the content in AppTheme.") }

public val LocalAppPalettes: ProvidableCompositionLocal<AppPalettes> =
    staticCompositionLocalOf { error("No AppPalettes provided, wrap the content in AppTheme.") }

@Composable
public fun AppTheme(
    seed: Color,
    mode: AppThemeMode = AppThemeMode.System,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
    content: @Composable () -> Unit,
) {
    val palettes = rememberAppPalettes(seed = seed, isDark = mode.isDark(), seeds = seeds)
    val target = remember(palettes) { palettes.toColors() }
    val colors = animateAppColors(target)

    CompositionLocalProvider(
        LocalAppPalettes provides palettes,
        LocalAppColors provides colors,
        content = content,
    )
}

@Composable
public fun rememberAppPalettes(
    seed: Color,
    isDark: Boolean,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
): AppPalettes {
    val scheme = rememberDynamicScheme(seedColor = seed, isDark = isDark)
    val stock = rememberTonalPalette(seed = seeds.stock, harmonizeWith = seed)
    val pink = rememberTonalPalette(seed = seeds.pink, harmonizeWith = seed)
    val blue = rememberTonalPalette(seed = seeds.blue, harmonizeWith = seed)
    val yellow = rememberTonalPalette(seed = seeds.yellow, harmonizeWith = seed)

    return remember(scheme, stock, pink, blue, yellow) {
        AppPalettes(scheme = scheme, stock = stock, pink = pink, blue = blue, yellow = yellow)
    }
}

@Composable
public fun rememberAppColors(
    seed: Color,
    isDark: Boolean,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
): AppColors {
    val palettes = rememberAppPalettes(seed = seed, isDark = isDark, seeds = seeds)
    return remember(palettes) { palettes.toColors() }
}
