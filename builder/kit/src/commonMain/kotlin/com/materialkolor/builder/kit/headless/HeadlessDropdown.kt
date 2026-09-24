package com.materialkolor.builder.kit.headless

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.foldMenuRow
import com.materialkolor.builder.kit.control.foldOption
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.isOverlayShown
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.popoverExit
import com.materialkolor.builder.kit.skin.headless.rememberOverlayVisibility
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlin.math.max

/**
 * A popover list anchored under whatever it shares a parent with.
 *
 * It opens in a focusable popup, or as a popover in the overlay host, so Esc and a click outside
 * both call [onDismissRequest], and focus lands on [initialFocus], or else on the first row, or else
 * on the list itself when every row is disabled. The arrows and Tab walk the rows. In the host focus
 * goes back to [returnFocusTo] once the list has gone, since no popup window hands it back, unless a
 * row has already moved it somewhere else.
 *
 * @param[expanded] Whether the list is open.
 * @param[onDismissRequest] Called when the list asks to close. It must set [expanded] to false,
 * since the list stays open until it does.
 * @param[style] The skin's overlay style.
 * @param[minWidth] The narrowest the list may be, the anchor's width for a select.
 * @param[initialFocus] The row that takes focus when the list opens, the selected option for a select.
 * @param[returnFocusTo] The trigger or the field that opened the list.
 * @param[content] The rows. A row calls `close` rather than [onDismissRequest], so the list lets go of
 * the page at once and a row that moves focus there keeps it.
 */
@Composable
internal fun HeadlessDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    style: OverlayStyle,
    minWidth: Dp = OverlayMetrics.menuMinWidth,
    initialFocus: FocusRequester? = null,
    returnFocusTo: FocusRequester? = null,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val inTree = LocalOverlaysInTree.current
    val anchor = if (inTree) rememberOverlayAnchor() else null
    val state = rememberOverlayVisibility(expanded)
    val shown = state.isOverlayShown(expanded)
    if (inTree) ReturnFocusWhenGone(shown, returnFocusTo, currentOverlayHost())
    if (!shown) return
    val host = inTreeOverlayHost()
    val tokens = LocalBuilderTokens.current
    val gap = with(LocalDensity.current) { tokens.spacing.extraSmall.roundToPx() }
    val provider = remember(gap) { DropdownPositionProvider(gap) }
    val layer = remember { OverlayLayer(OverlayKind.Popover) }
    val close = {
        layer.open = false
        onDismissRequest()
    }
    val list: @Composable () -> Unit = {
        AnimatedVisibility(visibleState = state, enter = popoverEnter(), exit = popoverExit()) {
            val focus = remember { OverlayFocus() }
            DropdownList(style, minWidth, listModifier = focus.modifier) { this.content(close) }
            LaunchedEffect(Unit) { focus.enter(initialFocus) }
        }
    }
    if (host != null && anchor != null) {
        val placement = remember(anchor, provider) { OverlayPlacement(anchor, provider) }
        OverlayPortal(host, layer, expanded, placement, close, list)
    } else {
        Popup(
            popupPositionProvider = provider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            content = list,
        )
    }
}

/**
 * The container a dropdown's rows sit in, the open list's and a panel's drawn in place alike. It
 * stands on the style's surface with its shadow and edge, keeps between the narrowest and widest a
 * list gets, and scrolls past the tallest.
 *
 * @param[style] The skin's overlay style.
 * @param[minWidth] The narrowest the list may be, never under the menu's own narrowest.
 * @param[modifier] Applied to the container, outside the room it keeps for its shadow.
 * @param[listModifier] Applied to the scrolling list inside its edge, where the open list keeps
 * focus in.
 * @param[content] The rows.
 */
