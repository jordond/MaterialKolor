package com.materialkolor.sample.customtheme.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.MaterialKolors
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.ktx.from
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette

/**
 * Build the whole theme from one seed color and the accent seeds.
 *
 * @param[seed] The color the scheme is generated from.
 * @param[isDark] Whether to build the dark theme or the light one.
 * @param[seeds] The accent seeds the theme owns on top of [seed].
 * @return The full set of theme colors.
 */
public fun appColors(
    seed: Color,
    isDark: Boolean,
    seeds: AppThemeSeeds = AppThemeSeeds.Default,
): AppColors {
    val scheme = DynamicScheme(seedColor = seed, isDark = isDark)
    return AppPalettes.from(seed = seed, seeds = seeds).toColors(scheme)
}

internal data class AppPalettes(
    val love: TonalPalette,
    val cold: TonalPalette,
    val warm: TonalPalette,
    val coffee: TonalPalette,
    val matcha: TonalPalette,
    val iced: TonalPalette,
    val tea: TonalPalette,
    val chocolate: TonalPalette,
) {
    companion object {
        fun from(
            seed: Color,
            seeds: AppThemeSeeds,
        ): AppPalettes =
            AppPalettes(
                love = TonalPalette.from(seeds.love.harmonize(seed)),
                cold = TonalPalette.from(seeds.cold.harmonize(seed)),
                warm = TonalPalette.from(seeds.warm.harmonize(seed)),
                coffee = TonalPalette.from(seeds.coffee.harmonize(seed)),
                matcha = TonalPalette.from(seeds.matcha.harmonize(seed)),
                iced = TonalPalette.from(seeds.iced.harmonize(seed)),
                tea = TonalPalette.from(seeds.tea.harmonize(seed)),
                chocolate = TonalPalette.from(seeds.chocolate.harmonize(seed)),
            )
    }
}

/**
 * Fold the scheme and the extra palettes into the flat record the app reads.
 *
 * The four Material families come from roles, because roles already solve the accent and container
 * problem and there is no reason to redo that work. Everything Material has no name for, the three
 * app-owned families, the pressed and raised states, the surface and border steps, the decorative
 * drink colors, is a tone this theme picks off a ramp. Each of those tones gets its content color
 * from [onTone] rather than a handwritten table.
 */
internal fun AppPalettes.toColors(scheme: DynamicScheme): AppColors {
    val kolors = MaterialKolors(scheme)
    val tones = ThemeTones(isDark = scheme.isDark)
    val neutral = scheme.neutralPalette
    val neutralVariant = scheme.neutralVariantPalette
    val primaryRamp = scheme.primaryPalette

    return AppColors(
        isLight = !scheme.isDark,
        primary = kolors.primary(),
        onPrimary = kolors.onPrimary(),
        primaryContainer = kolors.primaryContainer(),
        onPrimaryContainer = kolors.onPrimaryContainer(),
        primaryPressed = primaryRamp.toneColor(tones.pressed),
        primaryRaised = primaryRamp.toneColor(tones.raised),
        secondary = kolors.secondary(),
        onSecondary = kolors.onSecondary(),
        secondaryContainer = kolors.secondaryContainer(),
        onSecondaryContainer = kolors.onSecondaryContainer(),
        tertiary = kolors.tertiary(),
        onTertiary = kolors.onTertiary(),
        tertiaryContainer = kolors.tertiaryContainer(),
        onTertiaryContainer = kolors.onTertiaryContainer(),
        error = kolors.error(),
        onError = kolors.onError(),
        errorContainer = kolors.errorContainer(),
        onErrorContainer = kolors.onErrorContainer(),
        love = love.toneColor(tones.accent),
        onLove = love.onTone(tones.accent),
        loveContainer = love.toneColor(tones.container),
        onLoveContainer = love.onTone(tones.container),
        cold = cold.toneColor(tones.accent),
        onCold = cold.onTone(tones.accent),
        coldContainer = cold.toneColor(tones.container),
        onColdContainer = cold.onTone(tones.container),
        warm = warm.toneColor(tones.accent),
        onWarm = warm.onTone(tones.accent),
        warmContainer = warm.toneColor(tones.container),
        onWarmContainer = warm.onTone(tones.container),
        surface = kolors.surface(),
        surfaceRaised = neutral.toneColor(tones.surfaceRaised),
        surfaceSunken = neutral.toneColor(tones.surfaceSunken),
        surfaceInverse = kolors.inverseSurface(),
        onSurface = kolors.onSurface(),
        onSurfaceInverse = kolors.inverseOnSurface(),
        textStrong = neutralVariant.toneColor(tones.textStrong),
        textMuted = neutralVariant.toneColor(tones.textMuted),
        borderFaint = neutralVariant.toneColor(tones.borderFaint),
        borderSoft = neutralVariant.toneColor(tones.borderSoft),
        borderStrong = neutralVariant.toneColor(tones.borderStrong),
        drinkCoffee = coffee.toneColor(DECORATIVE_TONE),
        drinkMatcha = matcha.toneColor(DECORATIVE_TONE),
        drinkIced = iced.toneColor(DECORATIVE_TONE),
        drinkTea = tea.toneColor(DECORATIVE_TONE),
        drinkChoc = chocolate.toneColor(DECORATIVE_TONE),
        scrim = kolors.scrim(),
        focusRing = primaryRamp.toneColor(FOCUS_RING_TONE),
        shadow = kolors.shadow(),
    )
}

private class ThemeTones(
    isDark: Boolean,
) {
    val accent = if (isDark) 80 else 40
    val container = if (isDark) 30 else 90

    val pressed = if (isDark) 70 else 32
    val raised = if (isDark) 88 else 46

    val surfaceRaised = if (isDark) 12 else 100
    val surfaceSunken = if (isDark) 4 else 94

    val textStrong = if (isDark) 90 else 10
    val textMuted = if (isDark) 70 else 40

    val borderFaint = if (isDark) 22 else 92
    val borderSoft = if (isDark) 32 else 85
    val borderStrong = if (isDark) 65 else 55
}

/**
 * One tone that reads on a light and a dark surface, so the drink colors never change with mode.
 */
private const val DECORATIVE_TONE = 50

/**
 * The focus ring is the same accent tone in both modes, so focus never moves when the mode does.
 */
private const val FOCUS_RING_TONE = 60
