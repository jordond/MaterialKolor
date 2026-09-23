package com.materialkolor.builder.preview.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.split.PaneSpec
import androidx.compose.ui.semantics.Role as SemanticsRole

// The entry, its cards and their frame, then the samples of the Containment, Navigation and
// Feedback cards. MaterialGallery.kt keeps the samples of the other three groups.

/** The padding inside a dialog, which Material 3 does not publish. */
private val DialogPadding = 24.dp

/** How far along both progress indicators are. */
private const val DemoProgress = 0.6f

private val Places = listOf(
    "Lisbon, Portugal" to "12 to 19 May",
    "Kyoto, Japan" to "3 to 14 Oct",
    "Oslo, Norway" to "Past",
)
private val TabLabels = listOf("Flights", "Hotels", "Cars")

private enum class GalleryDestination(
    val icon: ImageVector,
) {
    Explore(Lucide.Compass),
    Trips(Lucide.Plane),
    Saved(Lucide.Bookmark),
}

/**
 * The Material 3 components gallery (F-21).
 *
 * Every card holds stock Material 3 components on their default colors, under the theme the pane
 * already wears, so the Expressive flavour shows the same set in its own shapes and motion. Each
 * component shows up enabled and disabled, apart from the few Material 3 gives no disabled look.
 * Nothing in the gallery opens a popup or a dialog window, since on the web the first one takes
 * the accessibility mirror over for good (D40). Menus, dialogs and tooltips are drawn in place.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[expressive] Whether to show the Expressive flavour instead.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal fun MaterialGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    expressive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize().previewRoles(Role.Surface, Role.OnSurface)) {
        GalleryGrid(
            cards = MaterialCards,
            listState = state.rememberListState("gallery.material"),
            gap = SectionGap,
            header = { group -> MaterialGroupHeader(group) },
            card = { card, cardModifier -> MaterialCardFrame(card, state, cardModifier) },
        )
    }
}

/** Every card of the Material 3 gallery, in the order they show within each group. */
internal val MaterialCards: List<GalleryCard> = listOf(
    GalleryCard("Filled button", GalleryGroup.Actions) { FilledButtons() },
    GalleryCard("Tonal button", GalleryGroup.Actions) { TonalButtons() },
    GalleryCard("Elevated button", GalleryGroup.Actions) { ElevatedButtons() },
    GalleryCard("Outlined button", GalleryGroup.Actions) { OutlinedButtons() },
    GalleryCard("Text button", GalleryGroup.Actions) { TextButtons() },
    GalleryCard("Icon buttons", GalleryGroup.Actions) { IconButtons() },
    GalleryCard("Floating action button", GalleryGroup.Actions) { FloatingActionButtons() },
    GalleryCard("Extended FAB", GalleryGroup.Actions) { state -> ExtendedFab(state) },
    GalleryCard("Filled text field", GalleryGroup.Inputs) { state -> FilledTextFields(state) },
    GalleryCard("Outlined text field", GalleryGroup.Inputs) { state -> OutlinedTextFields(state) },
    GalleryCard("Slider", GalleryGroup.Inputs) { state -> Sliders(state) },
    GalleryCard("Range slider", GalleryGroup.Inputs) { state -> RangeSliders(state) },
    GalleryCard("Checkbox", GalleryGroup.Selection) { state -> Checkboxes(state) },
    GalleryCard("Radio button", GalleryGroup.Selection) { state -> RadioButtons(state) },
    GalleryCard("Switch", GalleryGroup.Selection) { state -> Switches(state) },
    GalleryCard("Segmented buttons", GalleryGroup.Selection) { state -> SegmentedButtons(state) },
    GalleryCard("Filter chips", GalleryGroup.Selection) { state -> FilterChips(state) },
    GalleryCard("Assist and suggestion chips", GalleryGroup.Selection) { AssistChips() },
    GalleryCard("Input chips", GalleryGroup.Selection) { state -> InputChips(state) },
    GalleryCard("Menu", GalleryGroup.Selection) { state -> InlineMenu(state) },
    GalleryCard("Filled card", GalleryGroup.Containment) { FilledCards() },
    GalleryCard("Elevated card", GalleryGroup.Containment) { ElevatedCards() },
    GalleryCard("Outlined card", GalleryGroup.Containment) { OutlinedCards() },
    GalleryCard("List", GalleryGroup.Containment) { ListItems() },
    GalleryCard("Dialog", GalleryGroup.Containment) { state -> InlineDialog(state) },
    GalleryCard("Top app bar", GalleryGroup.Navigation) { TopAppBars() },
    GalleryCard("Navigation bar", GalleryGroup.Navigation) { state -> NavigationBars(state) },
    GalleryCard("Navigation rail", GalleryGroup.Navigation) { state -> NavigationRails(state) },
    GalleryCard("Tabs", GalleryGroup.Navigation) { state -> Tabs(state) },
    GalleryCard("Badges", GalleryGroup.Feedback) { state -> Badges(state) },
    GalleryCard("Progress indicators", GalleryGroup.Feedback) { ProgressIndicators() },
    GalleryCard("Snackbar", GalleryGroup.Feedback) { state -> Snackbars(state) },
    GalleryCard("Tooltips", GalleryGroup.Feedback) { state -> InlineTooltips(state) },
)

