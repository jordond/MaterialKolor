package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedMenuItemName
import com.materialkolor.builder.kit.control.foldedOptionName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import androidx.compose.ui.semantics.Role as SemanticsRole

// The samples of the Select and Menu cards, whose lists open in place.

private val RowHeight = 36.dp
private val SortOrders = listOf("Newest", "Oldest", "Name", "Size")

/**
 * One row of the menu, and whether it runs or is greyed out.
 */
private enum class MenuAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
) {
    Rename("Rename", Lucide.Pencil),
    Duplicate("Duplicate", Lucide.Copy),
    Archive("Archive", Lucide.Archive, enabled = false),
    Delete("Delete", Lucide.Trash2),
}

/**
 * A select whose list of options opens in place under its field, in the card's own layout and not
 * in a popup. The field says whether the list is open, and each option whether it is the one
 * chosen, on the web too. A pick closes the list. It starts open, so the card shows the list.
 */
@Composable
internal fun InPlaceSelect(state: DemoAppState) {
    val open = !state.isOn(GalleryKeys.SortShut)
    val picked = state.choice(GalleryKeys.Sort, SortOrders.size)
    GalleryColumn {
        SelectField(
            label = "Sort by",
            value = SortOrders[picked],
            open = open,
            enabled = true,
        ) { state.setOn(GalleryKeys.SortShut, open) }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(panelMotion()) + expandVertically(panelMotion()),
            exit = fadeOut(panelMotion()) + shrinkVertically(panelMotion()),
        ) {
            InPlacePanel(Modifier.selectableGroup()) {
                SortOrders.forEachIndexed { index, order ->
                    val selected = index == picked
                    // The last order is out of reach, the disabled look of an option.
                    val enabled = index != SortOrders.lastIndex
                    PanelRow(
                        label = order,
                        selected = selected,
                        enabled = enabled,
                        name = Modifier
                            .semantics { this.selected = selected }
                            .foldedOptionName(order, selected, enabled),
                    ) {
                        state.choose(GalleryKeys.Sort, SortOrders.size, index)
                        state.setOn(GalleryKeys.SortShut, true)
                    }
                }
            }
        }
        SelectField(label = "Group by", value = "Folder", open = false, enabled = false) {}
    }
}

/**
 * The field of a select, its label over its value, that opens and closes its list.
 */
@Composable
private fun SelectField(
    label: String,
    value: String,
    open: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    UnstyledButton(
        onClick = onToggle,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(enabled, UnstyledGalleryComponent.SelectField)
            .foldedExpandedName("$label, $value", open, enabled)
            .expandActions(open, onToggle)
            .galleryFocusRing(interactions)
            .clip(ControlShape)
            .border(1.dp, if (enabled) UnstyledToken.Outline.color else disabledContainer, ControlShape),
        role = SemanticsRole.DropdownList,
        interactionSource = interactions,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = SmallStyle, color = tint(UnstyledToken.OnSurfaceVariant, enabled), maxLines = 1)
                Text(value, style = BodyStyle, color = tint(UnstyledToken.OnSurface, enabled), maxLines = 1)
            }
            val chevron = tint(UnstyledToken.OnSurface, enabled)
            UnstyledIcon(Lucide.ChevronDown, null, Modifier.size(IconSize), tint = chevron)
        }
    }
}

/**
 * A menu that opens in place under its button, pushing what follows down rather than floating over
 * it, since a popup would take the web mirror over. A pick or a second press closes it. It starts
 * open, so the card shows the menu.
 */
@Composable
internal fun InPlaceMenu(state: DemoAppState) {
    val open = !state.isOn(GalleryKeys.MenuShut)
    GalleryColumn {
        EnabledPair { enabled ->
            MenuButton(open = enabled && open, enabled = enabled) { state.setOn(GalleryKeys.MenuShut, open) }
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(panelMotion()) + expandVertically(panelMotion()),
            exit = fadeOut(panelMotion()) + shrinkVertically(panelMotion()),
        ) {
            InPlacePanel {
                for (action in MenuAction.entries) {
                    if (action == MenuAction.Delete) {
                        UnstyledHorizontalSeparator(
                            color = UnstyledToken.OutlineVariant.color,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .previewRoles(UnstyledGalleryComponent.Separator),
                        )
                    }
                    PanelRow(
                        label = action.label,
                        selected = false,
                        enabled = action.enabled,
                        name = Modifier.foldedMenuItemName(action.label, enabled = action.enabled),
                        icon = action.icon,
                        danger = action == MenuAction.Delete,
                    ) { state.setOn(GalleryKeys.MenuShut, true) }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    open: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    GalleryButton(
        label = "Options",
        style = GalleryButtonStyle.Outlined,
        enabled = enabled,
        icon = Lucide.ChevronDown,
        modifier = Modifier.foldedExpandedName("Options", open, enabled).expandActions(open, onToggle),
        onClick = onToggle,
    )
}

/**
 * The raised panel the menu and the select's list show on, in place in the card.
 */
@Composable
private fun InPlacePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(UnstyledGalleryComponent.Menu)
            .clip(CardShape)
            .background(UnstyledToken.SurfaceContainerHighest.color)
            .border(1.dp, UnstyledToken.OutlineVariant.color, CardShape)
            .padding(4.dp)
            .then(modifier),
    ) { content() }
}

/**
 * One row of the menu or of the select's list. A picked option shows on the secondary container
 * with a check, a row that deletes in the error color.
 *
 * @param[name] How the row is named, through the kit fold modifier that matches it.
 */
@Composable
private fun PanelRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    name: Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = when {
        selected -> UnstyledGalleryComponent.SelectedMenuItem
        danger -> UnstyledGalleryComponent.DangerMenuItem
        else -> UnstyledGalleryComponent.MenuItem
    }
    val content = when {
        !enabled -> disabledContent
        selected -> UnstyledToken.OnSecondaryContainer.color
        danger -> UnstyledToken.Error.color
        else -> UnstyledToken.OnSurface.color
    }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .previewRoles(enabled, component)
            .then(name)
            .galleryFocusRing(interactions)
            .clip(ControlShape)
            .background(if (selected) UnstyledToken.SecondaryContainer.color else Color.Transparent),
        interactionSource = interactions,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(IconSize)) {
                val shown = icon ?: Lucide.Check.takeIf { selected }
                if (shown != null) UnstyledIcon(shown, null, Modifier.fillMaxSize(), tint = content)
            }
            Text(label, style = BodyStyle, color = content, maxLines = 1)
        }
    }
}
