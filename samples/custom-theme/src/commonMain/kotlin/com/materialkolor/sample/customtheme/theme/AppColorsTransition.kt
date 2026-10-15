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

internal fun lerp(
    start: AppColors,
    stop: AppColors,
    fraction: Float,
): AppColors {
    if (fraction <= 0f) return start
    if (fraction >= 1f) return stop

    return AppColors(
        isLight = if (fraction < 0.5f) start.isLight else stop.isLight,
        paper = lerp(start.paper, stop.paper, fraction),
        paperShade = lerp(start.paperShade, stop.paperShade, fraction),
        paperEdge = lerp(start.paperEdge, stop.paperEdge, fraction),
        ink = lerp(start.ink, stop.ink, fraction),
        inkSoft = lerp(start.inkSoft, stop.inkSoft, fraction),
        primary = lerp(start.primary, stop.primary, fraction),
        onPrimary = lerp(start.onPrimary, stop.onPrimary, fraction),
        error = lerp(start.error, stop.error, fraction),
        onError = lerp(start.onError, stop.onError, fraction),
        pink = lerp(start.pink, stop.pink, fraction),
        onPink = lerp(start.onPink, stop.onPink, fraction),
        blue = lerp(start.blue, stop.blue, fraction),
        onBlue = lerp(start.onBlue, stop.onBlue, fraction),
        yellow = lerp(start.yellow, stop.yellow, fraction),
        onYellow = lerp(start.onYellow, stop.onYellow, fraction),
        highlight = lerp(start.highlight, stop.highlight, fraction),
        scrim = lerp(start.scrim, stop.scrim, fraction),
    )
}
