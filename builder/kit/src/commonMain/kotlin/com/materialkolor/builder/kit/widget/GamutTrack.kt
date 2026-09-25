package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.max
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.a11y.LocalFocusVisibility
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One of the three HCT channels a picker track sets.
 *
 * @property[range] The values the track covers.
 */
internal enum class HctChannel(
    val range: ClosedFloatingPointRange<Double>,
) {
    Hue(0.0..FullTurn),
    Chroma(0.0..ChromaCeiling),
    Tone(0.0..ToneCeiling),
    ;

    /**
     * Where [value] sits along the track, from 0 to 1.
     */
    fun fractionOf(value: Double): Float =
        ((value - range.start) / (range.endInclusive - range.start)).toFloat().coerceIn(0f, 1f)

    /**
     * The value [fraction] of the way along the track.
     */
    fun valueAt(fraction: Float): Double = range.start + fraction.coerceIn(0f, 1f) * (range.endInclusive - range.start)
}

/**
 * The most chroma sRGB can show at a hue and tone.
 *
 * [edge] answers at the exact hue and tone and is what limits the chroma a picker sets.
 * [maxChroma] keeps one answer per whole hue and whole tone for shading the tracks, which is as fine
 * as a track can show and small enough to keep every one of them.
 */
internal object GamutLimit {
    private val cache = DoubleArray(HueSlots * ToneSlots) { Double.NaN }

    /**
     * The most chroma at exactly [hue] and [tone], found by a binary search over the chroma asked of
     * [Hct.from].
     *
     * Asked for more than sRGB can show, [Hct.from] mostly hands back the color on the edge, but not
     * at the exact hue and tone of a corner of the sRGB cube. There #00FF00 comes back with a chroma
     * of 57 rather than 108. So the search only trusts an ask that comes back within an eight bit
     * rounding of itself, [SearchSlack], and answers with the smaller of the last such ask and the
     * chroma it came back with. Asking the picker for that much always lands on the edge.
     */
    fun edge(
        hue: Double,
        tone: Double,
    ): Double {
        var reached = 0.0
        var beyond = ChromaCeiling
        repeat(SearchSteps) {
            val ask = (reached + beyond) / 2
            if (Hct.from(hue, ask, tone).chroma >= ask - SearchSlack) reached = ask else beyond = ask
        }
        return min(reached, Hct.from(hue, reached, tone).chroma)
    }

    /**
     * The most chroma at [hue] and [tone], both rounded to whole numbers first. Only for shading.
     */
    fun maxChroma(
        hue: Double,
        tone: Double,
    ): Double {
        val wholeHue = hue.roundToInt().mod(HueSlots)
        val wholeTone = tone.roundToInt().coerceIn(0, ToneSlots - 1)
        val slot = wholeHue * ToneSlots + wholeTone
        val cached = cache[slot]
        if (!cached.isNaN()) return cached
        return edge(wholeHue.toDouble(), wholeTone.toDouble()).also { found -> cache[slot] = found }
    }
}

/**
 * A track for one HCT [channel] of [picker], drawn as the colors it reaches with the parts sRGB
 * cannot show shaded.
 *
 * The colors and the thumb are read while drawing, so a drag redraws the track without composing
 * anything. Only the spoken value is composed, as a whole number, so the track composes again only
 * when that number changes. A drag reports [EditPhase.Dragging] for every change
 * and [EditPhase.Released] once when it lets go. The arrows move one and ten with Shift, Page Up and
 * Page Down move ten, Home and End jump to the ends, and each of those reports
 * [EditPhase.Discrete]. The track reads out as a slider named [label]. On the web it has no role,
 * so its role word and value fold into the name, "Hue, slider, 299" (D37, D40), and the name goes
 * in as text (S5).
 */
