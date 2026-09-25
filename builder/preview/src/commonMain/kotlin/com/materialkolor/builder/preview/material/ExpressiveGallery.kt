package com.materialkolor.builder.preview.material

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Mountain
import com.composables.icons.lucide.NotebookPen
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Sailboat
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Tent
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.TreePalm
import com.composables.icons.lucide.X
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import androidx.compose.ui.semantics.Role as SemanticsRole

// The Expressive cards of the Material 3 gallery (F-21), which join MaterialCards when the pane
// wears the Expressive flavour. ExpressiveFeedback.kt keeps the samples of the Feedback cards.
// Everything a sample remembers lives in DemoAppState under a "gallery." key, so both copies of a
// split agree, and nothing opens a popup or a window (D40).

private val ShapeSize = 56.dp

private val Amenities = listOf("Wi-Fi", "Pool", "Pets")
private val Transports = listOf("Bus", "Train", "Ferry")
private val BookingOptions = listOf("Book with points", "Hold for a day")
private val ToolbarActions = listOf(
    Lucide.Camera to "Take a photo",
    Lucide.Bookmark to "Save",
    Lucide.Bus to "Directions",
)

private enum class CreateAction(
    val label: String,
    val icon: ImageVector,
) {
    Trip("New trip", Lucide.Plane),
    Note("New note", Lucide.NotebookPen),
    Place("Save a place", Lucide.MapPin),
}

/**
 * A Material shape with a name to read out and an icon to hold.
 */
private enum class ShapeSample(
    val label: String,
    val icon: ImageVector,
) {
    Cookie("Cookie", Lucide.Coffee),
    Sunny("Sunny", Lucide.TreePalm),
    Clover("Clover", Lucide.Heart),
    Arch("Arch", Lucide.Tent),
    Gem("Gem", Lucide.Mountain),
    Pill("Pill", Lucide.Sailboat),
}

/**
 * Every Expressive card, each shown in its group after the Material 3 cards.
 */
internal val ExpressiveCards: List<GalleryCard> = listOf(
    GalleryCard("Button group", GalleryGroup.Actions) { state -> ButtonGroups(state) },
    GalleryCard("Split button", GalleryGroup.Actions) { state -> SplitButtons(state) },
    GalleryCard("FAB menu", GalleryGroup.Actions) { state -> FabMenu(state) },
    GalleryCard("Connected button group", GalleryGroup.Selection) { state -> ConnectedButtons(state) },
    GalleryCard("Material shapes", GalleryGroup.Containment) { state -> ShapeSamples(state) },
    GalleryCard("Flexible top app bar", GalleryGroup.Navigation) { FlexibleTopAppBars() },
    GalleryCard("Floating toolbar", GalleryGroup.Navigation) { FloatingToolbars() },
    GalleryCard("Loading indicators", GalleryGroup.Feedback) { LoadingIndicators() },
    GalleryCard("Wavy progress indicators", GalleryGroup.Feedback) { WavyProgressIndicators() },
)

/**
 * The gallery of the Expressive flavour, the Material 3 cards and then the Expressive ones.
 */
internal val ExpressiveGalleryCards: List<GalleryCard> = MaterialCards + ExpressiveCards

/**
 * A standard button group of toggle buttons, the pressed one widening as its neighbours give way.
 *
 * Every button takes an equal share of the row, so none ever spills into the group's overflow
 * menu, which opens in a popup. The overflow button draws nothing either, so nothing could open it.
 */
@Composable
internal fun ButtonGroups(state: DemoAppState) {
    ButtonGroup(overflowIndicator = {}, modifier = Modifier.fillMaxWidth()) {
        Amenities.forEachIndexed { index, amenity ->
            val enabled = index != Amenities.lastIndex
            customItem(
                buttonGroupContent = {
                    val key = "gallery.amenity.$amenity"
                    val checked = state.isOn(key)
                    val interactions = remember { MutableInteractionSource() }
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = { on -> state.setOn(key, on) },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactions)
                            .previewRoles(enabled, ExpressiveComponent.ToggleButton)
                            .foldedToggleName(amenity, checked, enabled),
                        enabled = enabled,
                        interactionSource = interactions,
                    ) { Text(amenity, maxLines = 1) }
                },
                menuContent = {},
            )
        }
    }
}

/**
 * A split button and its disabled copy. The trailing half opens the booking options in place under
 * the pair rather than in a menu window, and picking one closes them again.
 */
@Composable
internal fun SplitButtons(state: DemoAppState) {
    val open = state.isOn(SplitOpenKey)
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        EnabledAndDisabled { enabled ->
            val expanded = open && enabled
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = {},
                        modifier = Modifier.previewRoles(
                            enabled,
                            MaterialComponent.FilledButton,
                            GalleryComponent.DisabledButton,
                        ),
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = Lucide.Plane,
                            contentDescription = null,
                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Book")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        checked = expanded,
                        onCheckedChange = { checked -> state.setOn(SplitOpenKey, checked) },
                        modifier = Modifier
                            .previewRoles(enabled, MaterialComponent.FilledButton, GalleryComponent.DisabledButton)
                            .foldedExpandedName("Booking options", expanded, enabled),
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                            contentDescription = null,
                            modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                        )
                    }
                },
            )
        }
        if (open) BookingMenu { state.setOn(SplitOpenKey, false) }
    }
}

/**
 * The split button's options as they look open, drawn in place like the gallery's menu.
 */
