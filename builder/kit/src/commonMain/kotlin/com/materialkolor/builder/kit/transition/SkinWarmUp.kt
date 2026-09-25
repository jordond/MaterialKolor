package com.materialkolor.builder.kit.transition

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.floor

// The warm-up behind SkinTransition.warmUp. Skia compiles a GPU program the first time it meets a new
// mix of shape, paint and clip, and on the web each compile holds the frame while the GPU process
// works. The first switch to Fluent met about forty at once. The host draws the same mixes here a
// step at a time, under a cover of the live frame, in idle time before anyone switches.

/**
 * How far out the warm-up cuts its circles, as shares of the distance to the farthest corner.
 */
private val WarmRadii = floatArrayOf(0.3f, 0.6f, 0.9f)

/**
 * How opaque the warm-up draws a crossfade's old frame. Any share short of one takes the same path.
 */
private const val WarmFade = 0.5f

/**
 * One frame of a warm-up, in the order they run. The sample's steps come first, one frame after
 * another, so the sample leaves as soon as it can.
 */
internal enum class WarmStep(
    val drawsSample: Boolean,
) {
    /**
     * The sample as it is, the frame a switch lands on.
     */
    Sample(drawsSample = true),

    /**
     * The sample behind the reveal's circle, the old frame of the first switch back.
     */
    SampleCircle(drawsSample = true),

    /**
     * The sample faded, the old frame of a crossfade out of it.
     */
    SampleFade(drawsSample = true),

    /**
     * The live frame behind the circle, the old frame of the first switch.
     */
    LiveCircle(drawsSample = false),

    /**
     * The live frame faded, the old frame of the first crossfade.
     */
    LiveFade(drawsSample = false),
}

/**
 * A step the host has yet to draw, and what it completes once it has.
 */
internal class WarmPass(
    val step: WarmStep,
    val drawn: CompletableDeferred<Unit>,
)

/**
 * Composes the warm-up's sample while there is one.
 *
 * It lays the sample out at the host's size but places it past the host's start edge, so no pointer
 * reaches it, keeps it out of the semantics tree and turns focus away at its edge. Its node records
 * it into [SkinTransition.warmLayer] and draws nothing, and only the host draws that layer, under the
 * live frame.
 */
@Composable
internal fun WarmUpSlot(transition: SkinTransition) {
    val sample = transition.warmSample ?: return
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(-placeable.width, 0) }
            }.clearAndSetSemantics { }
            .focusProperties { onEnter = { cancelFocusChange() } }
            .focusGroup()
            .drawWithContent {
                transition.warmLayer.record { this@drawWithContent.drawContent() }
                transition.warmRecorded = true
            },
        propagateMinConstraints = true,
    ) {
        sample()
    }
}

/**
 * Draws [step] with the live frame on top of it.
 *
 * The live frame goes into [SkinTransition.snapshot] first, the way a capture records it, and the
 * cover draws it again in two halves. A draw that covers the whole target in one colour lets Skia
 * drop everything drawn before it unseen, programs and all, and neither half covers the whole.
 */
internal fun ContentDrawScope.drawWarmUp(
    transition: SkinTransition,
    step: WarmStep,
    circle: Path,
) {
    val live = transition.snapshot
    live.record { this@drawWarmUp.drawContent() }
    live.alpha = 1f
    val sample = transition.warmLayer.takeIf { transition.warmRecorded }
    when (step) {
        WarmStep.Sample -> {
            if (sample != null) drawLayer(sample)
        }
        WarmStep.SampleCircle -> {
            if (sample != null) behindCircles(circle) { drawLayer(sample) }
        }
        WarmStep.SampleFade -> {
            if (sample != null) drawFaded(sample)
        }
        WarmStep.LiveCircle -> {
            behindCircles(circle) { drawLayer(live) }
        }
        WarmStep.LiveFade -> {
            drawFaded(live)
        }
    }
    val half = floor(size.width / 2f)
    clipRect(right = half) { drawLayer(live) }
    clipRect(left = half) { drawLayer(live) }
}

/**
 * Runs [draw] behind a circle out of the middle at each of [WarmRadii], as a reveal clips it.
 */
private inline fun DrawScope.behindCircles(
    circle: Path,
    draw: DrawScope.() -> Unit,
) {
    val center = size.center
    val farthest = farthestCorner(center, size)
    for (share in WarmRadii) {
        clipPath(circlePath(circle, center, farthest * share), ClipOp.Difference) { draw() }
    }
}

/**
 * The density the warm layer is emptied with. An empty recording draws nothing, so any will do.
 */
internal val EmptyDensity: Density = Density(1f)

/**
 * Draws [layer] the way a crossfade draws the old frame, then leaves it opaque again.
 */
private fun DrawScope.drawFaded(layer: GraphicsLayer) {
    layer.alpha = WarmFade
    drawLayer(layer)
    layer.alpha = 1f
}
