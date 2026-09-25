package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderCheckbox
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderProgress
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderTabs
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.custom.CustomPaneTheme
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.split.PaneSpec

// The entry, its cards and their frame, then the samples of the Containment, Navigation and
// Feedback cards. CustomGallery.kt keeps the slots each control paints and the samples of the
// Actions, Inputs and Selection cards.

private val Palettes = listOf(
    "Harbour" to "Blue, tonal spot",
    "Meadow" to "Green, vibrant",
    "Ember" to "Archived",
)
private val Sections = listOf("Scheme", "Palettes", "Export")

private enum class GalleryDestination(
    val icon: IconId,
) {
    Workspace(IconId.Folder),
    Inspect(IconId.Inspect),
    Export(IconId.Export),
}

/**
 * The Custom components gallery.
 *
 * Every card holds the builder's own kit controls under [CustomPaneTheme], so they use the
 * pane's Custom slots and not the chrome's. Each control shows up enabled and disabled, apart from
 * the few the kit gives no disabled look. Nothing in the gallery opens a popup, a dialog or an
 * overlay, since on the web a window takes the accessibility mirror over for good. Menus, the
 * select's list, the dialog, the sheet, the tooltip and the toast are drawn in place. Right presses
 * are swallowed and the text fields get a toolbar that never shows, as in the Material 3 gallery.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal fun CustomGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    CustomPaneTheme(spec.result.customSlots, spec.isDark, LocalReducedMotion.current) {
        val tokens = LocalBuilderTokens.current
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            GalleryGrid(
                cards = CustomCards,
                listState = state.rememberListState("gallery.custom"),
                gap = tokens.spacing.large,
                modifier = modifier
                    .fillMaxSize()
                    .previewRoles(CustomComponent.Canvas)
                    .background(tokens.canvas)
                    .gallerySwallowRightPresses(),
                header = { group -> CustomGroupHeader(group) },
                card = { card, cardModifier -> CustomCardFrame(card, state, cardModifier) },
            )
        }
    }
}

/**
 * Every card of the Custom gallery, in the order they show within each group.
 */
internal val CustomCards: List<GalleryCard> = listOf(
    GalleryCard("Primary button", GalleryGroup.Actions) { ActionButtons(Emphasis.Primary, "Export", IconId.Export) },
    GalleryCard("Secondary button", GalleryGroup.Actions) { ActionButtons(Emphasis.Secondary, "Share", IconId.Share) },
    GalleryCard("Subtle button", GalleryGroup.Actions) { ActionButtons(Emphasis.Subtle, "Reset", null) },
    GalleryCard("Danger button", GalleryGroup.Actions) { ActionButtons(Emphasis.Danger, "Delete", IconId.Trash) },
    GalleryCard("Icon buttons", GalleryGroup.Actions) { IconButtons() },
    GalleryCard("Toggle button", GalleryGroup.Actions) { state -> ToggleButtons(state) },
    GalleryCard("Text field", GalleryGroup.Inputs) { state -> TextFields(state) },
    GalleryCard("Slider", GalleryGroup.Inputs) { state -> Sliders(state) },
    GalleryCard("Checkbox", GalleryGroup.Selection) { state -> Checkboxes(state) },
    GalleryCard("Switch", GalleryGroup.Selection) { state -> Switches(state) },
    GalleryCard("Segmented control", GalleryGroup.Selection) { state -> SegmentedControls(state) },
    GalleryCard("Choice chips", GalleryGroup.Selection) { state -> ChoiceChips(state) },
    GalleryCard("Filter chips", GalleryGroup.Selection) { state -> FilterChips(state) },
    GalleryCard("Select", GalleryGroup.Selection) { state -> InlineSelect(state) },
    GalleryCard("Menu", GalleryGroup.Selection) { state -> InlineMenu(state) },
    GalleryCard("Card", GalleryGroup.Containment) { Cards() },
    GalleryCard("List", GalleryGroup.Containment) { ListRows() },
    GalleryCard("Disclosure", GalleryGroup.Containment) { state -> Disclosures(state) },
    GalleryCard("Dialog", GalleryGroup.Containment) { state -> InlineDialog(state) },
    GalleryCard("Sheet", GalleryGroup.Containment) { state -> InlineSheet(state) },
    GalleryCard("Tabs", GalleryGroup.Navigation) { state -> Tabs(state) },
    GalleryCard("App bar", GalleryGroup.Navigation) { AppBar() },
    GalleryCard("Navigation list", GalleryGroup.Navigation) { state -> NavigationList(state) },
    GalleryCard("Badges", GalleryGroup.Feedback) { Badges() },
    GalleryCard("Progress", GalleryGroup.Feedback) { Progress() },
    GalleryCard("Tooltip", GalleryGroup.Feedback) { InlineTooltip() },
    GalleryCard("Toast", GalleryGroup.Feedback) { state -> InlineToast(state) },
)

@Composable
private fun CustomGroupHeader(group: GalleryGroup) {
    BuilderText(
        text = group.name,
        modifier = Modifier.padding(top = LocalBuilderTokens.current.spacing.small).semantics { heading() },
        style = BuilderTextStyle.Title,
    )
}

