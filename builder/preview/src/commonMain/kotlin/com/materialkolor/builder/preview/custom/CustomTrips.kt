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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.inspect.previewRoles
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.TripTint
import com.materialkolor.builder.preview.trips.Trips
import com.materialkolor.builder.preview.trips.TripsDestination
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.openTrip

// The frame of the Custom Trips app, the layout for each width, the rail and the trip list.
// CustomTripDetail.kt draws the open trip. The measures live in TripsLayout, the shapes are the kit's.

/**
 * The width of the navigation list beside the trip list on a tablet or desktop.
 */
private val RailWidth = 168.dp

/**
 * The Trips travel app drawn with the builder's own kit controls, the Custom sample app of the App tab.
 *
 * Everything the app remembers lives in [state], so the two copies of a split agree. A phone gets
 * the trip list with the open trip under it, in one scrolling column. A tablet or desktop gets a
 * navigation list, the trip list and the open trip side by side, each scrolling on its own.
 *
 * Every color comes from the pane's Custom slots. The trip thumbnails take the document's accents,
 * one per trip, and fall back to the slots past the last accent. Every part names what it paints.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[colors] The pane's slots and the document's accents.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun CustomTripsApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    colors: CustomColors,
    modifier: Modifier = Modifier,
) {
    val app = colors.pair(CustomSlot.Surface, CustomSlot.TextStrong)
    Box(modifier.fillMaxSize().previewRoles(app).background(app.fill.color)) {
        val listWidth = TripsLayout.listWidth(deviceWidth)
        if (listWidth == null) TripsPhone(state, colors) else TripsPanes(state, colors, listWidth)
    }
}

@Composable
private fun TripsPhone(
    state: DemoAppState,
    colors: CustomColors,
) {
    val filter = TripFilter.at(state.tabIndex)
    val open = openTrip(state)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = state.rememberListState("trips.phone"),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TripsLayout.PaneGap,
                top = TripsLayout.PaneGap,
                end = TripsLayout.PaneGap,
                bottom = TripsLayout.FabClearance,
            ),
        ) {
            tripList(state, colors, filter, open)
            item(key = "detail") {
                CustomTripDetail(
                    trip = Trips[open],
                    state = state,
                    colors = colors,
                    modifier = Modifier.padding(top = TripsLayout.SectionGap).tripPane(colors),
                )
            }
        }
        NewTripButton(Modifier.align(Alignment.BottomEnd).padding(TripsLayout.SectionGap))
    }
}

@Composable
private fun TripsPanes(
    state: DemoAppState,
    colors: CustomColors,
    listWidth: Dp,
) {
    val filter = TripFilter.at(state.tabIndex)
    val open = openTrip(state)
    Row(Modifier.fillMaxSize()) {
        TripsRail()
        BuilderDivider(Modifier.previewRoles(CustomComponent.Divider), orientation = Orientation.Vertical)
        LazyColumn(
            state = state.rememberListState("trips.list"),
            modifier = Modifier.width(listWidth).fillMaxHeight(),
            contentPadding = PaddingValues(
                start = TripsLayout.PaneGap,
                top = TripsLayout.SectionGap,
                end = TripsLayout.PaneGap,
                bottom = TripsLayout.SectionGap,
            ),
        ) {
            tripList(state, colors, filter, open)
        }
        LazyColumn(
            state = state.rememberListState("trips.detail"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = TripsLayout.PaneGap, end = TripsLayout.PaneGap, bottom = TripsLayout.PaneGap)
                .tripPane(colors),
        ) {
            item(key = "detail") { CustomTripDetail(Trips[open], state, colors) }
        }
    }
}

/**
 * The sunken panel the open trip sits on.
 */
@Composable
private fun Modifier.tripPane(colors: CustomColors): Modifier {
    val pane = colors.pair(CustomSlot.SurfaceSunken, CustomSlot.TextStrong)
    return clip(customPanelShape()).background(pane.fill.color).previewRoles(pane)
}

/**
 * The New trip button over the kit's navigation list, Trips the current row.
 */
@Composable
private fun TripsRail() {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = Modifier.width(RailWidth).fillMaxHeight().padding(TripsLayout.PaneGap),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
    ) {
        NewTripButton(Modifier.padding(bottom = TripsLayout.PaneGap))
        for (destination in TripsDestination.entries) {
            BuilderListRow(
                headline = destination.label,
                modifier = Modifier.previewRoles(CustomComponent.ListRow),
                leading = { TripGlyph(destination.icon, tokens.textStrong) },
                onClick = {},
                selected = destination == TripsDestination.Trips,
            )
        }
    }
}

@Composable
private fun NewTripButton(modifier: Modifier = Modifier) {
    BuilderButton(
        onClick = {},
        label = "New trip",
        modifier = modifier.previewRoles(Emphasis.Primary.component),
        emphasis = Emphasis.Primary,
        icon = IconId.Plus,
    )
}

/**
 * The header, search, filters, the trips [filter] keeps with the [open] one selected, and offline maps.
 */
