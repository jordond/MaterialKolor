package com.materialkolor.builder.preview.inklet

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.materialkolor.builder.preview.material.MaterialComponent
import com.materialkolor.builder.preview.material.previewRoles
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.TripTint
import com.materialkolor.builder.preview.trips.Trips
import com.materialkolor.builder.preview.trips.TripsDestination
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.openTrip
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.material3.InkletCard
import dev.ggoggam.inklet.material3.InkletFilterChip
import dev.ggoggam.inklet.material3.InkletIconButton
import dev.ggoggam.inklet.material3.InkletToggle
import dev.ggoggam.inklet.material3.InkletVariant
import dev.ggoggam.inklet.material3.inkletDecoration
import dev.ggoggam.inklet.material3.inkletSurface

// The measures live in TripsLayout, shared by every library's Trips. The corners are Inklet's own,
// the 12 dp its containers default to, rounder for the search field and the open trip's pane.
internal val InkletRowShape = RoundedCornerShape(12.dp)
private val PaneCorner = 24.dp
private val SearchCorner = 24.dp
private val ThumbCorner = 12.dp

/**
 * The Trips travel app drawn in Inklet, the sample app of the App tab.
 *
 * It lays out the way Material's Trips does. A phone gets the trip list with the open trip under it
 * in one scrolling column, and a tablet or desktop gets a navigation rail, the list and the open trip
 * side by side. Buttons, chips, cards, the text field, the checkboxes, the toggle, the progress bar
 * and the divider are Inklet's. The rail and the trip rows stay Material 3, which Inklet has no
 * counterpart for, and the search field, thumbnails and panes are plain boxes under Inklet's pen.
 * Everything the app remembers lives in [state], and every sketch has a fixed seed, so the two
 * copies of a split agree.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun InkletTrips(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize().previewRoles(Role.Surface, Role.OnSurface)) {
        val listWidth = TripsLayout.listWidth(deviceWidth)
        if (listWidth == null) TripsPhone(state) else TripsPanes(state, listWidth)
    }
}

@Composable
private fun TripsPhone(state: DemoAppState) {
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
            tripList(state, filter, open)
            item(key = "detail") {
                InkletTripDetail(Trips[open], state, Modifier.padding(top = TripsLayout.SectionGap).tripPane())
            }
        }
        NewTripButton(Modifier.align(Alignment.BottomEnd).padding(TripsLayout.SectionGap))
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
            contentPadding = PaddingValues(
                start = TripsLayout.RailGap,
                top = TripsLayout.SectionGap,
                end = TripsLayout.PaneGap,
                bottom = TripsLayout.SectionGap,
            ),
        ) {
            tripList(state, filter, open)
        }
        LazyColumn(
            state = state.rememberListState("trips.detail"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = TripsLayout.PaneGap, end = TripsLayout.PaneGap, bottom = TripsLayout.PaneGap)
                .tripPane(),
        ) {
            item(key = "detail") { InkletTripDetail(Trips[open], state) }
        }
    }
}

/**
 * The low container the open trip sits on, sketched in the outline variant.
 */
@Composable
private fun Modifier.tripPane(): Modifier {
    val colors = MaterialTheme.colorScheme
    return inkletSurface(
        containerColor = colors.surfaceContainerLow,
        ink = colors.outlineVariant,
        cornerRadius = PaneCorner,
        seed = PaneSeed,
    ).previewRoles(Role.SurfaceContainerLow, Role.OnSurface, Role.OutlineVariant)
}