/**
 * The kit card a card's controls sit in, titled with the card's name.
 */
@Composable
private fun CustomCardFrame(
    card: GalleryCard,
    state: DemoAppState,
    modifier: Modifier,
) {
    BuilderCard(modifier.previewRoles(CustomComponent.Card)) {
        BuilderText(card.title, style = BuilderTextStyle.SectionLabel)
        card.content(state)
    }
}

@Composable
internal fun Cards() {
    EnabledPair { enabled ->
        BuilderCard(
            modifier = Modifier.weight(1f).previewRoles(CustomComponent.SampleCard),
            onClick = {},
            enabled = enabled,
        ) {
            BuilderIcon(IconId.Folder, contentDescription = null)
            BuilderText("Harbour", style = BuilderTextStyle.Label)
            BuilderText(if (enabled) "Edited today" else "Disabled", emphasis = Emphasis.Secondary)
        }
    }
}

@Composable
internal fun ListRows() {
    Column {
        Palettes.forEachIndexed { index, (name, detail) ->
            if (index > 0) BuilderDivider(Modifier.previewRoles(CustomComponent.Divider))
            BuilderListRow(
                headline = name,
                modifier = Modifier.previewRoles(CustomComponent.ListRow),
                supporting = detail,
                icon = IconId.Folder,
                onClick = {},
                enabled = index != Palettes.lastIndex,
            )
        }
    }
}

@Composable
internal fun Disclosures(state: DemoAppState) {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            BuilderDisclosure(
                expanded = enabled && state.isOn(AdvancedKey),
                onExpandedChange = { expanded -> state.setOn(AdvancedKey, expanded) },
                title = if (enabled) "Advanced" else "Legacy options",
                modifier = Modifier.fillMaxWidth().previewRoles(CustomComponent.Disclosure),
                summary = "Contrast and spec version",
                enabled = enabled,
            ) { BuilderText("Standard contrast, spec 2025", emphasis = Emphasis.Secondary) }
        }
    }
}

/**
 * A dialog as it looks open, laid out in place rather than over a scrim.
 */
@Composable
internal fun InlineDialog(state: DemoAppState) {
    val tokens = LocalBuilderTokens.current
    val understood = state.isChecked(UnderstoodKey)
    GalleryOverlayPanel(RoundedCornerShape(tokens.radius.large), Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        ) {
            BuilderText("Delete this palette?", style = BuilderTextStyle.Title)
            BuilderText("Harbour leaves every device, and this cannot be undone.", emphasis = Emphasis.Secondary)
            BuilderCheckbox(
                checked = understood,
                onCheckedChange = { checked -> state.setChecked(UnderstoodKey, checked) },
                label = "I understand",
                modifier = Modifier.previewRoles(CustomComponent.Checkbox),
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            ) {
                BuilderButton(
                    onClick = { state.setChecked(UnderstoodKey, false) },
                    label = "Cancel",
                    modifier = Modifier.previewRoles(Emphasis.Subtle.component),
                    emphasis = Emphasis.Subtle,
                )
                BuilderButton(
                    onClick = { state.setChecked(UnderstoodKey, false) },
                    label = "Delete",
                    modifier = Modifier.previewRoles(Emphasis.Danger.component),
                    emphasis = Emphasis.Danger,
                    enabled = understood,
                )
            }
        }
    }
}

/**
 * A bottom sheet at its peek, drawn in place rather than over the page.
 */
@Composable
internal fun InlineSheet(state: DemoAppState) {
    val tokens = LocalBuilderTokens.current
    val sheet = RoundedCornerShape(topStart = tokens.radius.large, topEnd = tokens.radius.large)
    GalleryOverlayPanel(sheet, Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = tokens.spacing.small)
                .size(width = tokens.spacing.section, height = tokens.spacing.extraSmall)
                .background(tokens.textMuted, CircleShape),
        )
        Row(
            modifier = Modifier.padding(start = tokens.spacing.large, end = tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText("Share", modifier = Modifier.weight(1f), style = BuilderTextStyle.Title)
            BuilderIconButton(
                onClick = {},
                icon = IconId.Close,
                contentDescription = "Close",
                modifier = Modifier.previewRoles(Emphasis.Subtle.component),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = tokens.spacing.large, vertical = tokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            for (enabled in EnabledThenDisabled) {
                val key = if (enabled) LinkKey else EmbedKey
                BuilderSwitch(
                    checked = state.isOn(key),
                    onCheckedChange = { on -> state.setOn(key, on) },
                    label = if (enabled) "Anyone with the link" else "Embed in a page",
                    modifier = Modifier.previewRoles(CustomComponent.Switch),
                    enabled = enabled,
                )
            }
        }
    }
}

/**
 * The kit gives tabs no disabled look, so every tab is enabled.
 */
@Composable
internal fun Tabs(state: DemoAppState) {
    BuilderTabs(
        tabs = Sections,
        selected = Sections[state.tabIndex.coerceIn(0, Sections.lastIndex)],
        onSelect = { section -> state.tabIndex = Sections.indexOf(section) },
        label = { section -> section },
        modifier = Modifier.previewRoles(CustomComponent.Tabs),
    )
}

