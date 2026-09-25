package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.picker_chroma
import com.materialkolor.builder.kit.generated.resources.picker_hue
import com.materialkolor.builder.kit.generated.resources.picker_tone
import com.materialkolor.builder.kit.generated.resources.picker_track_range
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.hct.Hct
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The HCT color picker behind the seed, the key colors, the pins, the accents and the second seed
 * of a Cmf theme.
 *
 * Three tracks set hue, chroma and tone, each drawn as the colors it reaches with what sRGB cannot
 * show shaded. Under them sits a format switch with a field that shows and takes the color as hex,
 * RGB, HSL or OKLCH.
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
        val made = Hct.from(hue, chroma, tone)
        reachedChroma = made.chroma
        return emit(Argb(made.toInt()), phase)
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
 * The tracks and the format switch. Nothing here reads the color, so a drag leaves it alone and
 * only the tracks and fields below it recompose.
 */
@Composable
private fun PickerBody(
    picker: PickerState,
    modifier: Modifier,
    onBodyComposed: (() -> Unit)?,
) {
    onBodyComposed?.invoke()
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        for (channel in HctChannel.entries) TrackRow(picker, channel)
        PickerFormat(picker)
    }
}

/**
 * A track with its value beside it as a field, so the value can be typed as well as dragged.
 */
@Composable
private fun TrackRow(
    picker: PickerState,
    channel: HctChannel,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(channel.nameResource())
    val range = channel.range
    val rangeMessage = stringResource(
        Res.string.picker_track_range,
        range.start.roundToInt(),
        range.endInclusive.roundToInt(),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GamutTrack(picker, channel, label, Modifier.weight(1f))
        BuilderTextField(
            value = picker.shownOf(channel).toString(),
            onCommit = { typed ->
                val target = typed.trim().toDoubleOrNull()
                if (target != null) picker.set(channel, target, EditPhase.Discrete)
            },
            label = label,
            modifier = Modifier.width(spacing.section * TrackFieldWidthInSections),
            error = { draft -> if (draft.trim().toDoubleOrNull()?.let { it in range } == true) null else rangeMessage },
            style = BuilderTextStyle.Value,
        )
    }
}

private fun HctChannel.nameResource(): StringResource =
    when (this) {
        HctChannel.Hue -> Res.string.picker_hue
        HctChannel.Chroma -> Res.string.picker_chroma
        HctChannel.Tone -> Res.string.picker_tone
    }

/**
 * A track's value field is three section gaps wide, room for "360" and the field's padding.
 */
private const val TrackFieldWidthInSections: Int = 3
