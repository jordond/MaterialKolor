package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.User
import com.composables.icons.lucide.X
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import com.materialkolor.builder.preview.material.EnabledAndDisabled
import com.materialkolor.builder.preview.material.Gap
import com.materialkolor.builder.preview.material.PaneGap
import com.materialkolor.builder.preview.material.previewRoles
import dev.ggoggam.inklet.material3.InkletAssistChip
import dev.ggoggam.inklet.material3.InkletBadge
import dev.ggoggam.inklet.material3.InkletButton
import dev.ggoggam.inklet.material3.InkletCheckbox
import dev.ggoggam.inklet.material3.InkletFilterChip
import dev.ggoggam.inklet.material3.InkletIconButton
import dev.ggoggam.inklet.material3.InkletIconToggleButton
import dev.ggoggam.inklet.material3.InkletInputChip
import dev.ggoggam.inklet.material3.InkletRadioButton
import dev.ggoggam.inklet.material3.InkletSlider
import dev.ggoggam.inklet.material3.InkletSuggestionChip
import dev.ggoggam.inklet.material3.InkletTextField
import dev.ggoggam.inklet.material3.InkletToggle
import dev.ggoggam.inklet.material3.InkletVariant
import kotlin.math.roundToInt

// The samples of the Actions, Inputs and Selection cards of InkletCards. InkletGalleryPanels.kt keeps
// the rest. Everything a sample remembers lives in DemoAppState under a "gallery.inklet." key, so both
// copies of a split agree, and every sketch takes a seed from here so both copies draw the same strokes.
// A disabled copy takes its enabled twin's seed plus one.

/**
 * The stops on the slider, 0 to 10.
 */
private const val SliderStops = 11
private const val SliderTop = SliderStops - 1f

private val Seats = listOf("Window", "Aisle", "Middle")
private val Interests = listOf("Beach", "Hiking", "Skiing")

@Composable
internal fun SolidButtons() {
    EnabledAndDisabled { enabled ->
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.SolidButton),
            enabled = enabled,
            seed = seedOf(SolidSeed, enabled),
        ) { Text("Book") }
    }
}

@Composable
internal fun OutlineButtons() {
    EnabledAndDisabled { enabled ->
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.OutlineButton),
            enabled = enabled,
            variant = InkletVariant.Outline,
            seed = seedOf(OutlineSeed, enabled),
        ) { Text("Details") }
    }
}

@Composable
internal fun ScribbleButtons() {
    EnabledAndDisabled { enabled ->
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.OutlineButton),
            enabled = enabled,
            variant = InkletVariant.Scribble,
            seed = seedOf(ScribbleSeed, enabled),
        ) { Text("Share") }
    }
}

/**
 * An outline, a solid and a scribble icon button, and a toggle that remembers its pick.
 */
@Composable
internal fun IconButtons(state: DemoAppState) {
    val liked = state.isOn(LikedKey)
    EnabledAndDisabled { enabled ->
        InkletIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.OutlineIconButton),
            enabled = enabled,
            seed = seedOf(IconSeed, enabled),
        ) { Icon(Lucide.Share2, contentDescription = "Share") }
        InkletIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.SolidIconButton),
            enabled = enabled,
            variant = InkletVariant.Solid,
            seed = seedOf(IconSeed + 2, enabled),
        ) { Icon(Lucide.Plus, contentDescription = "Add") }
        InkletIconButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, InkletComponent.OutlineIconButton),
            enabled = enabled,
            variant = InkletVariant.Scribble,
            seed = seedOf(IconSeed + 4, enabled),
        ) { Icon(Lucide.Bookmark, contentDescription = "Save") }
        InkletIconToggleButton(
            checked = liked,
            onCheckedChange = { on -> state.setOn(LikedKey, on) },
            modifier = Modifier.previewRoles(enabled, InkletComponent.IconToggleButton),
            enabled = enabled,
            seed = seedOf(IconSeed + 6, enabled),
        ) { Icon(Lucide.Heart, contentDescription = "Like") }
    }
}

/**
 * Inklet gives badges no disabled look, so a plain one and a scribbled one in the error color show.
 */
