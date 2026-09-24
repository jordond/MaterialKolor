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
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val PROGRESS_MILLIS = 300

/**
 * A full width bar that fills in primary as [progress] grows, easing to each new value. `UnstyledProgress` gives it
 * the progress bar semantics.
 *
 * @param[progress] How far along, from 0 to 1.
 * @param[modifier] Applied to the track.
 */
@Composable
internal fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = PROGRESS_MILLIS),
        label = "progress",
    )
    UnstyledProgress(
        progress = shown,
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(Shapes.Pill)
            .background(MaterialKolorTokens.surfaceContainerHighest.color),
    ) {
        Indicator(
            Modifier
                .clip(Shapes.Pill)
                .background(MaterialKolorTokens.primary.color),
        )
    }
}
