package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Share2
import com.composeunstyled.CheckedIndicator
import com.composeunstyled.Indicator
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledCheckbox
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledProgress
import com.composeunstyled.UnstyledSwitch
import com.composeunstyled.SwitchThumb
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.kit.headless.InnerTextWithoutHandles
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.tabMovesFocus
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.drawTripScene
import androidx.compose.ui.semantics.Role as SemanticsRole

// The open trip, and the offline maps card that closes the list. The switch, the checkboxes, the
// progress bar and the field are drawn the way the gallery draws them.

private const val SelectionAlpha = 0.4f
private val ControlHeight = 36.dp
private val ProgressHeight = 6.dp
private val BoxSize = 20.dp
private val BoxShape = RoundedCornerShape(4.dp)
private val SwitchTrackWidth = 44.dp
private val SwitchTrackHeight = 24.dp
private val SwitchThumbSize = 16.dp

/**
 * The open trip, scene first and the note last.
 */
@Composable
internal fun UnstyledTripDetail(
    trip: Trip,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(TripsLayout.SectionGap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.SectionGap),
    ) {
        TripScene(Modifier.fillMaxWidth().height(TripsLayout.SceneHeight))
        Column(Modifier.padding(horizontal = TripsLayout.TextInset)) {
            Text(trip.name, style = ValueStyle, color = UnstyledToken.OnSurface.color)
            Text(trip.summary, style = BodyStyle, color = UnstyledToken.OnSurfaceVariant.color)
        }
        TripActions()
        DayPlan(trip)
        UnstyledHorizontalSeparator(
            UnstyledToken.OutlineVariant.color,
            Modifier.previewRoles(UnstyledGalleryComponent.Separator),
        )
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state)
        PackingCard(state)
        NoteCard(state)
    }
}

/**
 * A sky, a sun and three ridges in the scheme's colors, where a photo would go.
 */
@Composable
private fun TripScene(modifier: Modifier = Modifier) {
    val sky = UnstyledToken.PrimaryContainer.color
    val sun = UnstyledToken.TertiaryContainer.color
    val ridges = listOf(
        UnstyledToken.Secondary.color,
        UnstyledToken.Primary.color,
        UnstyledToken.OnPrimaryContainer.color,
    )
    Canvas(modifier.previewRoles(UnstyledComponent.Scene).clip(CardShape)) {
        drawTripScene(sky, sun, ridges)
    }
}

/**
 * Check in on primary, then the tonal and outlined buttons of the gallery, then the two lightest
 * actions as link buttons.
 */
@Composable
private fun TripActions() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        GalleryButton("Check in", GalleryButtonStyle.Filled, enabled = true, icon = Lucide.Check)
        GalleryButton("Share plan", GalleryButtonStyle.Tonal, enabled = true, icon = Lucide.Share2)
        GalleryButton("Edit", GalleryButtonStyle.Outlined, enabled = true, icon = Lucide.Pencil)
        LinkButton("Add to calendar", Lucide.Calendar)
        LinkButton("Directions", Lucide.MapPin)
    }
}

/**
 * The lightest button, its icon and label in primary with nothing behind them.
 */