private fun LazyListScope.tripList(
    state: DemoAppState,
    colors: CustomColors,
    filter: TripFilter,
    open: Int,
) {
    item(key = "header") { TripsHeader(colors) }
    item(key = "search") { TripsSearch(colors) }
    item(key = "filters") {
        BuilderChoiceChips(
            options = TripFilter.entries,
            selected = filter,
            onSelect = { picked -> state.tabIndex = picked.ordinal },
            label = "Filter trips",
            modifier = Modifier.padding(bottom = TripsLayout.Gap).previewRoles(CustomComponent.Chip),
        ) { option -> option.label }
    }
    val shown = filter.trips()
    if (shown.isEmpty()) {
        item(key = "empty") {
            BuilderText(
                text = "No ${filter.label.lowercase()} trips yet",
                modifier = Modifier.padding(TripsLayout.PaneGap).previewRoles(CustomSlot.TextMuted),
                emphasis = Emphasis.Secondary,
            )
        }
    }
    items(shown, key = { (index, _) -> "trip.$index" }) { (index, trip) ->
        BuilderListRow(
            headline = trip.name,
            modifier = Modifier.padding(vertical = TripsLayout.RowGap).previewRoles(CustomComponent.ListRow),
            supporting = trip.dates,
            leading = { TripThumb(index, trip, colors) },
            onClick = { state.selectedItem = index },
            selected = index == open,
            trailing = { BuilderText(trip.countdown, style = BuilderTextStyle.Label, emphasis = Emphasis.Secondary) },
        )
    }
    item(key = "offline") { OfflineMaps(state) }
}

@Composable
private fun TripsHeader(colors: CustomColors) {
    Row(Modifier.fillMaxWidth().padding(start = TripsLayout.PaneGap), verticalAlignment = Alignment.CenterVertically) {
        BuilderText("Trips", Modifier.weight(1f).semantics { heading() }, style = BuilderTextStyle.Title)
        NotificationsButton(colors)
    }
}

/**
 * The bell with its count of new notifications. The kit has no bell, so the app draws one in the
 * kit's pressable, which gives it the press and the focus ring every kit button shows, and hangs
 * the kit's danger badge on it.
 */
@Composable
private fun NotificationsButton(colors: CustomColors) {
    val ink = colors.slot(CustomSlot.TextStrong)
    val painted = listOf(ink.ref) + CustomComponent.DangerBadge.refs
    BuilderPressable(
        onClick = {},
        label = "Notifications, 2 new",
        modifier = Modifier.previewRoles(*painted.toTypedArray()),
        shape = CircleShape,
    ) {
        TripGlyph(Lucide.Bell, ink.color)
        BuilderBadge("2", Modifier.align(Alignment.TopEnd), BadgeStatus.Danger)
    }
}

/**
 * A search field as it looks at rest, which the demo never opens.
 */
@Composable
private fun TripsSearch(colors: CustomColors) {
    val pill = colors.pair(CustomSlot.SurfaceSunken, CustomSlot.TextMuted)
    Row(
        modifier = Modifier
            .padding(vertical = TripsLayout.Gap)
            .fillMaxWidth()
            .height(TripsLayout.SearchHeight)
            .clip(CircleShape)
            .background(pill.fill.color)
            .previewRoles(pill)
            .padding(horizontal = TripsLayout.SectionGap),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderIcon(IconId.Search, contentDescription = null, tint = pill.ink.color)
        BuilderText("Search trips", color = pill.ink.color, maxLines = 1)
    }
}

/**
 * The trip's glyph on the container of the document's accent for its place in [Trips], or on the
 * slots of its tint past the last accent.
 */
@Composable
private fun TripThumb(
    index: Int,
    trip: Trip,
    colors: CustomColors,
) {
    val thumb = colors.accentContainer(index) ?: when (trip.tint) {
        TripTint.Primary -> colors.pair(CustomSlot.PrimaryContainer, CustomSlot.OnPrimaryContainer)
        TripTint.Tertiary -> colors.pair(CustomSlot.TertiaryContainer, CustomSlot.OnTertiaryContainer)
        TripTint.Neutral -> colors.pair(CustomSlot.SurfaceSunken, CustomSlot.TextMuted)
    }
    Box(
        modifier = Modifier
            .size(TripsLayout.ThumbSize)
            .clip(RoundedCornerShape(LocalBuilderTokens.current.radius.small))
            .background(thumb.fill.color)
            .previewRoles(thumb),
        contentAlignment = Alignment.Center,
    ) {
        TripGlyph(trip.icon, thumb.ink.color)
    }
}

/**
 * The offline maps switch on a kit card, a setting for every trip.
 */
@Composable
private fun OfflineMaps(state: DemoAppState) {
    BuilderCard(Modifier.padding(top = TripsLayout.Gap).fillMaxWidth().previewRoles(CustomComponent.SampleCard)) {
        BuilderSwitch(
            checked = state.isOn(OfflineMapsSwitch),
            onCheckedChange = { on -> state.setOn(OfflineMapsSwitch, on) },
            label = "Offline maps",
            modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.Switch),
        )
        BuilderText("Maps work without signal", emphasis = Emphasis.Secondary)
    }
}

/**
 * A Lucide glyph in [tint], standing beside a label that already says what it means.
 */
@Composable
internal fun TripGlyph(
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

/**
 * The rounded shape the app's own panels are cut to.
 */
@Composable
internal fun customPanelShape(): RoundedCornerShape = RoundedCornerShape(LocalBuilderTokens.current.radius.medium)