@Composable
private fun MaterialGroupHeader(group: GalleryGroup) {
    Text(
        text = group.name,
        modifier = Modifier.padding(top = Gap).semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}

/** The outlined frame a card's components sit in, titled with the card's name. */
@Composable
private fun MaterialCardFrame(
    card: GalleryCard,
    state: DemoAppState,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.previewRoles(Role.Surface, Role.OnSurface, Role.OutlineVariant),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(Dp.Hairline, colors.outlineVariant),
    ) {
        Column(Modifier.padding(SectionGap), verticalArrangement = Arrangement.spacedBy(PaneGap)) {
            Text(card.title, style = MaterialTheme.typography.titleSmall)
            card.content(state)
        }
    }
}

@Composable
internal fun FilledCards() {
    CardPair { enabled ->
        Card(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .previewRoles(enabled, MaterialComponent.FilledCard, GalleryComponent.DisabledFilledCard),
            enabled = enabled,
        ) { CardText(enabled) }
    }
}

@Composable
internal fun ElevatedCards() {
    CardPair { enabled ->
        ElevatedCard(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .previewRoles(enabled, MaterialComponent.ElevatedCard, GalleryComponent.DisabledElevatedCard),
            enabled = enabled,
        ) { CardText(enabled) }
    }
}

@Composable
internal fun OutlinedCards() {
    CardPair { enabled ->
        OutlinedCard(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .previewRoles(enabled, MaterialComponent.OutlinedCard, GalleryComponent.DisabledOutlinedCard),
            enabled = enabled,
        ) { CardText(enabled) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ListItems() {
    Column {
        Places.forEachIndexed { index, (place, dates) ->
            val enabled = index != Places.lastIndex
            if (index > 0) HorizontalDivider(Modifier.previewRoles(MaterialComponent.HorizontalDivider))
            ListItem(
                onClick = {},
                modifier = Modifier.previewRoles(enabled, MaterialComponent.ListItem),
                enabled = enabled,
                leadingContent = { Icon(Lucide.MapPin, contentDescription = null) },
                supportingContent = { Text(dates) },
            ) { Text(place) }
        }
    }
}

/** An alert dialog as it looks open, laid out in place rather than in a window (D40). */
@Composable
internal fun InlineDialog(state: DemoAppState) {
    val understood = state.isChecked(UnderstoodKey)
    Surface(
        modifier = Modifier.fillMaxWidth().previewRoles(GalleryComponent.Dialog),
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(Modifier.padding(DialogPadding), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
            Icon(
                Lucide.Archive,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                tint = AlertDialogDefaults.iconContentColor,
            )
            Text(
                text = "Delete this trip?",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = AlertDialogDefaults.titleContentColor,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Lisbon and its photos leave every device, and this cannot be undone.",
                color = AlertDialogDefaults.textContentColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            ControlRow(
                label = "I understand",
                modifier = Modifier
                    .toggleable(understood, role = SemanticsRole.Checkbox) { on -> state.setChecked(UnderstoodKey, on) }
                    .previewRoles(MaterialComponent.Checkbox),
            ) { Checkbox(checked = understood, onCheckedChange = null) }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                TextButton(
                    onClick = { state.setChecked(UnderstoodKey, false) },
                    modifier = Modifier.previewRoles(MaterialComponent.TextButton),
                ) { Text("Cancel") }
                TextButton(
                    onClick = { state.setChecked(UnderstoodKey, false) },
                    modifier = Modifier.previewRoles(
                        understood,
                        MaterialComponent.TextButton,
                        GalleryComponent.DisabledVariant,
                    ),
                    enabled = understood,
                ) { Text("Delete") }
            }
        }
    }
}

@Composable
internal fun TopAppBars() {
    TopAppBar(
        title = { Text("Lisbon") },
        modifier = Modifier.previewRoles(GalleryComponent.TopAppBar),
        navigationIcon = {
            IconButton(onClick = {}, modifier = Modifier.previewRoles(Role.OnSurface)) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = {}, modifier = Modifier.previewRoles(GalleryComponent.AppBarAction)) {
                Icon(Lucide.Share2, contentDescription = "Share")
            }
            IconButton(onClick = {}, modifier = Modifier.previewRoles(GalleryComponent.AppBarAction), enabled = false) {
                Icon(Lucide.Trash2, contentDescription = "Delete")
            }
        },
        windowInsets = WindowInsets(0),
    )
}

@Composable
internal fun NavigationBars(state: DemoAppState) {
    val picked = pickedDestination(state)
    NavigationBar(modifier = Modifier.previewRoles(GalleryComponent.NavigationBar), windowInsets = WindowInsets(0)) {
        for (destination in GalleryDestination.entries) {
            val enabled = destination != GalleryDestination.Saved
            NavigationBarItem(
                selected = destination == picked,
                onClick = { state.selectedItem = destination.ordinal },
                icon = { Icon(destination.icon, contentDescription = null) },
                modifier = Modifier.previewRoles(
                    enabled,
                    GalleryComponent.NavigationBarItem,
                    GalleryComponent.DisabledVariant,
                ),
                enabled = enabled,
                label = { Text(destination.name) },
            )
        }
    }
}

@Composable
internal fun NavigationRails(state: DemoAppState) {
    val picked = pickedDestination(state)
    NavigationRail(modifier = Modifier.previewRoles(MaterialComponent.NavigationRail), windowInsets = WindowInsets(0)) {
        for (destination in GalleryDestination.entries) {
            val enabled = destination != GalleryDestination.Saved
            NavigationRailItem(
                selected = destination == picked,
                onClick = { state.selectedItem = destination.ordinal },
                icon = { Icon(destination.icon, contentDescription = null) },
                modifier = Modifier.previewRoles(
                    enabled,
                    MaterialComponent.NavigationRailItem,
                    GalleryComponent.DisabledVariant,
                ),
                enabled = enabled,
                label = { Text(destination.name) },
            )
        }
    }
}

@Composable
internal fun Tabs(state: DemoAppState) {
    // The last tab is the disabled one, so the tab index never lands on it.
    val picked = state.tabIndex.coerceIn(0, TabLabels.size - 2)
    PrimaryTabRow(selectedTabIndex = picked, modifier = Modifier.previewRoles(GalleryComponent.TabRow)) {
        TabLabels.forEachIndexed { index, label ->
            val enabled = index != TabLabels.lastIndex
            Tab(
                selected = index == picked,
                onClick = { state.tabIndex = index },
                modifier = Modifier.previewRoles(enabled, GalleryComponent.Tab, GalleryComponent.DisabledVariant),
                enabled = enabled,
                text = { Text(label) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Badges on icon buttons, cleared and brought back by a click on either enabled button. */
@Composable
internal fun Badges(state: DemoAppState) {
    val read = state.isOn(ReadKey)
    EnabledAndDisabled { enabled ->
        IconButton(
            onClick = { state.setOn(ReadKey, !read) },
            modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            enabled = enabled,
        ) {
            BadgedBox(badge = { if (!read) Badge(Modifier.previewRoles(MaterialComponent.Badge)) { Text("8") } }) {
                Icon(Lucide.Mail, contentDescription = if (read) "Messages" else "Messages, 8 new")
            }
        }
        IconButton(
            onClick = { state.setOn(ReadKey, !read) },
            modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            enabled = enabled,
        ) {
            BadgedBox(badge = { if (!read) Badge(Modifier.previewRoles(MaterialComponent.Badge)) }) {
                Icon(Lucide.Bell, contentDescription = if (read) "Notifications" else "Notifications, new")
            }
        }
    }
}

/** Determinate indicators only, since an endless one would never let the preview settle. */
@Composable
internal fun ProgressIndicators() {
    Row(horizontalArrangement = Arrangement.spacedBy(SectionGap), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier.previewRoles(GalleryComponent.CircularProgressIndicator),
        )
        LinearProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier.weight(1f).previewRoles(MaterialComponent.LinearProgressIndicator),
        )
    }
}

/** A snackbar shown in place, its action flipping the message. Snackbar actions have no disabled look. */
@Composable
internal fun Snackbars(state: DemoAppState) {
    val restored = state.isOn(RestoredKey)
    Snackbar(
        modifier = Modifier.previewRoles(GalleryComponent.Snackbar),
        action = {
            TextButton(
                onClick = { state.setOn(RestoredKey, !restored) },
                modifier = Modifier.previewRoles(GalleryComponent.SnackbarAction),
                colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
            ) { Text(if (restored) "Archive" else "Undo") }
        },
    ) { Text(if (restored) "Trip restored" else "Trip archived") }
}

/**
 * A plain tooltip over the button it labels and a rich tooltip, both drawn in place rather than
 * in a popup (D40). Tooltips have no disabled look.
 */
@Composable
internal fun InlineTooltips(state: DemoAppState) {
    val saved = state.isOn(SavedKey)
    val label = if (saved) "Saved" else "Save trip"
    val rich = TooltipDefaults.richTooltipColors()
    Column(verticalArrangement = Arrangement.spacedBy(PaneGap)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Gap / 2),
        ) {
            Surface(
                modifier = Modifier.previewRoles(GalleryComponent.PlainTooltip),
                shape = TooltipDefaults.plainTooltipContainerShape,
                color = TooltipDefaults.plainTooltipContainerColor,
                contentColor = TooltipDefaults.plainTooltipContentColor,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = Gap, vertical = Gap / 2),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(
                onClick = { state.setOn(SavedKey, !saved) },
                modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            ) { Icon(Lucide.Bookmark, contentDescription = label) }
        }
        Surface(
            modifier = Modifier.previewRoles(GalleryComponent.RichTooltip),
            shape = TooltipDefaults.richTooltipContainerShape,
            color = rich.containerColor,
            contentColor = rich.contentColor,
        ) {
            Column(Modifier.padding(start = SectionGap, top = PaneGap, end = Gap)) {
                Text("Offline maps", color = rich.titleContentColor, style = MaterialTheme.typography.titleSmall)
                Text("Download the map so it works without a signal.", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {},
                    modifier = Modifier.previewRoles(MaterialComponent.TextButton),
                    colors = ButtonDefaults.textButtonColors(contentColor = rich.actionContentColor),
                ) { Text("Learn more") }
            }
        }
    }
}

private const val UnderstoodKey = "gallery.dialog.understood"
private const val ReadKey = "gallery.badges.read"
private const val RestoredKey = "gallery.snackbar.restored"
private const val SavedKey = "gallery.tooltip.saved"

/** The destination the navigation samples show selected, never the disabled last one. */
private fun pickedDestination(state: DemoAppState): GalleryDestination =
    GalleryDestination.entries[state.selectedItem.coerceIn(0, GalleryDestination.entries.size - 2)]

/** A card's enabled copy and its disabled one, sharing the row. */
@Composable
private fun CardPair(content: @Composable RowScope.(enabled: Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        content(true)
        content(false)
    }
}

@Composable
private fun CardText(enabled: Boolean) {
    Column(Modifier.padding(PaneGap), verticalArrangement = Arrangement.spacedBy(Gap / 2)) {
        Icon(Lucide.Plane, contentDescription = null)
        Text("Lisbon", style = MaterialTheme.typography.titleSmall)
        Text(if (enabled) "12 to 19 May" else "Disabled", style = MaterialTheme.typography.bodySmall)
    }
}
