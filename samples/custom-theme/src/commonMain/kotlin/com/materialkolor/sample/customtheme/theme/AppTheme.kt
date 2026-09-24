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

/**
 * Generate the theme from [seed] and hand it to [content]. A new seed or mode fades the colors over instead of
 * swapping them in one frame.
 */
@Composable
public fun AppTheme(
    seed: Color,
    mode: AppThemeMode = AppThemeMode.System,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
    content: @Composable () -> Unit,
) {
    val target = rememberAppColors(seed = seed, isDark = mode.isDark(), seeds = seeds)
    val colors = animateAppColors(target)
    CompositionLocalProvider(LocalAppColors provides colors, content = content)
}

@Composable
public fun rememberAppColors(
    seed: Color,
    isDark: Boolean,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
): AppColors {
    val scheme = rememberDynamicScheme(seedColor = seed, isDark = isDark)
    val palettes = AppPalettes(
        love = rememberTonalPalette(seed = seeds.love, harmonizeWith = seed),
        cold = rememberTonalPalette(seed = seeds.cold, harmonizeWith = seed),
        warm = rememberTonalPalette(seed = seeds.warm, harmonizeWith = seed),
        coffee = rememberTonalPalette(seed = seeds.coffee, harmonizeWith = seed),
        matcha = rememberTonalPalette(seed = seeds.matcha, harmonizeWith = seed),
        iced = rememberTonalPalette(seed = seeds.iced, harmonizeWith = seed),
        tea = rememberTonalPalette(seed = seeds.tea, harmonizeWith = seed),
        chocolate = rememberTonalPalette(seed = seeds.chocolate, harmonizeWith = seed),
    )

    return remember(scheme, palettes) { palettes.toColors(scheme) }
}
