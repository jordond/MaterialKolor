package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Inbox
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.composeunstyled.DisclosedContent
import com.composeunstyled.DisclosureButton
import com.composeunstyled.Indicator
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.TabPanel
import com.composeunstyled.Text
import com.composeunstyled.Thumb
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDisclosure
import com.composeunstyled.UnstyledHorizontalSeparator
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledProgress
import com.composeunstyled.UnstyledTabGroup
import com.composeunstyled.UnstyledVerticalScrollbar
import com.composeunstyled.UnstyledVerticalSeparator
import com.composeunstyled.rememberScrollbarState
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.kit.control.foldedTabName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose

// The samples of the Containment, Navigation and Feedback cards.

private const val FileCount = 24
private val ScrollAreaHeight = 132.dp
private val ScrollbarWidth = 6.dp
private val ProgressHeight = 6.dp
private val Tabs = listOf("Overview", "Activity", "Settings")
private val TabPanels = listOf("Three projects, one shared", "Harbour was edited today", "Only you can change these")

/** A destination of the navigation list, the last one out of reach. */
private enum class GalleryDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Lucide.House),
    Inbox("Inbox", Lucide.Inbox),
    Team("Team", Lucide.Users),
}

/** A badge's words and how it is painted. */
private enum class GalleryBadge(
    val label: String,
    val component: UnstyledGalleryComponent,
    val container: DashboardToken,
    val content: DashboardToken,
) {
    New(
        label = "New",
        component = UnstyledGalleryComponent.Badge,
        container = DashboardToken.PrimaryContainer,
        content = DashboardToken.OnPrimaryContainer,
    ),
    Beta(
        label = "Beta",
        component = UnstyledGalleryComponent.TertiaryBadge,
        container = DashboardToken.TertiaryContainer,
        content = DashboardToken.OnTertiaryContainer,
    ),
    Failed(
        label = "Failed",
        component = UnstyledGalleryComponent.ErrorBadge,
        container = DashboardToken.ErrorContainer,
        content = DashboardToken.OnErrorContainer,
    ),
}

@Composable
internal fun SampleCards() {
    EnabledPair { enabled ->
        val interactions = remember { MutableInteractionSource() }
        UnstyledButton(
            onClick = {},
            enabled = enabled,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier
                .weight(1f)
                .previewRoles(enabled, UnstyledGalleryComponent.SampleCard)
                .galleryFocusRing(interactions, offset = true)
                .clip(ControlShape)
                .background(if (enabled) DashboardToken.SurfaceContainerHigh.color else disabledContainer),
            interactionSource = interactions,
            contentAlignment = Alignment.TopStart,
        ) {
            val strong = tint(DashboardToken.OnSurface, enabled)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                UnstyledIcon(Lucide.Folder, null, Modifier.size(IconSize), tint = strong)
                Text(if (enabled) "Harbour" else "Ember", style = LabelStyle, color = strong)
                Text(
                    text = if (enabled) "Edited today" else "Archived",
                    style = SmallStyle,
                    color = tint(DashboardToken.OnSurfaceVariant, enabled),
                )
            }
        }
    }
}

@Composable
internal fun Disclosures(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            val expanded = enabled && state.isOn(GalleryKeys.Details)
            GalleryDisclosure(
                title = if (enabled) "Shipping details" else "Billing history",
                detail = "Ships in two days from Lisbon",
                expanded = expanded,
                enabled = enabled,
            ) { open -> state.setOn(GalleryKeys.Details, open) }
        }
    }
}

/**
 * A heading that shows and hides the text under it. The heading says whether it is open, on the
 * web too, and its panel opens and closes with the gallery's panel motion.
 */
