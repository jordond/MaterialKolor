package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderToast
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldMenuRow
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.headless.HeadlessModal
import com.materialkolor.builder.kit.headless.HeadlessTooltip
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.headless.ReturnFocusWhenGone
import com.materialkolor.builder.kit.headless.keepTaps
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.Hairline
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The Material3 dress for the headless overlays.
 *
 * It dresses the overlays Material has no component for, the side panel, the end sheet, the bottom
 * sheet with three detents and the scroll area. Where overlays render in the page (D40) it also
 * dresses the headless tooltip that stands in for `PlainTooltip`, and through [materialMenuStyle]
 * the menu and the select's list that stand in for `DropdownMenu`. The dialog there draws Material's
 * own dialog container over the headless modal, whose veil reads `tokens.scrim` directly rather
 * than anything from here.
 *
 * @param[surface] The container colour.
 * @param[popoverShape] The shape of a popover, a tooltip and a toast.
 * @param[shadow] The elevation shadow under a container.
 */
@Composable
internal fun materialOverlayStyle(
    surface: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    popoverShape: Shape = MaterialTheme.shapes.extraSmall,
    shadow: Dp = 1.dp,
): OverlayStyle {
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    return OverlayStyle(
        surface = surface,
        content = colors.onSurface,
        muted = colors.onSurfaceVariant,
        border = null,
        popoverShape = popoverShape,
        dialogShape = shapes.extraLarge,
        panelRadius = LocalBuilderTokens.current.radius.medium,
        shadow = shadow,
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

/** The dress of Material's menu container, for the menu and the select's list drawn in the page. */
@Composable
internal fun materialMenuStyle(): OverlayStyle =
    materialOverlayStyle(
        surface = tonal(MenuDefaults.containerColor, MenuDefaults.TonalElevation),
        popoverShape = MenuDefaults.shape,
        shadow = MenuDefaults.ShadowElevation,
    )

/** [color] lifted by [elevation] the way a Material `Surface` tints the plain surface colour. */
@Composable
private fun tonal(
    color: Color,
    elevation: Dp,
): Color {
    val colors = MaterialTheme.colorScheme
    return if (color == colors.surface) colors.surfaceColorAtElevation(elevation) else color
}

/**
 * Material's `AlertDialog`. Its window keeps focus inside, focus starts on the first action unless
 * something inside has already taken it, a field that asks for it as it opens, and Esc closes it
 * even where the platform does not turn Esc into back.
 *
 * `AlertDialog` always opens a window of its own, so where overlays render in the page (D40) it
 * gives way to [MaterialPageDialog].
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
    if (LocalOverlaysInTree.current) {
        MaterialPageDialog(visible, onDismissRequest, title, returnFocusTo, modifier, actions, content)
        return
    }
    ReturnFocusWhenGone(visible, returnFocusTo)
    if (!visible) return
    val firstAction = remember { FocusRequester() }
    val focusInside = remember { mutableStateOf(false) }
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
            .onFocusChanged { state -> focusInside.value = state.hasFocus }
            .onKeyEvent { event ->
                val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                if (escape) onDismissRequest()
                escape
            },
        title = { Text(title) },
        text = { Column(content = content) },
        properties = DialogProperties(animateTransition = !LocalReducedMotion.current),
    )
    LaunchedEffect(Unit) { if (!focusInside.value) firstAction.requestFocus() }
}

/**
 * Material's dialog container over the headless modal, for where overlays render in the page (D40).
 *
 * It takes the container colour, shape and tonal elevation of `AlertDialogDefaults` and Material's
 * headline for the title, and moves with the kit's motion. The headless modal holds focus inside,
 * starting on the first action, or on the panel when there is none.
 */
@Composable
private fun MaterialPageDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    returnFocusTo: FocusRequester?,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val typography = MaterialTheme.typography
    HeadlessModal(visible, onDismissRequest, tokens.scrim, Alignment.Center, returnFocusTo) {
        Surface(
            modifier = modifier
                .animateEnterExit(enter = popoverEnter(), exit = popoverExit())
                .padding(tokens.spacing.large)
                .widthIn(min = OverlayMetrics.dialogMinWidth, max = OverlayMetrics.dialogMaxWidth)
                .semantics { paneTitle = title }
                .keepTaps(),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(tokens.spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.large),
            ) {
                Text(title, color = AlertDialogDefaults.titleContentColor, style = typography.headlineSmall)
                CompositionLocalProvider(
                    LocalContentColor provides AlertDialogDefaults.textContentColor,
                    LocalTextStyle provides typography.bodyMedium,
                ) { Column(content = content) }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

/**
 * Material's `DropdownMenu` under [anchor]. Where overlays render in the page (D40) the same rows
 * sit in the headless dropdown in Material's menu container, since `DropdownMenu` always opens a
 * popup.
 */
@Composable
internal fun MaterialMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<BuilderMenuItem>,
    modifier: Modifier,
    anchor: @Composable () -> Unit,
) {
    if (LocalOverlaysInTree.current) {
        val trigger = remember { FocusRequester() }
        Box(modifier.focusRequester(trigger)) {
            anchor()
            HeadlessDropdown(expanded, onDismissRequest, materialMenuStyle(), returnFocusTo = trigger) { close ->
                MaterialMenuRows(items, close)
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
        val selected = item.selected
        DropdownMenuItem(
            text = { Text(item.label) },
            onClick = {
                onDismissRequest()
                item.onClick()
            },
            modifier = Modifier
                .semantics {
                    role = if (selected == null) Role.Button else Role.RadioButton
                    if (selected != null) this.selected = selected
                }.foldMenuRow(item.label, selected, item.enabled),
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
            trailingIcon = if (selected == true) {
                { BuilderIcon(IconId.Check, contentDescription = null) }
            } else {
                null
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
 * The field already shows the choice as its text, so on the web its name is the label alone, with
 * the disabled note while it is disabled, and the choice is read once (S5 row 10).
 *
 * Where overlays render in the page (D40) the field stays and the options open in the headless
 * dropdown in Material's menu container, as wide as the field, with focus on the chosen option.
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
    val inTree = LocalOverlaysInTree.current
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
                }.foldState(label, null, enabled),
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
                style = materialMenuStyle(),
                minWidth = with(density) { fieldWidth.toDp() },
                initialFocus = if (selected in options) selectedRow else null,
                returnFocusTo = field,
            ) { close ->
                val pick = { option: T ->
                    close()
                    onSelect(option)
                }
                MaterialSelectRows(options, selected, optionLabel, pick, selectedRow)
            }
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
    if (LocalOverlaysInTree.current) {
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
