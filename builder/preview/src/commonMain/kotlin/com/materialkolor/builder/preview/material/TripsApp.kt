package com.materialkolor.builder.preview.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import androidx.compose.ui.semantics.Role as SemanticsRole

// Material 3 has no spacing tokens, so the app keeps its measures here, after the direction D board.
private val Gap = 8.dp
private val PaneGap = 12.dp
private val SectionGap = 16.dp
private val FabClearance = 88.dp
private val SceneHeight = 184.dp
private val ThumbSize = 52.dp
private val StopSize = 36.dp
private val PaneShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(18.dp)
private val ThumbShape = RoundedCornerShape(14.dp)
private val TabletListWidth = 340.dp
private val DesktopListWidth = 360.dp

/**
 * The Trips travel app, the Material 3 sample app of the App tab (F-20).
 *
 * Everything the app remembers lives in [state], so the two copies of a split agree. A phone gets
 * the trip list with the open trip under it, in one scrolling column. A tablet or desktop gets a
 * navigation rail, the list and the open trip side by side, each scrolling on its own. Every
 * component keeps its default colors so the scheme shows through, and declares the roles it reads.
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
    val trip = Trips[state.selectedItem.coerceIn(Trips.indices)]
    val pane = MaterialTheme.colorScheme.surfaceContainerLow
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = state.rememberListState("trips.phone"),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PaneGap, top = PaneGap, end = PaneGap, bottom = FabClearance),
        ) {
            tripList(state, filter)
            item(key = "detail") {
                TripDetail(trip, state, Modifier.padding(top = SectionGap).tripPane(pane))
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
    val trip = Trips[state.selectedItem.coerceIn(Trips.indices)]
    Row(Modifier.fillMaxSize()) {
        TripsRail()
        LazyColumn(
            state = state.rememberListState("trips.list"),
            modifier = Modifier.width(listWidth).fillMaxHeight(),
            contentPadding = PaddingValues(start = 4.dp, top = SectionGap, end = PaneGap, bottom = SectionGap),
        ) {
            tripList(state, filter)
        }
        LazyColumn(
            state = state.rememberListState("trips.detail"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = PaneGap, end = PaneGap, bottom = PaneGap)
                .tripPane(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            item(key = "detail") { TripDetail(trip, state) }
        }
    }
}

/** The rounded low container the open trip sits on. */
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

/** The header, search, filters and the trips [filter] keeps. */
private fun LazyListScope.tripList(
    state: DemoAppState,
    filter: TripFilter,
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
        TripRow(trip, selected = index == state.selectedItem) { state.selectedItem = index }
    }
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
            .height(48.dp)
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
            .padding(vertical = 2.dp)
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

/** The open trip, scene first and the notes last. */
@Composable
private fun TripDetail(
    trip: Trip,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.padding(SectionGap), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        TripScene(Modifier.fillMaxWidth().height(SceneHeight))
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(trip.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(trip.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        TripActions()
        DayPlan(trip)
        HorizontalDivider(Modifier.previewRoles(MaterialComponent.HorizontalDivider))
        OfflineMaps(state)
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state)
        PackingCard(state)
        NoteCard(state)
    }
}

/** A sky, a sun and three ridges in the scheme's colors, where a photo would go. */
@Composable
private fun TripScene(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val sky = colors.primaryContainer
    val sun = colors.tertiaryContainer
    val ridges = listOf(colors.secondary, colors.primary, colors.onPrimaryContainer)
    Canvas(
        modifier
            .clip(RowShape)
            .previewRoles(
                Role.PrimaryContainer,
                Role.TertiaryContainer,
                Role.Secondary,
                Role.Primary,
                Role.OnPrimaryContainer,
            ),
    ) {
        val scaleX = size.width / SceneSize.width
        val scaleY = size.height / SceneSize.height
        drawRect(sky)
        drawCircle(sun, radius = SceneSunRadius * scaleY, center = Offset(SceneSun.x * scaleX, SceneSun.y * scaleY))
        SceneRidges.forEachIndexed {
            index,
            ridge,
            ->
            drawPath(ridgePath(ridge, scaleX, scaleY, size.height), ridges[index])
        }
    }
}

