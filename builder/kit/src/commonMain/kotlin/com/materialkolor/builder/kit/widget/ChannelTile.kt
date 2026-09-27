package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
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
    val low = range.start.roundToInt()
    val high = range.endInclusive.roundToInt()
    val rangeMessage = stringResource(Res.string.picker_track_range, low, high)
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
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    label.uppercase(),
                    style = BuilderTextStyle.Value,
                    emphasis = Emphasis.Secondary,
                    maxLines = 1,
                )
                BuilderText(
                    "$low$RangeDash$high",
                    style = BuilderTextStyle.Value,
                    emphasis = Emphasis.Subtle,
                    maxLines = 1,
                )
            }
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
 * How far Shift steps.
 */
private const val BigStep: Int = 10

/**
 * Between the two ends of a tile's range, "0–360".
 */
private const val RangeDash: String = "–"
