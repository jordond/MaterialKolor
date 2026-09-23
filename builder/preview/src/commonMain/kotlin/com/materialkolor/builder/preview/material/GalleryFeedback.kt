package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.materialkolor.builder.preview.canvas.DemoAppState

// The samples of the Feedback cards of MaterialCards in GalleryEntry.kt.

/** How far along both progress indicators are. */
private const val DemoProgress = 0.6f

/** Badges on icon buttons, cleared and brought back by a click on either enabled button. */
@Composable
internal fun Badges(state: DemoAppState) {
    val read = state.isOn(ReadKey)
    EnabledAndDisabled { enabled ->
        IconButton(
            onClick = { state.setOn(ReadKey, !read) },
            modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            enabled = enabled,
        ) {
            BadgedBox(badge = { if (!read) Badge(Modifier.previewRoles(MaterialComponent.Badge)) { Text("8") } }) {
                Icon(Lucide.Mail, contentDescription = if (read) "Messages" else "Messages, 8 new")
            }
        }
        IconButton(
            onClick = { state.setOn(ReadKey, !read) },
            modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            enabled = enabled,
        ) {
            BadgedBox(badge = { if (!read) Badge(Modifier.previewRoles(MaterialComponent.Badge)) }) {
                Icon(Lucide.Bell, contentDescription = if (read) "Notifications" else "Notifications, new")
            }
        }
    }
}

/** Determinate indicators only, since an endless one would never let the preview settle. */
@Composable
internal fun ProgressIndicators() {
    Row(horizontalArrangement = Arrangement.spacedBy(SectionGap), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier.previewRoles(GalleryComponent.CircularProgressIndicator),
        )
        LinearProgressIndicator(
            progress = { DemoProgress },
            modifier = Modifier.weight(1f).previewRoles(MaterialComponent.LinearProgressIndicator),
        )
    }
}

/** A snackbar shown in place, its action flipping the message. Snackbar actions have no disabled look. */
@Composable
internal fun Snackbars(state: DemoAppState) {
    val restored = state.isOn(RestoredKey)
    Snackbar(
        modifier = Modifier.previewRoles(GalleryComponent.Snackbar),
        action = {
            TextButton(
                onClick = { state.setOn(RestoredKey, !restored) },
                modifier = Modifier.previewRoles(GalleryComponent.SnackbarAction),
                colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
            ) { Text(if (restored) "Archive" else "Undo") }
        },
    ) { Text(if (restored) "Trip restored" else "Trip archived") }
}

/**
 * A plain tooltip over the button it labels and a rich tooltip, both drawn in place rather than
 * in a popup (D40). Tooltips have no disabled look.
 */
@Composable
internal fun InlineTooltips(state: DemoAppState) {
    val saved = state.isOn(SavedKey)
    val label = if (saved) "Saved" else "Save trip"
    val rich = TooltipDefaults.richTooltipColors()
    Column(verticalArrangement = Arrangement.spacedBy(PaneGap)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Gap / 2),
        ) {
            Surface(
                modifier = Modifier.previewRoles(GalleryComponent.PlainTooltip),
                shape = TooltipDefaults.plainTooltipContainerShape,
                color = TooltipDefaults.plainTooltipContainerColor,
                contentColor = TooltipDefaults.plainTooltipContentColor,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = Gap, vertical = Gap / 2),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(
                onClick = { state.setOn(SavedKey, !saved) },
                modifier = Modifier.previewRoles(MaterialComponent.IconButton),
            ) { Icon(Lucide.Bookmark, contentDescription = label) }
        }
        Surface(
            modifier = Modifier.previewRoles(GalleryComponent.RichTooltip),
            shape = TooltipDefaults.richTooltipContainerShape,
            color = rich.containerColor,
            contentColor = rich.contentColor,
        ) {
            Column(Modifier.padding(start = SectionGap, top = PaneGap, end = Gap)) {
                Text("Offline maps", color = rich.titleContentColor, style = MaterialTheme.typography.titleSmall)
                Text("Download the map so it works without a signal.", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {},
                    modifier = Modifier.previewRoles(MaterialComponent.TextButton),
                    colors = ButtonDefaults.textButtonColors(contentColor = rich.actionContentColor),
                ) { Text("Learn more") }
            }
        }
    }
}

// Keys into DemoAppState.
private const val ReadKey = "gallery.badges.read"
private const val RestoredKey = "gallery.snackbar.restored"
private const val SavedKey = "gallery.tooltip.saved"