@Composable
private fun LinkButton(
    label: String,
    icon: ImageVector,
) {
    val interactions = remember { MutableInteractionSource() }
    val content = UnstyledToken.Primary.color
    UnstyledButton(
        onClick = {},
        modifier = Modifier
            .height(ControlHeight)
            .previewRoles(UnstyledComponent.LinkButton)
            .galleryFocusRing(interactions)
            .clip(ControlShape),
        contentPadding = PaddingValues(horizontal = 12.dp),
        interactionSource = interactions,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap), verticalAlignment = Alignment.CenterVertically) {
            UnstyledIcon(icon, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
            Text(label, style = LabelStyle, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun DayPlan(trip: Trip) {
    val muted = UnstyledToken.OnSurfaceVariant.color
    Column(
        Modifier.padding(horizontal = TripsLayout.TextInset),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        if (trip.plan.isEmpty()) {
            Text("Nothing planned yet", style = BodyStyle, color = muted)
        } else {
            Text(
                text = "Day 1 · Friday",
                modifier = Modifier.previewRoles(UnstyledComponent.DayLabel),
                style = LabelStyle,
                color = UnstyledToken.Primary.color,
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
                        .previewRoles(UnstyledComponent.Stop)
                        .clip(CircleShape)
                        .background(UnstyledToken.SurfaceContainerHighest.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = LabelStyle, color = muted)
                }
                Text(stop.what, Modifier.weight(1f), style = BodyStyle, color = UnstyledToken.OnSurface.color)
                Text(stop.time, style = SmallStyle.copy(fontFamily = FontFamily.Monospace), color = muted)
            }
        }
    }
}

/**
 * An alert in the error container, offering to turn offline maps on.
 */
@Composable
private fun MapsNeedSignal(state: DemoAppState) {
    val content = UnstyledToken.OnErrorContainer.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.Alert)
            .clip(ControlShape)
            .background(UnstyledToken.ErrorContainer.color)
            .padding(start = TripsLayout.SectionGap, end = TripsLayout.Gap, top = TripsLayout.Gap, bottom = TripsLayout.Gap),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnstyledIcon(Lucide.CloudOff, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
        Text("Maps need signal until offline maps is on", Modifier.weight(1f), style = BodyStyle, color = content)
        val interactions = remember { MutableInteractionSource() }
        UnstyledButton(
            onClick = { state.setOn(OfflineMapsSwitch, true) },
            modifier = Modifier
                .height(ControlHeight)
                .previewRoles(UnstyledComponent.AlertButton)
                .galleryFocusRing(interactions, offset = true)
                .clip(ControlShape)
                .background(UnstyledToken.Error.color),
            contentPadding = PaddingValues(horizontal = 14.dp),
            interactionSource = interactions,
        ) {
            Text("Turn on", style = LabelStyle, color = UnstyledToken.OnError.color, maxLines = 1)
        }
    }
}

/**
 * A surface box at the [container] level, outlined the way the gallery's cards are.
 */
@Composable
private fun TripCard(
    container: UnstyledToken,
    component: UnstyledComponent,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(component)
            .clip(CardShape)
            .background(container.color)
            .border(1.dp, UnstyledToken.OutlineVariant.color, CardShape),
    ) {
        content()
    }
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    TripCard(UnstyledToken.SurfaceContainer, UnstyledComponent.PackingCard) {
        Column(Modifier.padding(TripsLayout.SectionGap), verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Packing", Modifier.weight(1f), style = HeadingStyle, color = UnstyledToken.OnSurface.color)
                Text("$packed of ${items.size}", style = SmallStyle, color = UnstyledToken.OnSurfaceVariant.color)
            }
            UnstyledProgress(
                progress = packed / items.size.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .previewRoles(UnstyledGalleryComponent.Progress)
                    .clip(PillShape)
                    .background(UnstyledToken.SurfaceContainerHighest.color),
            ) {
                Indicator(Modifier.clip(PillShape).background(UnstyledToken.Primary.color))
            }
            for (item in items) {
                PackingCheckbox(item.label, state.isChecked(item.key)) { ticked -> state.setChecked(item.key, ticked) }
            }
        }
    }
}

/**
 * A box that is ticked or not with its label beside it, a toggle whose name carries its state
 * onto the web, as the gallery draws one.
 */
@Composable
private fun PackingCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (checked) UnstyledGalleryComponent.CheckedCheckbox else UnstyledGalleryComponent.Checkbox
    val edge = if (checked) Modifier else Modifier.border(2.dp, UnstyledToken.Outline.color, BoxShape)
    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(component)
            .foldedToggleName(label, checked)
            .galleryFocusRing(interactions, offset = true),
        interactionSource = interactions,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(BoxSize)
                    .clip(BoxShape)
                    .background(if (checked) UnstyledToken.Primary.color else Color.Transparent)
                    .then(edge),
                contentAlignment = Alignment.Center,
            ) {
                CheckedIndicator {
                    UnstyledIcon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = UnstyledToken.OnPrimary.color,
                    )
                }
            }
            Text(label, style = BodyStyle, color = UnstyledToken.OnSurface.color)
        }
    }
}

