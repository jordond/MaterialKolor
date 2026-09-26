package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.tabMovesFocus
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.drawTripScene
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.HyperlinkButton
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.ListItemDefaults
import io.github.composefluent.component.ListItemSeparator
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextFieldDefaults
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.CalendarLtr
import io.github.composefluent.icons.regular.Edit
import io.github.composefluent.scheme.collectVisualState
import io.github.composefluent.surface.Card

/**
 * The open trip, scene first and the note last.
 */
@Composable
internal fun TripDetail(
    trip: Trip,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val colors = FluentTheme.colors
    val shades = TripsShades(colors.darkMode)
    Column(
        modifier.padding(TripsLayout.SectionGap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.SectionGap),
    ) {
        TripScene(shades, Modifier.fillMaxWidth().height(TripsLayout.SceneHeight))
        Column(Modifier.padding(horizontal = TripsLayout.TextInset)) {
            Text(trip.name, style = FluentTheme.typography.title)
            Text(trip.summary, color = colors.text.text.secondary)
        }
        TripActions(shades)
        DayPlan(trip, shades)
        ListItemSeparator(Modifier.fluentNeutralRoles())
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state)
        PackingCard(state)
        NoteCard(state)
    }
}

/**
 * A sky, a sun and three ridges in the accent shades, where a photo would go.
 */
@Composable
private fun TripScene(
    shades: TripsShades,
    modifier: Modifier = Modifier,
) {
    val palette = FluentTheme.colors.shades
    Canvas(modifier.clip(FluentTheme.shapes.overlay).fluentShadeRoles(*shades.scene.toTypedArray())) {
        drawTripScene(
            sky = palette.color(shades.sky),
            sun = palette.color(shades.sun),
            ridges = shades.ridges.map { shade -> palette.color(shade) },
        )
    }
}

/**
 * Check in on the accent fill, Share plan as a standard button, Edit as a subtle one, and the two
 * lightest actions as links.
 */
@Composable
private fun TripActions(shades: TripsShades) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AccentButton(onClick = {}, modifier = Modifier.fluentAccentRoles()) { Text("Check in") }
        Button(onClick = {}, modifier = Modifier.fluentNeutralRoles()) { Text("Share plan") }
        SubtleButton(onClick = {}, modifier = Modifier.fluentNeutralRoles()) {
            Icon(Icons.Regular.Edit, contentDescription = null)
            Text("Edit")
        }
        HyperlinkButton(onClick = {}, modifier = Modifier.fluentShadeRoles(shades.accentText)) {
            Icon(Icons.Regular.CalendarLtr, contentDescription = null)
            Text("Add to calendar")
        }
        HyperlinkButton(onClick = {}, modifier = Modifier.fluentShadeRoles(shades.accentText)) {
            Icon(Lucide.MapPin, contentDescription = null, modifier = Modifier.size(ListItemDefaults.iconSize))
            Text("Directions")
        }
    }
}

@Composable
private fun DayPlan(
    trip: Trip,
    shades: TripsShades,
) {
    val colors = FluentTheme.colors
    val type = FluentTheme.typography
    Column(
        Modifier.padding(horizontal = TripsLayout.TextInset),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        if (trip.plan.isEmpty()) {
            Text("Nothing planned yet", color = colors.text.text.secondary)
        } else {
            Text(
                text = "Day 1 · Friday",
                modifier = Modifier.fluentShadeRoles(shades.accentText),
                color = colors.text.accent.primary,
                style = type.bodyStrong,
            )
        }
        trip.plan.forEachIndexed { index, stop ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
            ) {
                Box(
                    modifier = Modifier
                        .size(TripsLayout.StopSize)
                        .clip(CircleShape)
                        .background(colors.controlAlt.tertiary)
                        .fluentNeutralRoles(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = colors.text.text.secondary, style = type.caption)
                }
                Text(stop.what, Modifier.weight(1f))
                Text(stop.time, color = colors.text.text.secondary, style = type.caption)
            }
        }
    }
}

/**
 * Fluent's critical info bar, offering to turn offline maps on. Its colors are Fluent's fixed
 * system ones, which no scheme color names.
 */
@Composable
private fun MapsNeedSignal(state: DemoAppState) {
    InfoBar(
        title = {},
        message = { Text("Maps need signal until offline maps is on") },
        severity = InfoBarSeverity.Critical,
        modifier = Modifier.fillMaxWidth().fluentNeutralRoles(),
        action = {
            Button(onClick = { state.setOn(OfflineMapsSwitch, true) }, modifier = Modifier.fluentNeutralRoles()) {
                Text("Turn on")
            }
        },
    )
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val colors = FluentTheme.colors
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    val progress = packed / items.size.toFloat()
    Card(Modifier.fillMaxWidth().fluentNeutralRoles()) {
        Column(Modifier.padding(TripsLayout.SectionGap), verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Packing", Modifier.weight(1f), style = FluentTheme.typography.subtitle)
                Text(
                    "$packed of ${items.size}",
                    color = colors.text.text.secondary,
                    style = FluentTheme.typography.caption,
                )
            }
            // The determinate bar, since the endless one would loop past frozen motion.
            ProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .fluentAccentRoles()
                    .semantics {
                        contentDescription = "Packing"
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    },
            )
            for (item in items) PackingCheckBox(item, state)
        }
    }
}

/**
 * Fluent's check box under a layer that toggles it with its state and names it for the web. Its
 * own clickable reports no state.
 */
@Composable
private fun PackingCheckBox(
    item: PackingItem,
    state: DemoAppState,
) {
    val checked = state.isChecked(item.key)
    FluentOverlaid(
        control = Modifier
            .fluentFillRoles(checked)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = { ticked -> state.setChecked(item.key, ticked) },
            ).foldedToggleName(item.label, checked),
    ) {
        CheckBox(checked = checked, label = item.label, onCheckStateChange = {})
    }
}

@Composable
private fun NoteCard(state: DemoAppState) {
    Card(Modifier.fillMaxWidth().fluentNeutralRoles()) {
        // One line, since on a field over several lines a long press on the web would put up selection
        // handles, popups that take the accessibility mirror over. When a popup on the web stops taking
        // the mirror over, the note goes to several lines, together with `tabMovesFocus` below.
        NoteBox(
            value = state.text,
            onValueChange = { text -> state.text = text },
            modifier = Modifier
                .padding(TripsLayout.SectionGap)
                .fillMaxWidth()
                .tabMovesFocus(LocalFocusManager.current)
                .fluentNeutralRoles(),
        )
    }
}

/**
 * A single line text box with Fluent's own decoration, built from the foundation field the way the
 * gallery's text box is, so its inner text goes through the kit's [InnerTextWithoutHandles]. On
 * the web a long press on its text then puts up no selection handles.
 */
@Composable
private fun NoteBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults
        .defaultTextFieldColors()
        .schemeFor(interactions.collectVisualState(false, focusFirst = true))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = FluentTheme.typography.body.copy(color = colors.contentColor),
        cursorBrush = colors.cursorBrush,
        interactionSource = interactions,
        singleLine = true,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                interactionSource = interactions,
                enabled = true,
                color = colors,
                shape = FluentTheme.shapes.control,
                header = { Text("Note for the group") },
                placeholder = null,
                leadingIcon = null,
                trailing = null,
                innerTextField = { InnerTextWithoutHandles(innerTextField) },
            )
        },
    )
}
