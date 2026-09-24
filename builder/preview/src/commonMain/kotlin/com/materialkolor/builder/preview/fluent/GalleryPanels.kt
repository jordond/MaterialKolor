package com.materialkolor.builder.preview.fluent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.kit.control.foldedTabName
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Layer
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Badge
import io.github.composefluent.component.BadgeDefaults
import io.github.composefluent.component.BadgeStatus
import io.github.composefluent.component.Button
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.ExpanderItem
import io.github.composefluent.component.ExpanderItemSeparator
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.InfoBarSeverity
import io.github.composefluent.component.ListItem
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.SelectorBar
import io.github.composefluent.component.SelectorBarItem
import io.github.composefluent.component.TabItem
import io.github.composefluent.component.TabRow
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ChevronDown
import io.github.composefluent.icons.regular.Document
import io.github.composefluent.icons.regular.Folder
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Key
import io.github.composefluent.icons.regular.Mail
import io.github.composefluent.icons.regular.Send
import io.github.composefluent.surface.Card

// The samples of the Containment, Navigation and Feedback cards of FluentCards in GalleryEntry.kt.

/** How far along the progress bar and ring are. */
private const val DemoProgress = 0.6f

private val Folders = listOf("Recent", "Shared", "Favorites")
private val TabLabels = listOf("Home", "Photos", "Music")
private val Mailboxes = listOf("Inbox" to Icons.Regular.Mail, "Sent" to Icons.Regular.Send, "Archive" to Icons.Regular.Folder)

/** A clickable Fluent card and its disabled copy, sharing the row. */
@Composable
internal fun SampleCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        for (enabled in EnabledThenDisabled) {
            Card(
                onClick = {},
                modifier = Modifier.weight(1f).previewRoles(enabled, FluentGalleryComponent.SampleCard),
                disabled = !enabled,
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Regular.Document, contentDescription = null)
                    Text("Report.docx", style = FluentTheme.typography.bodyStrong)
                    Text(if (enabled) "Edited today" else "Disabled", style = FluentTheme.typography.caption)
                }
            }
        }
    }
}

@Composable
internal fun Expanders(state: DemoAppState) {
    GalleryColumn {
        GalleryExpander(
            title = "Shipping details",
            caption = "Where and when it arrives",
            icon = Icons.Regular.Home,
            open = state.isOn(FluentGalleryKeys.Details),
            enabled = true,
            onToggle = { open -> state.setOn(FluentGalleryKeys.Details, open) },
            rows = listOf("Address" to "12 Harbour Street", "Delivery" to "Weekdays after 5 pm"),
        )
        GalleryExpander(
            title = "Payment",
            caption = "Card ending 4242",
            icon = Icons.Regular.Key,
            open = false,
            enabled = false,
            onToggle = {},
            rows = emptyList(),
        )
    }
}

/**
 * An expander built from Fluent's expander parts, the way the Settings app builds its groups.
 *
 * Its header is Fluent's clickable card, so it carries its colors and the web's expanded fold
 * itself, which the one piece `Expander` keeps out of reach. The rows under it open with the skin's
 * panel motion, and at once while motion is frozen, where `Expander` would always animate.
 */
@Composable
private fun GalleryExpander(
    title: String,
    caption: String,
    icon: ImageVector,
    open: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    rows: List<Pair<String, String>>,
) {
    Column(Modifier.fillMaxWidth()) {
        CardExpanderItem(
            onClick = { onToggle(!open) },
            heading = { Text(title) },
            modifier = Modifier
                .fillMaxWidth()
                .previewRoles(enabled, FluentGalleryComponent.Expander)
                .foldedExpandedName(title, open, enabled),
            enabled = enabled,
            icon = { Icon(icon, contentDescription = null) },
            caption = { Text(caption) },
            dropdown = {
                Icon(
                    imageVector = Icons.Regular.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = if (open) 180f else 0f },
                )
            },
        )
        GalleryPanelMotion(open) {
            Column {
                for ((heading, detail) in rows) {
                    ExpanderItemSeparator()
                    ExpanderItem(
                        heading = { Text(heading) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = null,
                        caption = { Text(detail) },
                        dropdown = null,
                    )
                }
            }
        }
    }
}

