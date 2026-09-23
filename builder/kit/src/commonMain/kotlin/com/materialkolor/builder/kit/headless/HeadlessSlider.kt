package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.composeunstyled.UnstyledSlider
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.inputAlpha
import com.materialkolor.builder.kit.skin.headless.inputFocusRing
import com.materialkolor.builder.kit.skin.headless.inputStateDescription
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How [HeadlessSlider] draws its track, stops and thumb.
 *
 * @property[trackHeight] The track's thickness.
 * @property[trackShape] The track's corners.
 * @property[activeTrack] The track from the start up to the thumb.
 * @property[inactiveTrack] The rest of the track.
 * @property[thumbSize] The thumb's width and height.
 * @property[thumbShape] The thumb's corners.
 * @property[thumb] The thumb fill.
 * @property[thumbOutline] The ring that lifts the thumb off the track.
 * @property[thumbOutlineWidth] How thick that ring is.
 * @property[stopSize] The dot drawn at each named stop.
 * @property[stop] The stop dots.
 * @property[focus] The keyboard focus ring around the thumb.
 */
@Immutable
internal class SliderStyle(
    val trackHeight: Dp,
    val trackShape: Shape,
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumbSize: Dp,
    val thumbShape: Shape,
    val thumb: Color,
    val thumbOutline: Color,
    val thumbOutlineWidth: Dp,
    val stopSize: Dp,
    val stop: Color,
    val focus: Color,
)

/**
 * What a slider does with input, whichever skin draws it.
 *
 * A drag lands on a named stop when it ends up within [snapDistance] of one. Keys never snap, so an
 * arrow press next to a stop still moves one [step] (AR-07).
 *
 * @property[range] The values the slider covers.
 * @property[step] How far one arrow press moves.
 * @property[stops] The named values a drag snaps to.
 * @property[snapDistance] How close a drag has to come to a stop to land on it.
 */
@Immutable
internal class SliderRules(
    val range: ClosedFloatingPointRange<Float>,
    val step: Float,
    val stops: List<Float>,
    val snapDistance: Float,
) {
    init {
        require(range.endInclusive > range.start) { "A slider needs a range wider than nothing, got $range" }
        require(step > 0f) { "A slider step has to move, got $step" }
        require(snapDistance >= 0f) { "A snap distance cannot be negative, got $snapDistance" }
        require(stops.all { stop -> stop in range }) { "Every stop has to sit inside $range, got $stops" }
    }

    /** Pulls [value] onto the nearest stop when it is within [snapDistance] of it. */
    fun snap(value: Float): Float {
        val nearest = stops.minByOrNull { stop -> abs(stop - value) } ?: return value
        return if (abs(nearest - value) <= snapDistance) nearest else value
    }

    /** Moves [value] by [steps] steps, landing on the step grid that starts at the range's start. */
    fun move(
        value: Float,
        steps: Int,
    ): Float {
        val index = ((value - range.start) / step).roundToInt() + steps
        return (range.start + index * step).coerceIn(range.start, range.endInclusive)
    }

    /** Where [value] sits along the range, from 0 to 1. */
    fun fractionOf(value: Float): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
}

/** How many steps Shift and the page keys move at once. */
internal const val SliderBigStep: Int = 10

/**
 * The slider keys, read before the underlying slider sees them so every skin steps the same way.
 *
 * The arrows move one step and ten with Shift, Page Up and Page Down move ten, Home and End jump to
 * the ends. Each key press reports a new value, and letting go of the key reports the end of the
 * change once.
 */
internal fun Modifier.sliderKeys(
    value: Float,
    rules: SliderRules,
    enabled: Boolean,
    isRtl: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
): Modifier {
    if (!enabled) return this
    return onPreviewKeyEvent { event ->
        val stride = if (event.isShiftPressed) SliderBigStep else 1
        val forward = if (isRtl) -stride else stride
        val target = when (event.key) {
            Key.DirectionRight -> rules.move(value, forward)
            Key.DirectionLeft -> rules.move(value, -forward)
            Key.DirectionUp -> rules.move(value, stride)
            Key.DirectionDown -> rules.move(value, -stride)
            Key.PageUp -> rules.move(value, SliderBigStep)
            Key.PageDown -> rules.move(value, -SliderBigStep)
            Key.MoveHome -> rules.range.start
            Key.MoveEnd -> rules.range.endInclusive
            else -> return@onPreviewKeyEvent false
        }
        when (event.type) {
            KeyEventType.KeyDown -> if (target != value) onValueChange(target)
            KeyEventType.KeyUp -> onValueChangeFinished()
        }
        true
    }
}

/**
 * The slider's name and spoken value. Set-progress comes from the slider underneath, and the
 * disabled state is spoken as well since the web mirror drops it (AR-10).
 */
internal fun Modifier.sliderSemantics(
    label: String,
    stateDescription: String,
    enabled: Boolean,
): Modifier =
    semantics {
        contentDescription = label
        this.stateDescription = inputStateDescription(stateDescription, enabled)
    }

/** The value as a slider reads it out when the caller has nothing better, to two decimals. */
internal fun sliderValueDescription(value: Float): String {
    val hundredths = (value * 100).roundToInt()
    val sign = if (hundredths < 0) "-" else ""
    val whole = abs(hundredths) / 100
    val fraction = (abs(hundredths) % 100).toString().padStart(2, '0')
    return "$sign$whole.$fraction"
}

/**
 * A slider over Compose Unstyled's, with named stops drawn on the track.
 *
 * Drags report every frame and snap to [SliderRules.stops], letting go reports once. Keys go
 * through [sliderKeys].
 */
@Composable
internal fun HeadlessSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    rules: SliderRules,
    label: String,
    stateDescription: String,
    style: SliderStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    UnstyledSlider(
        value = value,
        onValueChange = { raw -> onValueChange(rules.snap(raw)) },
        modifier = modifier
            .heightIn(min = LocalLayout.current.primaryTouchTarget)
            .sliderKeys(value, rules, enabled, isRtl, onValueChange, onValueChangeFinished)
            .sliderSemantics(label, stateDescription, enabled)
            .alpha(inputAlpha(enabled)),
        enabled = enabled,
        interactionSource = interactions,
        valueRange = rules.range,
        onValueChangeFinished = onValueChangeFinished,
        track = { state -> SliderTrack(state.fraction, rules, style) },
        thumb = {
            Box(
                Modifier
                    .inputFocusRing(interactions, style.focus, style.thumbShape)
                    .size(style.thumbSize)
                    .background(style.thumb, style.thumbShape)
                    .border(style.thumbOutlineWidth, style.thumbOutline, style.thumbShape),
            )
        },
    )
}

/**
 * The track with its active part and a dot at each stop. Both are inset by half a thumb, since
 * that is how far in the thumb's centre stops at either end.
 */
@Composable
private fun SliderTrack(
    fraction: Float,
    rules: SliderRules,
    style: SliderStyle,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(style.trackHeight)
            .clip(style.trackShape)
            .drawBehind {
                val inset = style.thumbSize.toPx() / 2
                val travel = (size.width - 2 * inset).coerceAtLeast(0f)
                drawRect(style.inactiveTrack)
                drawRect(style.activeTrack, size = Size(inset + travel * fraction, size.height))
                for (stop in rules.stops) {
                    drawCircle(
                        color = style.stop,
                        radius = style.stopSize.toPx() / 2,
                        center = Offset(inset + travel * rules.fractionOf(stop), size.height / 2),
                    )
                }
            },
    )
}
