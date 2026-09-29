package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.motion.LocalTabVisible
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGrid
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.GalleryHiddenTextToolbar
import com.materialkolor.builder.preview.canvas.gallerySwallowRightPresses
import com.materialkolor.builder.preview.material.MaterialCardFrame
import com.materialkolor.builder.preview.material.MaterialGroupHeader
import com.materialkolor.builder.preview.material.SectionGap
import com.materialkolor.builder.preview.material.previewRoles
import com.materialkolor.builder.preview.split.PaneSpec
import dev.ggoggam.inklet.InkletStyle
import dev.ggoggam.inklet.InkletTheme

/**
 * The pen every Inklet pane draws with, Inklet's own defaults.
 */
private val PaneStyle = InkletStyle()

@Composable
internal actual fun InkletPaneTheme(content: @Composable () -> Unit) {
    val still = LocalMotionFrozen.current || LocalReducedMotion.current || !LocalTabVisible.current
    InkletTheme(style = PaneStyle, reduceMotion = still, content = content)
}

/**
 * The Inklet components gallery.
 *
 * It follows the Material 3 gallery, the same grid, group headers and card frames, with Inklet's
 * sketched components in the cards. Every sketch has a fixed seed, so both copies of a split draw
 * the same strokes, and every control keeps its state in [DemoAppState] so both copies agree. Each
 * component shows up enabled and disabled where Inklet gives it a disabled look. Nothing opens a
 * popup, and the text fields get the same hidden toolbar and swallowed right clicks as Material's.
 */
@Composable
internal actual fun InkletGalleryEntry(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
) {
    Surface(modifier.fillMaxSize().previewRoles(Role.Surface, Role.OnSurface)) {
        CompositionLocalProvider(LocalTextToolbar provides GalleryHiddenTextToolbar) {
            GalleryGrid(
                cards = InkletCards,
                listState = state.rememberListState("gallery.inklet"),
                gap = SectionGap,
                modifier = Modifier.gallerySwallowRightPresses(),
                header = { group -> MaterialGroupHeader(group) },
                card = { card, cardModifier -> MaterialCardFrame(card, state, cardModifier) },
            )
        }
    }
}

@Composable
internal actual fun InkletAppEntry(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier,
) {
    InkletTrips(state, deviceWidth, modifier)
}

/**
 * Every card of the Inklet gallery, in the order they show within each group.
 */
internal val InkletCards: List<GalleryCard> = listOf(
    GalleryCard("Solid button", GalleryGroup.Actions) { SolidButtons() },
    GalleryCard("Outline button", GalleryGroup.Actions) { OutlineButtons() },
    GalleryCard("Scribble button", GalleryGroup.Actions) { ScribbleButtons() },
    GalleryCard("Icon buttons", GalleryGroup.Actions) { state -> IconButtons(state) },
    GalleryCard("Badge", GalleryGroup.Actions) { Badges() },
    GalleryCard("Text field", GalleryGroup.Inputs) { state -> TextFields(state) },
    GalleryCard("Slider", GalleryGroup.Inputs) { state -> Sliders(state) },
    GalleryCard("Checkbox", GalleryGroup.Selection) { state -> Checkboxes(state) },
    GalleryCard("Radio button", GalleryGroup.Selection) { state -> RadioButtons(state) },
    GalleryCard("Toggle", GalleryGroup.Selection) { state -> Toggles(state) },
    GalleryCard("Chips", GalleryGroup.Selection) { state -> Chips(state) },
    GalleryCard("Card", GalleryGroup.Containment) { Cards() },
    GalleryCard("Divider", GalleryGroup.Containment) { Dividers() },
    GalleryCard("Surfaces", GalleryGroup.Containment) { Surfaces() },
    GalleryCard("Decorations", GalleryGroup.Containment) { Decorations() },
    GalleryCard("Tabs", GalleryGroup.Navigation) { state -> Tabs(state) },
    GalleryCard("Linear progress", GalleryGroup.Feedback) { LinearProgress() },
    GalleryCard("Circular progress", GalleryGroup.Feedback) { CircularProgress() },
)
