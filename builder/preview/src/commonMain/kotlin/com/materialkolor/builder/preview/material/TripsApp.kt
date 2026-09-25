package com.materialkolor.builder.preview.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import androidx.compose.ui.semantics.Role as SemanticsRole

// Material 3 has no spacing tokens, so the app keeps its measures here, after the direction D board.
// The first four are shared with the open trip in TripDetail.kt.
internal val Gap = 8.dp
internal val PaneGap = 12.dp
internal val SectionGap = 16.dp
internal val RowShape = RoundedCornerShape(18.dp)
private val FabClearance = 88.dp
private val ThumbSize = 52.dp
private val SearchHeight = 48.dp
private val PaneShape = RoundedCornerShape(24.dp)
private val ThumbShape = RoundedCornerShape(14.dp)
private val TabletListWidth = 340.dp
private val DesktopListWidth = 360.dp

/**
 * The space between the rail and the trip list.
 */
private val RailGap = 4.dp

/**
 * Half the space between two trip rows.
 */
private val RowGap = 2.dp

/**
 * The Trips travel app, the Material 3 sample app of the App tab (F-20).
 *
 * Everything the app remembers lives in [state], so the two copies of a split agree. A phone gets
 * the trip list with the open trip under it, in one scrolling column. A tablet or desktop gets a
 * navigation rail, the list and the open trip side by side, each scrolling on its own. The offline
 * maps card closes the list, so the switch and the lowest container show on the first screen at
 * every width.
 *
 * Most components keep their default colors so the scheme shows through. The selected trip row,
 * the cards on the other container levels and the Turn on button in the error snackbar pick theirs
 * from the scheme instead. Every one declares the roles it reads.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun TripsApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize().previewRoles(Role.Surface, Role.OnSurface)) {
        when (deviceWidth) {
            DeviceWidth.Phone -> TripsPhone(state)
            DeviceWidth.Tablet -> TripsPanes(state, TabletListWidth)
            DeviceWidth.Desktop -> TripsPanes(state, DesktopListWidth)
        }
    }
}

@Composable
private fun TripsPhone(state: DemoAppState) {
    val filter = TripFilter.at(state.tabIndex)
    val open = openTrip(state)
    val pane = MaterialTheme.colorScheme.surfaceContainerLow
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = state.rememberListState("trips.phone"),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PaneGap, top = PaneGap, end = PaneGap, bottom = FabClearance),
        ) {
            tripList(state, filter, open)
            item(key = "detail") {
                TripDetail(Trips[open], state, Modifier.padding(top = SectionGap).tripPane(pane))
            }
        }
        NewTripButton(Modifier.align(Alignment.BottomEnd).padding(SectionGap))
    }
}

@Composable
private fun TripsPanes(
    state: DemoAppState,
    listWidth: Dp,
) {
    val filter = TripFilter.at(state.tabIndex)
    val open = openTrip(state)
    Row(Modifier.fillMaxSize()) {
        TripsRail()
        LazyColumn(
            state = state.rememberListState("trips.list"),
            modifier = Modifier.width(listWidth).fillMaxHeight(),
            contentPadding = PaddingValues(start = RailGap, top = SectionGap, end = PaneGap, bottom = SectionGap),
        ) {
            tripList(state, filter, open)
        }
        LazyColumn(
            state = state.rememberListState("trips.detail"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = PaneGap, end = PaneGap, bottom = PaneGap)
                .tripPane(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            item(key = "detail") { TripDetail(Trips[open], state) }
        }
    }
}

/**
 * Where the open trip sits in [Trips], clamped so a stale index still opens one.
 */
private fun openTrip(state: DemoAppState): Int = state.selectedItem.coerceIn(Trips.indices)

/**
 * The rounded low container the open trip sits on.
 */
private fun Modifier.tripPane(color: Color): Modifier =
    clip(PaneShape).background(color).previewRoles(Role.SurfaceContainerLow, Role.OnSurface)

@Composable
private fun TripsRail() {
    NavigationRail(
        modifier = Modifier.fillMaxHeight().previewRoles(MaterialComponent.NavigationRail),
        header = { NewTripButton(Modifier.padding(vertical = PaneGap)) },
        windowInsets = WindowInsets(0),
    ) {
        for (destination in TripsDestination.entries) {
            NavigationRailItem(
                selected = destination == TripsDestination.Trips,
                onClick = {},
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                modifier = Modifier.previewRoles(MaterialComponent.NavigationRailItem),
            )
        }
    }
}

