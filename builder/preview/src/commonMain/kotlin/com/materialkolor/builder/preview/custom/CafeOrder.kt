package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState

// The order, as a pane beside the menu on a tablet or desktop and in place of the menu on a phone.

/** The order on the surface beside the menu, its lines scrolling over the total and the buttons. */
@Composable
internal fun OrderPane(
    state: DemoAppState,
    colors: CafeColors,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val pane = colors.pair(CustomSlot.Surface, CustomSlot.TextStrong)
    Column(modifier.previewRoles(pane).background(pane.fill.color)) {
        LazyColumn(
            state = state.rememberListState("cafe.order"),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            orderHead(state)
            orderLines(state)
        }
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            OrderTotal(state, colors, Modifier.clip(cafePanelShape()))
            OrderActions(state, colors)
        }
    }
}

/** The order's title and how it is had. */
internal fun LazyListScope.orderHead(state: DemoAppState) {
    item(key = "title") {
        BuilderText(CafeCopy.YourOrder, Modifier.semantics { heading() }, style = BuilderTextStyle.Title)
    }
    item(key = "type") {
        BuilderChoiceChips(
            options = OrderType.entries,
            selected = state.orderType,
            onSelect = { type -> state.pick(type) },
            label = CafeCopy.OrderType,
            modifier = Modifier.previewRoles(CustomComponent.Chip),
        ) { type -> type.label }
    }
}

/** A line for each drink in the order, or a note that it is empty. */
internal fun LazyListScope.orderLines(state: DemoAppState) {
    val lines = state.orderLines()
    if (lines.isEmpty()) {
        item(key = "empty") { BuilderText(CafeCopy.EmptyOrder, emphasis = Emphasis.Secondary) }
    }
    items(lines, key = { (item, _) -> "line.${item.id}" }) { (item, quantity) -> OrderLine(item, quantity, state) }
}

@Composable
private fun OrderLine(
    item: CafeItem,
    quantity: Int,
    state: DemoAppState,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderBadge("$quantity", Modifier.previewRoles(CustomComponent.NeutralBadge), BadgeStatus.Neutral)
            BuilderText(item.name, Modifier.weight(1f), style = BuilderTextStyle.Label, maxLines = 1)
            BuilderText(CafeCopy.price(item.cents * quantity), maxLines = 1)
            BuilderIconButton(
                onClick = { state.removeOne(item) },
                icon = IconId.Close,
                contentDescription = CafeCopy.removeOne(item.name),
                modifier = Modifier.previewRoles(Emphasis.Subtle.component),
            )
        }
        BuilderDivider(Modifier.padding(top = spacing.small).previewRoles(CustomComponent.Divider))
    }
}

/** Clear and Place order, with the confirmation of the last order drawn in place above them. */
@Composable
internal fun OrderActions(
    state: DemoAppState,
    colors: CafeColors,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val (count, _) = state.orderTotal()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        if (state.isOn(PlacedKey)) OrderPlaced(state, colors)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            BuilderButton(
                onClick = { state.clearOrder() },
                label = CafeCopy.Clear,
                modifier = Modifier.previewRoles(Emphasis.Danger.component),
                emphasis = Emphasis.Danger,
                enabled = count > 0,
            )
            BuilderButton(
                onClick = { state.placeOrder() },
                label = CafeCopy.PlaceOrder,
                modifier = Modifier.weight(1f).previewRoles(Emphasis.Primary.component),
                emphasis = Emphasis.Primary,
                icon = IconId.Check,
                enabled = count > 0,
            )
        }
    }
}

/** That the order went through, on the primary container, until someone dismisses it. */
@Composable
private fun OrderPlaced(
    state: DemoAppState,
    colors: CafeColors,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val note = colors.pair(CustomSlot.PrimaryContainer, CustomSlot.OnPrimaryContainer)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(note)
            .background(note.fill.color, cafePanelShape())
            .padding(start = spacing.large, top = spacing.small, bottom = spacing.small),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderIcon(IconId.Check, contentDescription = null, tint = note.ink.color)
        BuilderText(CafeCopy.Placed, Modifier.weight(1f), color = note.ink.color)
        BuilderIconButton(
            onClick = { state.setOn(PlacedKey, false) },
            icon = IconId.Close,
            contentDescription = CafeCopy.Dismiss,
            modifier = Modifier.previewRoles(Emphasis.Subtle.component),
        )
    }
}