@Composable
internal fun Badges() {
    Row(horizontalArrangement = Arrangement.spacedBy(PaneGap), verticalAlignment = Alignment.CenterVertically) {
        InkletBadge("New", Modifier.previewRoles(InkletComponent.Badge), seed = BadgeSeed)
        InkletBadge(
            text = "2 seats left",
            modifier = Modifier.previewRoles(Role.Error),
            color = MaterialTheme.colorScheme.error,
            scribble = true,
            seed = BadgeSeed + 1,
        )
    }
}

/**
 * A field sharing the gallery's text, one showing an error and a disabled one holding a fixed value.
 */
@Composable
internal fun TextFields(state: DemoAppState) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        InkletTextField(
            value = state.text,
            onValueChange = { typed -> state.text = typed },
            modifier = Modifier.fillMaxWidth().previewRoles(InkletComponent.TextField),
            label = { Text("Destination") },
            supportingText = { Text("Where the trip goes") },
            seed = FieldSeed,
        )
        InkletTextField(
            value = "31 Feb",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().previewRoles(InkletComponent.ErrorTextField),
            isError = true,
            readOnly = true,
            label = { Text("Return") },
            supportingText = { Text("February has no 31st") },
            seed = FieldSeed + 1,
            errorMessage = "February has no 31st",
        )
        InkletTextField(
            value = "Lisbon",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().previewRoles(InkletComponent.Disabled),
            enabled = false,
            label = { Text("Origin") },
            seed = FieldSeed + 2,
        )
    }
}

/**
 * The slider is continuous, so a drag snaps to the nearest of its 11 stops as it goes.
 */
@Composable
internal fun Sliders(state: DemoAppState) {
    val stop = state.choice(VolumeKey, SliderStops, default = 4)
    Column {
        for (enabled in listOf(true, false)) {
            InkletSlider(
                value = stop / SliderTop,
                onValueChange = { value -> state.choose(VolumeKey, SliderStops, (value * SliderTop).roundToInt()) },
                modifier = Modifier
                    .semantics { contentDescription = "Volume" }
                    .previewRoles(enabled, InkletComponent.Slider),
                enabled = enabled,
                seed = seedOf(SliderSeed, enabled),
            )
        }
    }
}

@Composable
internal fun Checkboxes(state: DemoAppState) {
    Column {
        for ((index, item) in listOf("Passports", "Chargers", "Visas").withIndex()) {
            val key = "gallery.inklet.packing.$item"
            val checked = state.isChecked(key)
            val enabled = index < 2
            LabelledControl(item, enabled, onClick = { state.setChecked(key, !checked) }) {
                InkletCheckbox(
                    checked = checked,
                    onCheckedChange = { on -> state.setChecked(key, on) },
                    modifier = Modifier
                        .semantics { contentDescription = item }
                        .previewRoles(InkletComponent.Control),
                    enabled = enabled,
                    seed = CheckboxSeed + index,
                )
            }
        }
    }
}

@Composable
internal fun RadioButtons(state: DemoAppState) {
    val picked = state.choice(SeatKey, Seats.size)
    Column(Modifier.selectableGroup()) {
        Seats.forEachIndexed { index, seat ->
            val enabled = index != Seats.lastIndex
            val pick = { state.choose(SeatKey, Seats.size, index) }
            LabelledControl(seat, enabled, onClick = pick) {
                InkletRadioButton(
                    selected = picked == index,
                    onClick = pick,
                    modifier = Modifier
                        .semantics { contentDescription = seat }
                        .previewRoles(InkletComponent.Control),
                    enabled = enabled,
                    seed = RadioSeed + index,
                )
            }
        }
    }
}

@Composable
internal fun Toggles(state: DemoAppState) {
    Column {
        for ((index, setting) in listOf("Offline maps", "Roaming").withIndex()) {
            val key = "gallery.inklet.setting.$setting"
            val on = state.isOn(key)
            val enabled = index == 0
            LabelledControl(setting, enabled, onClick = { state.setOn(key, !on) }, controlFirst = false) {
                InkletToggle(
                    checked = on,
                    onCheckedChange = { flipped -> state.setOn(key, flipped) },
                    modifier = Modifier
                        .semantics { contentDescription = setting }
                        .previewRoles(InkletComponent.Control),
                    enabled = enabled,
                    seed = ToggleSeed + index,
                )
            }
        }
    }
}