/** The ridge [points] scaled to the canvas, closed along its [bottom] edge. */
private fun ridgePath(
    points: FloatArray,
    scaleX: Float,
    scaleY: Float,
    bottom: Float,
): Path {
    val path = Path()
    path.moveTo(0f, bottom)
    for (index in points.indices step 2) path.lineTo(points[index] * scaleX, points[index + 1] * scaleY)
    path.lineTo(points[points.size - 2] * scaleX, bottom)
    path.close()
    return path
}

@Composable
private fun TripActions() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Gap), verticalArrangement = Arrangement.spacedBy(Gap)) {
        Button(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.FilledButton)) { Text("Check in") }
        FilledTonalButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.TonalButton)) {
            Text("Share plan")
        }
        OutlinedButton(
            onClick = {},
            modifier = Modifier.previewRoles(MaterialComponent.OutlinedButton),
        ) { Text("Edit") }
        AssistChip(
            onClick = {},
            label = { Text("Add to calendar") },
            modifier = Modifier.previewRoles(MaterialComponent.AssistChip),
            leadingIcon = {
                Icon(Lucide.Calendar, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
        )
        AssistChip(
            onClick = {},
            label = { Text("Directions") },
            modifier = Modifier.previewRoles(MaterialComponent.AssistChip),
            leadingIcon = {
                Icon(
                    Lucide.MapPin,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    }
}

@Composable
private fun DayPlan(trip: Trip) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(Gap)) {
        if (trip.plan.isEmpty()) {
            Text("Nothing planned yet", color = colors.onSurfaceVariant, style = type.bodyMedium)
        } else {
            Text("Day 1 · Friday", Modifier.previewRoles(Role.Primary), color = colors.primary, style = type.labelLarge)
        }
        trip.plan.forEachIndexed { index, stop ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PaneGap)) {
                Box(
                    modifier = Modifier
                        .size(StopSize)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest)
                        .previewRoles(Role.SurfaceContainerHighest, Role.OnSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = colors.onSurfaceVariant, style = type.labelMedium)
                }
                Text(stop.what, Modifier.weight(1f), style = type.bodyLarge)
                Text(
                    text = stop.time,
                    color = colors.onSurfaceVariant,
                    style = type.labelMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
private fun OfflineMaps(state: DemoAppState) {
    val on = state.isOn(OfflineMapsSwitch)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(value = on, role = SemanticsRole.Switch) { checked -> state.setOn(OfflineMapsSwitch, checked) }
            .previewRoles(MaterialComponent.Switch)
            .padding(horizontal = 4.dp, vertical = Gap),
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

/** A snackbar in the error container, offering to turn offline maps on. */
@Composable
private fun MapsNeedSignal(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.ErrorContainer, Role.OnErrorContainer),
        shape = MaterialTheme.shapes.small,
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = SectionGap, end = Gap),
            horizontalArrangement = Arrangement.spacedBy(PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.CloudOff, contentDescription = null)
            Text(
                "Maps need signal until offline maps is on",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { state.setOn(OfflineMapsSwitch, true) },
                modifier = Modifier.previewRoles(Role.Error),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
            ) {
                Text("Turn on")
            }
        }
    }
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    Card(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.SurfaceContainer, Role.OnSurface),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
    ) {
        Column(Modifier.padding(SectionGap), verticalArrangement = Arrangement.spacedBy(Gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Packing", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "$packed of ${items.size}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = { packed / items.size.toFloat() },
                modifier = Modifier.fillMaxWidth().previewRoles(MaterialComponent.LinearProgressIndicator),
            )
            for (item in items) {
                val checked = state.isChecked(item.key)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(value = checked, role = SemanticsRole.Checkbox) { ticked ->
                            state.setChecked(item.key, ticked)
                        }.previewRoles(MaterialComponent.Checkbox),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(item.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(state: DemoAppState) {
    Card(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.SurfaceContainerLowest, Role.OnSurface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        OutlinedTextField(
            value = state.text,
            onValueChange = { text -> state.text = text },
            modifier = Modifier.padding(SectionGap).fillMaxWidth().previewRoles(MaterialComponent.OutlinedTextField),
            label = { Text("Note for the group") },
            minLines = 2,
        )
    }
}
