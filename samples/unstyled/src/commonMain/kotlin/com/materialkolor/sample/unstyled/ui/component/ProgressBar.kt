package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.Indicator
import com.composeunstyled.UnstyledProgress
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.shape

private const val PROGRESS_MILLIS = 300

@Composable
internal fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pill = ShapeTokens.pill.shape
    val shown by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = PROGRESS_MILLIS),
        label = "progress",
    )

    UnstyledProgress(
        progress = shown,
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .sunken(pill),
    ) {
        Indicator(
            Modifier
                .clip(pill)
                .background(GradientTokens.accent.brush),
        )
    }
}
