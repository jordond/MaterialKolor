package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composeunstyled.FocusVisibilityProvider
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledVerticalSeparator
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.TripTint
import com.materialkolor.builder.preview.trips.Trips
import com.materialkolor.builder.preview.trips.TripsDestination
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.openTrip
import androidx.compose.ui.semantics.Role as SemanticsRole

// The measures live in TripsLayout, shared by every library's Trips. The sizes of the parts
// Compose Unstyled leaves to the app are the gallery's own.
private val NewTripSize = 56.dp
private val RailItemWidth = 64.dp
private val RailItemHeight = 56.dp
private val ChipHeight = 32.dp
private val BadgeSize = 16.dp
private val PaneShape = CardShape

/**
 * The Trips travel app, the Unstyled sample app of the App tab.
 *
 * The same app as the Material 3 one, drawn from Compose Unstyled primitives the way the Unstyled
 * gallery styles them, each colored from the MaterialKolor tokens of the pane's theme. Everything
 * the app remembers lives in [state], so the two copies of a split agree. A phone gets the trip
 * list with the open trip under it, in one scrolling column. A tablet or desktop gets a
 * navigation list down the side, the list and the open trip side by side, each scrolling on its
 * own.
 *
 * Nothing opens a popup or a window. The icon button's tooltip shows in place, the app swallows
 * right-button presses and hands the note field a toolbar that never shows, like the gallery.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun UnstyledTripsApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    // Tells keyboard focus from a press, so a click leaves no tooltip or focus ring behind.
    FocusVisibilityProvider(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            Box(
                Modifier
                    .fillMaxSize()
                    .previewRoles(UnstyledComponent.App)
                    .background(UnstyledToken.Surface.color)
                    .gallerySwallowRightPresses(),
            ) {
                val listWidth = TripsLayout.listWidth(deviceWidth)
                if (listWidth == null) TripsPhone(state) else TripsPanes(state, listWidth)
            }
        }
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
                UnstyledTripDetail(Trips[open], state, Modifier.padding(top = TripsLayout.SectionGap).tripPane())
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
                start = TripsLayout.PaneGap,
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
            item(key = "detail") { UnstyledTripDetail(Trips[open], state) }
        }
    }
}

/**
 * The rounded low container the open trip sits on.
 */
@Composable
private fun Modifier.tripPane(): Modifier =
    previewRoles(UnstyledComponent.TripPane).clip(PaneShape).background(UnstyledToken.SurfaceContainerLow.color)

/**
 * The side navigation, the new trip button over the four destinations, Trips the current one.
 */
@Composable
private fun TripsRail() {
    Row(Modifier.fillMaxHeight().previewRoles(UnstyledComponent.Rail)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = TripsLayout.PaneGap),
            verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NewTripButton(Modifier.padding(vertical = TripsLayout.PaneGap))
            for (destination in TripsDestination.entries) {
                RailItem(destination, selected = destination == TripsDestination.Trips)
            }
        }
        UnstyledVerticalSeparator(UnstyledToken.OutlineVariant.color)
    }
}

/**
 * A destination of the side navigation, its icon over its label, which reads whether it is the
 * current one, on the web too. Only Trips has a screen, so a press goes nowhere.
 */
@Composable
private fun RailItem(
    destination: TripsDestination,
    selected: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (selected) UnstyledComponent.SelectedNavItem else UnstyledComponent.NavItem
    val content = if (selected) UnstyledToken.OnSecondaryContainer.color else UnstyledToken.OnSurfaceVariant.color
    UnstyledButton(
        onClick = {},
        modifier = Modifier
            .size(RailItemWidth, RailItemHeight)
            .previewRoles(component)
            .semantics { this.selected = selected }
            .foldedSelectedName(destination.label, selected)
            .galleryFocusRing(interactions)
            .clip(ControlShape)
            .background(if (selected) UnstyledToken.SecondaryContainer.color else Color.Transparent),
        interactionSource = interactions,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            UnstyledIcon(destination.icon, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
            Text(destination.label, style = SmallStyle, color = content, maxLines = 1)
        }
    }
}

/**
 * The floating new trip button, a filled icon button in the pill shape.
 */
@Composable
private fun NewTripButton(modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    UnstyledButton(
        onClick = {},
        modifier = modifier
            .size(NewTripSize)
            .previewRoles(UnstyledComponent.NewTripButton)
            .semantics { contentDescription = "New trip" }
            .galleryFocusRing(interactions, offset = true)
            .clip(PillShape)
            .background(UnstyledToken.Primary.color),
        interactionSource = interactions,
    ) {
        UnstyledIcon(Lucide.Plus, contentDescription = null, tint = UnstyledToken.OnPrimary.color)
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
                modifier = Modifier.padding(TripsLayout.PaneGap),
                style = BodyStyle,
                color = UnstyledToken.OnSurfaceVariant.color,
            )
        }
    }
    items(shown, key = { (index, _) -> "trip.$index" }) { (index, trip) ->
        TripRow(trip, selected = index == open) { state.selectedItem = index }
    }
    item(key = "offline") { OfflineMaps(state) }
}