/**
 * Assist, suggestion, filter and input chips, the last of each set disabled.
 */
@Composable
internal fun Chips(state: DemoAppState) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        EnabledAndDisabled { enabled ->
            InkletAssistChip(
                onClick = {},
                label = { Text("Add to calendar") },
                modifier = Modifier.previewRoles(enabled, InkletComponent.AssistChip),
                enabled = enabled,
                leadingIcon = {
                    Icon(Lucide.Calendar, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize))
                },
                seed = seedOf(ChipSeed, enabled),
            )
            InkletSuggestionChip(
                onClick = {},
                label = { Text("Lisbon") },
                modifier = Modifier.previewRoles(enabled, InkletComponent.SuggestionChip),
                enabled = enabled,
                icon = { Icon(Lucide.Star, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                seed = seedOf(ChipSeed + 2, enabled),
            )
        }
        Wrapping {
            Interests.forEachIndexed { index, interest ->
                val key = "gallery.inklet.interest.$interest"
                val selected = state.isOn(key)
                val enabled = index != Interests.lastIndex
                InkletFilterChip(
                    selected = selected,
                    onClick = { state.setOn(key, !selected) },
                    label = { Text(interest) },
                    modifier = Modifier.previewRoles(enabled, InkletComponent.SelectableChip),
                    enabled = enabled,
                    leadingIcon = if (selected) {
                        { Icon(Lucide.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                    seed = ChipSeed + 4 + index,
                )
            }
        }
        EnabledAndDisabled { enabled ->
            val name = if (enabled) "Ana" else "Ben"
            val key = "gallery.inklet.traveller.$name"
            val selected = state.isOn(key)
            InkletInputChip(
                selected = selected,
                onClick = { state.setOn(key, !selected) },
                label = { Text(name) },
                modifier = Modifier.previewRoles(enabled, InkletComponent.SelectableChip),
                enabled = enabled,
                leadingIcon = {
                    Icon(Lucide.User, contentDescription = null, Modifier.size(InputChipDefaults.IconSize))
                },
                trailingIcon = {
                    Icon(Lucide.X, contentDescription = null, Modifier.size(InputChipDefaults.IconSize))
                },
                seed = seedOf(ChipSeed + 8, enabled),
            )
        }
    }
}

// Keys into DemoAppState.
private const val LikedKey = "gallery.inklet.liked"
private const val VolumeKey = "gallery.inklet.volume"
private const val SeatKey = "gallery.inklet.seat"

// The seed of each sample's first sketch. The gaps leave room for its disabled twin and siblings.
private const val SolidSeed = 100
private const val OutlineSeed = 110
private const val ScribbleSeed = 120
private const val IconSeed = 130
private const val BadgeSeed = 140
private const val FieldSeed = 150
private const val SliderSeed = 160
private const val CheckboxSeed = 170
private const val RadioSeed = 180
private const val ToggleSeed = 190
private const val ChipSeed = 200

/**
 * The seed of the enabled copy of a sample, or of its disabled twin one after it.
 */
internal fun seedOf(
    seed: Int,
    enabled: Boolean,
): Int = if (enabled) seed else seed + 1

/**
 * Lays its content out in a row that wraps when the card is narrow.
 */
@Composable
internal fun Wrapping(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/**
 * An Inklet control and its label on one row. The control takes its own clicks and carries the
 * label for screen readers, and a tap on the label does what a click on the control does without
 * making the label a second focus stop.
 *
 * @param[controlFirst] Whether the control leads, as a checkbox or radio button does, or trails
 * like a toggle.
 */
@Composable
internal fun LabelledControl(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    controlFirst: Boolean = true,
    control: @Composable () -> Unit,
) {
    val tap by rememberUpdatedState(onClick)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (controlFirst) control()
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .pointerInput(enabled) { if (enabled) detectTapGestures { tap() } }
                .clearAndSetSemantics {},
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
            },
        )
        if (!controlFirst) control()
    }
}

/**
 * How far Material fades the content of a disabled component.
 */
internal const val DisabledAlpha: Float = 0.38f
