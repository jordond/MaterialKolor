package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.RadioButton
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedControl
import io.github.composefluent.component.SegmentedItemPosition
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text

// The samples of the Selection cards of FluentCards in GalleryEntry.kt. Where a component has no
// enabled and disabled pair, the last option of its set is the disabled one.

/**
 * What Fluent's switch says beside itself, in the gallery and in Trips.
 */
internal object SwitcherCopy {
    const val On = "On"
    const val Off = "Off"
}

private val DeliveryOptions = listOf("Standard", "Express", "Overnight")
private val ViewOptions = listOf("Day", "Week", "Month")

@Composable
internal fun CheckBoxes(state: DemoAppState) {
    GalleryColumn {
        val checked = state.isChecked(FluentGalleryKeys.Updates)
        GalleryCheckBox("Email me updates", checked, enabled = true) { on ->
            state.setChecked(FluentGalleryKeys.Updates, on)
        }
        GalleryCheckBox("Keep a copy on this device", checked = true, enabled = false) {}
    }
}

/**
 * Fluent's check box under a layer that toggles it with its state and names it for the web. Its
 * own clickable reports no state and ignores [enabled], and it keeps its interaction source to
 * itself, so it shows no hover under the layer.
 */
@Composable
internal fun GalleryCheckBox(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val component = if (checked) FluentGalleryComponent.CheckedCheckBox else FluentGalleryComponent.CheckBox
    FluentOverlaid(
        control = Modifier
            .previewRoles(enabled, component)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).foldedToggleName(label, checked, enabled),
    ) {
        CheckBox(checked = checked, label = label, enabled = enabled, onCheckStateChange = {})
    }
}

/**
 * Fluent's radio buttons, each under a layer that picks it with the radio role and names it for the web.
 */
@Composable
internal fun RadioButtons(state: DemoAppState) {
    val picked = state.choice(FluentGalleryKeys.Delivery, DeliveryOptions.size)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Gap)) {
        DeliveryOptions.forEachIndexed { index, option ->
            val enabled = index != DeliveryOptions.lastIndex
            val selected = picked == index
            val interactions = remember { MutableInteractionSource() }
            val component =
                if (selected) FluentGalleryComponent.SelectedRadioButton else FluentGalleryComponent.RadioButton
            FluentOverlaid(
                control = Modifier
                    .previewRoles(enabled, component)
                    .selectable(
                        selected = selected,
                        interactionSource = interactions,
                        indication = null,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { state.choose(FluentGalleryKeys.Delivery, DeliveryOptions.size, index) },
                    ).foldedChoiceName(option, selected, enabled),
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    label = option,
                    enabled = enabled,
                    interactionSource = interactions,
                )
            }
        }
    }
}

@Composable
internal fun ToggleSwitches(state: DemoAppState) {
    GalleryColumn {
        val on = state.isOn(FluentGalleryKeys.Wifi)
        GallerySwitch("Wi-Fi", on, enabled = true) { flipped -> state.setOn(FluentGalleryKeys.Wifi, flipped) }
        GallerySwitch("Airplane mode", on = false, enabled = false) {}
    }
}

/**
 * A setting's name and Fluent's switch, under a layer that toggles it with the switch role and
 * names it for the web. `Switcher` takes no modifier and reports itself as a plain button.
 */
@Composable
private fun GallerySwitch(
    label: String,
    on: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (on) FluentGalleryComponent.OnSwitch else FluentGalleryComponent.Switch
    FluentOverlaid(
        control = Modifier
            .previewRoles(enabled, component)
            .toggleable(
                value = on,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            ).foldedSwitchName(label, on, enabled),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val text = FluentTheme.colors.text.text
            Text(label, Modifier.weight(1f), color = if (enabled) text.primary else text.disabled)
            Switcher(
                checked = on,
                onCheckStateChange = {},
                text = if (on) SwitcherCopy.On else SwitcherCopy.Off,
                textBefore = true,
                enabled = enabled,
                interactionSource = interactions,
            )
        }
    }
}

/**
 * Fluent's segmented control, one choice of three with the last disabled. A segment reports no
 * role or selection, so its modifier adds both and the name carries them onto the web.
 */
@Composable
internal fun SegmentedControls(state: DemoAppState) {
    val picked = state.choice(FluentGalleryKeys.View, ViewOptions.size)
    SegmentedControl(Modifier.previewRoles(FluentGalleryComponent.SegmentedControl).selectableGroup()) {
        ViewOptions.forEachIndexed { index, option ->
            val enabled = index != ViewOptions.lastIndex
            val chosen = picked == index
            val component = if (chosen) FluentGalleryComponent.SelectedSegment else FluentGalleryComponent.Segment
            SegmentedButton(
                checked = chosen,
                onCheckedChanged = { state.choose(FluentGalleryKeys.View, ViewOptions.size, index) },
                modifier = Modifier
                    .previewRoles(enabled, component)
                    .semantics {
                        role = Role.RadioButton
                        selected = chosen
                    }.foldedChoiceName(option, chosen, enabled),
                enabled = enabled,
                position = when (index) {
                    0 -> SegmentedItemPosition.Start
                    ViewOptions.lastIndex -> SegmentedItemPosition.End
                    else -> SegmentedItemPosition.Center
                },
                text = { Text(option) },
            )
        }
    }
}
