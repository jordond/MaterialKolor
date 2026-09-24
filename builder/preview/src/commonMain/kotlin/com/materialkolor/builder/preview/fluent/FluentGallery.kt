package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.Layer
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.BasicSlider
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.SliderDefaults
import io.github.composefluent.component.SliderState
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.component.TextFieldDefaults
import io.github.composefluent.component.ToggleButton
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Edit
import io.github.composefluent.icons.regular.Share
import io.github.composefluent.scheme.collectVisualState
import kotlin.math.roundToInt

// The samples of the Actions and Inputs cards of FluentCards in GalleryEntry.kt, which keeps the
// Selection ones with GallerySelection.kt and the rest with GalleryPanels.kt. Everything a sample
// remembers lives in DemoAppState under a "gallery.fluent." key, so both copies of a split agree.

/** The stops on the slider, 0 to 10. */
private const val SliderStops = 11
private const val SliderTop = SliderStops - 1

/** The slider's thumb, its ring included, and the dot inside it at rest, under a pointer and pressed. */
private val ThumbSize = 22.dp
private val ThumbDot = 12.dp
private val ThumbDotHovered = 14.dp
private val ThumbDotPressed = 10.dp

@Composable
internal fun StandardButtons() {
    EnabledAndDisabled { enabled ->
        Button(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.Button),
            disabled = !enabled,
        ) { Text("Save") }
    }
}

@Composable
internal fun AccentButtons() {
    EnabledAndDisabled { enabled ->
        AccentButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.AccentButton),
            disabled = !enabled,
        ) { Text("Send") }
    }
}

@Composable
internal fun SubtleButtons() {
    EnabledAndDisabled { enabled ->
        SubtleButton(
            onClick = {},
            modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.SubtleButton),
            disabled = !enabled,
        ) { Text("Cancel") }
    }
}

/** A standard icon button and two subtle ones, a row of them enabled and a row disabled. */
@Composable
internal fun IconButtons() {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                Button(
                    onClick = {},
                    modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.Button),
                    disabled = !enabled,
                    iconOnly = true,
                ) { Icon(Icons.Regular.Share, contentDescription = "Share") }
                SubtleButton(
                    onClick = {},
                    modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.SubtleButton),
                    disabled = !enabled,
                    iconOnly = true,
                ) { Icon(Icons.Regular.Edit, contentDescription = "Edit") }
                SubtleButton(
                    onClick = {},
                    modifier = Modifier.previewRoles(enabled, FluentGalleryComponent.SubtleButton),
                    disabled = !enabled,
                    iconOnly = true,
                ) { Icon(Icons.Regular.Delete, contentDescription = "Delete") }
            }
        }
    }
}

/**
 * A button that stays pressed in while it is on, painted with the accent fill then. It reports
 * itself as a checkbox but not whether it is checked, so its modifier adds that, and the name
 * carries it onto the web.
 */
@Composable
internal fun ToggleButtons(state: DemoAppState) {
    val checked = state.isOn(FluentGalleryKeys.Bold)
    EnabledAndDisabled { enabled ->
        val component = if (checked) FluentGalleryComponent.CheckedToggleButton else FluentGalleryComponent.ToggleButton
        ToggleButton(
            checked = checked,
            onCheckedChanged = { on -> state.setOn(FluentGalleryKeys.Bold, on) },
            modifier = Modifier
                .previewRoles(enabled, component)
                .foldedToggleName("Bold", checked, enabled)
                .semantics { toggleableState = ToggleableState(checked) },
            disabled = !enabled,
        ) { Text("Bold") }
    }
}

@Composable
internal fun TextBoxes(state: DemoAppState) {
    GalleryColumn {
        GalleryTextBox(
            value = state.text,
            onValueChange = { typed -> state.text = typed },
            header = "Display name",
            placeholder = "Your name",
            enabled = true,
        )
        GalleryTextBox(
            value = "ana@example.com",
            onValueChange = {},
            header = "Email",
            placeholder = "",
            enabled = false,
        )
    }
}

/**
 * A single line text box with Fluent's own decoration, built from the foundation field so its
 * inner text goes through the kit's [InnerTextWithoutHandles]. On the web a long press on its text
 * then puts up no selection handles, which would take the page's accessibility mirror over (D45).
 * Fluent's own `TextField` keeps its inner text to itself, so it has no place for that.
 */
