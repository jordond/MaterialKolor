package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.kotlinLiteralOf
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
 */
@Immutable
internal class RoleSwatch(
    val name: String,
    val argb: Argb,
    val ink: Color,
    val tone: Double,
    val contrast: Double?,
    val target: RampTarget,
)

/**
 * The swatch as a tile that opens its menu when pressed.
 *
 * The menu copies the hex or the Kotlin literal and shows the swatch on its ramp. A role also gets
 * Pin this role, pinning the role in the swatch's mode to the color it has now, or Unpin this role
 * when that mode is already pinned. It stays disabled while the target has no role pins.
 *
 * @param[swatch] The swatch.
 * @param[document] The document the swatch was resolved from, which says what is pinned.
 * @param[pinsEnabled] Whether the target lets a role be pinned.
 * @param[dispatcher] Where the menu sends what it was asked to do.
 * @param[modifier] Applied to the tile.
 */
@Composable
internal fun RolePopover(
    swatch: RoleSwatch,
    document: ThemeDocument,
    pinsEnabled: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val copyHex = WorkspaceAction.CopyText(swatch.argb.toHex(), stringResource(Res.string.tabs_copied_hex, swatch.name))
    val copyKotlin = WorkspaceAction.CopyText(
        kotlinLiteralOf(swatch.argb),
        stringResource(Res.string.tabs_copied_kotlin, swatch.name),
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
        pinItem(swatch, document, pinsEnabled, dispatcher)?.let(::add)
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Pin or Unpin for a role swatch in the swatch's mode, or null for any other swatch. */
@Composable
private fun pinItem(
    swatch: RoleSwatch,
    document: ThemeDocument,
    pinsEnabled: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
): BuilderMenuItem? {
    val target = swatch.target as? RampTarget.OfRole ?: return null
    val mode = if (target.isDark) PinMode.Dark else PinMode.Light
    val pin = document.pins[target.role]
    val pinned = (if (target.isDark) pin?.dark else pin?.light) != null
    val change = DocumentChange.SetPin(target.role, mode, if (pinned) null else swatch.argb)
    return BuilderMenuItem(
        label = stringResource(if (pinned) Res.string.tabs_unpin else Res.string.tabs_pin),
        onClick = { dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete)) },
        icon = IconId.Pin,
        enabled = pinsEnabled,
    )
}
