package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.foldedTabName
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
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Layer
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Badge
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.Icon
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.MenuItem
import io.github.composefluent.component.NavigationDisplayMode
import io.github.composefluent.component.NavigationMenuItemScope
import io.github.composefluent.component.NavigationView
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import io.github.composefluent.component.rememberNavigationState
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add
import io.github.composefluent.icons.regular.Alert
import io.github.composefluent.icons.regular.Search

/**
 * The slot compose-fluent gives the button at the top of its navigation, which the new trip button
 * takes on a tablet's rail.
 */
private val RailButtonSize = DpSize(48.dp, 40.dp)

/**
 * The Trips travel app drawn with compose-fluent, the Fluent sample app of the App tab.
 *
 * Everything the app remembers lives in [state] under the same keys as the Material 3 app, so the
 * two copies of a split agree and switching libraries keeps what the user did. A phone gets the trip
 * list with the open trip under it, in one scrolling column, and a floating new trip button. A
 * tablet gets Fluent's navigation view as a rail of icons and a desktop gets it open, with the list
 * and the open trip side by side in its content layer, each scrolling on its own.
 *
 * Fluent takes only its accent from the theme, so the controls keep their own colors and paint the
 * accent fill where Fluent does, and the scene and the thumbnails are cut from the accent shades.
 * Every control and colored surface declares what it paints the way `FluentRoles.kt` says.
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
    Box(modifier.fillMaxSize().fluentNeutralRoles().background(FluentTheme.colors.background.solid.base)) {
        val listWidth = TripsLayout.listWidth(deviceWidth)
        if (listWidth == null) TripsPhone(state) else TripsPanes(state, deviceWidth, listWidth)
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
                TripPane(Modifier.padding(top = TripsLayout.SectionGap).fillMaxWidth()) {
                    TripDetail(Trips[open], state)
                }
            }
        }
        NewTripButton(iconOnly = false, modifier = Modifier.align(Alignment.BottomEnd).padding(TripsLayout.SectionGap))
    }
}

/**
 * The tablet and desktop layout, Fluent's navigation view with the list and the open trip in its
 * content layer. The view keeps the pages' own clickables inside and lays a clickable layer round
 * the rail, so the view stays out of focus and only the app's own parts, each in a focus group,
 * take it.
 */
@Composable
private fun TripsPanes(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    listWidth: Dp,
) {
    val expanded = deviceWidth == DeviceWidth.Desktop
    val navigation = rememberNavigationState(initialExpanded = remember(deviceWidth) { expanded })
    LaunchedEffect(navigation, expanded) { navigation.expanded = expanded }
    NavigationView(
        menuItems = {
            for (destination in TripsDestination.entries) {
                item(key = destination) { DestinationItem(destination) }
            }
        },
        modifier = Modifier.fillMaxSize().focusProperties { canFocus = false },
        displayMode = if (expanded) NavigationDisplayMode.Left else NavigationDisplayMode.LeftCompact,
        state = navigation,
        expandedButton = {
            val slot = if (expanded) Modifier.padding(horizontal = TripsLayout.RailGap) else Modifier.size(RailButtonSize)
            Box(slot.focusGroup(), contentAlignment = Alignment.Center) { NewTripButton(iconOnly = !expanded) }
        },
    ) {
        TripsSideBySide(state, listWidth)
    }
}

@Composable
private fun TripsSideBySide(
    state: DemoAppState,
    listWidth: Dp,
) {
    val filter = TripFilter.at(state.tabIndex)
    val open = openTrip(state)
    Row(Modifier.fillMaxSize().focusGroup()) {
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
        TripPane(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = TripsLayout.PaneGap, end = TripsLayout.PaneGap, bottom = TripsLayout.PaneGap),
        ) {
            LazyColumn(state = state.rememberListState("trips.detail"), modifier = Modifier.fillMaxSize()) {
                item(key = "detail") { TripDetail(Trips[open], state) }
            }
        }
    }
}

