package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.keycolors_clear
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
import com.materialkolor.builder.generated.resources.keycolors_reset_all
import com.materialkolor.builder.generated.resources.keycolors_use_as_seed
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The six key colors, each set by hand or still coming from the seed, with Reset all under them
 * (F-14).
 *
 * A target that ignores a palette keeps its row on screen but takes no input there, and says why
 * once under the rows. Fluent still takes the primary, which moves its accent ramp.
 */
@Composable
internal fun KeyColorRows(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val messages = rememberHexMessages(HexSubject.KeyColor)
    val primary = context.capabilities[Control.PrimaryOverride]
    val others = context.capabilities[Control.OtherOverrides]
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(label = stringResource(Res.string.keycolors_label), topic = InfoTopic.KeyColors)
        KeyColor.entries.forEach { slot ->
            val state = if (slot == KeyColor.Primary) primary else others
            KeyColorRow(context, dispatcher, slot, messages, enabled = state.usable)
        }
        others.explanation?.let { reason -> ReasonLine(reason) }
        if (!context.document.keyColors.isEmpty()) {
            BuilderButton(
                onClick = {
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
 * One key color. The field shows the color set by hand, or the key color the scheme derived from
 * the seed while there is none, and a paste or a typed color sets it as one undo entry. Beside it
 * sit Pick and either "From seed" or the button that hands the palette back to the seed.
 *
 * @param[enabled] Whether the target lets this palette be set.
 */
@Composable
internal fun KeyColorRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    slot: KeyColor,
    messages: HexMessages,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val stored = context.document.keyColors[slot]
    val derived = remember(context.result, slot) { context.result.ramps[slot, false].keyColor }
    val shown = stored ?: derived
    val name = stringResource(keyColorName(slot))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(shown)
            BuilderHexField(
                value = shown,
                onCommit = { argb, _ ->
                    val change = DocumentChange.SetKeyColor(slot, argb)
                    dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
                },
                label = name,
                errorMessage = messages::errorOf,
                noteMessage = messages::noteOf,
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
            BuilderIconButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.KeyColorOverride(slot))) },
                icon = IconId.Eyedropper,
                contentDescription = stringResource(Res.string.keycolors_pick, name),
                enabled = enabled,
            )
            if (stored == null) {
                BuilderText(text = stringResource(Res.string.keycolors_from_seed), emphasis = Emphasis.Secondary)
            } else {
                BuilderIconButton(
                    onClick = {
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
            PrimaryOverrideLine(context, dispatcher, stored)
        }
    }
}

/**
 * Under a primary set by hand, the reminder that it moves only the primary palette while the seed
 * keeps the rest (D12), and Use as seed for anyone who wants that color to drive everything. Use
 * as seed makes it the seed and hands the primary back to it, as one undo entry.
 */
@Composable
private fun PrimaryOverrideLine(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    primary: Argb,
) {
    val spacing = LocalBuilderTokens.current.spacing
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(text = stringResource(Res.string.keycolors_primary_only), emphasis = Emphasis.Secondary)
        BuilderButton(
            onClick = {
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
 * [color] as a small square ringed in the poster's ink, as big as the buttons beside it. It reads
 * out nothing, the hex beside it says the color.
 */
@Composable
internal fun ColorSwatch(
    color: Argb,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    Box(
        modifier = modifier
            .size(LocalLayout.current.minTouchTarget)
            .background(color.toColor(), shape)
            .border(tokens.outlineWidth, tokens.borderStrong, shape),
    )
}

/** What the row for [slot] is called. */
internal fun keyColorName(slot: KeyColor): StringResource =
    when (slot) {
        KeyColor.Primary -> Res.string.keycolors_name_primary
        KeyColor.Secondary -> Res.string.keycolors_name_secondary
        KeyColor.Tertiary -> Res.string.keycolors_name_tertiary
        KeyColor.Error -> Res.string.keycolors_name_error
        KeyColor.Neutral -> Res.string.keycolors_name_neutral
        KeyColor.NeutralVariant -> Res.string.keycolors_name_neutral_variant
    }