@Composable
private fun TripsRail() {
    NavigationRail(
        modifier = Modifier.fillMaxHeight().previewRoles(MaterialComponent.NavigationRail),
        header = { NewTripButton(Modifier.padding(vertical = TripsLayout.PaneGap)) },
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

/**
 * The new trip button, a solid Inklet icon button where Material's Trips has a floating one.
 */
@Composable
private fun NewTripButton(modifier: Modifier = Modifier) {
    InkletIconButton(
        onClick = {},
        modifier = modifier.size(NewTripSize).previewRoles(InkletComponent.SolidIconButton),
        variant = InkletVariant.Solid,
        seed = NewTripSeed,
    ) { Icon(Lucide.Plus, contentDescription = "New trip") }
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
                modifier = Modifier.padding(TripsLayout.PaneGap),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(shown, key = { (index, _) -> "trip.$index" }) { (index, trip) ->
        TripRow(trip, index, selected = index == open) { state.selectedItem = index }
    }
    item(key = "offline") { OfflineMaps(state) }
}

@Composable
private fun TripsHeader() {
    Row(Modifier.fillMaxWidth().padding(start = TripsLayout.PaneGap), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Text(
                text = "Trips",
                modifier = Modifier
                    .inkletDecoration(InkletDecoration.Underline, seed = HeaderSeed)
                    .previewRoles(Role.Primary, Role.OnSurface),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        InkletIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(InkletComponent.OutlineIconButton),
            seed = HeaderSeed + 1,
        ) {
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
            .padding(vertical = TripsLayout.Gap)
            .fillMaxWidth()
            .height(TripsLayout.SearchHeight)
            .inkletSurface(
                containerColor = colors.surfaceContainerHigh,
                ink = colors.outline,
                cornerRadius = SearchCorner,
                seed = SearchSeed,
            ).previewRoles(Role.SurfaceContainerHigh, Role.OnSurfaceVariant, Role.Outline)
            .padding(horizontal = TripsLayout.SectionGap),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
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
        modifier = Modifier.padding(bottom = TripsLayout.Gap).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        for (filter in TripFilter.entries) {
            val selected = filter == current
            InkletFilterChip(
                selected = selected,
                onClick = { onPick(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.previewRoles(InkletComponent.SelectableChip),
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
                seed = FilterSeed + filter.ordinal,
            )
        }
    }
}

/**
 * A Material list item, whose selected copy sits on a sketched secondary container.
 */
@Composable
private fun TripRow(
    trip: Trip,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val muted = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    val look = if (selected) {
        Modifier
            .inkletSurface(
                containerColor = colors.secondaryContainer,
                ink = colors.secondary,
                seed = RowSeed + index,
            ).previewRoles(Role.SecondaryContainer, Role.OnSecondaryContainer, Role.Secondary)
    } else {
        Modifier.previewRoles(MaterialComponent.ListItem)
    }
    ListItem(
        headlineContent = { Text(trip.name, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier
            .padding(vertical = TripsLayout.RowGap)
            .then(look)
            .clip(InkletRowShape)
            .selectable(selected = selected, onClick = onClick),
        supportingContent = { Text(trip.dates) },
        leadingContent = { TripThumb(trip, index) },
        trailingContent = { Text(trip.countdown, style = MaterialTheme.typography.labelMedium) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
            supportingContentColor = muted,
            trailingContentColor = muted,
        ),
    )
}

@Composable
private fun TripThumb(
    trip: Trip,
    index: Int,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (trip.tint) {
        TripTint.Primary -> colors.primaryContainer to colors.onPrimaryContainer
        TripTint.Tertiary -> colors.tertiaryContainer to colors.onTertiaryContainer
        TripTint.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(TripsLayout.ThumbSize)
            .inkletSurface(
                containerColor = container,
                ink = content,
                cornerRadius = ThumbCorner,
                seed =
                    ThumbSeed + index,
            ).previewRoles(trip.tint.container, trip.tint.content),
        contentAlignment = Alignment.Center,
    ) {
        Icon(trip.icon, contentDescription = null, tint = content)
    }
}

/**
 * The offline maps toggle on a card at the lowest container level, a setting for every trip.
 */
@Composable
private fun OfflineMaps(state: DemoAppState) {
    val on = state.isOn(OfflineMapsSwitch)
    InkletCard(
        modifier = Modifier
            .padding(top = TripsLayout.Gap)
            .fillMaxWidth()
            .previewRoles(Role.SurfaceContainerLowest, Role.OnSurface, Role.OutlineVariant),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        seed = OfflineSeed,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TripsLayout.SectionGap, vertical = TripsLayout.Gap),
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
            InkletToggle(
                checked = on,
                onCheckedChange = { checked -> state.setOn(OfflineMapsSwitch, checked) },
                modifier = Modifier
                    .semantics { contentDescription = "Offline maps" }
                    .previewRoles(InkletComponent.Control),
                seed = OfflineSeed + 1,
            )
        }
    }
}

/**
 * The new trip button's side, a floating action button's.
 */
private val NewTripSize = 56.dp

// The seed of each sketch, one range per part so no two parts draw alike.
private const val PaneSeed = 500
private const val NewTripSeed = 510
private const val HeaderSeed = 520
private const val SearchSeed = 530
private const val FilterSeed = 540
private const val RowSeed = 550
private const val ThumbSeed = 560
private const val OfflineSeed = 570
