package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderCheckbox
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderProgress
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.tabMovesFocus
import com.materialkolor.builder.preview.inspect.previewRoles
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.drawTripScene

/**
 * The open trip, scene first and the note last.
 */
@Composable
internal fun CustomTripDetail(
    trip: Trip,
    state: DemoAppState,
    colors: CustomColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(TripsLayout.SectionGap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.SectionGap),
    ) {
        TripScene(colors, Modifier.fillMaxWidth().height(TripsLayout.SceneHeight))
        Column(Modifier.padding(horizontal = TripsLayout.TextInset)) {
            BuilderText(trip.name, style = BuilderTextStyle.Title)
            BuilderText(trip.summary, emphasis = Emphasis.Secondary)
        }
        TripActions()
        DayPlan(trip, colors)
        BuilderDivider(Modifier.previewRoles(CustomComponent.Divider))
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state, colors)
        PackingCard(state)
        NoteCard(state)
    }
}

/**
 * A sky, a sun and three ridges in the pane's slots, where a photo would go.
 */
@Composable
private fun TripScene(
    colors: CustomColors,
    modifier: Modifier = Modifier,
) {
    val sky = colors.slot(CustomSlot.PrimaryContainer)
    val sun = colors.slot(CustomSlot.TertiaryContainer)
    val ridges = listOf(
        colors.slot(CustomSlot.Secondary),
        colors.slot(CustomSlot.Primary),
        colors.slot(CustomSlot.OnPrimaryContainer),
    )
    val painted = listOf(sky, sun) + ridges
    Canvas(modifier.clip(customPanelShape()).previewRoles(*painted.map { ink -> ink.ref }.toTypedArray())) {
        drawTripScene(sky.color, sun.color, ridges.map { ink -> ink.color })
    }
}

/**
 * Check in as the primary action, Share plan and Edit as secondary ones, and the two lightest as
 * subtle buttons, the kit's nearest to Material's assist chips.
 */
@Composable
private fun TripActions() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        BuilderButton(
            onClick = {},
            label = "Check in",
            modifier = Modifier.previewRoles(Emphasis.Primary.component),
            emphasis = Emphasis.Primary,
            icon = IconId.Check,
        )
        BuilderButton(
            onClick = {},
            label = "Share plan",
            modifier = Modifier.previewRoles(Emphasis.Secondary.component),
            icon = IconId.Share,
            tonal = true,
        )
        BuilderButton(onClick = {}, label = "Edit", modifier = Modifier.previewRoles(Emphasis.Secondary.component))
        for (label in listOf("Add to calendar", "Directions")) {
            BuilderButton(
                onClick = {},
                label = label,
                modifier = Modifier.previewRoles(Emphasis.Subtle.component),
                emphasis = Emphasis.Subtle,
            )
        }
    }
}

@Composable
private fun DayPlan(
    trip: Trip,
    colors: CustomColors,
) {
    val day = colors.slot(CustomSlot.Primary)
    val stop = colors.pair(CustomSlot.SurfaceRaised, CustomSlot.TextMuted)
    Column(
        Modifier.padding(horizontal = TripsLayout.TextInset),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        if (trip.plan.isEmpty()) {
            BuilderText("Nothing planned yet", emphasis = Emphasis.Secondary)
        } else {
            BuilderText(
                text = "Day 1 · Friday",
                modifier = Modifier.previewRoles(day.ref),
                style = BuilderTextStyle.Label,
                color = day.color,
            )
        }
        trip.plan.forEachIndexed { index, planned ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
            ) {
                Box(
                    modifier = Modifier
                        .size(TripsLayout.StopSize)
                        .clip(CircleShape)
                        .background(stop.fill.color)
                        .previewRoles(stop),
                    contentAlignment = Alignment.Center,
                ) {
                    BuilderText("${index + 1}", style = BuilderTextStyle.Label, color = stop.ink.color)
                }
                BuilderText(planned.what, Modifier.weight(1f))
                BuilderText(planned.time, style = BuilderTextStyle.Code, emphasis = Emphasis.Secondary)
            }
        }
    }
}

/**
 * A banner in the error container, offering to turn offline maps on. Drawn in place, never as a toast.
 */
@Composable
private fun MapsNeedSignal(
    state: DemoAppState,
    colors: CustomColors,
) {
    val banner = colors.pair(CustomSlot.ErrorContainer, CustomSlot.OnErrorContainer)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(banner)
            .background(banner.fill.color, customPanelShape())
            .padding(
                start = TripsLayout.SectionGap,
                top = TripsLayout.Gap,
                end = TripsLayout.Gap,
                bottom = TripsLayout.Gap,
            ),
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TripGlyph(Lucide.CloudOff, banner.ink.color)
        BuilderText("Maps need signal until offline maps is on", Modifier.weight(1f), color = banner.ink.color)
        BuilderButton(
            onClick = { state.setOn(OfflineMapsSwitch, true) },
            label = "Turn on",
            modifier = Modifier.previewRoles(Emphasis.Danger.component),
            emphasis = Emphasis.Danger,
        )
    }
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    BuilderCard(Modifier.fillMaxWidth().previewRoles(CustomComponent.SampleCard)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BuilderText("Packing", Modifier.weight(1f), style = BuilderTextStyle.Label)
            BuilderText("$packed of ${items.size}", emphasis = Emphasis.Secondary)
        }
        BuilderProgress(
            label = "Packed",
            modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.Progress),
            progress = packed / items.size.toFloat(),
        )
        for (item in items) {
            BuilderCheckbox(
                checked = state.isChecked(item.key),
                onCheckedChange = { ticked -> state.setChecked(item.key, ticked) },
                label = item.label,
                modifier = Modifier.previewRoles(CustomComponent.Checkbox),
            )
        }
    }
}

/**
 * The note on a kit card. The kit's field is one line, and commits when someone leaves it or
 * presses Enter, as the gallery's fields do.
 */
@Composable
private fun NoteCard(state: DemoAppState) {
    BuilderCard(Modifier.fillMaxWidth().previewRoles(CustomComponent.Card)) {
        BuilderTextField(
            value = state.text,
            onCommit = { text -> state.text = text },
            label = "Note for the group",
            modifier = Modifier
                .fillMaxWidth()
                .tabMovesFocus(LocalFocusManager.current)
                .previewRoles(CustomComponent.TextField),
        )
    }
}