@Composable
internal fun AppBar() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BuilderIconButton(
            onClick = {},
            icon = IconId.ChevronLeft,
            contentDescription = "Back",
            modifier = Modifier.previewRoles(Emphasis.Subtle.component),
        )
        BuilderText("Harbour", modifier = Modifier.weight(1f), style = BuilderTextStyle.Title, maxLines = 1)
        BuilderIconButton(
            onClick = {},
            icon = IconId.Undo,
            contentDescription = "Undo",
            modifier = Modifier.previewRoles(Emphasis.Subtle.component),
        )
        BuilderIconButton(
            onClick = {},
            icon = IconId.Redo,
            contentDescription = "Redo",
            modifier = Modifier.previewRoles(Emphasis.Subtle.component),
            enabled = false,
        )
    }
}

@Composable
internal fun NavigationList(state: DemoAppState) {
    val picked = GalleryDestination.entries[state.selectedItem.coerceIn(0, GalleryDestination.entries.size - 2)]
    Column {
        for (destination in GalleryDestination.entries) {
            BuilderListRow(
                headline = destination.name,
                modifier = Modifier.previewRoles(CustomComponent.ListRow),
                icon = destination.icon,
                onClick = { state.selectedItem = destination.ordinal },
                selected = destination == picked,
                enabled = destination != GalleryDestination.entries.last(),
            )
        }
    }
}

/**
 * Badges hold nothing to press, so none is disabled.
 */
@Composable
internal fun Badges() {
    GalleryColumn {
        for (row in BadgeStatus.entries.chunked(BadgesPerRow)) {
            Row(horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
                for (status in row) {
                    BuilderBadge(status.badgeLabel, Modifier.previewRoles(status.component), status)
                }
            }
        }
    }
}

private const val BadgesPerRow = 3

private val BadgeStatus.badgeLabel: String
    get() = when (this) {
        BadgeStatus.Neutral -> "12 roles"
        BadgeStatus.Info -> "New"
        BadgeStatus.Success -> "AAA"
        BadgeStatus.Warning -> "AA large"
        BadgeStatus.Danger -> "Fails"
    }

private val BadgeStatus.component: CustomComponent
    get() = when (this) {
        BadgeStatus.Neutral -> CustomComponent.NeutralBadge
        BadgeStatus.Info -> CustomComponent.PrimaryAction
        BadgeStatus.Success -> CustomComponent.StatusBadge
        BadgeStatus.Warning -> CustomComponent.StatusBadge
        BadgeStatus.Danger -> CustomComponent.DangerBadge
    }

/**
 * A determinate bar over an indeterminate one. Neither has anything to press.
 */
@Composable
internal fun Progress() {
    GalleryColumn {
        BuilderProgress("Exporting", Modifier.fillMaxWidth().previewRoles(CustomComponent.Progress), progress = 0.6f)
        BuilderProgress("Resolving", Modifier.fillMaxWidth().previewRoles(CustomComponent.Progress))
    }
}

/**
 * A tooltip as it looks shown, drawn in place under its anchor rather than in a popup.
 */
@Composable
internal fun InlineTooltip() {
    val tokens = LocalBuilderTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall)) {
        BuilderIconButton(
            onClick = {},
            icon = IconId.Copy,
            contentDescription = "Copy hex",
            modifier = Modifier.previewRoles(Emphasis.Subtle.component),
        )
        BuilderText(
            text = "Copy hex",
            modifier = Modifier
                .previewRoles(CustomComponent.Inverse)
                .background(tokens.textStrong, RoundedCornerShape(tokens.radius.medium))
                .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
            style = BuilderTextStyle.Label,
            color = tokens.panel,
        )
    }
}

/**
 * A toast as it looks raised, drawn in place rather than in the overlay host's top slot.
 */
@Composable
internal fun InlineToast(state: DemoAppState) {
    val tokens = LocalBuilderTokens.current
    val undone = state.isOn(UndoneKey)
    val shape = RoundedCornerShape(tokens.radius.medium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(CustomComponent.Inverse)
            .background(tokens.textStrong, shape)
            .padding(start = tokens.spacing.large, end = tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = if (undone) "Copy undone" else "Palette copied",
            modifier = Modifier.weight(1f).padding(vertical = tokens.spacing.medium),
            color = tokens.panel,
        )
        BuilderText(
            text = if (undone) "Redo" else "Undo",
            modifier = Modifier
                .previewRoles(CustomComponent.Inverse)
                .clickable(role = Role.Button) { state.setOn(UndoneKey, !undone) }
                .padding(tokens.spacing.small),
            style = BuilderTextStyle.Label,
            color = tokens.panel,
        )
    }
}

// Keys into DemoAppState.
private const val AdvancedKey = "gallery.custom.advanced"
private const val UnderstoodKey = "gallery.custom.understood"
private const val LinkKey = "gallery.custom.link"
private const val EmbedKey = "gallery.custom.embed"
private const val UndoneKey = "gallery.custom.undone"
