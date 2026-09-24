package com.materialkolor.builder.preview.material

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.tabMovesFocus
import androidx.compose.ui.semantics.Role as SemanticsRole

private val SceneHeight = 184.dp
private val StopSize = 36.dp

/** How far the trip's text sits in from the scene and the buttons. */
private val TextInset = 4.dp

/** The open trip, scene first and the notes last. */
@Composable
internal fun TripDetail(
    trip: Trip,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.padding(SectionGap), verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        TripScene(Modifier.fillMaxWidth().height(SceneHeight))
        Column(Modifier.padding(horizontal = TextInset)) {
            Text(trip.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(trip.summary, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        TripActions()
        DayPlan(trip)
        HorizontalDivider(Modifier.previewRoles(MaterialComponent.HorizontalDivider))
        if (!state.isOn(OfflineMapsSwitch)) MapsNeedSignal(state)
        PackingCard(state)
        NoteCard(state)
    }
}

/** A sky, a sun and three ridges in the scheme's colors, where a photo would go. */
@Composable
private fun TripScene(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val sky = colors.primaryContainer
    val sun = colors.tertiaryContainer
    val ridges = listOf(colors.secondary, colors.primary, colors.onPrimaryContainer)
    Canvas(
        modifier
            .clip(RowShape)
            .previewRoles(
                Role.PrimaryContainer,
                Role.TertiaryContainer,
                Role.Secondary,
                Role.Primary,
                Role.OnPrimaryContainer,
            ),
    ) {
        val scaleX = size.width / SceneSize.width
        val scaleY = size.height / SceneSize.height
        drawRect(sky)
        drawCircle(sun, radius = SceneSunRadius * scaleY, center = Offset(SceneSun.x * scaleX, SceneSun.y * scaleY))
        SceneRidges.forEachIndexed {
            index,
            ridge,
            ->
            drawPath(ridgePath(ridge, scaleX, scaleY, size.height), ridges[index])
        }
    }
}

/** The ridge [points] scaled to the canvas, closed along its [bottom] edge. */
private fun ridgePath(
    points: FloatArray,
    scaleX: Float,
    scaleY: Float,
    bottom: Float,
): Path {
    val path = Path()
    path.moveTo(0f, bottom)
    for (index in points.indices step 2) path.lineTo(points[index] * scaleX, points[index + 1] * scaleY)
    path.lineTo(points[points.size - 2] * scaleX, bottom)
    path.close()
    return path
}

@Composable
private fun TripActions() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Gap), verticalArrangement = Arrangement.spacedBy(Gap)) {
        Button(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.FilledButton)) { Text("Check in") }
        FilledTonalButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.TonalButton)) {
            Text("Share plan")
        }
        OutlinedButton(
            onClick = {},
            modifier = Modifier.previewRoles(MaterialComponent.OutlinedButton),
        ) { Text("Edit") }
        AssistChip(
            onClick = {},
            label = { Text("Add to calendar") },
            modifier = Modifier.previewRoles(MaterialComponent.AssistChip),
            leadingIcon = {
                Icon(Lucide.Calendar, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
            },
        )
        AssistChip(
            onClick = {},
            label = { Text("Directions") },
            modifier = Modifier.previewRoles(MaterialComponent.AssistChip),
            leadingIcon = {
                Icon(
                    Lucide.MapPin,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    }
}

@Composable
private fun DayPlan(trip: Trip) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(Modifier.padding(horizontal = TextInset), verticalArrangement = Arrangement.spacedBy(Gap)) {
        if (trip.plan.isEmpty()) {
            Text("Nothing planned yet", color = colors.onSurfaceVariant, style = type.bodyMedium)
        } else {
            Text("Day 1 · Friday", Modifier.previewRoles(Role.Primary), color = colors.primary, style = type.labelLarge)
        }
        trip.plan.forEachIndexed { index, stop ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PaneGap)) {
                Box(
                    modifier = Modifier
                        .size(StopSize)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest)
                        .previewRoles(Role.SurfaceContainerHighest, Role.OnSurfaceVariant),
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

/** A snackbar in the error container, offering to turn offline maps on. */
@Composable
private fun MapsNeedSignal(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.ErrorContainer, Role.OnErrorContainer),
        shape = MaterialTheme.shapes.small,
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = SectionGap, end = Gap),
            horizontalArrangement = Arrangement.spacedBy(PaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.CloudOff, contentDescription = null)
            Text(
                "Maps need signal until offline maps is on",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { state.setOn(OfflineMapsSwitch, true) },
                modifier = Modifier.previewRoles(Role.Error),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
            ) {
                Text("Turn on")
            }
        }
    }
}

@Composable
private fun PackingCard(state: DemoAppState) {
    val colors = MaterialTheme.colorScheme
    val items = PackingItem.entries
    val packed = items.count { item -> state.isChecked(item.key) }
    Card(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.SurfaceContainer, Role.OnSurface),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
    ) {
        Column(Modifier.padding(SectionGap), verticalArrangement = Arrangement.spacedBy(Gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Packing", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    "$packed of ${items.size}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            LinearProgressIndicator(
                progress = { packed / items.size.toFloat() },
                modifier = Modifier.fillMaxWidth().previewRoles(MaterialComponent.LinearProgressIndicator),
            )
            for (item in items) {
                val checked = state.isChecked(item.key)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(value = checked, role = SemanticsRole.Checkbox) { ticked ->
                            state.setChecked(item.key, ticked)
                        }.previewRoles(MaterialComponent.Checkbox),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(item.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(state: DemoAppState) {
    Card(
        modifier = Modifier.fillMaxWidth().previewRoles(Role.SurfaceContainerLowest, Role.OnSurface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        // b-228b
        // One line, since on a field over several lines a long press on the web would put up selection
        // handles, popups that take the accessibility mirror over (D45).
        SampleOutlinedTextField(
            value = state.text,
            onValueChange = { text -> state.text = text },
            label = "Note for the group",
            // b-227
            modifier = Modifier
                .padding(SectionGap)
                .fillMaxWidth()
                .tabMovesFocus(LocalFocusManager.current)
                .previewRoles(MaterialComponent.OutlinedTextField),
        )
    }
}
