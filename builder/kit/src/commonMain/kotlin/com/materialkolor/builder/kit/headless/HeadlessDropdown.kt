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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
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
 * It opens in a focusable popup, so Esc and a click outside both call [onDismissRequest], and focus
 * lands on the first row. The arrows and Tab walk the rows.
 *
 * @param[expanded] Whether the list is open.
 * @param[onDismissRequest] Called when the list asks to close.
 * @param[style] The skin's overlay style.
 * @param[minWidth] The narrowest the list may be, the anchor's width for a select.
 * @param[content] The rows.
 */
@Composable
internal fun HeadlessDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    style: OverlayStyle,
    minWidth: Dp = MenuMinWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberOverlayVisibility(expanded)
    if (!state.isOverlayShown(expanded)) return
    val tokens = LocalBuilderTokens.current
    val gap = with(LocalDensity.current) { tokens.spacing.extraSmall.roundToPx() }
    val provider = remember(gap) { DropdownPositionProvider(gap) }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(visibleState = state, enter = popoverEnter(), exit = popoverExit()) {
            val firstRow = remember { FocusRequester() }
            Column(
                modifier = Modifier
                    .padding(tokens.spacing.small)
                    .shadow(style.shadow, style.popoverShape)
                    .clip(style.popoverShape)
                    .background(style.surface)
                    .then(if (style.border != null) Modifier.border(style.border, style.popoverShape) else Modifier)
                    .widthIn(min = maxOf(minWidth, MenuMinWidth), max = MenuMaxWidth)
                    .width(IntrinsicSize.Max)
                    .heightIn(max = MenuMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .focusRequester(firstRow)
                    .padding(tokens.spacing.extraSmall),
                content = content,
            )
            LaunchedEffect(Unit) { firstRow.requestFocus() }
        }
    }
}

/**
 * One row of a dropdown.
 *
 * A row that knows whether it is [selected] is an option and carries a check next to its label, so
 * the selection never rests on colour alone (AR-03). A row without is a plain command.
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
) {
    val tokens = LocalBuilderTokens.current
    val interaction = remember { MutableInteractionSource() }
    val ink = if (emphasis == Emphasis.Danger) tokens.danger else style.content
    val action = if (selected == null) {
        Modifier.clickable(interaction, null, enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier.selectable(selected, interaction, null, enabled, Role.RadioButton, onClick)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LocalLayout.current.minTouchTarget)
            .overlayFeedback(interaction, style, enabled = enabled, selected = selected == true)
            .then(action)
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
    Box(modifier) {
        anchor()
        HeadlessDropdown(expanded, onDismissRequest, style) {
            for (item in items) {
                HeadlessDropdownItem(
                    label = item.label,
                    onClick = {
                        onDismissRequest()
                        item.onClick()
                    },
                    style = style,
                    icon = item.icon,
                    emphasis = item.emphasis,
                    enabled = item.enabled,
                )
            }
        }
    }
}

/**
 * A field showing the chosen option, opening the list of the rest.
 *
 * The field reads as a dropdown list with the chosen option as its state, and each option reads as
 * a radio button that knows whether it is selected.
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
    val tokens = LocalBuilderTokens.current
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val current = optionLabel(selected)
    Box(modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = LocalLayout.current.minTouchTarget)
                .onSizeChanged { size -> fieldWidth = size.width }
                .background(style.field, style.itemShape)
                .border(style.fieldBorder, style.itemShape)
                .overlayFeedback(interaction, style, enabled = enabled)
                .clickable(interaction, null, enabled, role = Role.DropdownList) { expanded = !expanded }
                .semantics { stateDescription = current }
                .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                BuilderText(label, style = BuilderTextStyle.Value, color = style.muted, maxLines = 1)
                BuilderText(current, style = BuilderTextStyle.Label, color = style.content, maxLines = 1)
            }
            BuilderIcon(IconId.ChevronDown, contentDescription = null, tint = style.content)
        }
        HeadlessDropdown(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            style = style,
            minWidth = with(density) { fieldWidth.toDp() },
        ) {
            for (option in options) {
                HeadlessDropdownItem(
                    label = optionLabel(option),
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    style = style,
                    selected = option == selected,
                )
            }
        }
    }
}

private val MenuMinWidth = 160.dp
private val MenuMaxWidth = 360.dp
private val MenuMaxHeight = 400.dp

/** Under the anchor and lined up with its start, or above it when the window runs out below. */
private class DropdownPositionProvider(
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