@Composable
internal fun DropdownList(
    style: OverlayStyle,
    minWidth: Dp = OverlayMetrics.menuMinWidth,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = modifier
            .padding(tokens.spacing.small)
            .shadow(style.shadow, style.popoverShape)
            .clip(style.popoverShape)
            .background(style.surface)
            .then(if (style.border != null) Modifier.border(style.border, style.popoverShape) else Modifier)
            .widthIn(min = maxOf(minWidth, OverlayMetrics.menuMinWidth), max = OverlayMetrics.menuMaxWidth)
            .width(IntrinsicSize.Max)
            .heightIn(max = OverlayMetrics.menuMaxHeight)
            .verticalScroll(rememberScrollState())
            .then(listModifier)
            .padding(tokens.spacing.extraSmall),
        content = content,
    )
}

/**
 * One row of a dropdown.
 *
 * A row that knows whether it is [selected] carries a check next to its label while it is the
 * current one, so the selection never rests on colour alone (AR-03). A row without is a plain
 * command. On the web a row folds its role word into its name, "menu item" or, for [asOption],
 * "option", then its state and the disabled note (D37, D40).
 */
@Composable
internal fun HeadlessDropdownItem(
    label: String,
    onClick: () -> Unit,
    style: OverlayStyle,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    emphasis: Emphasis = Emphasis.Primary,
    enabled: Boolean = true,
    selected: Boolean? = null,
    asOption: Boolean = false,
) {
    val tokens = LocalBuilderTokens.current
    val interaction = remember { MutableInteractionSource() }
    val ink = if (emphasis == Emphasis.Danger) tokens.danger else style.content
    val action = if (selected == null) {
        Modifier.clickable(interaction, null, enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected, interaction, null, enabled, Role.RadioButton, onClick)
    }
    val named = if (asOption) {
        Modifier.foldOption(label, selected == true, enabled)
    } else {
        Modifier.foldMenuRow(label, selected, enabled)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LocalLayout.current.minTouchTarget)
            .overlayFeedback(interaction, style, enabled = enabled, selected = selected == true)
            .then(action)
            .then(named)
            .padding(horizontal = tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        if (icon != null) BuilderIcon(icon, contentDescription = null, tint = ink)
        BuilderText(
            text = label,
            modifier = Modifier.weight(1f),
            style = BuilderTextStyle.Label,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected == true) BuilderIcon(IconId.Check, contentDescription = null, tint = ink)
    }
}

/** A menu of commands under [anchor], closing itself once a row is chosen. */
@Composable
internal fun HeadlessMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<BuilderMenuItem>,
    style: OverlayStyle,
    modifier: Modifier,
    anchor: @Composable () -> Unit,
) {
    val trigger = remember { FocusRequester() }
    Box(modifier.focusRequester(trigger)) {
        anchor()
        HeadlessDropdown(expanded, onDismissRequest, style, returnFocusTo = trigger) { close ->
            HeadlessMenuRows(items, style, close)
        }
    }
}

/** A menu drawn open where it stands, the rows of [HeadlessMenu] in its list with nothing floating. */
@Composable
internal fun HeadlessMenuPanel(
    items: List<BuilderMenuItem>,
    style: OverlayStyle,
    modifier: Modifier,
) {
    DropdownList(style, modifier = modifier) { HeadlessMenuRows(items, style, close = {}) }
}

/** One row per item, each calling [close] before it runs its item. */
@Composable
private fun HeadlessMenuRows(
    items: List<BuilderMenuItem>,
    style: OverlayStyle,
    close: () -> Unit,
) {
    for (item in items) {
        HeadlessDropdownItem(
            label = item.label,
            onClick = {
                close()
                item.onClick()
            },
            style = style,
            icon = item.icon,
            emphasis = item.emphasis,
            enabled = item.enabled,
            selected = item.selected,
        )
    }
}

/**
 * A field showing the chosen option, opening the list of the rest.
 *
 * The field reads as a dropdown list with the chosen option as its state, and each option reads as
 * a radio button that knows whether it is selected. Down and Alt+Down open the list as well as a
 * click, Enter and Space, and the list opens with focus on the chosen option. The field fills the
 * width and height the select is given, so a click anywhere inside those bounds opens it.
 */
