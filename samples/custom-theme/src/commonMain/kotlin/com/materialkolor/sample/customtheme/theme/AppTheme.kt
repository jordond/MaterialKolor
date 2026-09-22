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

/**
 * Which theme the app shows, with [System] deferring to the platform.
 */
public enum class AppThemeMode {
    Light,
    Dark,
    System,
    ;

    /**
     * Resolve this mode against the platform setting.
     */
    @Composable
    @ReadOnlyComposable
    public fun isDark(): Boolean =
        when (this) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkTheme()
        }
}

/**
 * The theme colors for the current subtree.
 *
 * Reading this outside [AppTheme] throws, which is what you want. A missing theme is a bug, not a
 * reason to paint something arbitrary.
 */
public val LocalAppColors: ProvidableCompositionLocal<AppColors> =
    staticCompositionLocalOf { error("No AppColors provided, wrap the content in AppTheme.") }

/**
 * Generate the theme from [seed] and hand it to [content].
 *
 * Light and dark come from the same inputs, so there is no second palette to keep in step. Changing
 * [seed] changes both at once.
 *
 * @param[seed] The color the whole theme is generated from.
 * @param[mode] Which theme to show.
 * @param[seeds] The accent seeds the theme owns on top of [seed].
 * @param[content] The themed content.
 */
@Composable
public fun AppTheme(
    seed: Color,
    mode: AppThemeMode = AppThemeMode.System,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
    content: @Composable () -> Unit,
) {
    val colors = rememberAppColors(seed = seed, isDark = mode.isDark(), seeds = seeds)
    CompositionLocalProvider(LocalAppColors provides colors, content = content)
}

/**
 * Generate and remember the theme colors.
 *
 * Split out of [AppTheme] so a screen can build a second theme, a preview strip or a seed picker,
 * without nesting providers.
 *
 * @param[seed] The color the whole theme is generated from.
 * @param[isDark] Whether to build the dark theme or the light one.
 * @param[seeds] The accent seeds the theme owns on top of [seed].
 * @return The remembered theme colors.
 */
@Composable
public fun rememberAppColors(
    seed: Color,
    isDark: Boolean,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
): AppColors {
    val scheme = rememberDynamicScheme(seedColor = seed, isDark = isDark)

    // The scheme carries six ramps. These are the eight accents it has no room for.
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
