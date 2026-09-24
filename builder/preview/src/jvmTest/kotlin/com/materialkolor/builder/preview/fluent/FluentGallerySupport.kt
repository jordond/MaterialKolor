package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec

// What the Fluent gallery tests share, the harness, the matchers and the state they compare.

/** The switches and boxes the gallery keeps in [DemoAppState]. */
internal val GallerySwitches: List<String> =
    listOf(FluentGalleryKeys.Bold, FluentGalleryKeys.Wifi, FluentGalleryKeys.Details)

/** The single choices the gallery keeps in [DemoAppState], each a switch per option. */
internal val GalleryChoices: List<String> = listOf(
    FluentGalleryKeys.Volume,
    FluentGalleryKeys.Delivery,
    FluentGalleryKeys.View,
    FluentGalleryKeys.Folder,
    FluentGalleryKeys.Tab,
    FluentGalleryKeys.Page,
)

/** Wide enough for four columns and tall enough that every card composes. */
internal val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/** Anything a user can press, type into or drag. */
internal val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

/** Anything that takes keyboard focus. */
internal val GalleryFocusable: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus)

/** A node named [name] exactly. */
internal fun galleryNamed(name: String): SemanticsMatcher = hasContentDescription(name)

internal fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its colors, itself or through the control it is part of. The nearest
 * node that declares them may be an ancestor, as long as it sits inside a card and is not the card's
 * frame among [frames]. A Fluent button keeps its clickable on a row inside the part its modifier
 * reaches.
 */
internal fun SemanticsNode.galleryDeclaresRoles(frames: Set<Int>): Boolean {
    val holder = generateSequence(this) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null &&
        holder.id !in frames &&
        generateSequence(holder.parent) { node -> node.parent }.any { node -> node.id in frames }
}

/** Every node under this one in the unmerged tree. */
internal fun SemanticsNode.galleryDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.galleryDescendants() }

/** The frame of the card called [title], the node its title text sits in. */
internal fun SemanticsNodeInteractionsProvider.galleryFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/** Whether anything in the card called [title] declares its colors, its frame aside. */
internal fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

/** Everything the gallery keeps in [DemoAppState], to tell whether anything changed. */
internal fun DemoAppState.gallerySnapshot(): List<Any> {
    val picks = GalleryChoices.flatMap { group -> (0 until 12).map { option -> isOn("$group.$option") } }
    return picks + GallerySwitches.map { switch -> isOn(switch) } +
        isChecked(FluentGalleryKeys.Updates) + isChecked(FluentGalleryKeys.Understood) + text
}

/**
 * The Fluent gallery in a Fluent pane of [spec], under the Fluent chrome, with motion frozen. With
 * [webFolds] the kit's fold modifiers fold state into names as they do on the web.
 */
@OptIn(KitTestApi::class)
@Composable
internal fun GalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    webFolds: Boolean = false,
) {
    val probe: ((String) -> Unit)? = composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides probe) {
        Chrome(FluentSkin) {
            if (webFolds) {
                ProvideWebFoldsForTest { PreviewPane(spec, modifier) { ComponentsTab(spec, state) } }
            } else {
                PreviewPane(spec, modifier) { ComponentsTab(spec, state) }
            }
        }
    }
}
