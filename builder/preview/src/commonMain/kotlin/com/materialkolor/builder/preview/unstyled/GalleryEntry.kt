package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Star
import com.composeunstyled.FocusVisibilityProvider
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.focusRing
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.split.PaneSpec
import androidx.compose.ui.semantics.Role as SemanticsRole

// The entry, its cards and their frame, the parts every card shares, then the samples of the
// Actions cards. GalleryInputs.kt keeps the Inputs and Selection samples, GalleryPanels.kt the
// select and the menu, and UnstyledGallery.kt the Containment, Navigation and Feedback samples.

private const val DisabledContentAlpha = 0.38f
private const val DisabledContainerAlpha = 0.12f
private val GalleryButtonHeight = 40.dp
private val GalleryIconButtonSize = 40.dp
private val GalleryFocusRingWidth = 2.dp
private val GalleryTooltipGap = 6.dp

/**
 * Keys of what the gallery's controls remember in [DemoAppState].
 */
internal object GalleryKeys {
    const val List: String = "gallery.unstyled"
    const val Favourite: String = "gallery.unstyled.favourite"
    const val Starred: String = "gallery.unstyled.starred"
    const val Volume: String = "gallery.unstyled.volume"
    const val Newsletter: String = "gallery.unstyled.newsletter"
    const val Wifi: String = "gallery.unstyled.wifi"
    const val Plan: String = "gallery.unstyled.plan"
    const val SortShut: String = "gallery.unstyled.sortShut"
    const val Sort: String = "gallery.unstyled.sort"
    const val MenuShut: String = "gallery.unstyled.menuShut"
    const val Details: String = "gallery.unstyled.details"
    const val Files: String = "gallery.unstyled.files"
    const val Archive: String = "gallery.unstyled.archive"
    const val Tab: String = "gallery.unstyled.tab"
    const val Destination: String = "gallery.unstyled.destination"
}

/**
 * The Unstyled components gallery.
 *
 * Every card holds Compose Unstyled primitives the gallery styles itself from the pane's
 * `MaterialKolorTokens`, the way the Trips app does, so each part names the tokens it paints for
 * Inspect. Each control shows up enabled and disabled, apart from the few that hold nothing to
 * press. Nothing opens a popup, a window or a portal, since on the web the first one takes the
 * accessibility mirror over for good. The menu, the select's list and the tooltips show in place.
 * The gallery swallows right-button presses and hands its text fields a toolbar that never shows,
 * like the other galleries, and a state that changes goes into the name on the web through the
 * kit's fold modifiers.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal fun UnstyledGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    // Tells keyboard focus from a press, so a click leaves no tooltip or focus ring behind.
    FocusVisibilityProvider(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            GalleryGrid(
                cards = UnstyledCards,
                listState = state.rememberListState(GalleryKeys.List),
                gap = SectionGap,
                modifier = Modifier
                    .previewRoles(UnstyledGalleryComponent.Gallery)
                    .background(UnstyledToken.Surface.color)
                    .gallerySwallowRightPresses(),
                header = { group -> UnstyledGroupHeader(group) },
                card = { card, cardModifier -> UnstyledCardFrame(card, state, cardModifier) },
            )
        }
    }
}

/**
 * Every card of the Unstyled gallery, in the order they show within each group.
 */
internal val UnstyledCards: List<GalleryCard> = listOf(
    GalleryCard("Filled button", GalleryGroup.Actions) { ActionButtons(GalleryButtonStyle.Filled) },
    GalleryCard("Tonal button", GalleryGroup.Actions) { ActionButtons(GalleryButtonStyle.Tonal) },
    GalleryCard("Outlined button", GalleryGroup.Actions) { ActionButtons(GalleryButtonStyle.Outlined) },
    GalleryCard("Icon buttons", GalleryGroup.Actions) { IconButtons() },
    GalleryCard("Toggle button", GalleryGroup.Actions) { state -> ToggleButtons(state) },
    GalleryCard("Text field", GalleryGroup.Inputs) { state -> TextFields(state) },
    GalleryCard("Slider", GalleryGroup.Inputs) { state -> Sliders(state) },
    GalleryCard("Checkbox", GalleryGroup.Selection) { state -> Checkboxes(state) },
    GalleryCard("Switch", GalleryGroup.Selection) { state -> Switches(state) },
    GalleryCard("Radio group", GalleryGroup.Selection) { state -> RadioButtons(state) },
    GalleryCard("Select", GalleryGroup.Selection) { state -> InPlaceSelect(state) },
    GalleryCard("Menu", GalleryGroup.Selection) { state -> InPlaceMenu(state) },
    GalleryCard("Card", GalleryGroup.Containment) { SampleCards() },
    GalleryCard("Disclosure", GalleryGroup.Containment) { state -> Disclosures(state) },
    GalleryCard("Separators", GalleryGroup.Containment) { Separators() },
    GalleryCard("Scroll area", GalleryGroup.Containment) { state -> ScrollAreas(state) },
    GalleryCard("Tabs", GalleryGroup.Navigation) { state -> GalleryTabs(state) },
    GalleryCard("Navigation list", GalleryGroup.Navigation) { state -> NavigationList(state) },
    GalleryCard("Progress", GalleryGroup.Feedback) { GalleryProgress() },
    GalleryCard("Tooltip", GalleryGroup.Feedback) { InPlaceTooltip() },
    GalleryCard("Badges", GalleryGroup.Feedback) { Badges() },
)

