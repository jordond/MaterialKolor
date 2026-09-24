package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composeunstyled.CheckedIndicator
import com.composeunstyled.RadioButton
import com.composeunstyled.SelectedIndicator
import com.composeunstyled.SwitchThumb
import com.composeunstyled.Text
import com.composeunstyled.UnstyledCheckbox
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledRadioGroup
import com.composeunstyled.UnstyledSlider
import com.composeunstyled.UnstyledSwitch
import com.composeunstyled.focusRing
import com.materialkolor.builder.kit.a11y.foldsValueIntoName
import com.materialkolor.builder.kit.a11y.sliderRoleWord
import com.materialkolor.builder.kit.a11y.valueNodeName
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role as SemanticsRole

// The samples of the Inputs and Selection cards, but for the select and the menu, which
// GalleryPanels.kt keeps.

private const val SliderStops = 11
private const val SelectionAlpha = 0.4f
private val SliderHeight = 24.dp
private val ThumbSize = 18.dp
private val BoxSize = 20.dp
private val BoxShape = RoundedCornerShape(4.dp)
private val SwitchTrackWidth = 44.dp
private val SwitchTrackHeight = 24.dp
private val SwitchThumbSize = 16.dp
private val Plans = listOf("Free", "Team", "Enterprise")

@Composable
internal fun TextFields(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            GalleryTextField(
                value = state.text,
                onValueChange = { text -> state.text = text },
                label = "Project name",
                placeholder = "Harbour",
                enabled = enabled,
            )
        }
    }
}

/**
 * A single line field on the gallery's own filled look, built from the foundation field and a
 * decoration of the gallery's own, so its inner text goes through the kit's
 * [InnerTextWithoutHandles]. On the web a long press on its text then puts up no selection
 * handles, which would take the page's accessibility mirror over (D45). Compose Unstyled's own
 * field keeps its inner text to itself, so it has no place for that.
 */
@Composable
private fun GalleryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val primary = DashboardToken.Primary.color
    val accent = when {
        !enabled -> disabledContent
        focused -> primary
        else -> DashboardToken.OnSurfaceVariant.color
    }
    val container = if (enabled) DashboardToken.SurfaceContainerHighest.color else disabledContainer
    val selection = TextSelectionColors(handleColor = primary, backgroundColor = primary.copy(alpha = SelectionAlpha))
    CompositionLocalProvider(LocalTextSelectionColors provides selection) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().previewRoles(enabled, UnstyledGalleryComponent.TextField),
            enabled = enabled,
            textStyle = BodyStyle.copy(color = tint(DashboardToken.OnSurface, enabled)),
            cursorBrush = SolidColor(primary),
            interactionSource = interactions,
            singleLine = true,
            decorationBox = { innerTextField ->
                Column(Modifier.clip(ControlShape).background(container)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = Gap)) {
                        Text(label, style = SmallStyle, color = accent)
                        Box {
                            if (value.isEmpty()) {
                                val hint = tint(DashboardToken.OnSurfaceVariant, enabled)
                                Text(placeholder, style = BodyStyle, color = hint)
                            }
                            InnerTextWithoutHandles(innerTextField)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(if (focused) 2.dp else 1.dp).background(accent))
                }
            },
        )
    }
}

@Composable
internal fun Sliders(state: DemoAppState) {
    val stop = state.choice(GalleryKeys.Volume, SliderStops, default = 6)
    val folds = foldsValueIntoName
    val roleWord = sliderRoleWord
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            val interactions = remember { MutableInteractionSource() }
            val active = tint(DashboardToken.Primary, enabled)
            val rest = if (enabled) DashboardToken.SurfaceContainerHighest.color else disabledContainer
            UnstyledSlider(
                value = stop.toFloat(),
                onValueChange = { value ->
                    state.choose(GalleryKeys.Volume, SliderStops, value.roundToInt().coerceIn(0, SliderStops - 1))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SliderHeight)
                    .previewRoles(enabled, UnstyledGalleryComponent.Slider)
                    .semantics { valueNodeName("Volume", "$stop", folds, roleWord) },
                enabled = enabled,
                interactionSource = interactions,
                valueRange = 0f..(SliderStops - 1).toFloat(),
                steps = SliderStops - 2,
                track = { slider ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(PillShape)
                            .background(rest),
                    ) {
                        Box(Modifier.fillMaxWidth(slider.fraction).fillMaxHeight().background(active))
                    }
                },
                thumb = {
                    Box(
                        Modifier
                            .size(ThumbSize)
                            .focusRing(interactions, 2.dp, DashboardToken.Primary.color, CircleShape, offset = 2.dp)
                            .clip(CircleShape)
                            .background(active),
                    )
                },
            )
        }
    }
}