@Composable
internal fun GamutTrack(
    picker: PickerState,
    channel: HctChannel,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shown = picker.shownOf(channel)
    val valueText = shown.toString()
    val name = stateName(label, ControlState.Value(valueText), role = FoldedRole.Slider)
    val asText = LocalFoldsStateIntoName.current
    var focused by remember { mutableStateOf(false) }
    val visibility = LocalFocusVisibility.current // b-513
    Box(
        modifier
            .fillMaxWidth()
            .height(max(LocalLayout.current.primaryTouchTarget, tokens.spacing.section))
            .onFocusChanged { state -> focused = state.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val target = keyTarget(event.key, event.isShiftPressed, isRtl, picker.valueOf(channel), channel)
                    ?: return@onKeyEvent false
                picker.set(channel, target, EditPhase.Discrete)
                true
            }.focusable()
            .semantics {
                roleLessName(name, asText)
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = shown.toFloat(),
                    range = channel.range.start.toFloat()..channel.range.endInclusive.toFloat(),
                )
                setProgress { target -> picker.set(channel, target.toDouble(), EditPhase.Discrete) }
            }.pointerInput(picker, channel, tokens, isRtl) { dragTrack(picker, channel, tokens, isRtl) }
            .drawWithCache {
                val drawing = trackDrawing(trackPaint(channel, picker), tokens, isRtl)
                onDrawBehind { drawTrack(drawing, picker, channel, focused && visibility.isVisible) } // b-513
            },
    )
}

/**
 * Where a key moves [value] on [channel], or null for a key the track leaves alone.
 */
private fun keyTarget(
    key: Key,
    shift: Boolean,
    isRtl: Boolean,
    value: Double,
    channel: HctChannel,
): Double? {
    val stride = if (shift) BigStep else 1.0
    val forward = if (isRtl) -stride else stride
    return when (key) {
        Key.DirectionRight -> value + forward
        Key.DirectionLeft -> value - forward
        Key.DirectionUp -> value + stride
        Key.DirectionDown -> value - stride
        Key.PageUp -> value + BigStep
        Key.PageDown -> value - BigStep
        Key.MoveHome -> channel.range.start
        Key.MoveEnd -> channel.range.endInclusive
        else -> null
    }
}

/**
 * Follows one pointer from press to release. The press moves the thumb to it, every move after
 * that follows, and letting go reports [EditPhase.Released] once when anything moved.
 *
 * A drag cut off before the pointer lifts ends without [EditPhase.Released], so a caller that put
 * the prior color back keeps it. Leaving the screen cuts it off with a consumed up, and a change of
 * the track's keys by cancelling the gesture.
 */
private suspend fun PointerInputScope.dragTrack(
    picker: PickerState,
    channel: HctChannel,
    tokens: BuilderTokens,
    isRtl: Boolean,
) {
    val inset = tokens.spacing.extraLarge.toPx() / 2

    fun valueAt(x: Float): Double {
        val travel = (size.width - 2 * inset).coerceAtLeast(1f)
        val along = ((x - inset) / travel).coerceIn(0f, 1f)
        return channel.valueAt(if (isRtl) 1f - along else along)
    }
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        picker.startDrag()
        var moved = false
        val lifted = try {
            moved = picker.set(channel, valueAt(down.position.x), EditPhase.Dragging)
            horizontalDrag(down.id) { change ->
                change.consume()
                if (picker.set(channel, valueAt(change.position.x), EditPhase.Dragging)) moved = true
            }
        } catch (cancelled: CancellationException) {
            picker.endDrag(released = false)
            throw cancelled
        }
        picker.endDrag(released = moved && lifted)
    }
}

/**
 * What a track paints for the current color, worked out once per change of the channels it shows.
 *
 * @property[colors] Evenly spaced samples along the track.
 * @property[outside] The stretches, as fractions of the track, sRGB cannot show.
 */
private class TrackPaint(
    val colors: List<Color>,
    val outside: List<ClosedFloatingPointRange<Float>>,
)

/**
 * Samples [channel] across its range with the other two channels held where [picker] has them.
 *
 * The hue and tone tracks shade a sample whose hue and tone cannot reach the chroma the picker asks
 * for. The chroma track shades everything past the edge at the current hue and tone.
 */