@Composable
private fun GalleryDisclosure(
    title: String,
    detail: String,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    UnstyledDisclosure(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(enabled, UnstyledGalleryComponent.Disclosure)
            .clip(ControlShape)
            .background(if (enabled) DashboardToken.SurfaceContainer.color else disabledContainer),
    ) {
        Column {
            DisclosureButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .foldedExpandedName(title, expanded, enabled)
                    .galleryFocusRing(interactions),
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                interactionSource = interactions,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = LabelStyle,
                        color = tint(DashboardToken.OnSurface, enabled),
                    )
                    UnstyledIcon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize).rotate(if (expanded) 180f else 0f),
                        tint = tint(DashboardToken.OnSurfaceVariant, enabled),
                    )
                }
            }
            DisclosedContent(
                enter = fadeIn(panelMotion()) + expandVertically(panelMotion()),
                exit = fadeOut(panelMotion()) + shrinkVertically(panelMotion()),
            ) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    style = BodyStyle,
                    color = DashboardToken.OnSurfaceVariant.color,
                )
            }
        }
    }
}

@Composable
internal fun Separators() {
    val line = DashboardToken.OutlineVariant.color
    val text = DashboardToken.OnSurface.color
    GalleryColumn {
        Row(Modifier.height(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Inbox", style = BodyStyle, color = text)
            UnstyledVerticalSeparator(line, Modifier.previewRoles(UnstyledGalleryComponent.Separator))
            Text("Sent", style = BodyStyle, color = text)
            UnstyledVerticalSeparator(line, Modifier.previewRoles(UnstyledGalleryComponent.Separator))
            Text("Drafts", style = BodyStyle, color = text)
        }
        UnstyledHorizontalSeparator(line, Modifier.previewRoles(UnstyledGalleryComponent.Separator))
        Text("Three conversations", style = SmallStyle, color = DashboardToken.OnSurfaceVariant.color)
    }
}

@Composable
internal fun ScrollAreas(state: DemoAppState) {
    EnabledPair { enabled ->
        val key = if (enabled) GalleryKeys.Files else GalleryKeys.Archive
        ScrollArea(state.rememberListState(key), enabled, Modifier.weight(1f))
    }
}

/**
 * A short list that scrolls, with a scrollbar beside it. A disabled scrollbar shows faded and takes
 * no drag, while the list under it still scrolls.
 */
@Composable
private fun ScrollArea(
    list: LazyListState,
    enabled: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .height(ScrollAreaHeight)
            .previewRoles(UnstyledGalleryComponent.ScrollArea)
            .clip(ControlShape)
            .background(DashboardToken.SurfaceContainerLowest.color)
            .border(1.dp, DashboardToken.OutlineVariant.color, ControlShape)
            .padding(4.dp),
    ) {
        val muted = DashboardToken.OnSurfaceVariant.color
        val strong = DashboardToken.OnSurface.color
        LazyColumn(state = list, modifier = Modifier.weight(1f)) {
            items(FileCount, key = { index -> index }) { index ->
                Row(
                    modifier = Modifier.padding(horizontal = Gap, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UnstyledIcon(Lucide.FileText, null, Modifier.size(14.dp), tint = muted)
                    Text("Report ${index + 1}", style = SmallStyle, color = strong, maxLines = 1)
                }
            }
        }
        UnstyledVerticalScrollbar(
            scrollbarState = rememberScrollbarState(list),
            modifier = Modifier.fillMaxHeight().width(ScrollbarWidth),
            enabled = enabled,
        ) {
            Thumb(
                modifier = Modifier
                    .previewRoles(enabled, UnstyledGalleryComponent.ScrollbarThumb)
                    .clip(PillShape)
                    .background(if (enabled) DashboardToken.Outline.color else disabledContainer),
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun GalleryTabs(state: DemoAppState) {
    val picked = state.choice(GalleryKeys.Tab, Tabs.size)
    UnstyledTabGroup(
        selectedTab = picked,
        onSelectedTabChange = { index -> state.choose(GalleryKeys.Tab, Tabs.size, index) },
        tabs = Tabs.indices.toList(),
    ) {
        GalleryColumn {
            TabList(
                modifier = Modifier
                    .previewRoles(UnstyledGalleryComponent.TabList)
                    .clip(ControlShape)
                    .background(DashboardToken.SurfaceContainerHigh.color)
                    .padding(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tabs.forEachIndexed { index, label ->
                        // The last tab is out of reach, the disabled look.
                        val enabled = index != Tabs.lastIndex
                        val selected = index == picked
                        val interactions = remember { MutableInteractionSource() }
                        val component = if (selected) {
                            UnstyledGalleryComponent.SelectedTab
                        } else {
                            UnstyledGalleryComponent.Tab
                        }
                        val container = if (selected) DashboardToken.SurfaceContainerLowest.color else Color.Transparent
                        val content = when {
                            !enabled -> disabledContent
                            selected -> DashboardToken.OnSurface.color
                            else -> DashboardToken.OnSurfaceVariant.color
                        }
                        Tab(
                            key = index,
                            modifier = Modifier
                                .previewRoles(enabled, component)
                                .foldedTabName(label, selected, enabled)
                                .galleryFocusRing(interactions)
                                .clip(ControlShape)
                                .background(container),
                            enabled = enabled,
                            interactionSource = interactions,
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = LabelStyle,
                                color = content,
                            )
                        }
                    }
                }
            }
            TabPanel(key = picked) {
                Text(TabPanels[picked], style = BodyStyle, color = DashboardToken.OnSurfaceVariant.color)
            }
        }
    }
}

@Composable
internal fun NavigationList(state: DemoAppState) {
    val picked = state.choice(GalleryKeys.Destination, GalleryDestination.entries.size)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (destination in GalleryDestination.entries) {
            val enabled = destination != GalleryDestination.entries.last()
            val selected = destination.ordinal == picked
            NavigationItem(destination, selected, enabled) {
                state.choose(GalleryKeys.Destination, GalleryDestination.entries.size, destination.ordinal)
            }
        }
    }
}

/** A destination with its icon, which reads whether it is the current one, on the web too. */
@Composable
private fun NavigationItem(
    destination: GalleryDestination,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val component = if (selected) UnstyledGalleryComponent.SelectedNavItem else UnstyledGalleryComponent.NavItem
    val content = when {
        !enabled -> disabledContent
        selected -> DashboardToken.OnSecondaryContainer.color
        else -> DashboardToken.OnSurfaceVariant.color
    }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .previewRoles(enabled, component)
            .semantics { this.selected = selected }
            .foldedSelectedName(destination.label, selected, enabled)
            .galleryFocusRing(interactions)
            .clip(ControlShape)
            .background(if (selected) DashboardToken.SecondaryContainer.color else Color.Transparent),
        interactionSource = interactions,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UnstyledIcon(destination.icon, null, Modifier.size(IconSize), tint = content)
            Text(destination.label, style = LabelStyle, color = content)
        }
    }
}

@Composable
internal fun GalleryProgress() {
    GalleryColumn {
        for ((label, done) in listOf("Uploading" to 0.4f, "Exporting" to 0.75f)) {
            Text("$label, ${(done * 100).toInt()}%", style = SmallStyle, color = DashboardToken.OnSurfaceVariant.color)
            UnstyledProgress(
                progress = done,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .previewRoles(UnstyledGalleryComponent.Progress)
                    .semantics { contentDescription = label }
                    .clip(PillShape)
                    .background(DashboardToken.SurfaceContainerHighest.color),
            ) {
                Indicator(Modifier.clip(PillShape).background(DashboardToken.Primary.color))
            }
        }
    }
}

/**
 * A tooltip as it looks, shown in place under what it names, beside a button whose own tooltip
 * shows on hover and on keyboard focus.
 */
@Composable
internal fun InPlaceTooltip() {
    Row(horizontalArrangement = Arrangement.spacedBy(SectionGap), verticalAlignment = Alignment.Top) {
        GalleryIconButton(Lucide.Copy, "Copy link", enabled = true)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Link", style = LabelStyle, color = DashboardToken.OnSurface.color)
            DashboardTooltip("Copied to the clipboard")
        }
    }
}

@Composable
internal fun Badges() {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        for (badge in GalleryBadge.entries) {
            Text(
                text = badge.label,
                modifier = Modifier
                    .previewRoles(badge.component)
                    .clip(PillShape)
                    .background(badge.container.color)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                style = SmallStyle,
                color = badge.content.color,
            )
        }
    }
}
