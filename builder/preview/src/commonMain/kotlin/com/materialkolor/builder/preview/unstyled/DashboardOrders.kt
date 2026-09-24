package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Filter
import com.composables.icons.lucide.Lucide
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.focusRing
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedOptionName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choose
import androidx.compose.ui.semantics.Role as SemanticsRole

private val MenuWidth = 200.dp
private val MenuItemHeight = 36.dp
private val MenuGap = 4.dp
private val AmountWidth = 96.dp
private val StatusWidth = 112.dp
private val TableTop = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
private val TableBottom = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)

/**
 * The orders heading and the status button, whose menu opens in place under it and closes on a
 * pick or a second press.
 *
 * The menu is part of this item's layout, so it pushes the table down while open rather than
 * floating over it, and it lies where Inspect and assistive tech look for it. The button scrolls
 * the page so the open menu shows whole, an open from the keyboard puts focus on the first pick,
 * and a pick hands focus back to the button.
 */
@Composable
internal fun OrdersTitle(
    state: DemoAppState,
    focus: DashboardFocus,
    filter: OrderFilter,
    modifier: Modifier = Modifier,
) {
    val open = state.isOn(DashboardMenuSwitch)
    val toggle = { keyboard: Boolean ->
        state.setOn(DashboardMenuSwitch, !open)
        if (open) {
            focus.handBack(DashboardArea.StatusMenu, focus.statusButton)
        } else {
            focus.revealStatusMenu()
            if (keyboard) focus.moveTo(focus.firstStatus)
        }
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DashboardCopy.Orders,
                modifier = Modifier.weight(1f),
                style = HeadingStyle,
                color = DashboardToken.OnSurface.color,
            )
            DashboardButton(
                label = filter.label,
                style = ButtonStyle.Outlined,
                onClick = toggle,
                modifier = Modifier
                    .focusRequester(focus.statusButton)
                    .foldedExpandedName(filter.label, open)
                    .expandActions(open) { toggle(false) },
                icon = Lucide.Filter,
                role = SemanticsRole.DropdownList,
            )
        }
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.End),
            enter = fadeIn(panelMotion()) + expandVertically(panelMotion()),
            exit = fadeOut(panelMotion()) + shrinkVertically(panelMotion()),
        ) {
            StatusMenu(filter, focus, Modifier.padding(top = MenuGap)) { picked ->
                state.choose(DashboardFilterChoice, OrderFilter.entries.size, picked.ordinal)
                state.setOn(DashboardMenuSwitch, false)
                focus.handBack(DashboardArea.StatusMenu, focus.statusButton)
            }
        }
    }
}

@Composable
private fun StatusMenu(
    current: OrderFilter,
    focus: DashboardFocus,
    modifier: Modifier,
    onPick: (OrderFilter) -> Unit,
) {
    Column(
        modifier = modifier
            .bringIntoViewRequester(focus.statusMenu)
            .tracksFocus(focus, DashboardArea.StatusMenu)
            .width(MenuWidth)
            .previewRoles(UnstyledComponent.Menu)
            .clip(CardShape)
            .background(DashboardToken.SurfaceContainerHighest.color)
            .border(1.dp, DashboardToken.OutlineVariant.color, CardShape)
            .padding(MenuGap)
            .selectableGroup(),
    ) {
        for (option in OrderFilter.entries) {
            val selected = option == current
            val content = if (selected) DashboardToken.OnSecondaryContainer.color else DashboardToken.OnSurface.color
            val interactions = remember { MutableInteractionSource() }
            UnstyledButton(
                onClick = { onPick(option) },
                modifier = Modifier
                    .then(if (option.ordinal == 0) Modifier.focusRequester(focus.firstStatus) else Modifier)
                    .fillMaxWidth()
                    .height(MenuItemHeight)
                    .previewRoles(if (selected) UnstyledComponent.SelectedMenuItem else UnstyledComponent.MenuItem)
                    .semantics { this.selected = selected }
                    .foldedOptionName(option.label, selected)
                    .focusRing(interactions, 2.dp, DashboardToken.Primary.color, ControlShape)
                    .clip(ControlShape)
                    .background(if (selected) DashboardToken.SecondaryContainer.color else Color.Transparent),
                contentPadding = PaddingValues(horizontal = 12.dp),
                role = SemanticsRole.RadioButton,
                interactionSource = interactions,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(IconSize)) {
                        if (selected) {
                            UnstyledIcon(Lucide.Check, null, modifier = Modifier.fillMaxSize(), tint = content)
                        }
                    }
                    Text(option.label, style = BodyStyle, color = content)
                }
            }
        }
    }
}

/** The column names on the table's highest container. */
@Composable
internal fun TableHeader(modifier: Modifier = Modifier) {
    val muted = DashboardToken.OnSurfaceVariant.color
    Row(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.TableHeader)
            .clip(TableTop)
            .background(DashboardToken.SurfaceContainerHigh.color)
            .padding(horizontal = SectionGap, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(DashboardCopy.ColumnCustomer, Modifier.weight(1f), style = SmallStyle, color = muted)
        Text(
            text = DashboardCopy.ColumnAmount,
            modifier = Modifier.width(AmountWidth),
            style = SmallStyle,
            color = muted,
            textAlign = TextAlign.End,
        )
        Text(
            text = DashboardCopy.ColumnStatus,
            modifier = Modifier.width(StatusWidth).padding(start = SectionGap),
            style = SmallStyle,
            color = muted,
        )
    }
}

/** One order on the lowest container, rounded off at the foot of the table when [last]. */
@Composable
internal fun OrderRow(
    order: Order,
    last: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.TableRow)
            .clip(if (last) TableBottom else RectangleShape)
            .background(DashboardToken.SurfaceContainerLowest.color),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SectionGap, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(order.customer, style = LabelStyle, color = DashboardToken.OnSurface.color, maxLines = 1)
                Text(order.id, style = SmallStyle, color = DashboardToken.OnSurfaceVariant.color)
            }
            Text(
                text = order.amount,
                modifier = Modifier.width(AmountWidth),
                style = BodyStyle,
                color = DashboardToken.OnSurface.color,
                textAlign = TextAlign.End,
            )
            Box(Modifier.width(StatusWidth).padding(start = SectionGap)) { StatusMark(order.status) }
        }
        if (!last) UnstyledHorizontalSeparator(DashboardToken.OutlineVariant.color)
    }
}

/** Where an order stands, filled from its family or outlined when it has none. */
@Composable
private fun StatusMark(status: OrderStatus) {
    val container = status.container
    val paint = if (container == null) {
        Modifier
            .previewRoles(UnstyledComponent.OutlinedMark)
            .border(1.dp, DashboardToken.Outline.color, PillShape)
    } else {
        Modifier
            .previewRoles(container.role, status.content.role)
            .clip(PillShape)
            .background(container.color)
    }
    Text(
        text = status.label,
        modifier = paint.padding(horizontal = 10.dp, vertical = 2.dp),
        style = SmallStyle,
        fontWeight = FontWeight.Medium,
        color = status.content.color,
        maxLines = 1,
    )
}