/**
 * The card layer the open trip sits on, in Fluent's neutral card fill.
 */
@Composable
private fun TripPane(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Layer(
        modifier = modifier.fluentNeutralRoles(),
        shape = FluentTheme.shapes.overlay,
        color = FluentTheme.colors.background.card.default,
        border = BorderStroke(1.dp, FluentTheme.colors.stroke.card.default),
        content = content,
    )
}

/**
 * One destination of the navigation, the library's own item under a layer that names it and picks
 * it, in a focus group so the navigation view's fence leaves it a Tab stop. Only Trips has a screen.
 */
@Composable
private fun NavigationMenuItemScope.DestinationItem(destination: TripsDestination) {
    val selected = destination == TripsDestination.Trips
    val interaction = remember { MutableInteractionSource() }
    FluentOverlaid(
        modifier = Modifier.focusGroup(),
        control = Modifier
            .fluentFillRoles(selected)
            .selectable(selected = selected, interactionSource = interaction, indication = null, onClick = {})
            .foldedSelectedName(destination.label, selected),
    ) {
        MenuItem(
            selected = selected,
            onClick = {},
            text = { Text(destination.label) },
            icon = { Icon(destination.icon, contentDescription = null) },
            interactionSource = interaction,
        )
    }
}

/**
 * The accent button that starts a new trip, with its words beside the plus unless [iconOnly].
 */
@Composable
private fun NewTripButton(
    iconOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    AccentButton(onClick = {}, modifier = modifier.fluentAccentRoles(), iconOnly = iconOnly) {
        Icon(Icons.Regular.Add, contentDescription = if (iconOnly) "New trip" else null)
        if (!iconOnly) Text("New trip")
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
                color = FluentTheme.colors.text.text.secondary,
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
    Row(Modifier.fillMaxWidth().padding(start = TripsLayout.TextInset), verticalAlignment = Alignment.CenterVertically) {
        Text("Trips", Modifier.weight(1f), style = FluentTheme.typography.title)
        Box {
            SubtleButton(onClick = {}, modifier = Modifier.fluentNeutralRoles(), iconOnly = true) {
                Icon(Icons.Regular.Alert, contentDescription = "Notifications, 2 new")
            }
            Badge(
                backgroundColor = FluentTheme.colors.fillAccent.default,
                modifier = Modifier.align(Alignment.TopEnd).fluentAccentRoles(),
            ) { Text("2") }
        }
    }
}

/**
 * The search box as Fluent draws an empty one, words and a magnifier, which only looks the part.
 */
@Composable
private fun TripsSearch() {
    val colors = FluentTheme.colors
    Layer(
        modifier = Modifier
            .padding(vertical = TripsLayout.Gap)
            .fillMaxWidth()
            .height(TripsLayout.SearchHeight)
            .fluentNeutralRoles(),
        shape = FluentTheme.shapes.control,
        color = colors.control.default,
        border = BorderStroke(1.dp, colors.stroke.control.default),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = TripsLayout.PaneGap),
            horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Search trips", Modifier.weight(1f), color = colors.text.text.secondary)
            Icon(Icons.Regular.Search, contentDescription = null, tint = colors.text.text.secondary)
        }
    }
}

/**
 * Fluent's selector bar over the list. An item reports its selection but no role, so its modifier
 * adds the tab role and the name carries both onto the web.
 */
@Composable
private fun TripFilters(
    current: TripFilter,
    onPick: (TripFilter) -> Unit,
) {
    SelectorBar(Modifier.padding(bottom = TripsLayout.Gap).selectableGroup()) {
        for (filter in TripFilter.entries) {
            val selected = filter == current
            SelectorBarItem(
                selected = selected,
                onSelectedChange = { onPick(filter) },
                text = { Text(filter.label) },
                modifier = Modifier
                    .fluentFillRoles(selected)
                    .semantics { role = Role.Tab }
                    .foldedTabName(filter.label, selected),
            )
        }
    }
}