@Composable
private fun GalleryTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    header: String,
    placeholder: String,
    enabled: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults
        .defaultTextFieldColors()
        .schemeFor(interactions.collectVisualState(!enabled, focusFirst = true))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().previewRoles(enabled, FluentGalleryComponent.TextBox),
        enabled = enabled,
        textStyle = FluentTheme.typography.body.copy(color = colors.contentColor),
        cursorBrush = colors.cursorBrush,
        interactionSource = interactions,
        singleLine = true,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                interactionSource = interactions,
                enabled = enabled,
                color = colors,
                shape = FluentTheme.shapes.control,
                header = { Text(header) },
                placeholder = if (placeholder.isEmpty()) null else ({ Text(placeholder) }),
                leadingIcon = null,
                trailing = null,
                innerTextField = { InnerTextWithoutHandles(innerTextField) },
            )
        },
    )
}

@Composable
internal fun Sliders(state: DemoAppState) {
    val stop = state.choice(FluentGalleryKeys.Volume, SliderStops, default = 6)
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            GallerySlider(
                label = "Volume",
                stop = stop,
                enabled = enabled,
                onStop = { next -> state.choose(FluentGalleryKeys.Volume, SliderStops, next.coerceIn(0, SliderTop)) },
            )
        }
    }
}

/**
 * Fluent's slider on its own rail and track, with a thumb drawn here. Fluent's thumb opens its
 * value tip in a popup while it is dragged, so it stays out (D40).
 *
 * Fluent's slider only moves its thumb for a drag, so the value is taken when the drag ends and the
 * slider starts afresh at each new stop. That also moves it when the other copy of a split or a key
 * sets the stop. It carries no semantics of its own either, so the box around it takes focus, the
 * arrow keys and the value a screen reader reads and sets.
 */
@Composable
private fun GallerySlider(
    label: String,
    stop: Int,
    enabled: Boolean,
    onStop: (Int) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .previewRoles(enabled, FluentGalleryComponent.Slider)
            .semantics {
                contentDescription = label
                stateDescription = stop.toString()
                progressBarRangeInfo = ProgressBarRangeInfo(stop.toFloat(), 0f..SliderTop.toFloat(), SliderStops - 2)
                if (enabled) {
                    setProgress { value ->
                        onStop(value.roundToInt())
                        true
                    }
                } else {
                    disabled()
                }
            }
            // Ahead of the focus target, so the keys it hears reach it.
            .onKeyEvent { event ->
                val step = when (event.key) {
                    Key.DirectionRight, Key.DirectionUp -> 1
                    Key.DirectionLeft, Key.DirectionDown -> -1
                    else -> 0
                }
                val handled = enabled && step != 0 && event.type == KeyEventType.KeyDown
                if (handled) onStop(stop + step)
                handled
            }.focusable(enabled, interactions),
    ) {
        key(stop) {
            BasicSlider(
                value = stop.toFloat(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                valueRange = 0f..SliderTop.toFloat(),
                steps = SliderStops - 2,
                onValueChangeFinished = { value -> onStop(value.roundToInt()) },
                interactionSource = interactions,
                rail = { slider -> SliderDefaults.Rail(slider, enabled = enabled) },
                track = { slider -> SliderDefaults.Track(slider, enabled = enabled) },
                thumb = { slider -> SliderThumb(slider, enabled, interactions) },
            )
        }
    }
}

/** Fluent's thumb, a ring round an accent dot that grows under a pointer, with no value tip. */
@Composable
private fun SliderThumb(
    slider: SliderState,
    enabled: Boolean,
    interactions: MutableInteractionSource,
) {
    val colors = FluentTheme.colors
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val dot = when {
        pressed || slider.isDragging -> ThumbDotPressed
        hovered -> ThumbDotHovered
        else -> ThumbDot
    }
    val fill = when {
        !enabled -> colors.fillAccent.disabled
        pressed || slider.isDragging -> colors.fillAccent.tertiary
        else -> colors.fillAccent.default
    }
    Layer(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = (constraints.maxWidth - placeable.width) * slider.rawFraction
                    placeable.place(x.roundToInt(), (constraints.maxHeight - placeable.height) / 2)
                }
            }.requiredSize(ThumbSize)
            .hoverable(interactions, enabled),
        shape = CircleShape,
        color = colors.controlSolid.default,
        border = BorderStroke(1.dp, colors.borders.circle),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(dot).background(fill, CircleShape))
        }
    }
}
