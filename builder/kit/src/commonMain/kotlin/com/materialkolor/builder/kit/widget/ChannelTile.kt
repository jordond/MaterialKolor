package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.kit.control.BuilderInlineField
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.picker_chroma
import com.materialkolor.builder.kit.generated.resources.picker_hue
import com.materialkolor.builder.kit.generated.resources.picker_tone
import com.materialkolor.builder.kit.generated.resources.picker_track_range
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The hue, chroma and tone of [picker] as three tiles side by side, the exact, typed route to a color.
 */
@Composable
internal fun ChannelTiles(
    picker: PickerState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
    ) {
        for (channel in HctChannel.entries) ChannelTile(picker, channel, Modifier.weight(1f))
    }
}

/**
 * One [channel] of [picker] as a tile, a small label and range over the value in large mono.
 *
 * The value is a [BuilderInlineField], so typing edits a draft that Enter or leaving the tile
 * commits as one discrete edit, and a draft outside the range says so under the tile. Up and Down
 * step the value by one, ten with Shift, each a discrete edit of its own.
 */
@Composable
internal fun ChannelTile(
    picker: PickerState,
    channel: HctChannel,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val label = stringResource(channel.nameResource())
    val range = channel.range
    val rangeMessage =
        stringResource(Res.string.picker_track_range, range.start.roundToInt(), range.endInclusive.roundToInt())
    val shown = picker.shownOf(channel)
    var draft by remember(shown) { mutableStateOf<String?>(null) }
    val error = { text: String ->
        if (text.trim().toDoubleOrNull()?.let { it in range } ==
            true
        ) {
            null
        } else {
            rangeMessage
        }
    }
    val problem = draft?.let(error)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.panelRaised, RoundedCornerShape(tokens.radius.medium))
                .padding(horizontal = spacing.medium, vertical = spacing.small),
        ) {
            TileHeading(label.uppercase(), channel.rangeText(), Modifier.fillMaxWidth().clearAndSetSemantics {})
            BuilderInlineField(
                value = shown.toString(),
                onCommit = { typed ->
                    val target = typed.trim().toDoubleOrNull()
                    if (target != null) picker.set(channel, target, EditPhase.Discrete)
                },
                label = label,
                style = LocalBuilderType.current.value.copy(
                    fontSize = TileValueSize.sp,
                    lineHeight = TileValueLineHeight.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val stride = if (event.isShiftPressed) BigStep else 1
                    val step = when (event.key) {
                        Key.DirectionUp -> stride
                        Key.DirectionDown -> -stride
                        else -> return@onPreviewKeyEvent false
                    }
                    picker.set(channel, (picker.shownOf(channel) + step).toDouble(), EditPhase.Discrete)
                    true
                },
                error = error,
                onDraftChange = { text -> draft = text },
            )
        }
        if (problem != null) BuilderText(problem, style = BuilderTextStyle.Body, emphasis = Emphasis.Danger)
    }
}

/**
 * A tile's small capitals [label] with its [range] at the far end, as the canvas sets them.
 *
 * Where the widest channel's heading would not fit on one line, every tile puts its range on a line
 * of its own under the label, so neither is cut and the three values stay level. The tiles share
 * the row evenly, so each one comes to the same answer.
 */
@Composable
private fun TileHeading(
    label: String,
    range: String,
    modifier: Modifier,
) {
    val type = LocalBuilderType.current
    val ink = LocalBuilderTokens.current.textMuted
    val labelStyle = remember(type, ink) {
        type.value.merge(
            TextStyle(
                color = ink,
                fontSize = HeadingLabelSize,
                lineHeight = HeadingLine,
                fontWeight = FontWeight.Bold,
                letterSpacing = HeadingTracking,
            ),
        )
    }
    val rangeStyle = remember(type, ink) {
        type.value.merge(TextStyle(color = ink, fontSize = HeadingRangeSize, lineHeight = HeadingLine))
    }
    val headings = HctChannel.entries.map { entry ->
        stringResource(entry.nameResource()).uppercase() to
            entry.rangeText()
    }
    val measurer = rememberTextMeasurer()
    val gap = LocalBuilderTokens.current.spacing.extraSmall
    Layout(
        content = {
            BasicText(label, style = labelStyle, maxLines = 1, softWrap = false)
            BasicText(range, style = rangeStyle, maxLines = 1, softWrap = false)
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaced = measurables[0].measure(loose)
        val rangePlaced = measurables[1].measure(loose)
        val gapPx = gap.roundToPx()
        val widest = headings.maxOf { (name, span) ->
            measurer.measure(name, labelStyle).size.width + gapPx + measurer.measure(span, rangeStyle).size.width
        }
        val oneLine = !constraints.hasBoundedWidth || widest <= constraints.maxWidth
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else widest
        val height = if (oneLine) {
            max(labelPlaced.height, rangePlaced.height)
        } else {
            labelPlaced.height + rangePlaced.height
        }
        layout(width, constraints.constrainHeight(height)) {
            labelPlaced.placeRelative(0, 0)
            if (oneLine) {
                val lift = labelPlaced[FirstBaseline] - rangePlaced[FirstBaseline]
                rangePlaced.placeRelative(width - rangePlaced.width, lift)
            } else {
                rangePlaced.placeRelative(0, labelPlaced.height)
            }
        }
    }
}

/**
 * The span a channel takes, "0–360".
 */
private fun HctChannel.rangeText(): String = "${range.start.roundToInt()}$RangeDash${range.endInclusive.roundToInt()}"

internal fun HctChannel.nameResource(): StringResource =
    when (this) {
        HctChannel.Hue -> Res.string.picker_hue
        HctChannel.Chroma -> Res.string.picker_chroma
        HctChannel.Tone -> Res.string.picker_tone
    }

/**
 * The tile's value, in sp, set large enough to read from across the dialog.
 */
private const val TileValueSize: Int = 34
private const val TileValueLineHeight: Int = 44

/**
 * The heading's type, the label a size over its range, both on one line height.
 */
private val HeadingLabelSize: TextUnit = 11.sp
private val HeadingRangeSize: TextUnit = 10.sp
private val HeadingLine: TextUnit = 14.sp
private val HeadingTracking: TextUnit = 1.2.sp

/**
 * How far Shift steps.
 */
private const val BigStep: Int = 10

/**
 * Between the two ends of a tile's range, "0–360".
 */
private const val RangeDash: String = "–"