@Composable
private fun NoteCard(state: DemoAppState) {
    TripCard(UnstyledToken.SurfaceContainerLowest, UnstyledComponent.NoteCard) {
        NoteField(
            value = state.text,
            onValueChange = { text -> state.text = text },
            modifier = Modifier.padding(TripsLayout.SectionGap),
        )
    }
}

/**
 * The note, a single line field on the gallery's filled look. Its inner text goes through the kit's
 * [InnerTextWithoutHandles], so on the web a long press puts up no selection handles, which would
 * take the page's accessibility mirror over. One line for the same reason, with [tabMovesFocus]
 * kept for when it goes back to several.
 */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val primary = UnstyledToken.Primary.color
    val accent = if (focused) primary else UnstyledToken.OnSurfaceVariant.color
    val selection = TextSelectionColors(handleColor = primary, backgroundColor = primary.copy(alpha = SelectionAlpha))
    CompositionLocalProvider(LocalTextSelectionColors provides selection) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .tabMovesFocus(LocalFocusManager.current)
                .previewRoles(UnstyledGalleryComponent.TextField),
            textStyle = BodyStyle.copy(color = UnstyledToken.OnSurface.color),
            cursorBrush = SolidColor(primary),
            interactionSource = interactions,
            singleLine = true,
            decorationBox = { innerTextField ->
                Column(Modifier.clip(ControlShape).background(UnstyledToken.SurfaceContainerHighest.color)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = TripsLayout.Gap)) {
                        Text("Note for the group", style = SmallStyle, color = accent)
                        InnerTextWithoutHandles(innerTextField)
                    }
                    Box(Modifier.fillMaxWidth().height(if (focused) 2.dp else 1.dp).background(accent))
                }
            },
        )
    }
}

/**
 * The offline maps switch on a surface box at the lowest level, a setting for every trip. The whole
 * row is the switch, whose name carries its state onto the web.
 */
@Composable
internal fun OfflineMaps(state: DemoAppState) {
    val on = state.isOn(OfflineMapsSwitch)
    val interactions = remember { MutableInteractionSource() }
    val component = if (on) UnstyledGalleryComponent.CheckedSwitch else UnstyledGalleryComponent.Switch
    val (track, thumb) = if (on) {
        UnstyledToken.Primary.color to UnstyledToken.OnPrimary.color
    } else {
        UnstyledToken.SurfaceContainerHighest.color to UnstyledToken.Outline.color
    }
    TripCard(
        container = UnstyledToken.SurfaceContainerLowest,
        component = UnstyledComponent.OfflineMapsCard,
        modifier = Modifier.padding(top = TripsLayout.Gap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .previewRoles(component)
                .toggleable(
                    value = on,
                    interactionSource = interactions,
                    indication = null,
                    role = SemanticsRole.Switch,
                ) { switched -> state.setOn(OfflineMapsSwitch, switched) }
                .foldedSwitchName("Offline maps", on)
                .galleryFocusRing(interactions)
                .padding(horizontal = TripsLayout.SectionGap, vertical = TripsLayout.PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Offline maps", style = BodyStyle, color = UnstyledToken.OnSurface.color)
                Text("Maps work without signal", style = SmallStyle, color = UnstyledToken.OnSurfaceVariant.color)
            }
            UnstyledSwitch(
                checked = on,
                onCheckedChange = null,
                modifier = Modifier
                    .size(SwitchTrackWidth, SwitchTrackHeight)
                    .clip(PillShape)
                    .background(track)
                    .then(if (on) Modifier else Modifier.border(1.dp, thumb, PillShape))
                    .padding(4.dp),
            ) {
                SwitchThumb(
                    modifier = Modifier.size(SwitchThumbSize).clip(CircleShape).background(thumb),
                    animationSpec = panelMotion(),
                )
            }
        }
    }
}
