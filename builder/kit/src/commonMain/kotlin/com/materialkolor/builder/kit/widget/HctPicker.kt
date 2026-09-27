package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The HCT color picker behind the seed, the key colors, the pins, the accents and the second seed
 * of a Cmf theme.
 *
 * A plane of every chroma and tone at the current hue sets those two together, and a track under it
 * sets the hue, each drawn as the colors it reaches with what sRGB cannot show hatched. Three tiles
 * take the hue, chroma and tone typed, a row of tone stops jumps between tones, and a format switch
 * with a field shows and takes the color as hex, RGB, HSL or OKLCH.
 *
 * A drag reports [EditPhase.Dragging] for every change and [EditPhase.Released] once when it lets
 * go, so the caller previews live and files one undo entry. Keys, a typed track value and a typed
 * color report [EditPhase.Discrete]. The picker only reports. Putting back the prior value on
 * cancel or Esc, "no override" included, is the caller's to do.
 *
 * The picker keeps the chroma it was asked for across hue and tone changes, so dragging the hue
 * through a narrow stretch of the gamut and back leaves the chroma where it was. A new [value] from
 * outside, such as an undo, starts it over from that color. One that arrives mid drag is let go,
 * since the drag owns the color until it ends.
 *
 * @param[value] The color the picker shows.
 * @param[onChange] Called with each new color and where it is in its gesture.
 * @param[modifier] Applied to the picker.
 */
@Composable
public fun HctPicker(
    value: Argb,
    onChange: (Argb, EditPhase) -> Unit,
    modifier: Modifier = Modifier,
) {
    HctPicker(value, onChange, modifier, onBodyComposed = null)
}

/**
 * [HctPicker], with [onBodyComposed] called each time the picker's body composes.
 */
@Composable
internal fun HctPicker(
    value: Argb,
    onChange: (Argb, EditPhase) -> Unit,
    modifier: Modifier,
    onBodyComposed: (() -> Unit)?,
) {
    val report = rememberUpdatedState(onChange)
    val picker = remember { PickerState(value, report) }
    picker.follow(value)
    PickerBody(picker, modifier, onBodyComposed)
}

/**
 * The hue, chroma and tone a picker is at, and the color they make.
 *
 * [chroma] is the chroma asked for, which can be more than the current hue and tone reach.
 * [valueOf] gives the chroma the color actually has.
 */
@Stable
internal class PickerState(
    initial: Argb,
    private val report: State<(Argb, EditPhase) -> Unit>,
) {
    var hue: Double by mutableDoubleStateOf(0.0)
        private set

    var chroma: Double by mutableDoubleStateOf(0.0)
        private set

    var tone: Double by mutableDoubleStateOf(0.0)
        private set

    /**
     * The color the three channels make.
     */
    var color: Argb by mutableStateOf(initial)
        private set

    private var reachedChroma: Double by mutableDoubleStateOf(0.0)

    /**
     * The last value the caller handed in.
     */
    private var seen: Argb = initial

    private var dragging: Boolean = false

    private val shown = HctChannel.entries.map { channel -> derivedStateOf { valueOf(channel).roundToInt() } }

    init {
        adopt(initial)
    }

    /**
     * Where [channel] stands, with chroma as the color has it rather than as it was asked for.
     */
    fun valueOf(channel: HctChannel): Double =
        when (channel) {
            HctChannel.Hue -> hue
            HctChannel.Chroma -> reachedChroma
            HctChannel.Tone -> tone
        }

    /**
     * [valueOf] to the whole number, the way a track shows and reads it out. Composition reads this
     * rather than [valueOf], so a drag composes again only when the number on screen changes.
     */
    fun shownOf(channel: HctChannel): Int = shown[channel.ordinal].value

    /**
     * Starts over from a new [value] from outside, unless it is the color the picker already made.
     */
    fun follow(value: Argb) {
        if (value == seen) return
        seen = value
        if (!dragging && value != color) adopt(value)
    }

    /**
     * Moves [channel] to [target], clamped into its range, and reports the new color with [phase]
     * when it changed. Chroma also stops at the most the exact current hue and tone reach, so a color
     * on the edge of sRGB keeps its chroma.
     *
     * @return Whether the color changed.
     */
    fun set(
        channel: HctChannel,
        target: Double,
        phase: EditPhase,
    ): Boolean {
        val next = target.coerceIn(channel.range)
        when (channel) {
            HctChannel.Hue -> hue = next
            HctChannel.Chroma -> chroma = min(next, GamutLimit.edge(hue, tone))
            HctChannel.Tone -> tone = next
        }
        return make(phase)
    }

    /**
     * Moves chroma and tone together, the way the plane does, each clamped into its range and chroma
     * stopping at the most the hue reaches at the new tone. Reports the new color once with [phase]
     * when it changed.
     *
     * @return Whether the color changed.
     */
    fun setChromaAndTone(
        chroma: Double,
        tone: Double,
        phase: EditPhase,
    ): Boolean {
        this.tone = tone.coerceIn(HctChannel.Tone.range)
        this.chroma = min(chroma.coerceIn(HctChannel.Chroma.range), GamutLimit.edge(hue, this.tone))
        return make(phase)
    }

    /**
     * Takes a typed [argb] and reports it as [EditPhase.Discrete] when it changes the color.
     */
    fun type(argb: Argb) {
        if (argb == color) return
        adopt(argb)
        report.value(argb, EditPhase.Discrete)
    }

    fun startDrag() {
        dragging = true
    }

    /**
     * Ends a drag, reporting [EditPhase.Released] with the color it ended on when [released].
     */
    fun endDrag(released: Boolean) {
        dragging = false
        if (released) report.value(color, EditPhase.Released)
    }

    private fun make(phase: EditPhase): Boolean {
        val made = Hct.from(hue, chroma, tone)
        reachedChroma = made.chroma
        return emit(Argb(made.toInt()), phase)
    }

    private fun emit(
        argb: Argb,
        phase: EditPhase,
    ): Boolean {
        if (argb == color) return false
        color = argb
        report.value(argb, phase)
        return true
    }

    private fun adopt(value: Argb) {
        val hct = Hct.fromInt(value.value)
        hue = hct.hue
        chroma = hct.chroma
        tone = hct.tone
        reachedChroma = hct.chroma
        color = value
    }
}

