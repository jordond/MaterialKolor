package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderToast
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.HeadlessDialog
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.headless.HeadlessTooltip
import com.materialkolor.builder.kit.headless.ReturnFocusWhenGone
import com.materialkolor.builder.kit.headless.inTreeOverlayHost
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.Hairline
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The Material3 dress for the overlays Material has no component for, the side panel, the end
 * sheet, the bottom sheet with three detents and the scroll area.
 */
@Composable
internal fun materialOverlayStyle(): OverlayStyle {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    return OverlayStyle(
        surface = colors.surfaceContainerLow,
        content = colors.onSurface,
        muted = colors.onSurfaceVariant,
        border = null,
        popoverShape = shapes.extraSmall,
        dialogShape = shapes.extraLarge,
        panelRadius = LocalBuilderTokens.current.radius.medium,
        shadow = 1.dp,
        scrim = LocalBuilderTokens.current.scrim,
        itemShape = shapes.extraSmall,
        highlight = colors.onSurface.copy(alpha = MaterialHoverAlpha),
        selected = colors.secondaryContainer,
        focus = colors.secondary,
        field = colors.surfaceContainerLow,
        fieldBorder = BorderStroke(Hairline, colors.outline),
        tooltip = colors.inverseSurface,
        tooltipContent = colors.inverseOnSurface,
        tooltipBorder = null,
        toast = colors.inverseSurface,
        toastContent = colors.inverseOnSurface,
        toastBorder = null,
        thumb = colors.outline,
    )
}

/** Material's own hover state layer. */
private const val MaterialHoverAlpha = 0.08f

/**
 * Material's `AlertDialog`. Its window keeps focus inside, focus starts on the first action, and Esc
 * closes it even where the platform does not turn Esc into back.
 *
 * `AlertDialog` always opens a window of its own, so where overlays render in the page (D40) it
 * gives way to the headless dialog in Material's colours and shapes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MaterialDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (inTreeOverlayHost() != null) {
        val style = materialOverlayStyle()
        HeadlessDialog(visible, onDismissRequest, title, style, returnFocusTo, modifier, actions, content)
        return
    }
    ReturnFocusWhenGone(visible, returnFocusTo)
    if (!visible) return
    val firstAction = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row(
                modifier = Modifier.focusRequester(firstAction),
                horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
                content = actions,
            )
        },
        modifier = modifier
            .semantics { paneTitle = title }
            .onKeyEvent { event ->
                val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                if (escape) onDismissRequest()
                escape
            },
        title = { Text(title) },
        text = { Column(content = content) },
        properties = DialogProperties(animateTransition = !LocalReducedMotion.current),
    )
    LaunchedEffect(Unit) { firstAction.requestFocus() }
}

/**
 * Material's `DropdownMenu` under [anchor]. Where overlays render in the page (D40) the same rows
 * sit in the headless dropdown in Material's colours and shapes, since `DropdownMenu` always opens
 * a popup.
 */
@Composable
internal fun MaterialMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<BuilderMenuItem>,
    modifier: Modifier,
    anchor: @Composable () -> Unit,
) {
    if (inTreeOverlayHost() != null) {
        val trigger = remember { FocusRequester() }
        Box(modifier.focusRequester(trigger)) {
            anchor()
            HeadlessDropdown(expanded, onDismissRequest, materialOverlayStyle(), returnFocusTo = trigger) {
                MaterialMenuRows(items, onDismissRequest)
            }
        }
        return
    }
    Box(modifier) {
        anchor()
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
            MaterialMenuRows(items, onDismissRequest)
        }
    }
}

