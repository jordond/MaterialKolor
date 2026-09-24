package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Luggage
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.User
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role as SemanticsRole

// The samples of the Actions, Inputs and Selection cards of MaterialCards in GalleryEntry.kt, which
// keeps the rest with GalleryFeedback.kt. Everything a sample remembers lives in DemoAppState under
// a "gallery." key, so both copies of a split agree. Where a component has no enabled and disabled
// pair, the last option of its set is the disabled one.

/** The stops on both sliders, 0 to 10. */
private const val SliderStops = 11
private const val SliderTop = SliderStops - 1f

private val Seats = listOf("Window", "Aisle", "Middle")
private val Periods = listOf("Day", "Week", "Month")
private val Interests = listOf("Beach", "Hiking", "Skiing")
private val SortOrders = listOf(
    "By date" to Lucide.Calendar,
    "By name" to Lucide.ArrowUpDown,
    "By price" to Lucide.Luggage,
)

@Composable
internal fun FilledButtons() {
    EnabledAndDisabled { enabled ->
        Button(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, MaterialComponent.FilledButton, GalleryComponent.DisabledButton),
            enabled = enabled,
        ) { Text("Book") }
    }
}

@Composable
internal fun TonalButtons() {
    EnabledAndDisabled { enabled ->
        FilledTonalButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, MaterialComponent.TonalButton),
            enabled = enabled,
        ) { Text("Share") }
    }
}

@Composable
internal fun ElevatedButtons() {
    EnabledAndDisabled { enabled ->
        ElevatedButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, GalleryComponent.ElevatedButton, GalleryComponent.DisabledButton),
            enabled = enabled,
        ) { Text("Save") }
    }
}

@Composable
internal fun OutlinedButtons() {
    EnabledAndDisabled { enabled ->
        OutlinedButton(
            onClick = {},
            modifier = Modifier.previewRoles(
                enabled,
                MaterialComponent.OutlinedButton,
                GalleryComponent.DisabledOutlinedButton,
            ),
            enabled = enabled,
        ) { Text("Details") }
    }
}

@Composable
internal fun TextButtons() {
    EnabledAndDisabled { enabled ->
        TextButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, MaterialComponent.TextButton, GalleryComponent.DisabledVariant),
            enabled = enabled,
        ) { Text("Skip") }
    }
}

@Composable
internal fun IconButtons() {
    EnabledAndDisabled { enabled ->
        // A standard icon button paints the content color the card's surface sets, in both states.
        IconButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.IconButton), enabled = enabled) {
            Icon(Lucide.Heart, contentDescription = "Like")
        }
        FilledIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, GalleryComponent.FilledIconButton),
            enabled = enabled,
        ) { Icon(Lucide.Plus, contentDescription = "Add") }
        FilledTonalIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, GalleryComponent.TonalIconButton),
            enabled = enabled,
        ) { Icon(Lucide.Bookmark, contentDescription = "Save") }
        OutlinedIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, GalleryComponent.OutlinedIconButton),
            enabled = enabled,
        ) { Icon(Lucide.Share2, contentDescription = "Share") }
    }
}

/** Material 3 gives floating action buttons no disabled look, so only the enabled ones show. */
@Composable
internal fun FloatingActionButtons() {
    Row(horizontalArrangement = Arrangement.spacedBy(SectionGap), verticalAlignment = Alignment.CenterVertically) {
        SmallFloatingActionButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.Fab)) {
            Icon(Lucide.Pencil, contentDescription = "Edit trip")
        }
        FloatingActionButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.Fab)) {
            Icon(Lucide.Plus, contentDescription = "New trip")
        }
    }
}

/** Collapses to its icon and back on each click. It has no disabled look either. */
@Composable
internal fun ExtendedFab(state: DemoAppState) {
    val collapsed = state.isOn(FabCollapsedKey)
    ExtendedFloatingActionButton(
        text = { Text("New trip") },
        icon = { Icon(Lucide.Plus, contentDescription = if (collapsed) "New trip" else null) },
        onClick = { state.setOn(FabCollapsedKey, !collapsed) },
        modifier = Modifier.previewRoles(MaterialComponent.Fab),
        expanded = !collapsed,
    )
}

@Composable
internal fun FilledTextFields(state: DemoAppState) {
    TextFieldPair(state) { enabled, value, onValueChange, label ->
        // b-228b
        SampleTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.fillMaxWidth().previewRoles(enabled, GalleryComponent.TextField),
            enabled = enabled,
        )
    }
}