/**
 * Fluent's content dialog as it looks open, laid out in place rather than in a popup (D40). Delete
 * waits for the box to be ticked, and either button puts the box back.
 */
@Composable
internal fun InPlaceDialog(state: DemoAppState) {
    val understood = state.isChecked(FluentGalleryKeys.Understood)
    val colors = FluentTheme.colors
    Layer(
        modifier = Modifier.fillMaxWidth().previewRoles(FluentGalleryComponent.Dialog),
        shape = FluentTheme.shapes.overlay,
        color = colors.background.solid.base,
        border = BorderStroke(1.dp, colors.stroke.surface.default),
    ) {
        Column {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.background.layer.alt).padding(GalleryGap),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Delete this file?", style = FluentTheme.typography.subtitle)
                Text("Report.docx leaves every device, and this cannot be undone.")
                GalleryCheckBox("I understand", understood, enabled = true) { on ->
                    state.setChecked(FluentGalleryKeys.Understood, on)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.stroke.surface.default))
            Row(Modifier.fillMaxWidth().padding(GalleryGap), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                AccentButton(
                    onClick = { state.setChecked(FluentGalleryKeys.Understood, false) },
                    modifier = Modifier.weight(1f).previewRoles(understood, FluentGalleryComponent.AccentButton),
                    disabled = !understood,
                ) { Text("Delete") }
                Button(
                    onClick = { state.setChecked(FluentGalleryKeys.Understood, false) },
                    modifier = Modifier.weight(1f).previewRoles(FluentGalleryComponent.Button),
                ) { Text("Cancel") }
            }
        }
    }
}

/**
 * Fluent's selector bar, one item of three with the last disabled. An item reports its selection
 * but no role, so its modifier adds the tab role and the name carries both onto the web.
 */
@Composable
internal fun SelectorBars(state: DemoAppState) {
    val picked = state.choice(FluentGalleryKeys.Folder, Folders.size)
    SelectorBar(Modifier.selectableGroup()) {
        Folders.forEachIndexed { index, folder ->
            val enabled = index != Folders.lastIndex
            val chosen = picked == index
            val component =
                if (chosen) FluentGalleryComponent.SelectedSelectorItem else FluentGalleryComponent.SelectorItem
            SelectorBarItem(
                selected = chosen,
                onSelectedChange = { state.choose(FluentGalleryKeys.Folder, Folders.size, index) },
                text = { Text(folder) },
                modifier = Modifier
                    .previewRoles(enabled, component)
                    .semantics { role = Role.Tab }
                    .foldedTabName(folder, chosen, enabled),
                enabled = enabled,
            )
        }
    }
}

/**
 * Fluent's tab view strip. Fluent gives tabs no disabled look, so every tab is enabled. A tab
 * reports neither its role nor its selection, so its modifier adds both.
 */
@Composable
internal fun Tabs(state: DemoAppState) {
    val picked = state.choice(FluentGalleryKeys.Tab, TabLabels.size)
    TabRow(
        // Read from the state, since the tab row keeps the first lambda it is handed.
        selectedKey = { state.choice(FluentGalleryKeys.Tab, TabLabels.size) },
        modifier = Modifier.fillMaxWidth().previewRoles(FluentGalleryComponent.TabRow).selectableGroup(),
    ) {
        TabLabels.forEachIndexed { index, label ->
            item(key = index) {
                val chosen = picked == index
                TabItem(
                    selected = chosen,
                    onSelectedChanged = { state.choose(FluentGalleryKeys.Tab, TabLabels.size, index) },
                    text = { Text(label) },
                    modifier = Modifier
                        .previewRoles(FluentGalleryComponent.Tab)
                        .semantics {
                            role = Role.Tab
                            selected = chosen
                        }.foldedTabName(label, chosen),
                )
            }
        }
    }
}

/**
 * Fluent's list items as a navigation list, the current one marked with the accent pill, each under
 * a layer that picks it and names it for the web. A list item keeps its clickable inside.
 */