/**
 * A trip as Fluent's list item, the open one with the accent pill, under a layer that picks it and
 * names it for the web. The layer declares the thumbnail's colors too, since it covers it.
 */
@Composable
private fun TripRow(
    trip: Trip,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shades = TripsShades(FluentTheme.colors.darkMode)
    val refs = (if (selected) FluentAccentRefs else emptyList()) + thumbRefs(trip.tint, shades)
    FluentOverlaid(
        control = Modifier
            .previewRoles(*refs.distinct().toTypedArray())
            .selectable(selected = selected, interactionSource = interaction, indication = null, onClick = onClick)
            .foldedSelectedName(trip.name, selected),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            selected = selected,
            onSelectedChanged = {},
            text = {
                Row(
                    modifier = Modifier.padding(vertical = TripsLayout.Gap),
                    horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TripThumb(trip, shades)
                    Column {
                        Text(trip.name, style = FluentTheme.typography.bodyStrong)
                        Text(trip.dates, style = FluentTheme.typography.caption, color = FluentTheme.colors.text.text.secondary)
                    }
                }
            },
            trailing = { Text(trip.countdown) },
            interaction = interaction,
        )
    }
}

/**
 * The colors a thumbnail of [tint] paints, the accent fill, a shade with accent text on it, or a
 * neutral fill with none.
 */
private fun thumbRefs(
    tint: TripTint,
    shades: TripsShades,
): List<ColorRef> =
    when (tint) {
        TripTint.Primary -> FluentAccentRefs
        TripTint.Tertiary -> listOf(ColorRef.OfFluentShade(shades.accentText), ColorRef.OfFluentShade(shades.tint))
        TripTint.Neutral -> emptyList()
    }

@Composable
private fun TripThumb(
    trip: Trip,
    shades: TripsShades,
) {
    val colors = FluentTheme.colors
    val (fill: Color, ink: Color) = when (trip.tint) {
        TripTint.Primary -> colors.fillAccent.default to colors.text.onAccent.primary
        TripTint.Tertiary -> colors.shades.color(shades.tint) to colors.text.accent.primary
        TripTint.Neutral -> colors.controlAlt.quaternary to colors.text.text.secondary
    }
    Box(
        modifier = Modifier
            .size(TripsLayout.ThumbSize)
            .clip(FluentTheme.shapes.overlay)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(trip.icon, contentDescription = null, tint = ink)
    }
}

/**
 * The offline maps setting as a Fluent settings card, a setting for every trip.
 */
@Composable
private fun OfflineMaps(state: DemoAppState) {
    CardExpanderItem(
        heading = { Text("Offline maps") },
        modifier = Modifier.padding(top = TripsLayout.Gap).fillMaxWidth().fluentNeutralRoles(),
        icon = null,
        caption = { Text("Maps work without signal") },
        trailing = { OfflineMapsSwitcher(state) },
    )
}

/**
 * Fluent's switch, under a layer that toggles it with the switch role, names it for the web and
 * declares the accent fill while it is on. `Switcher` takes no modifier and reports itself as a
 * plain button.
 */
@Composable
private fun OfflineMapsSwitcher(state: DemoAppState) {
    val on = state.isOn(OfflineMapsSwitch)
    val interaction = remember { MutableInteractionSource() }
    FluentOverlaid(
        control = Modifier
            .fluentFillRoles(on)
            .toggleable(
                value = on,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = { checked -> state.setOn(OfflineMapsSwitch, checked) },
            ).foldedSwitchName("Offline maps", on),
    ) {
        Switcher(
            checked = on,
            onCheckStateChange = {},
            text = if (on) SwitcherCopy.On else SwitcherCopy.Off,
            textBefore = true,
            interactionSource = interaction,
        )
    }
}