private fun trackPaint(
    channel: HctChannel,
    picker: PickerState,
): TrackPaint {
    val hue = if (channel == HctChannel.Hue) 0.0 else picker.hue
    val chroma = if (channel == HctChannel.Chroma) 0.0 else picker.chroma
    val tone = if (channel == HctChannel.Tone) 0.0 else picker.tone
    val colors = ArrayList<Color>(SampleCount)
    val reached = BooleanArray(SampleCount)
    for (index in 0 until SampleCount) {
        val at = channel.valueAt(index / (SampleCount - 1f))
        val sampleHue = if (channel == HctChannel.Hue) at else hue
        val sampleChroma = if (channel == HctChannel.Chroma) at else chroma
        val sampleTone = if (channel == HctChannel.Tone) at else tone
        colors += Color(Hct.from(sampleHue, sampleChroma, sampleTone).toInt())
        reached[index] = GamutLimit.maxChroma(sampleHue, sampleTone) >= sampleChroma - ShadeSlack
    }
    if (channel == HctChannel.Chroma) {
        val edge = channel.fractionOf(GamutLimit.maxChroma(hue, tone) + ShadeSlack)
        return TrackPaint(colors, if (edge < 1f) listOf(edge..1f) else emptyList())
    }
    return TrackPaint(colors, outsideRuns(reached))
}

/**
 * The runs of samples that were not [reached], each widened by half a sample to either side.
 */
private fun outsideRuns(reached: BooleanArray): List<ClosedFloatingPointRange<Float>> {
    val half = 0.5f / (SampleCount - 1)
    val runs = mutableListOf<ClosedFloatingPointRange<Float>>()
    var start = -1
    for (index in 0..reached.size) {
        val outside = index < reached.size && !reached[index]
        if (outside && start < 0) start = index
        if (!outside && start >= 0) {
            val from = (start / (SampleCount - 1f) - half).coerceAtLeast(0f)
            val to = ((index - 1) / (SampleCount - 1f) + half).coerceAtMost(1f)
            runs += from..to
            start = -1
        }
    }
    return runs
}

/**
 * What a track draws that holds still while its thumb moves, built once per size, paint, tokens and
 * direction so a drag frame allocates nothing.
 *
 * @property[shaded] The left and right edges in pixels of each shaded stretch, one pair after another.
 */
private class TrackDrawing(
    val tokens: BuilderTokens,
    val isRtl: Boolean,
    val thumbRadius: Float,
    val travel: Float,
    val top: Float,
    val trackHeight: Float,
    val outline: Path,
    val brush: Brush,
    val shaded: FloatArray,
    val hatchGap: Float,
    val hatchWidth: Float,
    val focusRing: Stroke,
    val rim: Stroke,
    val innerRim: Stroke,
) {
    /**
     * How far from the left [fraction] of the way along the travel sits, in pixels.
     */
    fun xOf(fraction: Float): Float = thumbRadius + travel * (if (isRtl) 1f - fraction else fraction)
}

/**
 * Lays out the track for the current size and builds everything a frame reuses.
 */
private fun CacheDrawScope.trackDrawing(
    paint: TrackPaint,
    tokens: BuilderTokens,
    isRtl: Boolean,
): TrackDrawing {
    val thumbRadius = tokens.spacing.extraLarge.toPx() / 2
    val stroke = tokens.spacing.extraSmall.toPx() / 2
    val trackHeight = tokens.spacing.large.toPx()
    val top = (size.height - trackHeight) / 2
    val colors = if (isRtl) paint.colors.reversed() else paint.colors
    val track = RoundRect(0f, top, size.width, top + trackHeight, CornerRadius(trackHeight / 2))
    val drawing = TrackDrawing(
        tokens = tokens,
        isRtl = isRtl,
        thumbRadius = thumbRadius,
        travel = (size.width - 2 * thumbRadius).coerceAtLeast(0f),
        top = top,
        trackHeight = trackHeight,
        outline = Path().apply { addRoundRect(track) },
        brush = Brush.horizontalGradient(colors, startX = thumbRadius, endX = size.width - thumbRadius),
        shaded = FloatArray(paint.outside.size * 2),
        hatchGap = tokens.spacing.small.toPx(),
        hatchWidth = stroke / 2,
        focusRing = Stroke(stroke),
        rim = Stroke(stroke),
        innerRim = Stroke(stroke / 2),
    )
    paint.outside.forEachIndexed { index, run ->
        // A stretch that reaches either end of the travel runs on under the rounded cap past it.
        val leftEnd = if (isRtl) run.endInclusive == 1f else run.start == 0f
        val rightEnd = if (isRtl) run.start == 0f else run.endInclusive == 1f
        val from = drawing.xOf(run.start)
        val to = drawing.xOf(run.endInclusive)
        drawing.shaded[2 * index] = if (leftEnd) 0f else minOf(from, to)
        drawing.shaded[2 * index + 1] = if (rightEnd) size.width else maxOf(from, to)
    }
    return drawing
}

