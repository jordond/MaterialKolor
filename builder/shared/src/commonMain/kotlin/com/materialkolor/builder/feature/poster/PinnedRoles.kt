package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInputModeManager
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.KeyColor
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
 * The roles pinned to colors of their own, one row per pinned mode with its clear, then Clear all.
 * It lists the document as stored, so a target that ignores pins still shows them, says why in
 * place of the notice and lets none of them go.
 *
 * A clear hands a keyboard user's focus to the next pin's clear, or the previous one at the end of
 * the list. With the list gone it goes to the last key color's Pick in [picks].
 *
 * @param[picks] The key color rows' Pick buttons, when they stand above the list.
 */
@Composable
internal fun PinnedRoles(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    picks: KeyColorPicks? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val input = LocalInputModeManager.current
    val state = context.capabilities[Control.RolePins]
    val document = context.document
    val pinned = remember(document.pins) { PinnedMode.of(document.pins) }
    val clears = remember { PinClears() }
    val reason = state.explanation
    val afterList = picks?.get(KeyColor.entries.last())
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(label = stringResource(Res.string.pins_label), topic = InfoTopic.Pins)
        when {
            reason != null -> ReasonLine(reason)
            pinned.isNotEmpty() -> BuilderText(text = stringResource(Res.string.pins_notice))
            else -> BuilderText(text = stringResource(Res.string.pins_empty), emphasis = Emphasis.Secondary)
        }
        pinned.forEachIndexed { index, pin ->
            // Keyed so a row that stays keeps its button, and the focus handed to it, when one above goes.
            key(pin.role, pin.mode) {
                val next = pinned.getOrNull(index + 1) ?: pinned.getOrNull(index - 1)
                PinRow(
                    pin = pin,
                    document = document,
                    enabled = state.usable,
                    dispatcher = dispatcher,
                    clear = clears[pin],
                    onClear = {
                        val target = next?.let { other -> clears[other] } ?: afterList
                        target?.let { focus -> input.handFocusTo(focus) }
                    },
                )
            }
        }
        if (pinned.isNotEmpty()) {
            BuilderButton(
                onClick = {
                    afterList?.let { focus -> input.handFocusTo(focus) }
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.ClearPins, EditPhase.Discrete))
                },
                label = stringResource(Res.string.pins_clear_all),
                emphasis = Emphasis.Subtle,
                enabled = state.usable,
            )
        }
    }
}

/**
 * One pinned mode, the role as the contrast readout names it, the mode, the color and its clear.
 * The clear takes focus through [clear] and calls [onClear] before it lets the pin go.
 */
@Composable
private fun PinRow(
    pin: PinnedMode,
    document: ThemeDocument,
    enabled: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    clear: FocusRequester,
    onClear: () -> Unit,
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
                onClear()
                val change = DocumentChange.SetPin(pin.role, pin.mode, null)
                dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
            },
            icon = IconId.Close,
            contentDescription = stringResource(Res.string.pins_clear, role, mode),
            modifier = Modifier.focusRequester(clear),
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
        /**
         * Every pinned mode in [pins], in role order and light before dark.
         */
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

/**
 * The clear button of each pinned mode, kept for as long as the list is on screen.
 */
private class PinClears {
    private val requesters = mutableMapOf<Pair<Role, PinMode>, FocusRequester>()

    /**
     * The clear button of [pin].
     */
    operator fun get(pin: PinnedMode): FocusRequester = requesters.getOrPut(pin.role to pin.mode) { FocusRequester() }
}

/**
 * What the list calls this mode.
 */
private val PinMode.label: StringResource
    get() = when (this) {
        PinMode.Light -> Res.string.pins_mode_light
        PinMode.Dark -> Res.string.pins_mode_dark
    }
