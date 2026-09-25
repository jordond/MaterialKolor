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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.split.PaneSpec
import androidx.compose.ui.semantics.Role as SemanticsRole

// The entry, its cards and their frame, then the samples of the Containment and Navigation cards.
// MaterialGallery.kt keeps the samples of the Actions, Inputs and Selection cards, and
// GalleryFeedback.kt those of the Feedback cards.

/**
 * The padding inside a dialog, which Material 3 does not publish.
 */
private val DialogPadding = 24.dp

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
 * The Material 3 components gallery.
 *
 * Every card holds stock Material 3 components on their default colors, under the theme the pane
 * already uses, so the Expressive flavour shows the same set in its own shapes and motion, and
 * then its own components after them in each group. Each component shows up enabled and disabled,
 * apart from the few Material 3 gives no disabled look.
 * Nothing in the gallery opens a popup or a dialog window, since on the web the first one takes
 * the accessibility mirror over for good. Menus, dialogs and tooltips are drawn in place.
 * The text fields would open a context menu on a right click and a text toolbar on a long press,
 * both popups, so the gallery swallows right-button presses and hands its text fields a toolbar
 * that never shows. Copy and paste still work from the keyboard.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[expressive] Whether to add the Expressive components, [ExpressiveCards].
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
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            GalleryGrid(
                cards = if (expressive) ExpressiveGalleryCards else MaterialCards,
                listState = state.rememberListState("gallery.material"),
                gap = SectionGap,
                modifier = Modifier.gallerySwallowRightPresses(),
                header = { group -> MaterialGroupHeader(group) },
                card = { card, cardModifier -> MaterialCardFrame(card, state, cardModifier) },
            )
        }
    }
}

/**
 * Every card of the Material 3 gallery, in the order they show within each group.
 */
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

/**
 * The outlined frame a card's components sit in, titled with the card's name.
 */
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
                modifier = Modifier.previewRoles(
                    enabled,
                    MaterialComponent.ListItem,
                    GalleryComponent.DisabledListItem,
                ),
                enabled = enabled,
                leadingContent = { Icon(Lucide.MapPin, contentDescription = null) },
                supportingContent = { Text(dates) },
            ) { Text(place) }
        }
    }
}

/**
 * An alert dialog as it looks open, laid out in place rather than in a window.
 */
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

/**
 * Material 3 gives tabs no disabled look, so every tab is enabled.
 */
@Composable
internal fun Tabs(state: DemoAppState) {
    val picked = state.tabIndex.coerceIn(0, TabLabels.lastIndex)
    PrimaryTabRow(selectedTabIndex = picked, modifier = Modifier.previewRoles(GalleryComponent.TabRow)) {
        TabLabels.forEachIndexed { index, label ->
            Tab(
                selected = index == picked,
                onClick = { state.tabIndex = index },
                modifier = Modifier.previewRoles(GalleryComponent.Tab),
                text = { Text(label) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val UnderstoodKey = "gallery.dialog.understood"

/**
 * The destination the navigation samples show selected, never the disabled last one.
 */
private fun pickedDestination(state: DemoAppState): GalleryDestination =
    GalleryDestination.entries[state.selectedItem.coerceIn(0, GalleryDestination.entries.size - 2)]

/**
 * A card's enabled copy and its disabled one, sharing the row.
 */
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
