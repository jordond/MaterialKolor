package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.pins_clear
import com.materialkolor.builder.generated.resources.pins_clear_all
import com.materialkolor.builder.generated.resources.pins_empty
import com.materialkolor.builder.generated.resources.pins_label
import com.materialkolor.builder.generated.resources.pins_mode_dark
import com.materialkolor.builder.generated.resources.pins_mode_light
import com.materialkolor.builder.generated.resources.pins_notice
import com.materialkolor.builder.generated.resources.pins_row
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The roles pinned to colors of their own, one row per pinned mode with its clear, then Clear all
 * (F-15). It lists the document as stored, so a target that ignores pins still shows them, says
 * why in place of the notice and lets none of them go.
 */
@Composable
internal fun PinnedRoles(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val state = context.capabilities[Control.RolePins]
    val document = context.document
    val pinned = remember(document.pins) { PinnedMode.of(document.pins) }
    val reason = state.explanation
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(label = stringResource(Res.string.pins_label), topic = InfoTopic.Pins)
        when {
            reason != null -> ReasonLine(reason)
            pinned.isNotEmpty() -> BuilderText(text = stringResource(Res.string.pins_notice))
            else -> BuilderText(text = stringResource(Res.string.pins_empty), emphasis = Emphasis.Secondary)
        }
        pinned.forEach { pin -> PinRow(pin, document, enabled = state.usable, dispatcher = dispatcher) }
        if (pinned.isNotEmpty()) {
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.ClearPins, EditPhase.Discrete)) },
                label = stringResource(Res.string.pins_clear_all),
                emphasis = Emphasis.Subtle,
                enabled = state.usable,
            )
        }
    }
}

/** One pinned mode, the role as the contrast readout names it, the mode, the color and its clear. */
@Composable
private fun PinRow(
    pin: PinnedMode,
    document: ThemeDocument,
    enabled: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val role = ColorRef.OfRole(pin.role).readoutName(document)
    val mode = stringResource(pin.mode.label)
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorSwatch(pin.argb)
        BuilderText(
            text = stringResource(Res.string.pins_row, role, mode),
            modifier = Modifier.weight(1f),
        )
        BuilderText(text = pin.argb.toHex(), style = BuilderTextStyle.Value)
        BuilderIconButton(
            onClick = {
                val change = DocumentChange.SetPin(pin.role, pin.mode, null)
                dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
            },
            icon = IconId.Close,
            contentDescription = stringResource(Res.string.pins_clear, role, mode),
            enabled = enabled,
        )
    }
}

/**
 * One mode of one pinned role, a row of the list.
 *
 * @property[role] The pinned role.
 * @property[mode] The mode the color holds in.
 * @property[argb] The color the role takes in that mode.
 */
@Immutable
internal data class PinnedMode(
    val role: Role,
    val mode: PinMode,
    val argb: Argb,
) {
    companion object {
        /** Every pinned mode in [pins], in role order and light before dark. */
        fun of(pins: Map<Role, RolePin>): List<PinnedMode> =
            Role.entries.flatMap { role ->
                val pin = pins[role]
                listOfNotNull(
                    pin?.light?.let { argb -> PinnedMode(role, PinMode.Light, argb) },
                    pin?.dark?.let { argb -> PinnedMode(role, PinMode.Dark, argb) },
                )
            }
    }
}

/** What the list calls this mode. */
private val PinMode.label: StringResource
    get() = when (this) {
        PinMode.Light -> Res.string.pins_mode_light
        PinMode.Dark -> Res.string.pins_mode_dark
    }
