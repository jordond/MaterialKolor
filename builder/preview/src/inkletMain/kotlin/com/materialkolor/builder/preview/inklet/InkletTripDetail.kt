package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.tabMovesFocus
import com.materialkolor.builder.preview.material.previewRoles
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.Trip
import com.materialkolor.builder.preview.trips.TripsLayout
import com.materialkolor.builder.preview.trips.drawTripScene
import dev.ggoggam.inklet.InkletDecoration
import dev.ggoggam.inklet.material3.InkletAssistChip
import dev.ggoggam.inklet.material3.InkletButton
import dev.ggoggam.inklet.material3.InkletCard
import dev.ggoggam.inklet.material3.InkletCheckbox
import dev.ggoggam.inklet.material3.InkletDivider
import dev.ggoggam.inklet.material3.InkletLinearProgressIndicator
import dev.ggoggam.inklet.material3.InkletTextField
import dev.ggoggam.inklet.material3.InkletVariant
import dev.ggoggam.inklet.material3.inkletBorder
import dev.ggoggam.inklet.material3.inkletDecoration
import dev.ggoggam.inklet.material3.inkletSurface

/**
 * The open trip, scene first and the notes last, as in Material's Trips.
 */
@Composable
internal fun InkletTripDetail(
    trip: Trip,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.padding(TripsLayout.SectionGap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.SectionGap),
    ) {
        TripScene(Modifier.fillMaxWidth().height(TripsLayout.SceneHeight))
        Column(Modifier.padding(horizontal = TripsLayout.TextInset)) {
            Text(trip.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(trip.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        TripActions()
        DayPlan(trip)
        InkletDivider(Modifier.previewRoles(InkletComponent.Divider), seed = DetailSeed)
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state)
        PackingCard(state)
        NoteCard(state)
    }
}

/**
 * The same sky, sun and ridges as Material's scene, framed in pen.
 */
@Composable
private fun TripScene(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val sky = colors.primaryContainer
    val sun = colors.tertiaryContainer
    val ridges = listOf(colors.secondary, colors.primary, colors.onPrimaryContainer)
    Canvas(
        modifier
            .inkletBorder(color = colors.onPrimaryContainer, seed = DetailSeed + 1)
            .clip(InkletRowShape)
            .previewRoles(
                Role.PrimaryContainer,
                Role.TertiaryContainer,
                Role.Secondary,
                Role.Primary,
                Role.OnPrimaryContainer,
            ),
    ) {
        drawTripScene(sky, sun, ridges)
    }
}

@Composable
private fun TripActions() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(InkletComponent.SolidButton),
            seed = ActionSeed,
        ) { Text("Check in") }
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(Role.SecondaryContainer, Role.OnSecondaryContainer),
            variant = InkletVariant.Scribble,
            colors = ButtonDefaults.filledTonalButtonColors(),
            seed = ActionSeed + 1,
        ) { Text("Share plan") }
        InkletButton(
            onClick = {},
            modifier = Modifier.previewRoles(InkletComponent.OutlineButton),
            variant = InkletVariant.Outline,
            seed = ActionSeed + 2,
        ) { Text("Edit") }
        InkletAssistChip(
            onClick = {},
            label = { Text("Add to calendar") },
            modifier = Modifier.previewRoles(InkletComponent.AssistChip),
            leadingIcon = {
                Icon(Lucide.Calendar, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
            seed = ActionSeed + 3,
        )
        InkletAssistChip(
            onClick = {},
            label = { Text("Directions") },
            modifier = Modifier.previewRoles(InkletComponent.AssistChip),
            leadingIcon = {
                Icon(Lucide.MapPin, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
            seed = ActionSeed + 4,
        )
    }
}

@Composable
private fun DayPlan(trip: Trip) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier.padding(horizontal = TripsLayout.TextInset),
        verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap),
    ) {
        if (trip.plan.isEmpty()) {
            Text("Nothing planned yet", color = colors.onSurfaceVariant, style = type.bodyMedium)
        } else {
            Text(
                text = "Day 1 · Friday",
                modifier = Modifier
                    .inkletDecoration(InkletDecoration.Highlight, seed = PlanSeed)
                    .previewRoles(Role.Primary),
                color = colors.primary,
                style = type.labelLarge,
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
                        .inkletSurface(
                            containerColor = colors.surfaceContainerHighest,
                            ink = colors.onSurfaceVariant,
                            cornerRadius = TripsLayout.StopSize / 2,
                            seed = PlanSeed + 1 + index,
                        ).previewRoles(Role.SurfaceContainerHighest, Role.OnSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = colors.onSurfaceVariant, style = type.labelMedium)
                }
                Text(stop.what, Modifier.weight(1f), style = type.bodyLarge)
                Text(
                    text = stop.time,
                    color = colors.onSurfaceVariant,
                    style = type.labelMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

/**
 * A sketched note in the error container, offering to turn offline maps on.
 */
@Composable
private fun MapsNeedSignal(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onErrorContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .inkletSurface(
                    containerColor = colors.errorContainer,
                    ink = colors.onErrorContainer,
                    seed = SignalSeed,
                ).previewRoles(Role.ErrorContainer, Role.OnErrorContainer)
                .padding(start = TripsLayout.SectionGap, end = TripsLayout.Gap)
                .padding(vertical = TripsLayout.Gap),
            horizontalArrangement = Arrangement.spacedBy(TripsLayout.PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.CloudOff, contentDescription = null)
            Text(
                "Maps need signal until offline maps is on",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            InkletButton(
                onClick = { state.setOn(OfflineMapsSwitch, true) },
                modifier = Modifier.previewRoles(Role.OnErrorContainer),
                variant = InkletVariant.Outline,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onErrorContainer),
                seed = SignalSeed + 1,
            ) { Text("Turn on") }
        }
    }
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    InkletCard(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.SurfaceContainer, Role.OnSurface, Role.OutlineVariant),
        containerColor = colors.surfaceContainer,
        seed = PackingSeed,
    ) {
        Column(Modifier.padding(TripsLayout.SectionGap), verticalArrangement = Arrangement.spacedBy(TripsLayout.Gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Packing", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "$packed of ${items.size}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            InkletLinearProgressIndicator(
                progress = { packed / items.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Packed" }
                    .previewRoles(InkletComponent.Progress),
                seed = PackingSeed + 1,
            )
            for (item in items) {
                val checked = state.isChecked(item.key)
                LabelledControl(item.label, enabled = true, onClick = { state.setChecked(item.key, !checked) }) {
                    InkletCheckbox(
                        checked = checked,
                        onCheckedChange = { ticked -> state.setChecked(item.key, ticked) },
                        modifier = Modifier
                            .semantics { contentDescription = item.label }
                            .previewRoles(InkletComponent.Control),
                        seed = PackingSeed + 2 + item.ordinal,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(state: DemoAppState) {
    InkletCard(
        modifier = Modifier.fillMaxWidth().previewRoles(
            Role.SurfaceContainerLowest,
            Role.OnSurface,
            Role.OutlineVariant,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        seed = NoteSeed,
    ) {
        InkletTextField(
            value = state.text,
            onValueChange = { text -> state.text = text },
            modifier = Modifier
                .padding(TripsLayout.SectionGap)
                .fillMaxWidth()
                .tabMovesFocus(LocalFocusManager.current)
                .previewRoles(InkletComponent.TextField),
            label = { Text("Note for the group") },
            seed = NoteSeed + 1,
        )
    }
}

// The seed of each sketch, one range per part so no two parts draw alike.
private const val DetailSeed = 600
private const val ActionSeed = 610
private const val PlanSeed = 620
private const val SignalSeed = 630
private const val PackingSeed = 640
private const val NoteSeed = 650