@Composable
private fun MaterialMenuRows(
    items: List<BuilderMenuItem>,
    onDismissRequest: () -> Unit,
) {
    for (item in items) {
        val icon = item.icon
        val danger = item.emphasis == Emphasis.Danger
        val error = MaterialTheme.colorScheme.error
        DropdownMenuItem(
            text = { Text(item.label) },
            onClick = {
                onDismissRequest()
                item.onClick()
            },
            modifier = Modifier.semantics { role = Role.Button },
            leadingIcon = if (icon == null) {
                null
            } else {
                {
                    BuilderIcon(
                        id = icon,
                        contentDescription = null,
                        emphasis = item.emphasis,
                        tint = if (danger) error else Color.Unspecified,
                    )
                }
            },
            enabled = item.enabled,
            colors = if (danger) {
                MenuDefaults.itemColors(textColor = error, leadingIconColor = error, trailingIconColor = error)
            } else {
                MenuDefaults.itemColors()
            },
        )
    }
}

/**
 * Material's exposed dropdown, a read only outlined field over a `DropdownMenu`.
 *
 * Where overlays render in the page (D40) the field stays and the options open in the headless
 * dropdown in Material's colours and shapes, as wide as the field, with focus on the chosen option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> MaterialSelect(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    enabled: Boolean,
    modifier: Modifier,
) {
    val inTree = inTreeOverlayHost() != null
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val field = remember { FocusRequester() }
    val selectedRow = remember { FocusRequester() }
    val current = optionLabel(selected)
    val choose = { option: T ->
        expanded = false
        onSelect(option)
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open -> expanded = open && enabled },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .then(
                    if (inTree) {
                        Modifier.focusRequester(field).onSizeChanged { size -> fieldWidth = size.width }
                    } else {
                        Modifier
                    },
                ).semantics {
                    role = Role.DropdownList
                    stateDescription = current
                }.foldState(label, ControlState.Value(current), enabled),
            enabled = enabled,
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { BuilderIcon(IconId.ChevronDown, contentDescription = null) },
            singleLine = true,
        )
        if (inTree) {
            HeadlessDropdown(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                style = materialOverlayStyle(),
                minWidth = with(density) { fieldWidth.toDp() },
                initialFocus = if (selected in options) selectedRow else null,
                returnFocusTo = field,
            ) { MaterialSelectRows(options, selected, optionLabel, choose, selectedRow) }
        } else {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MaterialSelectRows(options, selected, optionLabel, choose, null)
            }
        }
    }
}

@Composable
private fun <T> MaterialSelectRows(
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onChoose: (T) -> Unit,
    selectedRow: FocusRequester?,
) {
    for (option in options) {
        val isSelected = option == selected
        DropdownMenuItem(
            text = { Text(optionLabel(option)) },
            onClick = { onChoose(option) },
            modifier = Modifier
                .then(if (isSelected && selectedRow != null) Modifier.focusRequester(selectedRow) else Modifier)
                .semantics {
                    role = Role.RadioButton
                    this.selected = isSelected
                }.foldState(optionLabel(option), ControlState.Selected(isSelected)),
            trailingIcon = if (isSelected) {
                { BuilderIcon(IconId.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }
}

/**
 * Material's plain tooltip. It is persistent, so a tooltip shown by keyboard focus stays until focus
 * leaves rather than timing out under the reader.
 *
 * `TooltipBox` always opens a popup, so where overlays render in the page (D40) the headless
 * tooltip draws the label in Material's inverse colours, out of the semantics tree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialTooltip(
    text: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (inTreeOverlayHost() != null) {
        HeadlessTooltip(text, materialOverlayStyle(), modifier, content)
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(isPersistent = true),
        modifier = modifier,
        content = content,
    )
}

/**
 * One toast as Material's `Snackbar`. The host stacks up to three of these itself, since a
 * `SnackbarHost` shows one at a time.
 */
@Composable
internal fun MaterialToast(
    toast: BuilderToast,
    onAction: () -> Unit,
    modifier: Modifier,
) {
    val label = toast.actionLabel
    Snackbar(
        modifier = modifier,
        action = if (label == null) {
            null
        } else {
            {
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                ) { Text(label) }
            }
        },
    ) { Text(toast.message) }
}