@Composable
internal fun OutlinedTextFields(state: DemoAppState) {
    TextFieldPair(state) { enabled, value, onValueChange, label ->
        // b-228b
        SampleOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.fillMaxWidth().previewRoles(enabled, MaterialComponent.OutlinedTextField),
            enabled = enabled,
        )
    }
}

@Composable
internal fun Sliders(state: DemoAppState) {
    val stop = state.choice(VolumeKey, SliderStops, default = 4)
    Column {
        for (enabled in listOf(true, false)) {
            Slider(
                value = stop.toFloat(),
                onValueChange = { value -> state.choose(VolumeKey, SliderStops, value.roundToInt()) },
                modifier = Modifier
                    .semantics { contentDescription = "Volume" }
                    .previewRoles(enabled, GalleryComponent.Slider),
                enabled = enabled,
                valueRange = 0f..SliderTop,
                steps = SliderStops - 2,
            )
        }
    }
}

@Composable
internal fun RangeSliders(state: DemoAppState) {
    val from = state.choice(RangeFromKey, SliderStops, default = 2)
    val to = state.choice(RangeToKey, SliderStops, default = 8)
    Column {
        for (enabled in listOf(true, false)) {
            RangeSlider(
                value = from.toFloat()..to.toFloat(),
                onValueChange = { range ->
                    state.choose(RangeFromKey, SliderStops, range.start.roundToInt())
                    state.choose(RangeToKey, SliderStops, range.endInclusive.roundToInt())
                },
                modifier = Modifier
                    .semantics { contentDescription = "Price range" }
                    .previewRoles(enabled, GalleryComponent.Slider),
                enabled = enabled,
                valueRange = 0f..SliderTop,
                steps = SliderStops - 2,
            )
        }
    }
}

@Composable
internal fun Checkboxes(state: DemoAppState) {
    Column {
        for ((index, item) in listOf("Passports", "Chargers", "Visas").withIndex()) {
            val key = "gallery.packing.$item"
            val checked = state.isChecked(key)
            val enabled = index < 2
            ControlRow(
                label = item,
                modifier = Modifier
                    .toggleable(checked, enabled = enabled, role = SemanticsRole.Checkbox) { on ->
                        state.setChecked(key, on)
                    }.previewRoles(enabled, MaterialComponent.Checkbox),
            ) { Checkbox(checked = checked, onCheckedChange = null, enabled = enabled) }
        }
    }
}

@Composable
internal fun RadioButtons(state: DemoAppState) {
    val picked = state.choice(SeatKey, Seats.size)
    Column(Modifier.selectableGroup()) {
        Seats.forEachIndexed { index, seat ->
            val enabled = index != Seats.lastIndex
            ControlRow(
                label = seat,
                modifier = Modifier
                    .selectable(picked == index, enabled = enabled, role = SemanticsRole.RadioButton) {
                        state.choose(SeatKey, Seats.size, index)
                    }.previewRoles(enabled, GalleryComponent.RadioButton),
            ) { RadioButton(selected = picked == index, onClick = null, enabled = enabled) }
        }
    }
}

@Composable
internal fun Switches(state: DemoAppState) {
    Column {
        for ((setting, enabled) in listOf("Offline maps" to true, "Roaming" to false)) {
            val key = "gallery.setting.$setting"
            val on = state.isOn(key)
            ControlRow(
                label = setting,
                modifier = Modifier
                    .toggleable(on, enabled = enabled, role = SemanticsRole.Switch) { flipped ->
                        state.setOn(key, flipped)
                    }.previewRoles(enabled, MaterialComponent.Switch, GalleryComponent.DisabledSwitch),
                controlFirst = false,
            ) { Switch(checked = on, onCheckedChange = null, enabled = enabled) }
        }
    }
}

@Composable
internal fun SegmentedButtons(state: DemoAppState) {
    val picked = state.choice(PeriodKey, Periods.size)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Periods.forEachIndexed { index, period ->
            val enabled = index != Periods.lastIndex
            SegmentedButton(
                selected = picked == index,
                onClick = { state.choose(PeriodKey, Periods.size, index) },
                shape = SegmentedButtonDefaults.itemShape(index, Periods.size),
                modifier = Modifier.previewRoles(
                    enabled,
                    GalleryComponent.SegmentedButton,
                    GalleryComponent.DisabledSegmentedButton,
                ),
                enabled = enabled,
                label = { Text(period) },
            )
        }
    }
}