/**
 * Draws the track, its shaded stretches and the thumb. The thumb's place and fill are read here,
 * so moving it only redraws.
 *
 * The thumb always sits in a halo of the panel color, [ThumbHaloReach] rim widths past its edge, so
 * its focus ring lands on the panel and never on the track. Drawn only with focus the halo would
 * leave the ring over the track's colors, which the focus color can match (AR-01, S5 rerun).
 */
private fun DrawScope.drawTrack(
    drawing: TrackDrawing,
    picker: PickerState,
    channel: HctChannel,
    focused: Boolean,
) {
    val tokens = drawing.tokens
    drawPath(drawing.outline, drawing.brush)
    clipPath(drawing.outline) {
        var index = 0
        while (index < drawing.shaded.size) {
            shade(drawing, left = drawing.shaded[index], right = drawing.shaded[index + 1])
            index += 2
        }
    }

    val stroke = drawing.rim.width
    val radius = drawing.thumbRadius
    val center = Offset(drawing.xOf(channel.fractionOf(picker.valueOf(channel))), size.height / 2)
    drawCircle(tokens.panel, radius = radius + stroke * ThumbHaloReach, center = center)
    if (focused) drawCircle(tokens.focus, radius = radius + stroke * 2, center = center, style = drawing.focusRing)
    drawCircle(Color(picker.color.value), radius = radius, center = center)
    drawCircle(tokens.textStrong, radius = radius - stroke / 2, center = center, style = drawing.rim)
    drawCircle(tokens.panel, radius = radius - stroke * 1.5f, center = center, style = drawing.innerRim)
}

/**
 * Veils a stretch of the track and hatches it, so it reads as out of reach without relying on color.
 */
private fun DrawScope.shade(
    drawing: TrackDrawing,
    left: Float,
    right: Float,
) {
    val top = drawing.top
    val height = drawing.trackHeight
    clipRect(left, top, right, top + height) {
        drawRect(drawing.tokens.scrim, topLeft = Offset(left, top), size = Size(right - left, height))
        var x = left - height
        while (x < right) {
            drawLine(
                color = drawing.tokens.panel,
                start = Offset(x, top + height),
                end = Offset(x + height, top),
                strokeWidth = drawing.hatchWidth,
            )
            x += drawing.hatchGap
        }
    }
}

private const val FullTurn: Double = 360.0

/**
 * Past the most chroma sRGB reaches at any hue and tone, which is a little over 113 for red.
 */
internal const val ChromaCeiling: Double = 120.0

private const val ToneCeiling: Double = 100.0

private const val HueSlots: Int = 360

private const val ToneSlots: Int = 101

/**
 * Halvings of [ChromaCeiling], enough to land within a few hundredths.
 */
private const val SearchSteps: Int = 12

/**
 * How far below the ask [Hct.from] may land and still count as reaching it.
 */
private const val SearchSlack: Double = 2.0

/**
 * How far short of the asked chroma a sample may fall before its stretch is shaded.
 */
private const val ShadeSlack: Double = 1.0

/**
 * How many samples a track's gradient is drawn from.
 */
private const val SampleCount: Int = 48

/**
 * How far Shift and the page keys move.
 */
private const val BigStep: Double = 10.0

/**
 * How far the panel halo reaches past the thumb, in rim widths. The focus ring stands one and a half
 * off the thumb and is one wide, and the halo runs one more past it.
 */
private const val ThumbHaloReach: Float = 3.5f
