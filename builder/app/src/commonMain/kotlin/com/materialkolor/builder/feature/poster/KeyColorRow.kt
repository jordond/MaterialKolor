package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.keycolors_clear
import com.materialkolor.builder.generated.resources.keycolors_edit
import com.materialkolor.builder.generated.resources.keycolors_from_seed
import com.materialkolor.builder.generated.resources.keycolors_label
import com.materialkolor.builder.generated.resources.keycolors_name_error
import com.materialkolor.builder.generated.resources.keycolors_name_neutral
import com.materialkolor.builder.generated.resources.keycolors_name_neutral_variant
import com.materialkolor.builder.generated.resources.keycolors_name_primary
import com.materialkolor.builder.generated.resources.keycolors_name_secondary
import com.materialkolor.builder.generated.resources.keycolors_name_tertiary
import com.materialkolor.builder.generated.resources.keycolors_pick
import com.materialkolor.builder.generated.resources.keycolors_primary_only
import com.materialkolor.builder.generated.resources.keycolors_primary_only_fluent
import com.materialkolor.builder.generated.resources.keycolors_reset_all
import com.materialkolor.builder.generated.resources.keycolors_use_as_seed
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The six key colors, each set by hand or still coming from the seed, with Reset all under them.
 *
 * A target that ignores a palette keeps its row on screen but takes no input there, and says why
 * once under the rows. Fluent still takes the primary, which moves its accent ramp.
 *
 * @param[picks] The rows' Pick buttons, where a keyboard user lands when a button they pressed goes.
 */
@Composable
internal fun KeyColorRows(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    picks: KeyColorPicks = remember { KeyColorPicks() },
) {
    val spacing = LocalBuilderTokens.current.spacing
    val input = LocalInputModeManager.current
    val messages = rememberHexMessages(HexSubject.KeyColor)
    val primary = context.capabilities[Control.PrimaryOverride]
    val others = context.capabilities[Control.OtherOverrides]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(label = stringResource(Res.string.keycolors_label), topic = InfoTopic.KeyColors)
        KeyColor.entries.forEach { slot ->
            val state = if (slot == KeyColor.Primary) primary else others
            KeyColorRow(context, dispatcher, slot, messages, enabled = state.usable, picks = picks)
        }
        others.explanation?.let { reason -> ReasonLine(reason) }
        if (!context.document.keyColors.isEmpty()) {
            BuilderButton(
                onClick = {
                    input.handFocusTo(picks[KeyColor.Primary])
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.ResetKeyColors, EditPhase.Discrete))
                },
                label = stringResource(Res.string.keycolors_reset_all),
                emphasis = Emphasis.Subtle,
                enabled = others.usable,
            )
        }
    }
}

/**
 * One key color as one slim row, its swatch, its name, its hex and Pick, the way board E draws it.
 * The hex shows the color set by hand, or the key color the scheme derived from the seed while
 * there is none, with a quiet "From seed" under the name. Clicking the hex, or tabbing onto it,
 * turns it into a field, and a paste or a typed color sets it as one undo entry. The field turns
 * back into text once focus leaves it. A color set by hand gets the button that hands the palette
 * back to the seed beside Pick. Clear hands a keyboard user's focus to Pick as it goes.
 *
 * @param[enabled] Whether the target lets this palette be set.
 * @param[picks] Where each row's Pick button takes focus.
 */
@Composable
internal fun KeyColorRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    slot: KeyColor,
    messages: HexMessages,
    enabled: Boolean,
    picks: KeyColorPicks,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val input = LocalInputModeManager.current
    val stored = context.document.keyColors[slot]
    val derived = remember(context.result, slot) { context.result.ramps[slot, false].keyColor }
    val shown = stored ?: derived
    val name = stringResource(keyColorName(slot))
    // b-527
    var editing by remember(slot) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(shown, size = tokens.iconSize)
            if (editing && enabled) {
                KeyColorField(
                    value = shown,
                    name = name,
                    messages = messages,
                    onCommit = { argb ->
                        val change = DocumentChange.SetKeyColor(slot, argb)
                        dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
                    },
                    onLeave = { editing = false },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f)) {
                    BuilderText(text = name, style = BuilderTextStyle.Label, maxLines = 1)
                    if (stored == null) {
                        val fromSeed = stringResource(Res.string.keycolors_from_seed)
                        BuilderText(text = fromSeed, emphasis = Emphasis.Secondary)
                    }
                }
                KeyColorHex(color = shown, name = name, enabled = enabled, onEdit = { editing = true })
            }
            BuilderIconButton(
                onClick = {
                    dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.KeyColorOverride(slot), picks[slot]))
                },
                icon = IconId.Eyedropper,
                contentDescription = stringResource(Res.string.keycolors_pick, name),
                modifier = pickButtonFocus(picks[slot]),
                enabled = enabled,
            )
            if (stored != null) {
                BuilderIconButton(
                    onClick = {
                        input.handFocusTo(picks[slot])
                        val change = DocumentChange.SetKeyColor(slot, null)
                        dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
                    },
                    icon = IconId.Close,
                    contentDescription = stringResource(Res.string.keycolors_clear, name),
                    enabled = enabled,
                )
            }
        }
        if (slot == KeyColor.Primary && stored != null) {
            PrimaryOverrideLine(context, dispatcher, stored, pick = picks[KeyColor.Primary])
        }
    }
}

