package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.Ramp
import com.materialkolor.builder.engine.resolve.RampStep
import com.materialkolor.builder.kit.a11y.collectIsFocusVisibleAsState
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.widget_copy
import com.materialkolor.builder.kit.generated.resources.widget_key_color
import com.materialkolor.builder.kit.generated.resources.widget_tone
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.ktx.contrastRatio
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How tall the continuous strip is.
 */
private val StripHeight: Dp = 16.dp

/**
 * The stroke width of a marker tick and of the ring at the key tone.
 */
private val TickWidth: Dp = 2.dp

/**
 * The narrowest a stop can be and still carry its tone as a label.
 */
private val LabeledStopWidth: Dp = 32.dp

/**
 * How far off a marker the pointer can rest on the strip and still light it.
 */
private val MarkerReach: Dp = 6.dp

/**
 * The tone at and above which a stop is light enough to take the darkest stop as ink.
 */
private const val LIGHT_TONE = 50

/**
 * The whole tonal range, 0 to 100.
 */
private const val TONE_RANGE = 100f

/**
 * The least contrast a stop's focus line needs against the stop and against its halo (WCAG 2.4.13).
 */
private const val FOCUS_CONTRAST = 3.0

/**
 * The two colors a focused stop's ring is drawn in.
 *
 * @property[line] The ring itself.
 * @property[halo] The thin bands on both sides of it.
 */
@Immutable
internal data class StopRing(
    val line: Color,
    val halo: Color,
)

/**
 * The ring for a focused stop in [stop]. It is [focus] between [panel] halos where the focus color
 * clears 3 to 1 on the stop and on the panel. On a stop too close to it, the line is whichever of
 * the ramp's [darkest] and [lightest] stops stands further from the stop, and the other one is the
 * halo, since the two ends of a ramp always stand well apart.
 */
internal fun stopRing(
    stop: Color,
    focus: Color,
    panel: Color,
    darkest: Color,
    lightest: Color,
): StopRing {
    if (focus.contrastRatio(stop) >= FOCUS_CONTRAST && focus.contrastRatio(panel) >= FOCUS_CONTRAST) {
        return StopRing(line = focus, halo = panel)
    }
    return if (darkest.contrastRatio(stop) >= lightest.contrastRatio(stop)) {
        StopRing(line = darkest, halo = lightest)
    } else {
        StopRing(line = lightest, halo = darkest)
    }
}

/**
 * A tone on a ramp that something picked, labeled on the strip.
 *
 * @property[name] What picked it, a role name such as "primary" or an accent role.
 * @property[tone] The HCT tone it landed on, which rarely sits on a stop.
 */
@Immutable
public data class RampMark(
    public val name: String,
    public val tone: Double,
)

/**
 * One tonal palette in one mode, for the palettes tab.
 *
 * The top row holds the stops, each a button that hands its tone to [onCopyTone]. Under it runs a
 * continuous 0 to 100 strip with a tick for every one of [markers] and a ring at [keyTone], labeled
 * below. Narrow rows fold the stops onto as many lines as it takes to keep every stop at the
 * layout's minimum touch target, and drop the tone labels, which the stops still read out.
 *
 * A caller that names the markers its own way passes [labels] false and gets no names under the
 * strip. It can light the markers at one rounded tone through [lit], and hear through [onLit] which
 * tone the pointer rests on along the strip, so its names and the markers light up together.
 *
 * @param[tones] The stops, darkest first, such as a [Ramp]'s steps.
 * @param[markers] Where roles landed on this palette.
 * @param[keyTone] The tone of the palette's key color.
 * @param[onCopyTone] Called with the stop that was pressed. The caller does the copying.
 * @param[modifier] Applied to the strip.
 * @param[labels] Whether to name the markers under the strip.
 * @param[lit] The rounded tone whose markers are drawn lit, or null for none.
 * @param[onLit] Called with the rounded tone of the marker the pointer rests on, and null as it
 * leaves, or null to not listen.
 */
