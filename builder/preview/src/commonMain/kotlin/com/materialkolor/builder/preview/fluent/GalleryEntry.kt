package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.split.PaneSpec
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Layer
import io.github.composefluent.component.Text

// The entry, its cards and their frame, and the parts every card shares. FluentGallery.kt keeps
// the Actions and Inputs samples, GallerySelection.kt the Selection ones, and GalleryPanels.kt the
// Containment, Navigation and Feedback ones.

/**
 * The space around the grid and between its cards.
 */
internal val GalleryGap = 16.dp

/**
 * The space between the controls inside a card.
 */
internal val Gap = 8.dp

/**
 * Keys of what the gallery's controls remember in [DemoAppState].
 */
internal object FluentGalleryKeys {
    const val List: String = "gallery.fluent"
    const val Bold: String = "gallery.fluent.bold"
    const val Volume: String = "gallery.fluent.volume"
    const val Updates: String = "gallery.fluent.updates"
    const val Delivery: String = "gallery.fluent.delivery"
    const val Wifi: String = "gallery.fluent.wifi"
    const val View: String = "gallery.fluent.view"
    const val Details: String = "gallery.fluent.details"
    const val Understood: String = "gallery.fluent.understood"
    const val Folder: String = "gallery.fluent.folder"
    const val Tab: String = "gallery.fluent.tab"
    const val Page: String = "gallery.fluent.page"
}

/**
 * The Fluent components gallery (F-21).
 *
 * Every card holds compose-fluent's own components on their default colors, under the Fluent theme
 * the pane already uses, and each shows up enabled and disabled apart from the few with nothing
 * to press. Nothing opens a popup, a window or a portal, since on the web the first one takes the
 * accessibility mirror over for good (D40). The slider's value tip, the dialog and the combo box
 * list are Fluent's popups, so the slider draws its own thumb, the dialog shows in place and the
 * combo box stays out. A control whose own clickable no modifier reaches, or which reports the
 * wrong role, sits under a [FluentOverlaid] layer that takes its place. The gallery swallows
 * right-button presses and hands its text box a toolbar that never shows, like the other
 * galleries, and a state that changes goes into the name on the web through the kit's fold
 * modifiers (D37).
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
internal fun FluentGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    Layer(
        modifier = modifier.fillMaxSize().previewRoles(FluentGalleryComponent.Gallery),
        shape = RectangleShape,
        color = FluentTheme.colors.background.solid.base,
        border = null,
    ) {
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            GalleryGrid(
                cards = FluentCards,
                listState = state.rememberListState(FluentGalleryKeys.List),
                gap = GalleryGap,
                modifier = Modifier.gallerySwallowRightPresses(),
                header = { group -> FluentGroupHeader(group) },
                card = { card, cardModifier -> FluentCardFrame(card, state, cardModifier) },
            )
        }
    }
}

/**
 * Every card of the Fluent gallery, in the order they show within each group.
 */
internal val FluentCards: List<GalleryCard> = listOf(
    GalleryCard("Button", GalleryGroup.Actions) { StandardButtons() },
    GalleryCard("Accent button", GalleryGroup.Actions) { AccentButtons() },
    GalleryCard("Subtle button", GalleryGroup.Actions) { SubtleButtons() },
    GalleryCard("Icon buttons", GalleryGroup.Actions) { IconButtons() },
    GalleryCard("Toggle button", GalleryGroup.Actions) { state -> ToggleButtons(state) },
    GalleryCard("Text box", GalleryGroup.Inputs) { state -> TextBoxes(state) },
    GalleryCard("Slider", GalleryGroup.Inputs) { state -> Sliders(state) },
    GalleryCard("Check box", GalleryGroup.Selection) { state -> CheckBoxes(state) },
    GalleryCard("Radio button", GalleryGroup.Selection) { state -> RadioButtons(state) },
    GalleryCard("Toggle switch", GalleryGroup.Selection) { state -> ToggleSwitches(state) },
    GalleryCard("Segmented control", GalleryGroup.Selection) { state -> SegmentedControls(state) },
    GalleryCard("Card", GalleryGroup.Containment) { SampleCards() },
    GalleryCard("Expander", GalleryGroup.Containment) { state -> Expanders(state) },
    GalleryCard("Dialog", GalleryGroup.Containment) { state -> InPlaceDialog(state) },
    GalleryCard("Selector bar", GalleryGroup.Navigation) { state -> SelectorBars(state) },
    GalleryCard("Tabs", GalleryGroup.Navigation) { state -> Tabs(state) },
    GalleryCard("Navigation list", GalleryGroup.Navigation) { state -> NavigationList(state) },
    GalleryCard("Info bar", GalleryGroup.Feedback) { InfoBars() },
    GalleryCard("Progress bar", GalleryGroup.Feedback) { ProgressBars() },
    GalleryCard("Progress ring", GalleryGroup.Feedback) { ProgressRings() },
    GalleryCard("Badges", GalleryGroup.Feedback) { Badges() },
)

@Composable
private fun FluentGroupHeader(group: GalleryGroup) {
    Text(
        text = group.name,
        modifier = Modifier.padding(top = Gap).semantics { heading() },
        style = FluentTheme.typography.subtitle,
    )
}

/**
 * The Fluent card a gallery card's controls sit in, titled with the card's name.
 */
@Composable
private fun FluentCardFrame(
    card: GalleryCard,
    state: DemoAppState,
    modifier: Modifier,
) {
    Layer(
        modifier = modifier.previewRoles(FluentGalleryComponent.Card),
        shape = FluentTheme.shapes.overlay,
        color = FluentTheme.colors.background.card.default,
    ) {
        Column(Modifier.padding(GalleryGap), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(card.title, style = FluentTheme.typography.bodyStrong)
            card.content(state)
        }
    }
}

/**
 * Enabled first, then disabled, the order every card shows its controls in.
 */
internal val EnabledThenDisabled: List<Boolean> = listOf(true, false)

/**
 * A control enabled and the same control disabled, in a row that wraps when the card is narrow.
 */
@Composable
internal fun EnabledAndDisabled(content: @Composable (enabled: Boolean) -> Unit) {
    Wrapping {
        for (enabled in EnabledThenDisabled) content(enabled)
    }
}

/**
 * Lays its content out in a row that wraps when the card is narrow.
 */
@Composable
internal fun Wrapping(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A card's controls stacked, a little apart.
 */
@Composable
internal fun GalleryColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Gap), content = content)
}
