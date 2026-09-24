package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInputModeManager
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.extras_tone_dark
import com.materialkolor.builder.generated.resources.extras_tone_light
import com.materialkolor.builder.generated.resources.extras_tone_reset
import com.materialkolor.builder.generated.resources.extras_tone_value_dark
import com.materialkolor.builder.generated.resources.extras_tone_value_light
import com.materialkolor.builder.generated.resources.extras_tones_label
import com.materialkolor.builder.generated.resources.extras_tones_note
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderSlider
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The Custom slots cut from a palette by tone, the only ones the table moves (D27). */
internal val ToneSlots: List<CustomSlot> =
    CustomSlot.entries.filter { slot -> slot.resolution !is SlotResolution.FromRole }

/** The tones a slider covers. */
private val ToneRange: ClosedFloatingPointRange<Float> = 0f..100f

/** One arrow press moves one tone. */
private const val TONE_STEP = 1f

/**
 * The Custom target's tone table (F-18, D27), a row for each slot cut from a palette by tone. A
 * slot that follows a role moves with that role, so it has no row.
 *
 * Each row names the slot the way the code does and shows it in both modes, with a light and a dark
 * slider. A drag moves the preview every frame and lets go as one undo entry, and moving one mode
 * keeps the other as the document has it. Reset puts a moved slot back on its own tones.
 */
@Composable
internal fun CustomToneTable(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val state = context.capabilities[Control.CustomToneTable]
    if (!state.shown) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            BuilderText(text = stringResource(Res.string.extras_tones_label), style = BuilderTextStyle.SectionLabel)
            BuilderText(text = stringResource(Res.string.extras_tones_note), emphasis = Emphasis.Secondary)
        }
        ToneSlots.forEach { slot -> CustomToneRow(context, dispatcher, slot, enabled = state.usable) }
        state.explanation?.let { reason -> ReasonLine(reason) }
    }
}

/** One slot, its name and Reset over a light and a dark slider. */
@Composable
private fun CustomToneRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    slot: CustomSlot,
    enabled: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val input = LocalInputModeManager.current
    val lightSlider = remember { FocusRequester() }
    val stored = context.document.customTones[slot]
    val name = ColorRef.OfSlot(slot).readoutName(context.document)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(text = name, modifier = Modifier.weight(1f), style = BuilderTextStyle.Value)
            if (stored != null) {
                BuilderButton(
                    onClick = {
                        input.handFocusTo(lightSlider)
                        val change = DocumentChange.SetCustomTone(slot, null)
                        dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
                    },
                    label = stringResource(Res.string.extras_tone_reset, name),
                    emphasis = Emphasis.Subtle,
                    enabled = enabled,
                )
            }
        }
        ToneSlider(
            context = context,
            dispatcher = dispatcher,
            slot = slot,
            isDark = false,
            enabled = enabled,
            modifier = Modifier.focusRequester(lightSlider),
        )
        ToneSlider(context = context, dispatcher = dispatcher, slot = slot, isDark = true, enabled = enabled)
    }
}

/**
 * The tone of [slot] in one mode, with the slot's color in that mode beside it. A drag or an arrow
 * press only moves this mode, and the other keeps the tone the document holds for it, none
 * included.
 */
@Composable
private fun ToneSlider(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    slot: CustomSlot,
    isDark: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val drag = remember { PendingTone() }
    val stored = context.document.customTones[slot]
    val tone = (if (isDark) stored?.dark else stored?.light) ?: slot.resolution.ownTone(isDark)
    val name = ColorRef.OfSlot(slot).readoutName(context.document)
    val label = stringResource(if (isDark) Res.string.extras_tone_dark else Res.string.extras_tone_light, name)
    val value = if (isDark) Res.string.extras_tone_value_dark else Res.string.extras_tone_value_light
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorSwatch(context.result.customSlots[slot, isDark])
        BuilderSlider(
            value = tone.toFloat(),
            onValueChange = { moved ->
                val next = stored.withTone(isDark, moved.roundToInt().coerceIn(0, MAX_TONE))
                drag.tone = next
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetCustomTone(slot, next), EditPhase.Dragging))
            },
            label = label,
            modifier = modifier.weight(1f),
            onValueChangeFinished = {
                drag.tone?.let { last ->
                    drag.tone = null
                    val change = DocumentChange.SetCustomTone(slot, last)
                    dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Released))
                }
            },
            valueRange = ToneRange,
            step = TONE_STEP,
            stateDescription = tone.toString(),
            enabled = enabled,
        )
        BuilderText(text = stringResource(value, tone), style = BuilderTextStyle.Value)
    }
}

/** [this] with the tone of the mode [isDark] picks moved to [tone], and the other mode kept as it is. */
private fun CustomTone?.withTone(
    isDark: Boolean,
    tone: Int,
): CustomTone =
    if (isDark) {
        CustomTone(light = this?.light, dark = tone)
    } else {
        CustomTone(light = tone, dark = this?.dark)
    }

/** The tone the slot's own resolution cuts it at in the mode [isDark] picks. A role has none. */
private fun SlotResolution.ownTone(isDark: Boolean): Int =
    when (this) {
        is SlotResolution.FromRamp -> if (isDark) dark else light
        is SlotResolution.OnRamp -> if (isDark) dark else light
        is SlotResolution.FromRole -> error("A slot that follows a role has no tone of its own")
    }

/** The last tones a drag reported, which its release lands on. Only the release reads it. */
private class PendingTone {
    var tone: CustomTone? = null
}

private const val MAX_TONE = 100
