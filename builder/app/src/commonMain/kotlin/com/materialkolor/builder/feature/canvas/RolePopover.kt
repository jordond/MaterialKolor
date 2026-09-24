package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.explanation
import com.materialkolor.builder.feature.poster.kotlinLiteralOf
import com.materialkolor.builder.feature.poster.reasonText
import com.materialkolor.builder.feature.poster.usable
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.tabs_copied_hex
import com.materialkolor.builder.generated.resources.tabs_copied_kotlin
import com.materialkolor.builder.generated.resources.tabs_copy_hex
import com.materialkolor.builder.generated.resources.tabs_copy_kotlin
import com.materialkolor.builder.generated.resources.tabs_pin
import com.materialkolor.builder.generated.resources.tabs_show_on_ramp
import com.materialkolor.builder.generated.resources.tabs_unpin
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.widget.SwatchTile
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * One swatch of the Roles tab, a role, a key color or a color of an accent, in one mode.
 *
 * @property[name] What the swatch is called, in lower camel case as in code.
 * @property[argb] The color it resolved to.
 * @property[ink] The color its name and hex are drawn in.
 * @property[tone] The tone [argb] resolved to.
 * @property[contrast] The ratio of its on-pair over it, or null when it has none.
 * @property[target] Where Show on ramp takes it, which also says what kind of swatch it is.
 * @property[pinned] Whether it is a role the document pins in this mode.
 */
@Immutable
internal data class RoleSwatch(
    val name: String,
    val argb: Argb,
    val ink: Color,
    val tone: Double,
    val contrast: Double?,
    val target: RampTarget,
    val pinned: Boolean = false,
)

/**
 * The swatch as a tile that opens its menu when pressed.
 *
 * The menu copies the hex or the Kotlin literal and shows the swatch on its ramp. A role also gets
 * Pin this role, pinning the role in the swatch's mode to the color it has now, or Unpin this role
 * when that mode is already pinned. It stays disabled while the target has no role pins, with the
 * reason under it. A copy the browser refuses hands focus back to the tile once its manual copy
 * dialog closes, since the menu is gone by then.
 *
 * @param[swatch] The swatch.
 * @param[pins] How the target treats role pins.
 * @param[dispatcher] Where the menu sends what it was asked to do.
 * @param[modifier] Applied to the tile.
 */
@Composable
internal fun RolePopover(
    swatch: RoleSwatch,
    pins: ControlState,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    LocalTileProbe.current?.invoke(swatch.name) // pf-1
    var open by remember { mutableStateOf(false) }
    val tile = remember { FocusRequester() } // b-308ba
    val copyHex = WorkspaceAction.CopyText(
        text = swatch.argb.toHex(),
        label = stringResource(Res.string.tabs_copied_hex, swatch.name),
        returnFocusTo = tile, // b-308ba
    )
    val copyKotlin = WorkspaceAction.CopyText(
        text = kotlinLiteralOf(swatch.argb),
        label = stringResource(Res.string.tabs_copied_kotlin, swatch.name),
        returnFocusTo = tile, // b-308ba
    )
    val items = buildList {
        add(
            BuilderMenuItem(
                label = stringResource(Res.string.tabs_copy_hex),
                onClick = { dispatcher.dispatch(copyHex) },
                icon = IconId.Copy,
            ),
        )
        add(
            BuilderMenuItem(
                label = stringResource(Res.string.tabs_copy_kotlin),
                onClick = { dispatcher.dispatch(copyKotlin) },
                icon = IconId.Copy,
            ),
        )
        addAll(pinItems(swatch, pins, dispatcher))
        add(
            BuilderMenuItem(
                label = stringResource(Res.string.tabs_show_on_ramp),
                onClick = { dispatcher.dispatch(WorkspaceAction.ShowOnRamp(swatch.target)) },
                icon = IconId.ChevronRight,
            ),
        )
    }
    BuilderMenu(expanded = open, onDismissRequest = { open = false }, items = items, modifier = modifier) {
        SwatchTile(
            name = swatch.name,
            color = swatch.argb.toColor(),
            onColor = swatch.ink,
            tone = swatch.tone,
            contrast = swatch.contrast,
            onCopy = { dispatcher.dispatch(copyHex) },
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().focusRequester(tile), // b-308ba
            pinned = swatch.pinned,
        )
    }
}

/**
 * Pin or Unpin for a role swatch in the swatch's mode, then why it is off when [pins] says so, or
 * nothing for any other swatch. The menu has no line for a note, so the reason is a row of its own
 * that nothing can choose.
 */
@Composable
private fun pinItems(
    swatch: RoleSwatch,
    pins: ControlState,
    dispatcher: Dispatcher<WorkspaceAction>,
): List<BuilderMenuItem> {
    val target = swatch.target as? RampTarget.OfRole ?: return emptyList()
    val mode = if (target.isDark) PinMode.Dark else PinMode.Light
    val change = DocumentChange.SetPin(target.role, mode, if (swatch.pinned) null else swatch.argb)
    val pin = BuilderMenuItem(
        label = stringResource(if (swatch.pinned) Res.string.tabs_unpin else Res.string.tabs_pin),
        onClick = { dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete)) },
        icon = IconId.Pin,
        enabled = pins.usable,
    )
    val reason = pins.explanation.takeUnless { pins.usable } ?: return listOf(pin)
    return listOf(pin, BuilderMenuItem(label = stringResource(reasonText(reason)), onClick = {}, enabled = false))
}