@Composable
private fun UnstyledGroupHeader(group: GalleryGroup) {
    Text(
        text = group.name,
        modifier = Modifier.padding(top = Gap).semantics { heading() },
        style = TitleStyle,
        color = UnstyledToken.OnSurface.color,
    )
}

/**
 * The card a gallery card's controls sit in, titled with the card's name. It clips nothing, so a
 * tooltip can float past its edge.
 */
@Composable
private fun UnstyledCardFrame(
    card: GalleryCard,
    state: DemoAppState,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .previewRoles(UnstyledGalleryComponent.Card)
            .background(UnstyledToken.SurfaceContainerLow.color, CardShape)
            .border(1.dp, UnstyledToken.OutlineVariant.color, CardShape)
            .padding(SectionGap),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(card.title, style = HeadingStyle, color = UnstyledToken.OnSurface.color)
        card.content(state)
    }
}

/**
 * Enabled first, then disabled, the order every card shows its controls in.
 */
internal val EnabledThenDisabled: List<Boolean> = listOf(true, false)

/**
 * What a disabled part draws its content in, faded OnSurface.
 */
internal val disabledContent: Color
    @Composable get() = UnstyledToken.OnSurface.color.copy(alpha = DisabledContentAlpha)

/**
 * What a disabled part fills or outlines its container with, a fainter OnSurface.
 */
internal val disabledContainer: Color
    @Composable get() = UnstyledToken.OnSurface.color.copy(alpha = DisabledContainerAlpha)

/**
 * The color of [token] while [enabled], and the disabled content color otherwise.
 */
@Composable
internal fun tint(
    token: UnstyledToken,
    enabled: Boolean,
): Color = if (enabled) token.color else disabledContent

/**
 * The focus ring every gallery control shows while the keyboard has it.
 */
@Composable
internal fun Modifier.galleryFocusRing(
    interactions: MutableInteractionSource,
    offset: Boolean = false,
): Modifier =
    focusRing(
        interactionSource = interactions,
        width = GalleryFocusRingWidth,
        color = UnstyledToken.Primary.color,
        shape = ControlShape,
        offset = if (offset) 2.dp else 0.dp,
    )

/**
 * A control enabled and the same control disabled, side by side.
 */
@Composable
internal fun EnabledPair(content: @Composable RowScope.(enabled: Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
        for (enabled in EnabledThenDisabled) content(enabled)
    }
}

/**
 * A card's controls stacked, a little apart.
 */
@Composable
internal fun GalleryColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap), content = content)
}

/**
 * How a labelled button is painted.
 */
internal enum class GalleryButtonStyle(
    val component: UnstyledGalleryComponent,
) {
    /**
     * The main action, on primary.
     */
    Filled(UnstyledGalleryComponent.FilledButton),

    /**
     * A quieter action, on the secondary container.
     */
    Tonal(UnstyledGalleryComponent.TonalButton),

    /**
     * A quieter action still, outlined.
     */
    Outlined(UnstyledGalleryComponent.OutlinedButton),
}

/**
 * A labelled button with a leading icon, painted [style] while [enabled] and in faded OnSurface
 * otherwise.
 */
