package com.materialkolor.sample.customtheme.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp

/**
 * Follow [target] with a short fade, so a new seed or mode moves every slot together instead of jumping.
 *
 * The first composition returns [target] untouched. When it changes, the colors fade from whatever is on screen to
 * the new record, so a change that lands halfway through a fade picks up from where the last one got to.
 *
 * @param[target] The colors to end up at.
 * @param[animationSpec] How the fade runs.
 * @return The colors to draw this frame.
 */
@Composable
public fun animateAppColors(
    target: AppColors,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 300),
): AppColors {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    var fraction by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect

        from = lerp(start = from, stop = to, fraction = fraction)
        to = target
        fraction = 0f
        animate(initialValue = 0f, targetValue = 1f, animationSpec = animationSpec) { value, _ ->
            fraction = value
        }
    }

    return lerp(start = from, stop = to, fraction = fraction)
}

/**
 * The colors [fraction] of the way from [start] to [stop], slot by slot. The ends return the records themselves.
 */
internal fun lerp(
    start: AppColors,
    stop: AppColors,
    fraction: Float,
): AppColors {
    if (fraction <= 0f) return start
    if (fraction >= 1f) return stop

    return AppColors(
        isLight = if (fraction < 0.5f) start.isLight else stop.isLight,
        primary = lerp(start.primary, stop.primary, fraction),
        onPrimary = lerp(start.onPrimary, stop.onPrimary, fraction),
        primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, fraction),
        onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, fraction),
        primaryPressed = lerp(start.primaryPressed, stop.primaryPressed, fraction),
        primaryRaised = lerp(start.primaryRaised, stop.primaryRaised, fraction),
        secondary = lerp(start.secondary, stop.secondary, fraction),
        onSecondary = lerp(start.onSecondary, stop.onSecondary, fraction),
        secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, fraction),
        onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, fraction),
        tertiary = lerp(start.tertiary, stop.tertiary, fraction),
        onTertiary = lerp(start.onTertiary, stop.onTertiary, fraction),
        tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, fraction),
        onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, fraction),
        error = lerp(start.error, stop.error, fraction),
        onError = lerp(start.onError, stop.onError, fraction),
        errorContainer = lerp(start.errorContainer, stop.errorContainer, fraction),
        onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, fraction),
        love = lerp(start.love, stop.love, fraction),
        onLove = lerp(start.onLove, stop.onLove, fraction),
        loveContainer = lerp(start.loveContainer, stop.loveContainer, fraction),
        onLoveContainer = lerp(start.onLoveContainer, stop.onLoveContainer, fraction),
        cold = lerp(start.cold, stop.cold, fraction),
        onCold = lerp(start.onCold, stop.onCold, fraction),
        coldContainer = lerp(start.coldContainer, stop.coldContainer, fraction),
        onColdContainer = lerp(start.onColdContainer, stop.onColdContainer, fraction),
        warm = lerp(start.warm, stop.warm, fraction),
        onWarm = lerp(start.onWarm, stop.onWarm, fraction),
        warmContainer = lerp(start.warmContainer, stop.warmContainer, fraction),
        onWarmContainer = lerp(start.onWarmContainer, stop.onWarmContainer, fraction),
        surface = lerp(start.surface, stop.surface, fraction),
        surfaceRaised = lerp(start.surfaceRaised, stop.surfaceRaised, fraction),
        surfaceSunken = lerp(start.surfaceSunken, stop.surfaceSunken, fraction),
        surfaceInverse = lerp(start.surfaceInverse, stop.surfaceInverse, fraction),
        onSurface = lerp(start.onSurface, stop.onSurface, fraction),
        onSurfaceInverse = lerp(start.onSurfaceInverse, stop.onSurfaceInverse, fraction),
        textStrong = lerp(start.textStrong, stop.textStrong, fraction),
        textMuted = lerp(start.textMuted, stop.textMuted, fraction),
        borderFaint = lerp(start.borderFaint, stop.borderFaint, fraction),
        borderSoft = lerp(start.borderSoft, stop.borderSoft, fraction),
        borderStrong = lerp(start.borderStrong, stop.borderStrong, fraction),
        drinkCoffee = lerp(start.drinkCoffee, stop.drinkCoffee, fraction),
        drinkMatcha = lerp(start.drinkMatcha, stop.drinkMatcha, fraction),
        drinkIced = lerp(start.drinkIced, stop.drinkIced, fraction),
        drinkTea = lerp(start.drinkTea, stop.drinkTea, fraction),
        drinkChoc = lerp(start.drinkChoc, stop.drinkChoc, fraction),
        scrim = lerp(start.scrim, stop.scrim, fraction),
        focusRing = lerp(start.focusRing, stop.focusRing, fraction),
        shadow = lerp(start.shadow, stop.shadow, fraction),
    )
}