/**
 * The title and the notifications button, whose tooltip floats over the search under it.
 */
@Composable
private fun TripsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().zIndex(1f).padding(start = TripsLayout.TextInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Trips", Modifier.weight(1f), style = TitleStyle, color = UnstyledToken.OnSurface.color)
        Box {
            GalleryIconButton(Lucide.Bell, "Notifications, 2 new", enabled = true)
            Text(
                text = "2",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(BadgeSize)
                    .previewRoles(UnstyledComponent.NotificationBadge)
                    .clip(CircleShape)
                    .background(UnstyledToken.Error.color),
                style = SmallStyle.copy(textAlign = TextAlign.Center),
                color = UnstyledToken.OnError.color,
            )
        }
    }
}

/**
 * A search pill that shows where searching would go. It takes no text.
 */
@Composable
private fun TripsSearch() {
    val muted = UnstyledToken.OnSurfaceVariant.color
    Row(
        modifier = Modifier
            .padding(vertical = TripsLayout.Gap)
            .fillMaxWidth()
            .height(TripsLayout.SearchHeight)
            .previewRoles(UnstyledComponent.Search)
            .clip(PillShape)
            .background(UnstyledToken.SurfaceContainerHigh.color)
            .padding(horizontal = TripsLayout.SectionGap),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnstyledIcon(Lucide.Search, contentDescription = null, modifier = Modifier.size(IconSize), tint = muted)
        Text("Search trips", style = BodyStyle, color = muted)
    }
}

/**
 * The filters as a row of pill chips, one of them picked, each read as a choice of the set.
 */
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
            FilterChip(filter.label, selected = filter == current) { onPick(filter) }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (selected) UnstyledComponent.SelectedFilterChip else UnstyledComponent.FilterChip
    val content = if (selected) UnstyledToken.OnSecondaryContainer.color else UnstyledToken.OnSurfaceVariant.color
    val edge = if (selected) Modifier else Modifier.border(1.dp, UnstyledToken.Outline.color, PillShape)
    Row(
        modifier = Modifier
            .height(ChipHeight)
            .previewRoles(component)
            .selectable(
                selected = selected,
                interactionSource = interactions,
                indication = null,
                role = SemanticsRole.RadioButton,
                onClick = onClick,
            ).foldedChoiceName(label, selected)
            .galleryFocusRing(interactions, offset = true)
            .clip(PillShape)
            .background(if (selected) UnstyledToken.SecondaryContainer.color else Color.Transparent)
            .then(edge)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) UnstyledIcon(Lucide.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        Text(label, style = LabelStyle, color = content)
    }
}

/**
 * A trip, the open one on the secondary container. The whole row is one control, read with its
 * dates and countdown and whether it is the open one, on the web too.
 */
@Composable
private fun TripRow(
    trip: Trip,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (selected) UnstyledComponent.SelectedTripRow else UnstyledComponent.TripRow
    val strong = if (selected) UnstyledToken.OnSecondaryContainer.color else UnstyledToken.OnSurface.color
    val muted = if (selected) UnstyledToken.OnSecondaryContainer.color else UnstyledToken.OnSurfaceVariant.color
    Row(
        modifier = Modifier
            .padding(vertical = TripsLayout.RowGap)
            .fillMaxWidth()
            .previewRoles(component)
            .selectable(selected = selected, interactionSource = interactions, indication = null, onClick = onClick)
            .foldedSelectedName("${trip.name}, ${trip.dates}, ${trip.countdown}", selected)
            .galleryFocusRing(interactions)
            .clip(CardShape)
            .background(if (selected) UnstyledToken.SecondaryContainer.color else Color.Transparent)
            .padding(horizontal = TripsLayout.PaneGap, vertical = TripsLayout.Gap),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TripThumb(trip)
        Column(Modifier.weight(1f)) {
            Text(trip.name, style = LabelStyle, color = strong, maxLines = 1)
            Text(trip.dates, style = SmallStyle, color = muted, maxLines = 1)
        }
        Text(trip.countdown, style = SmallStyle, color = muted, maxLines = 1)
    }
}

@Composable
private fun TripThumb(trip: Trip) {
    val (container, content) = when (trip.tint) {
        TripTint.Primary -> UnstyledToken.PrimaryContainer to UnstyledToken.OnPrimaryContainer
        TripTint.Tertiary -> UnstyledToken.TertiaryContainer to UnstyledToken.OnTertiaryContainer
        TripTint.Neutral -> UnstyledToken.SurfaceContainerHighest to UnstyledToken.OnSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(TripsLayout.ThumbSize)
            .previewRoles(trip.tint.container, trip.tint.content)
            .clip(ControlShape)
            .background(container.color),
        contentAlignment = Alignment.Center,
    ) {
        UnstyledIcon(trip.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = content.color)
    }
}