/**
 * A key color's hex as mono text, which turns into the field once clicked or focused. It reads out
 * as Edit with the key color's name and its hex.
 */
@Composable
private fun KeyColorHex(
    color: Argb,
    name: String,
    enabled: Boolean,
    onEdit: () -> Unit,
) {
    val hex = color.toHex()
    BuilderPressable(
        onClick = onEdit,
        label = stringResource(Res.string.keycolors_edit, name, hex),
        modifier = Modifier.onFocusChanged { focus -> if (focus.isFocused) onEdit() },
        enabled = enabled,
    ) {
        BuilderText(
            text = hex,
            modifier = Modifier.padding(horizontal = LocalBuilderTokens.current.spacing.extraSmall),
            style = BuilderTextStyle.Value,
            maxLines = 1,
        )
    }
}

/**
 * The field a key color's hex turns into, which takes focus as it comes in and calls [onLeave] once
 * focus has left it again.
 */
@Composable
private fun KeyColorField(
    value: Argb,
    name: String,
    messages: HexMessages,
    onCommit: (Argb) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    // The field reports itself unfocused before it takes focus, which is no leave.
    var held by remember { mutableStateOf(false) }
    BuilderHexField(
        value = value,
        onCommit = { argb, _ -> onCommit(argb) },
        label = name,
        errorMessage = messages::errorOf,
        noteMessage = messages::noteOf,
        modifier = modifier
            .focusRequester(focus)
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    held = true
                } else if (held) {
                    onLeave()
                }
            },
    )
    LaunchedEffect(focus) { focus.requestFocus() }
}

/**
 * Under a primary set by hand, the reminder that it moves only the primary palette while the seed
 * keeps the rest, and Use as seed for anyone who wants that color to drive everything. Use as seed
 * makes it the seed and hands the primary back to it, as one undo entry, and hands a keyboard
 * user's focus to [pick] as the line goes. Fluent takes no other palette, so there the reminder
 * says the primary moves the accent ramp instead.
 */
@Composable
private fun PrimaryOverrideLine(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    primary: Argb,
    pick: FocusRequester,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val input = LocalInputModeManager.current
    val onlyPalette = context.capabilities[Control.OtherOverrides] is ControlState.Disabled
    val reminder = if (onlyPalette) Res.string.keycolors_primary_only_fluent else Res.string.keycolors_primary_only
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(text = stringResource(reminder), emphasis = Emphasis.Secondary)
        BuilderButton(
            onClick = {
                input.handFocusTo(pick)
                val document = context.document
                val seeded = document.copy(
                    seed = primary,
                    seedSource = SeedSource.Picked,
                    keyColors = document.keyColors.with(KeyColor.Primary, null),
                )
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.Replace(seeded), EditPhase.Discrete))
            },
            label = stringResource(Res.string.keycolors_use_as_seed),
            emphasis = Emphasis.Subtle,
        )
    }
}

/**
 * [color] as a small square ringed in the poster's ink, as big as the buttons beside it unless
 * [size] says otherwise. It reads out nothing, the hex beside it says the color.
 */
@Composable
internal fun ColorSwatch(
    color: Argb,
    modifier: Modifier = Modifier,
    size: Dp = LocalLayout.current.minTouchTarget,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    Box(
        modifier = modifier
            .size(size)
            .background(color.toColor(), shape)
            .border(tokens.outlineWidth, tokens.borderStrong, shape),
    )
}

/**
 * The Pick button of each key color row, where the focus goes when a button beside it goes away.
 */
@Stable
internal class KeyColorPicks {
    private val requesters = KeyColor.entries.associateWith { FocusRequester() }

    /**
     * The Pick button of the row for [slot].
     */
    operator fun get(slot: KeyColor): FocusRequester = requesters.getValue(slot)
}

/**
 * What the row for [slot] is called.
 */
internal fun keyColorName(slot: KeyColor): StringResource =
    when (slot) {
        KeyColor.Primary -> Res.string.keycolors_name_primary
        KeyColor.Secondary -> Res.string.keycolors_name_secondary
        KeyColor.Tertiary -> Res.string.keycolors_name_tertiary
        KeyColor.Error -> Res.string.keycolors_name_error
        KeyColor.Neutral -> Res.string.keycolors_name_neutral
        KeyColor.NeutralVariant -> Res.string.keycolors_name_neutral_variant
    }