@Composable
internal fun NavigationList(state: DemoAppState) {
    val picked = state.choice(FluentGalleryKeys.Page, Mailboxes.size)
    Column(Modifier.selectableGroup()) {
        Mailboxes.forEachIndexed { index, (mailbox, icon) ->
            val enabled = index != Mailboxes.lastIndex
            val chosen = picked == index
            val interactions = remember { MutableInteractionSource() }
            val component =
                if (chosen) FluentGalleryComponent.SelectedNavigationItem else FluentGalleryComponent.NavigationItem
            FluentOverlaid(
                control = Modifier
                    .previewRoles(enabled, component)
                    .selectable(
                        selected = chosen,
                        interactionSource = interactions,
                        indication = null,
                        enabled = enabled,
                        onClick = { state.choose(FluentGalleryKeys.Page, Mailboxes.size, index) },
                    ).foldedSelectedName(mailbox, chosen, enabled),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    selected = chosen,
                    onSelectedChanged = {},
                    text = { Text(mailbox) },
                    icon = { Icon(icon, contentDescription = null) },
                    interaction = interactions,
                    enabled = enabled,
                )
            }
        }
    }
}

/** An info bar of each severity. None has an action, so there is nothing to disable. */
@Composable
internal fun InfoBars() {
    GalleryColumn {
        for (severity in InfoBarSeverity.entries) {
            InfoBar(
                title = { Text(severity.name) },
                message = { Text(severity.message()) },
                severity = severity,
                modifier = Modifier.fillMaxWidth().previewRoles(FluentGalleryComponent.InfoBar),
            )
        }
    }
}

/** The determinate bar, since the endless one would loop past frozen motion and a hidden tab. */
@Composable
internal fun ProgressBars() {
    ProgressBar(
        progress = DemoProgress,
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(FluentGalleryComponent.Progress)
            .progressSemantics("Uploading"),
    )
}

/** The determinate ring, for the same reason as the bar. */
@Composable
internal fun ProgressRings() {
    ProgressRing(
        progress = DemoProgress,
        modifier = Modifier.previewRoles(FluentGalleryComponent.Progress).progressSemantics("Syncing"),
    )
}

/** Status badges in Fluent's fixed system colors, then counts on a system color and on the accent. */
@Composable
internal fun Badges() {
    Wrapping {
        for (status in listOf(BadgeStatus.Attention, BadgeStatus.Success, BadgeStatus.Caution, BadgeStatus.Critical)) {
            Badge(status = status, modifier = Modifier.previewRoles(FluentGalleryComponent.Badge)) { shown ->
                BadgeDefaults.Icon(shown)
            }
        }
        Badge(status = BadgeStatus.Critical, modifier = Modifier.previewRoles(FluentGalleryComponent.Badge)) {
            Text("3")
        }
        Badge(
            backgroundColor = FluentTheme.colors.fillAccent.default,
            modifier = Modifier.previewRoles(FluentGalleryComponent.AccentBadge),
        ) { Text("12") }
    }
}

/** What an info bar of this severity says. */
private fun InfoBarSeverity.message(): String =
    when (this) {
        InfoBarSeverity.Informational -> "A new version is ready to install."
        InfoBarSeverity.Success -> "Your files are backed up."
        InfoBarSeverity.Warning -> "Your storage is almost full."
        InfoBarSeverity.Critical -> "Sign-in failed. Check your password."
    }

/** Names a progress indicator and says how far along it is, which Fluent's own leave out. */
private fun Modifier.progressSemantics(label: String): Modifier =
    semantics {
        contentDescription = label
        progressBarRangeInfo = ProgressBarRangeInfo(DemoProgress, 0f..1f)
    }

/**
 * Shows or hides an expander's rows with the skin's panel motion, growing along the height. Under
 * frozen motion they show or hide at once.
 */
@Composable
private fun GalleryPanelMotion(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val frozen = LocalMotionFrozen.current
    val motion = LocalBuilderMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = if (frozen) EnterTransition.None else expandVertically(motion.panelEnter()) + fadeIn(motion.panelEnter()),
        exit = if (frozen) ExitTransition.None else shrinkVertically(motion.panelExit()) + fadeOut(motion.panelExit()),
    ) { content() }
}
