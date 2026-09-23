package com.materialkolor.builder.kit.transition

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer

/**
 * Draws [content] and, while a reveal of [transition] runs, the old frame over it.
 *
 * Wrap everything a discrete change repaints, which is usually the whole builder. The content keeps
 * the host's minimum size, so a host that fills the window gets content that does too.
 *
 * The reveal runs entirely in the draw phase. Nothing inside [content] recomposes because of it,
 * only because of the change itself.
 */
@Composable
public fun SkinTransitionHost(
    transition: SkinTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.skinReveal(transition),
        propagateMinConstraints = true,
    ) {
        content()
    }
}

private fun Modifier.skinReveal(transition: SkinTransition): Modifier =
    drawWithCache {
        val circle = Path()
        onDrawWithContent {
            val capture = transition.pendingCapture
            if (capture != null) {
                transition.snapshot.record { this@onDrawWithContent.drawContent() }
                transition.snapshot.alpha = 1f
                drawLayer(transition.snapshot)
                capture.complete(Unit)
                return@onDrawWithContent
            }

            drawContent()
            val progress = transition.progress.value
            if (progress >= 1f) return@onDrawWithContent

            if (transition.crossfade) {
                drawSnapshot(transition, alpha = 1f - progress)
            } else {
                val center = revealCenter(transition.origin, size)
                val radius = progress * farthestCorner(center, size)
                clipPath(circlePath(circle, center, radius), ClipOp.Difference) {
                    drawSnapshot(transition, alpha = 1f)
                }
            }
        }
    }

/** Draws the captured frame, from the bitmap when there is one and the recorded layer otherwise. */
private fun DrawScope.drawSnapshot(
    transition: SkinTransition,
    alpha: Float,
) {
    val bitmap = transition.bitmap
    if (bitmap != null) {
        drawImage(bitmap, alpha = alpha)
    } else {
        transition.snapshot.alpha = alpha
        drawLayer(transition.snapshot)
    }
}