@Composable
public fun RampStrip(
    tones: List<RampStep>,
    markers: List<RampMark>,
    keyTone: Double,
    onCopyTone: (RampStep) -> Unit,
    modifier: Modifier = Modifier,
    labels: Boolean = true, // b-513
    lit: Int? = null,
    onLit: ((Int?) -> Unit)? = null,
) {
    val tokens = LocalBuilderTokens.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
        RampStops(tones, onCopyTone)
        ContinuousStrip(tones, markers, keyTone, lit, onLit)
        if (labels) MarkerLabels(markers, keyTone)
    }
}

/**
 * [RampStrip] for one of the engine's ramps, marked with the roles that picked from it.
 */
@Composable
public fun RampStrip(
    ramp: Ramp,
    onCopyTone: (RampStep) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markers = remember(ramp) {
        ramp.markers.map { marker ->
            RampMark(marker.role.name.replaceFirstChar { char -> char.lowercaseChar() }, marker.tone)
        }
    }
    RampStrip(ramp.steps, markers, ramp.keyTone, onCopyTone, modifier)
}

@Composable
private fun RampStops(
    tones: List<RampStep>,
    onCopyTone: (RampStep) -> Unit,
) {
    if (tones.isEmpty()) return
    val tokens = LocalBuilderTokens.current
    val target = LocalLayout.current.minTouchTarget
    val darkInk = tones.minBy { step -> step.tone }.argb.toColor()
    val lightInk = tones.maxBy { step -> step.tone }.argb.toColor()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fits = (maxWidth / target).toInt().coerceIn(1, tones.size)
        val rows = ceilDiv(tones.size, fits)
        val perRow = ceilDiv(tones.size, rows)
        val labeled = maxWidth / perRow >= LabeledStopWidth
        val shape = RoundedCornerShape(tokens.radius.small)
        Column(Modifier.clip(shape)) {
            for (row in tones.chunked(perRow)) {
                Row(Modifier.fillMaxWidth()) {
                    for (step in row) {
                        RampStop(
                            step = step,
                            darkest = darkInk,
                            lightest = lightInk,
                            labeled = labeled,
                            onClick = { onCopyTone(step) },
                            modifier = Modifier.weight(1f).heightIn(min = target),
                        )
                    }
                    repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun RampStop(
    step: RampStep,
    darkest: Color,
    lightest: Color,
    labeled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusVisibleAsState() // b-513
    val name = listOf(stringResource(Res.string.widget_tone, step.tone), step.argb.toHex()).joinToString(", ")
    val color = step.argb.toColor()
    val ink = if (step.tone >= LIGHT_TONE) darkest else lightest
    Box(
        modifier = modifier
            .background(color)
            .then(
                when {
                    focused -> Modifier.stopFocusRing(stopRing(color, tokens.focus, tokens.panel, darkest, lightest))
                    hovered -> Modifier.border(tokens.outlineWidth, tokens.borderStrong)
                    else -> Modifier
                },
            ).hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(Res.string.widget_copy),
                onClick = onClick,
            ).semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (labeled) BuilderText(step.tone.toString(), style = BuilderTextStyle.Value, color = ink, maxLines = 1)
    }
}

/**
 * The focus ring of a stop, a line with a halo on both sides the way the strip's ticks are drawn,
 * inside the stop since the stops are clipped together.
 */
private fun Modifier.stopFocusRing(ring: StopRing): Modifier {
    val haloWidth = WidgetFocusWidth / 2
    return border(haloWidth, ring.halo)
        .border(haloWidth + WidgetFocusWidth, ring.line)
        .border(haloWidth * 2 + WidgetFocusWidth, ring.halo)
}

/**
 * [count] split into groups of at most [size], rounded up.
 */
private fun ceilDiv(
    count: Int,
    size: Int,
): Int = (count + size - 1) / size

/**
 * The palette from 0 to 100 as one gradient, with a tick at every marker and a ring at the key tone.
 * The ticks and the ring at the [lit] tone are drawn in the accent, and [onLit] hears which marker
 * the pointer rests on.
 */
@Composable
private fun ContinuousStrip(
    tones: List<RampStep>,
    markers: List<RampMark>,
    keyTone: Double,
    lit: Int?,
    onLit: ((Int?) -> Unit)?,
) {
    if (tones.size < 2) return
    val tokens = LocalBuilderTokens.current
    val brush = remember(tones) {
        Brush.horizontalGradient(
            colorStops = tones.map { step -> step.tone / TONE_RANGE to step.argb.toColor() }.toTypedArray(),
        )
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(StripHeight)
            .then(if (onLit == null) Modifier else Modifier.lightOnHover(markers, keyTone, onLit)) // b-513
            .clip(RoundedCornerShape(tokens.radius.small))
            .background(brush),
    ) {
        val tick = TickWidth.toPx()
        for (marker in markers) {
            val x = (marker.tone / TONE_RANGE).toFloat() * size.width
            val ink = if (marker.tone.roundToInt() == lit) tokens.accent else tokens.textStrong // b-513
            drawLine(tokens.panel, Offset(x, 0f), Offset(x, size.height), strokeWidth = tick * 2)
            drawLine(ink, Offset(x, 0f), Offset(x, size.height), strokeWidth = tick)
        }
        val center = Offset((keyTone / TONE_RANGE).toFloat() * size.width, size.height / 2f)
        val radius = size.height / 2f - tick
        drawCircle(tokens.panel, radius, center, style = Stroke(tick * 2))
        val keyInk = if (keyTone.roundToInt() == lit) tokens.accent else tokens.textStrong
        drawCircle(keyInk, radius, center, style = Stroke(tick))
    }
}

// b-513

/**
 * Tells [onLit] the rounded tone of the marker or key tone nearest the pointer as it moves along
 * the strip, as long as one lies within [MarkerReach], and null once it leaves.
 */
private fun Modifier.lightOnHover(
    markers: List<RampMark>,
    keyTone: Double,
    onLit: (Int?) -> Unit,
): Modifier =
    pointerInput(markers, keyTone, onLit) {
        val tones = markers.map { marker -> marker.tone } + keyTone
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val x = event.changes
                    .firstOrNull()
                    ?.position
                    ?.x
                if (event.type == PointerEventType.Exit || x == null || size.width == 0) {
                    onLit(null)
                    continue
                }
                val at = x / size.width * TONE_RANGE
                val nearest = tones.minBy { tone -> abs(tone - at) }
                val reach = MarkerReach.toPx() / size.width * TONE_RANGE
                onLit(if (abs(nearest - at) <= reach) nearest.roundToInt() else null)
            }
        }
    }

/**
 * The names under the strip, each centred on its tone. A name that would run into the one before it
 * drops to the next free line, so every name stays readable however close the tones are.
 */
@Composable
private fun MarkerLabels(
    markers: List<RampMark>,
    keyTone: Double,
) {
    val tokens = LocalBuilderTokens.current
    val keyMark = RampMark(stringResource(Res.string.widget_key_color), keyTone)
    val marks = (markers + keyMark).sortedBy { mark -> mark.tone }
    val toneWords = marks.map { mark -> stringResource(Res.string.widget_tone, mark.tone.roundToInt()) }
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            marks.forEachIndexed { index, mark ->
                val name = "${mark.name}, ${toneWords[index]}"
                BuilderText(
                    mark.name,
                    modifier = Modifier.semantics { contentDescription = name },
                    style = BuilderTextStyle.Value,
                    emphasis = if (mark === keyMark) Emphasis.Primary else Emphasis.Secondary,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = tokens.spacing.small.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { measurable -> measurable.measure(loose) }
        val lineEnds = mutableListOf<Int>()
        val positions = placeables.mapIndexed { index, placeable ->
            val centre = (marks[index].tone / TONE_RANGE * width).roundToInt()
            val x = (centre - placeable.width / 2).coerceIn(0, (width - placeable.width).coerceAtLeast(0))
            val line = lineEnds.indexOfFirst { end -> end + gap <= x }.takeIf { free -> free >= 0 } ?: lineEnds.size
            if (line == lineEnds.size) lineEnds += x + placeable.width else lineEnds[line] = x + placeable.width
            Triple(placeable, x, line)
        }
        val lineHeight = placeables.maxOfOrNull { placeable -> placeable.height } ?: 0
        layout(width, lineHeight * lineEnds.size) {
            for ((placeable, x, line) in positions) placeable.place(x, line * lineHeight)
        }
    }
}