@Composable
private fun NewTripButton(modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = {}, modifier = modifier.previewRoles(MaterialComponent.Fab)) {
        Icon(Lucide.Plus, contentDescription = "New trip")
    }
}

/**
 * The header, search, filters, the trips [filter] keeps with the [open] one selected, and offline maps.
 */
private fun LazyListScope.tripList(
    state: DemoAppState,
    filter: TripFilter,
    open: Int,
) {
    item(key = "header") { TripsHeader() }
    item(key = "search") { TripsSearch() }
    item(key = "filters") {
        TripFilters(filter) { picked -> state.tabIndex = picked.ordinal }
    }
    val shown = filter.trips()
    if (shown.isEmpty()) {
        item(key = "empty") {
            Text(
                text = "No ${filter.label.lowercase()} trips yet",
                modifier = Modifier.padding(PaneGap),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(shown, key = { (index, _) -> "trip.$index" }) { (index, trip) ->
        TripRow(trip, selected = index == open) { state.selectedItem = index }
    }
    item(key = "offline") { OfflineMaps(state) }
}

@Composable
private fun TripsHeader() {
    Row(Modifier.fillMaxWidth().padding(start = PaneGap), verticalAlignment = Alignment.CenterVertically) {
        Text("Trips", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.IconButton)) {
            BadgedBox(badge = { Badge(Modifier.previewRoles(MaterialComponent.Badge)) { Text("2") } }) {
                Icon(Lucide.Bell, contentDescription = "Notifications, 2 new")
            }
        }
    }
}

@Composable
private fun TripsSearch() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .padding(vertical = Gap)
            .fillMaxWidth()
            .height(SearchHeight)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .previewRoles(Role.SurfaceContainerHigh, Role.OnSurfaceVariant)
            .padding(horizontal = SectionGap),
        horizontalArrangement = Arrangement.spacedBy(PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Search, contentDescription = null, tint = colors.onSurfaceVariant)
        Text("Search trips", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TripFilters(
    current: TripFilter,
    onPick: (TripFilter) -> Unit,
) {
    Row(
        modifier = Modifier.padding(bottom = Gap).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Gap),
    ) {
        for (filter in TripFilter.entries) {
            val selected = filter == current
            FilterChip(
                selected = selected,
                onClick = { onPick(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.previewRoles(MaterialComponent.FilterChip),
                leadingIcon = if (selected) {
                    {
                        Icon(
                            Lucide.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun TripRow(
    trip: Trip,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val muted = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    val roles = if (selected) {
        Modifier.previewRoles(Role.SecondaryContainer, Role.OnSecondaryContainer)
    } else {
        Modifier.previewRoles(MaterialComponent.ListItem)
    }
    ListItem(
        headlineContent = { Text(trip.name, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier
            .padding(vertical = RowGap)
            .clip(RowShape)
            .selectable(selected = selected, onClick = onClick)
            .then(roles),
        supportingContent = { Text(trip.dates) },
        leadingContent = { TripThumb(trip) },
        trailingContent = { Text(trip.countdown, style = MaterialTheme.typography.labelMedium) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) colors.secondaryContainer else colors.surface,
            contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
            supportingContentColor = muted,
            trailingContentColor = muted,
        ),
    )
}

@Composable
private fun TripThumb(trip: Trip) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (trip.tint) {
        TripTint.Primary -> colors.primaryContainer to colors.onPrimaryContainer
        TripTint.Tertiary -> colors.tertiaryContainer to colors.onTertiaryContainer
        TripTint.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(ThumbSize)
            .clip(ThumbShape)
            .background(container)
            .previewRoles(trip.tint.container, trip.tint.content),
        contentAlignment = Alignment.Center,
    ) {
        Icon(trip.icon, contentDescription = null, tint = content)
    }
}

/**
 * The offline maps switch on a card at the lowest container level, a setting for every trip.
 */
@Composable
private fun OfflineMaps(state: DemoAppState) {
    val on = state.isOn(OfflineMapsSwitch)
    Card(
        modifier = Modifier
            .padding(top = Gap)
            .fillMaxWidth()
            .previewRoles(Role.SurfaceContainerLowest, Role.OnSurface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = on, role = SemanticsRole.Switch) { checked ->
                    state.setOn(OfflineMapsSwitch, checked)
                }.previewRoles(MaterialComponent.Switch)
                .padding(horizontal = SectionGap, vertical = PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Offline maps", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Maps work without signal",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = on, onCheckedChange = null)
        }
    }
}
