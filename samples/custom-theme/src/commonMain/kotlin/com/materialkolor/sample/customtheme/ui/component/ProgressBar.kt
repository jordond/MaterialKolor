package com.materialkolor.sample.customtheme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.LocalAppColors

@Composable
internal fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .halftone(color = colors.pink, colors = colors, cell = 5.dp) { x, _ ->
                when {
                    x <= shown -> 1f
                    x <= shown + FADE -> 1f - (x - shown) / FADE * (1f - TINT)
                    else -> TINT
                }
            }.border(Rule, colors.ink),
    )
}

private const val FADE = 0.12f
private const val TINT = 0.1f