@Composable
private fun BookingMenu(onPick: () -> Unit) {
    Surface(
        modifier = Modifier.previewRoles(GalleryComponent.Menu),
        shape = MenuDefaults.shape,
        color = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
    ) {
        Column(Modifier.width(IntrinsicSize.Max).padding(vertical = Gap)) {
            for (option in BookingOptions) {
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = onPick,
                    modifier = Modifier.previewRoles(GalleryComponent.MenuItem),
                )
            }
        }
    }
}

/**
 * A floating action button that opens its menu upward in the layout, never in a window. Picking an
 * item closes it again. Material gives the menu no disabled look.
 */
@Composable
internal fun FabMenu(state: DemoAppState) {
    val open = state.isOn(FabMenuOpenKey)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButtonMenu(
            expanded = open,
            button = {
                ToggleFloatingActionButton(
                    checked = open,
                    onCheckedChange = { checked -> state.setOn(FabMenuOpenKey, checked) },
                    modifier = Modifier
                        .previewRoles(ExpressiveComponent.ToggleFloatingActionButton)
                        .foldedExpandedName("Create", open),
                ) {
                    val icon by remember { derivedStateOf { if (checkedProgress > 0.5f) Lucide.X else Lucide.Plus } }
                    Icon(icon, contentDescription = null, modifier = Modifier.animateIcon({ checkedProgress }))
                }
            },
        ) {
            for (action in CreateAction.entries) {
                FloatingActionButtonMenuItem(
                    onClick = { state.setOn(FabMenuOpenKey, false) },
                    text = { Text(action.label) },
                    icon = { Icon(action.icon, contentDescription = null) },
                    modifier = Modifier.previewRoles(ExpressiveComponent.FloatingActionButtonMenuItem),
                )
            }
        }
    }
}

/**
 * A connected row of toggle buttons that picks one way to travel, the picked one turning round.
 */
@Composable
internal fun ConnectedButtons(state: DemoAppState) {
    val count = Transports.size
    val picked = state.choice(TransportKey, count, default = 1)
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        Transports.forEachIndexed { index, transport ->
            val enabled = index != Transports.lastIndex
            val checked = index == picked
            ToggleButton(
                checked = checked,
                onCheckedChange = { state.choose(TransportKey, count, index) },
                modifier = Modifier
                    .weight(1f)
                    .previewRoles(enabled, ExpressiveComponent.ToggleButton)
                    .semantics { role = SemanticsRole.RadioButton }
                    .foldedChoiceName(transport, checked, enabled),
                enabled = enabled,
                shapes = connectedShapes(index, count),
            ) { Text(transport, maxLines = 1) }
        }
    }
}

/**
 * The shapes of a connected button, rounder at the outer end of the first and the last.
 */
@Composable
private fun connectedShapes(
    index: Int,
    count: Int,
): ToggleButtonShapes =
    when (index) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }

/**
 * Containers cut to Material shapes, one picked at a time. The picked one fills with the primary
 * color. Shapes have no disabled look.
 */
@Composable
internal fun ShapeSamples(state: DemoAppState) {
    val count = ShapeSample.entries.size
    val picked = state.choice(ShapeKey, count)
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        for (sample in ShapeSample.entries) {
            val selected = sample.ordinal == picked
            val (container, content) = if (selected) {
                colors.primary to colors.onPrimary
            } else {
                colors.secondaryContainer to colors.onSecondaryContainer
            }
            Box(
                modifier = Modifier
                    .size(ShapeSize)
                    .previewRoles(
                        if (selected) Role.Primary else Role.SecondaryContainer,
                        if (selected) Role.OnPrimary else Role.OnSecondaryContainer,
                    ).clip(sample.shape())
                    .background(container)
                    .selectable(selected, role = SemanticsRole.RadioButton) {
                        state.choose(ShapeKey, count, sample.ordinal)
                    }.foldedChoiceName(sample.label, selected),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sample.icon, contentDescription = null, tint = content)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeSample.shape(): Shape =
    when (this) {
        ShapeSample.Cookie -> MaterialShapes.Cookie9Sided
        ShapeSample.Sunny -> MaterialShapes.Sunny
        ShapeSample.Clover -> MaterialShapes.Clover4Leaf
        ShapeSample.Arch -> MaterialShapes.Arch
        ShapeSample.Gem -> MaterialShapes.Gem
        ShapeSample.Pill -> MaterialShapes.Pill
    }.toShape()

/**
 * A medium flexible top app bar at its full height, its title over a subtitle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FlexibleTopAppBars() {
    MediumFlexibleTopAppBar(
        title = { Text("Lisbon") },
        modifier = Modifier.previewRoles(ExpressiveComponent.FlexibleTopAppBar),
        subtitle = { Text("12 to 19 May") },
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

/**
 * A standard floating toolbar at rest in the card, its last action disabled.
 */
@Composable
internal fun FloatingToolbars() {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = Modifier.previewRoles(ExpressiveComponent.FloatingToolbar),
    ) {
        for ((icon, label) in ToolbarActions) {
            IconButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.IconButton)) {
                Icon(icon, contentDescription = label)
            }
        }
        IconButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.IconButton), enabled = false) {
            Icon(Lucide.Trash2, contentDescription = "Delete")
        }
    }
}

// Keys into DemoAppState.
private const val SplitOpenKey = "gallery.split.open"
private const val FabMenuOpenKey = "gallery.fabMenu.open"
private const val TransportKey = "gallery.transport"
private const val ShapeKey = "gallery.shape"