@Composable
internal fun <T> HeadlessSelect(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    enabled: Boolean,
    style: OverlayStyle,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val selectedRow = remember { FocusRequester() }
    val field = remember { FocusRequester() }
    Box(modifier, propagateMinConstraints = true) {
        SelectField(
            label = label,
            current = optionLabel(selected),
            enabled = enabled,
            style = style,
            modifier = Modifier
                .onSizeChanged { size -> fieldWidth = size.width }
                .focusRequester(field),
            action = Modifier
                .overlayFeedback(interaction, style, enabled = enabled)
                .onKeyEvent { event ->
                    val open = enabled && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                    if (open) expanded = true
                    open
                }.clickable(interaction, null, enabled, role = Role.DropdownList) { expanded = !expanded },
        )
        HeadlessDropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            style = style,
            minWidth = with(density) { fieldWidth.toDp() },
            initialFocus = if (selected in options) selectedRow else null,
            returnFocusTo = field,
        ) { close ->
            val pick = { option: T ->
                close()
                onSelect(option)
            }
            HeadlessSelectRows(options, selected, optionLabel, pick, style, selectedRow)
        }
    }
}

/**
 * A select drawn open where it stands, its field over the list of [HeadlessSelect] with nothing
 * floating. The field only shows the choice, so Tab goes straight to the options.
 */
@Composable
internal fun <T> HeadlessSelectPanel(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    style: OverlayStyle,
    modifier: Modifier,
) {
    Column(modifier.width(IntrinsicSize.Max)) {
        SelectField(
            label = label,
            current = optionLabel(selected),
            enabled = true,
            style = style,
            modifier = Modifier.fillMaxWidth(),
            action = Modifier.semantics(mergeDescendants = true) { role = Role.DropdownList },
        )
        DropdownList(style, modifier = Modifier.fillMaxWidth()) {
            HeadlessSelectRows(options, selected, optionLabel, onSelect, style, selectedRow = null)
        }
    }
}

/**
 * A select's field, its label over the choice and a chevron after them, at the far end when the
 * field is wider than they are. It reads as a dropdown list whose state is the choice, and [action]
 * makes it one. On the web its name carries the role word, "Style, pop-up button, Tonal spot" (D40).
 */
@Composable
private fun SelectField(
    label: String,
    current: String,
    enabled: Boolean,
    style: OverlayStyle,
    modifier: Modifier,
    action: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .heightIn(min = LocalLayout.current.minTouchTarget)
            .background(style.field, style.itemShape)
            .border(style.fieldBorder, style.itemShape)
            .then(action)
            .semantics { stateDescription = current }
            .foldState(label, ControlState.Value(current), enabled, role = FoldedRole.PopUpButton)
            .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            BuilderText(label, style = BuilderTextStyle.Value, color = style.muted, maxLines = 1)
            BuilderText(current, style = BuilderTextStyle.Label, color = style.content, maxLines = 1)
        }
        BuilderIcon(
            id = IconId.ChevronDown,
            contentDescription = null,
            modifier = Modifier.padding(start = tokens.spacing.small),
            tint = style.content,
        )
    }
}

/** One option row per option, the chosen one checked and, given [selectedRow], focused by it. */
@Composable
private fun <T> HeadlessSelectRows(
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onChoose: (T) -> Unit,
    style: OverlayStyle,
    selectedRow: FocusRequester?,
) {
    for (option in options) {
        val isSelected = option == selected
        HeadlessDropdownItem(
            label = optionLabel(option),
            onClick = { onChoose(option) },
            style = style,
            modifier = if (isSelected && selectedRow != null) Modifier.focusRequester(selectedRow) else Modifier,
            selected = isSelected,
            asOption = true,
        )
    }
}

/** Under the anchor and lined up with its start, or above it when the window runs out below. */
internal class DropdownPositionProvider(
    private val gap: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val start = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = start.coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            max(0, anchorBounds.top - gap - popupContentSize.height)
        }
        return IntOffset(x, y)
    }
}
