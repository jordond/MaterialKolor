package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.Timer
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.inspect.previewRoles

// The cafe's frame, its header and the layout for each device width. CafeMenu.kt draws the
// categories and the drinks, CafeOrder.kt the order.

/** The width of the category column and the order pane beside the menu on a tablet. */
private val TabletSides = 232.dp to 280.dp

/** The width of the category column and the order pane beside the menu on a desktop. */
private val DesktopSides = 264.dp to 340.dp

/** How many drinks a desktop lays side by side. */
private const val DesktopMenuColumns = 2

private val StoreMarkSize = 40.dp

/**
 * The cafe ordering app, the Custom sample app of the App tab (F-20).
 *
 * Everything the app remembers lives in [state], so the two copies of a split agree. A phone gets
 * one column, the stamp card, the categories and the drinks, with the order a tap away on the bar
 * at the bottom. A tablet or desktop gets the categories, the menu and the order side by side,
 * each scrolling on its own. Every part names the slots or accents it paints.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[colors] The pane's slots and the document's accents.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun CafeApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    colors: CafeColors,
    modifier: Modifier = Modifier,
) {
    val canvas = colors.pair(CustomSlot.SurfaceSunken, CustomSlot.TextStrong)
    Column(modifier.fillMaxSize().previewRoles(canvas).background(canvas.fill.color)) {
        CafeHeader(colors)
        when (deviceWidth) {
            DeviceWidth.Phone -> CafePhone(state, colors, Modifier.weight(1f))
            DeviceWidth.Tablet -> CafePanes(state, colors, TabletSides, menuColumns = 1, Modifier.weight(1f))
            DeviceWidth.Desktop -> CafePanes(state, colors, DesktopSides, DesktopMenuColumns, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CafePhone(
    state: DemoAppState,
    colors: CafeColors,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val orderOpen = state.isOn(PhoneOrderKey)
    Column(modifier.fillMaxWidth()) {
        LazyColumn(
            state = state.rememberListState(if (orderOpen) "cafe.phone.order" else "cafe.phone.menu"),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (orderOpen) {
                orderHead(state)
                orderLines(state)
                item(key = "actions") { OrderActions(state, colors) }
            } else {
                item(key = "stamps") { StampCard(colors) }
                item(key = "categories") { CategoryChips(state) }
                item(key = "banner") { CategoryBanner(state.category, colors) }
                items(state.category.items, key = { item -> item.id }) { item -> MenuItemCard(item, state, colors) }
            }
        }
        OrderTotal(state, colors) {
            BuilderButton(
                onClick = { state.setOn(PhoneOrderKey, !orderOpen) },
                label = if (orderOpen) CafeCopy.BackToMenu else CafeCopy.ViewOrder,
                modifier = Modifier.previewRoles(Emphasis.Secondary.component),
                icon = if (orderOpen) IconId.ChevronLeft else IconId.ChevronRight,
            )
        }
    }
}

@Composable
private fun CafePanes(
    state: DemoAppState,
    colors: CafeColors,
    sides: Pair<Dp, Dp>,
    menuColumns: Int,
    modifier: Modifier,
) {
    val (categoryWidth, orderWidth) = sides
    Row(modifier.fillMaxWidth()) {
        CategoryColumn(state, colors, Modifier.width(categoryWidth).fillMaxHeight())
        MenuPane(state, colors, menuColumns, Modifier.weight(1f).fillMaxHeight())
        BuilderDivider(Modifier.previewRoles(CustomComponent.Divider), orientation = Orientation.Vertical)
        OrderPane(state, colors, Modifier.width(orderWidth).fillMaxHeight())
    }
}

/** The store's name and hours on the raised surface, with when an order made now is ready. */
@Composable
private fun CafeHeader(colors: CafeColors) {
    val tokens = LocalBuilderTokens.current
    val bar = colors.pair(CustomSlot.SurfaceRaised, CustomSlot.TextStrong)
    val muted = colors.slot(CustomSlot.TextMuted)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .previewRoles(bar.fill.ref, bar.ink.ref, muted.ref)
                .background(bar.fill.color)
                .padding(horizontal = tokens.spacing.large, vertical = tokens.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreMark(colors)
            Column(Modifier.weight(1f)) {
                BuilderText(
                    text = CafeCopy.StoreName,
                    modifier = Modifier.semantics { heading() },
                    style = BuilderTextStyle.Title,
                    color = bar.ink.color,
                    maxLines = 1,
                )
                BuilderText(CafeCopy.OpenUntil, color = muted.color, maxLines = 1)
            }
            ReadyPill(colors)
        }
        BuilderDivider(Modifier.previewRoles(CustomComponent.Divider))
    }
}

/** The store's cup on a primary disc. */
@Composable
private fun StoreMark(colors: CafeColors) {
    val mark = colors.pair(CustomSlot.Primary, CustomSlot.OnPrimary)
    Box(
        modifier = Modifier
            .previewRoles(mark)
            .size(StoreMarkSize)
            .background(mark.fill.color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CafeGlyph(Lucide.Coffee, mark.ink.color)
    }
}

/** How long an order made now takes, on the tertiary container. */
@Composable
private fun ReadyPill(colors: CafeColors) {
    val tokens = LocalBuilderTokens.current
    val pill = colors.pair(CustomSlot.TertiaryContainer, CustomSlot.OnTertiaryContainer)
    Row(
        modifier = Modifier
            .previewRoles(pill)
            .background(pill.fill.color, CircleShape)
            .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CafeGlyph(Lucide.Timer, pill.ink.color, tokens.spacing.large)
        BuilderText(CafeCopy.ReadyIn, style = BuilderTextStyle.Label, color = pill.ink.color, maxLines = 1)
    }
}

/**
 * The order's count and total on the inverse surface. A phone puts the button that opens the
 * order in [action], a tablet or desktop shows the order beside it and leaves it out.
 */
@Composable
internal fun OrderTotal(
    state: DemoAppState,
    colors: CafeColors,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalBuilderTokens.current
    val bar = colors.pair(CustomSlot.SurfaceInverse, CustomSlot.OnSurfaceInverse)
    val (count, cents) = state.orderTotal()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(bar)
            .background(bar.fill.color)
            .padding(horizontal = tokens.spacing.large, vertical = tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CafeGlyph(Lucide.ShoppingBag, bar.ink.color)
        Column(Modifier.weight(1f)) {
            BuilderText(CafeCopy.items(count), style = BuilderTextStyle.Label, color = bar.ink.color, maxLines = 1)
            BuilderText(CafeCopy.price(cents), color = bar.ink.color, maxLines = 1)
        }
        action?.invoke()
    }
}

/** A Lucide glyph in [tint], standing beside a label that already says what it means. */
@Composable
internal fun CafeGlyph(
    icon: ImageVector,
    tint: Color,
    size: Dp = LocalBuilderTokens.current.iconSize,
) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** The rounded shape the cafe's own panels are cut to. */
@Composable
internal fun cafePanelShape(): RoundedCornerShape = RoundedCornerShape(LocalBuilderTokens.current.radius.medium)