@Composable
internal fun FilterChips(state: DemoAppState) {
    Wrapping {
        Interests.forEachIndexed { index, interest ->
            val key = "gallery.interest.$interest"
            val selected = state.isOn(key)
            val enabled = index != Interests.lastIndex
            FilterChip(
                selected = selected,
                onClick = { state.setOn(key, !selected) },
                label = { Text(interest) },
                modifier = Modifier.previewRoles(enabled, MaterialComponent.FilterChip),
                enabled = enabled,
                leadingIcon = if (selected) {
                    { Icon(Lucide.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
internal fun AssistChips() {
    EnabledAndDisabled { enabled ->
        AssistChip(
            onClick = {},
            label = { Text("Add to calendar") },
            modifier = Modifier.previewRoles(enabled, MaterialComponent.AssistChip),
            enabled = enabled,
            leadingIcon = {
                Icon(
                    Lucide.Calendar,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        SuggestionChip(
            onClick = {},
            label = { Text("Lisbon") },
            modifier = Modifier.previewRoles(enabled, GalleryComponent.SuggestionChip),
            enabled = enabled,
        )
    }
}

@Composable
internal fun InputChips(state: DemoAppState) {
    EnabledAndDisabled { enabled ->
        val name = if (enabled) "Ana" else "Ben"
        val key = "gallery.traveller.$name"
        val selected = state.isOn(key)
        InputChip(
            selected = selected,
            onClick = { state.setOn(key, !selected) },
            label = { Text(name) },
            modifier = Modifier.previewRoles(enabled, GalleryComponent.InputChip),
            enabled = enabled,
            leadingIcon = { Icon(Lucide.User, contentDescription = null, Modifier.size(InputChipDefaults.IconSize)) },
        )
    }
}

/** A menu as it looks open, drawn in place rather than in a popup (D40). */
@Composable
internal fun InlineMenu(state: DemoAppState) {
    val picked = state.choice(SortKey, SortOrders.size)
    Surface(
        modifier = Modifier.previewRoles(GalleryComponent.Menu),
        shape = MenuDefaults.shape,
        color = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
    ) {
        Column(Modifier.width(IntrinsicSize.Max).padding(vertical = Gap)) {
            SortOrders.forEachIndexed { index, (order, icon) ->
                val enabled = index != SortOrders.lastIndex
                DropdownMenuItem(
                    text = { Text(order) },
                    onClick = { state.choose(SortKey, SortOrders.size, index) },
                    modifier = Modifier.previewRoles(enabled, GalleryComponent.MenuItem),
                    leadingIcon = { Icon(icon, contentDescription = null) },
                    trailingIcon = if (picked == index) {
                        { Icon(Lucide.Check, contentDescription = "Selected") }
                    } else {
                        null
                    },
                    enabled = enabled,
                )
            }
        }
    }
}

// Keys into DemoAppState.
private const val FabCollapsedKey = "gallery.fab.collapsed"
private const val VolumeKey = "gallery.volume"
private const val RangeFromKey = "gallery.range.from"
private const val RangeToKey = "gallery.range.to"
private const val SeatKey = "gallery.seat"
private const val PeriodKey = "gallery.period"
private const val SortKey = "gallery.sort"

/** Lays its content out in a row that wraps when the card is narrow. */
@Composable
private fun Wrapping(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A component's enabled copy and then its disabled one, in a row that wraps. */
@Composable
internal fun EnabledAndDisabled(content: @Composable (enabled: Boolean) -> Unit) {
    Wrapping {
        content(true)
        content(false)
    }
}

/** Two text fields sharing the gallery's text, and a disabled one holding a fixed value. */
@Composable
private fun TextFieldPair(
    state: DemoAppState,
    field: @Composable (enabled: Boolean, value: String, onValueChange: (String) -> Unit, label: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        field(true, state.text, { typed -> state.text = typed }, "Destination")
        field(false, "Lisbon", {}, "Origin")
    }
}

/**
 * A control and its label on one row, the whole row taking the click so the label does too.
 *
 * @param[controlFirst] Whether the control leads, as a checkbox or radio button does, or trails
 * like a switch.
 */
@Composable
internal fun ControlRow(
    label: String,
    modifier: Modifier,
    controlFirst: Boolean = true,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        horizontalArrangement = Arrangement.spacedBy(PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (controlFirst) control()
        Text(label, Modifier.weight(1f))
        if (!controlFirst) control()
    }
}