@Composable
internal fun GalleryButton(
    label: String,
    style: GalleryButtonStyle,
    enabled: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactions = remember { MutableInteractionSource() }
    val outlined = style == GalleryButtonStyle.Outlined
    val container = when {
        outlined -> Color.Transparent
        !enabled -> disabledContainer
        style == GalleryButtonStyle.Filled -> UnstyledToken.Primary.color
        else -> UnstyledToken.SecondaryContainer.color
    }
    val content = when {
        !enabled -> disabledContent
        style == GalleryButtonStyle.Filled -> UnstyledToken.OnPrimary.color
        style == GalleryButtonStyle.Tonal -> UnstyledToken.OnSecondaryContainer.color
        else -> UnstyledToken.Primary.color
    }
    val outline = when {
        !outlined -> Modifier
        enabled -> Modifier.border(1.dp, UnstyledToken.Outline.color, ControlShape)
        else -> Modifier.border(1.dp, disabledContainer, ControlShape)
    }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = SectionGap),
        modifier = modifier
            .height(GalleryButtonHeight)
            .previewRoles(enabled, style.component)
            .galleryFocusRing(interactions, offset = true)
            .clip(ControlShape)
            .background(container)
            .then(outline),
        interactionSource = interactions,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
            UnstyledIcon(icon, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
            Text(label, style = LabelStyle, color = content, maxLines = 1)
        }
    }
}

/**
 * An icon with no label of its own, so [label] names it and shows as a tooltip under it while a
 * pointer rests on it or the keyboard brings focus to it. Esc hides the tooltip. The row holding it
 * draws above what follows, so the tooltip floats over that.
 */
@Composable
internal fun GalleryIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactions = remember { MutableInteractionSource() }
    val visibility = tooltipVisibility(interactions)
    Box(modifier.then(visibility.onEscape).zIndex(if (visibility.shown) 1f else 0f)) {
        UnstyledButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(GalleryIconButtonSize)
                .previewRoles(enabled, UnstyledGalleryComponent.IconButton)
                .semantics { contentDescription = label }
                .galleryFocusRing(interactions)
                .clip(ControlShape),
            interactionSource = interactions,
        ) {
            UnstyledIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = tint(UnstyledToken.OnSurfaceVariant, enabled),
            )
        }
        if (enabled && visibility.shown) {
            val below = Overhang.BelowStart
            UnstyledTooltip(label, Modifier.align(below.alignment).overhang(below, GalleryTooltipGap))
        }
    }
}

@Composable
internal fun ActionButtons(style: GalleryButtonStyle) {
    val (label, icon) = when (style) {
        GalleryButtonStyle.Filled -> "Save" to Lucide.Check
        GalleryButtonStyle.Tonal -> "Add" to Lucide.Plus
        GalleryButtonStyle.Outlined -> "Share" to Lucide.Share2
    }
    EnabledPair { enabled -> GalleryButton(label, style, enabled, icon) }
}

@Composable
internal fun IconButtons() {
    GalleryColumn {
        for (enabled in EnabledThenDisabled) {
            // The enabled row's tooltips float over the disabled row under it.
            Row(Modifier.zIndex(if (enabled) 1f else 0f), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                GalleryIconButton(Lucide.Search, "Search", enabled)
                GalleryIconButton(Lucide.Settings, "Settings", enabled)
                GalleryIconButton(Lucide.Share2, "Share", enabled)
            }
        }
    }
}

@Composable
internal fun ToggleButtons(state: DemoAppState) {
    EnabledPair { enabled ->
        GalleryToggleButton(Lucide.Heart, "Favourite", state, GalleryKeys.Favourite, enabled)
        GalleryToggleButton(Lucide.Star, "Star", state, GalleryKeys.Starred, enabled)
    }
}

/**
 * An icon button that stays pressed in while it is on, a checkbox to assistive tech. Its name
 * carries whether it is on onto the web.
 */
@Composable
private fun GalleryToggleButton(
    icon: ImageVector,
    label: String,
    state: DemoAppState,
    key: String,
    enabled: Boolean,
) {
    val interactions = remember { MutableInteractionSource() }
    val checked = state.isOn(key)
    val container = when {
        !checked -> Color.Transparent
        enabled -> UnstyledToken.TertiaryContainer.color
        else -> disabledContainer
    }
    val content = when {
        !enabled -> disabledContent
        checked -> UnstyledToken.OnTertiaryContainer.color
        else -> UnstyledToken.OnSurfaceVariant.color
    }
    val component = if (checked) UnstyledGalleryComponent.CheckedToggleButton else UnstyledGalleryComponent.ToggleButton
    Box(
        modifier = Modifier
            .size(GalleryIconButtonSize)
            .previewRoles(enabled, component)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = SemanticsRole.Checkbox,
                onValueChange = { on -> state.setOn(key, on) },
            ).foldedToggleName(label, checked, enabled)
            .galleryFocusRing(interactions)
            .clip(ControlShape)
            .background(container)
            .border(1.dp, if (enabled) UnstyledToken.Outline.color else disabledContainer, ControlShape),
        contentAlignment = Alignment.Center,
    ) {
        UnstyledIcon(icon, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
    }
}