/**
 * The plane, the hue track, the tiles, the tone stops and the format switch. Nothing here reads the
 * color, so a drag leaves it alone and only the parts below it recompose or redraw.
 *
 * From [TwoPaneWidth] of width it lays out in two panes, the plane over the hue track on the start
 * side and the rest on the end side. Narrower, it is one column in that order. Either way the plane
 * shrinks first when the height runs short, down to [PlaneMinHeight], before anything overflows.
 */
@Composable
private fun PickerBody(
    picker: PickerState,
    modifier: Modifier,
    onBodyComposed: (() -> Unit)?,
) {
    onBodyComposed?.invoke()
    val spacing = LocalBuilderTokens.current.spacing
    val hueLabel = stringResource(HctChannel.Hue.nameResource())
    BoxWithConstraints(modifier) {
        if (maxWidth >= TwoPaneWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                PlaneColumn(Modifier.width(PlaneMaxWidth)) {
                    GamutPlane(picker)
                    GamutTrack(picker, HctChannel.Hue, hueLabel)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                    ChannelTiles(picker)
                    ToneStops(picker)
                    PickerFormat(picker)
                }
            }
        } else {
            PlaneColumn(Modifier.fillMaxWidth()) {
                GamutPlane(picker)
                GamutTrack(picker, HctChannel.Hue, hueLabel)
                ChannelTiles(picker)
                ToneStops(picker)
                PickerFormat(picker)
            }
        }
    }
}

/**
 * A column whose first child is the plane. The rest take the column's width at their own heights,
 * and the plane takes what height is left, between [PlaneMinHeight] and [PlaneMaxHeight], with its
 * width following at the plane's ratio and centred.
 */
@Composable
private fun PlaneColumn(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val gap = LocalBuilderTokens.current.spacing.large
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val loose = Constraints(minWidth = width, maxWidth = width)
        val rest = measurables.drop(1).map { measurable -> measurable.measure(loose) }
        val restHeight = rest.sumOf { placeable -> placeable.height } + gapPx * rest.size
        val widest = minOf(width, PlaneMaxWidth.roundToPx())
        val tallest = minOf(PlaneMaxHeight.roundToPx(), (widest / PlaneRatio).roundToInt())
        val left = if (constraints.hasBoundedHeight) constraints.maxHeight - restHeight else tallest
        val planeHeight = left.coerceIn(minOf(PlaneMinHeight.roundToPx(), tallest), tallest)
        val planeWidth = if (planeHeight == tallest) widest else minOf(widest, (planeHeight * PlaneRatio).roundToInt())
        val plane = measurables.first().measure(Constraints.fixed(planeWidth, planeHeight))
        val height = constraints.constrainHeight(planeHeight + restHeight)
        layout(width, height) {
            plane.placeRelative((width - planeWidth) / 2, 0)
            var y = planeHeight + gapPx
            for (placeable in rest) {
                placeable.placeRelative(0, y)
                y += placeable.height + gapPx
            }
        }
    }
}

/**
 * The narrowest body that lays out in two panes.
 */
private val TwoPaneWidth: Dp = 780.dp

/**
 * The plane at its largest, and the least it shrinks to when the height runs short.
 */
private val PlaneMaxWidth: Dp = 452.dp
private val PlaneMaxHeight: Dp = 296.dp
private val PlaneMinHeight: Dp = 200.dp

private const val PlaneRatio: Float = 452f / 296f