@Composable
internal fun Checkboxes(state: DemoAppState) {
    GalleryColumn {
        val checked = state.isChecked(GalleryKeys.Newsletter)
        GalleryCheckbox("Email me the newsletter", checked, enabled = true) { on ->
            state.setChecked(GalleryKeys.Newsletter, on)
        }
        GalleryCheckbox("Keep a copy on this device", checked = true, enabled = false) {}
    }
}

/** A box that is ticked or not, with its label beside it, a toggle whose name carries its state onto the web. */
@Composable
private fun GalleryCheckbox(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (checked) UnstyledGalleryComponent.CheckedCheckbox else UnstyledGalleryComponent.Checkbox
    val box = when {
        !checked -> Color.Transparent
        enabled -> DashboardToken.Primary.color
        else -> disabledContainer
    }
    val edge = when {
        checked -> Modifier
        enabled -> Modifier.border(2.dp, DashboardToken.Outline.color, BoxShape)
        else -> Modifier.border(2.dp, disabledContent, BoxShape)
    }
    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier
            .previewRoles(enabled, component)
            .foldedToggleName(label, checked, enabled)
            .galleryFocusRing(interactions, offset = true),
        enabled = enabled,
        interactionSource = interactions,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(BoxSize)
                    .clip(BoxShape)
                    .background(box)
                    .then(edge),
                contentAlignment = Alignment.Center,
            ) {
                CheckedIndicator {
                    val mark = tint(DashboardToken.OnPrimary, enabled)
                    UnstyledIcon(Lucide.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = mark)
                }
            }
            Text(label, style = BodyStyle, color = tint(DashboardToken.OnSurface, enabled))
        }
    }
}

@Composable
internal fun Switches(state: DemoAppState) {
    GalleryColumn {
        GallerySwitch("Wi-Fi", state.isOn(GalleryKeys.Wifi), enabled = true) { on -> state.setOn(GalleryKeys.Wifi, on) }
        GallerySwitch("Airplane mode", on = true, enabled = false) {}
    }
}

/**
 * A switch with its label before it, the whole row one control, whose name carries its state onto
 * the web.
 */
@Composable
private fun GallerySwitch(
    label: String,
    on: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (on) UnstyledGalleryComponent.CheckedSwitch else UnstyledGalleryComponent.Switch
    val (track, thumb) = when {
        !enabled -> disabledContainer to disabledContent
        on -> DashboardToken.Primary.color to DashboardToken.OnPrimary.color
        else -> DashboardToken.SurfaceContainerHighest.color to DashboardToken.Outline.color
    }
    val edge = if (on) Modifier else Modifier.border(1.dp, thumb, PillShape)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(enabled, component)
            .toggleable(
                value = on,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = SemanticsRole.Switch,
                onValueChange = onChange,
            ).foldedSwitchName(label, on, enabled)
            .galleryFocusRing(interactions, offset = true),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = BodyStyle, color = tint(DashboardToken.OnSurface, enabled))
        UnstyledSwitch(
            checked = on,
            onCheckedChange = null,
            modifier = Modifier
                .size(SwitchTrackWidth, SwitchTrackHeight)
                .clip(PillShape)
                .background(track)
                .then(edge)
                .padding(4.dp),
            enabled = enabled,
        ) {
            SwitchThumb(
                modifier = Modifier.size(SwitchThumbSize).clip(CircleShape).background(thumb),
                animationSpec = panelMotion(),
            )
        }
    }
}

@Composable
internal fun RadioButtons(state: DemoAppState) {
    val picked = state.choice(GalleryKeys.Plan, Plans.size)
    UnstyledRadioGroup(
        value = picked,
        onValueChange = { index -> state.choose(GalleryKeys.Plan, Plans.size, index) },
        accessibilityLabel = "Plan",
    ) {
        GalleryColumn {
            Plans.forEachIndexed { index, plan ->
                // The last plan is out of reach, the disabled look.
                val enabled = index != Plans.lastIndex
                val selected = index == picked
                val interactions = remember { MutableInteractionSource() }
                val component = if (selected) {
                    UnstyledGalleryComponent.SelectedRadioButton
                } else {
                    UnstyledGalleryComponent.RadioButton
                }
                val ring = when {
                    !enabled -> disabledContent
                    selected -> DashboardToken.Primary.color
                    else -> DashboardToken.Outline.color
                }
                RadioButton(
                    value = index,
                    modifier = Modifier
                        .previewRoles(enabled, component)
                        .foldedChoiceName(plan, selected, enabled)
                        .galleryFocusRing(interactions, offset = true),
                    enabled = enabled,
                    interactionSource = interactions,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(BoxSize).border(2.dp, ring, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            SelectedIndicator {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(ring))
                            }
                        }
                        Text(plan, style = BodyStyle, color = tint(DashboardToken.OnSurface, enabled))
                    }
                }
            }
        }
    }
}
