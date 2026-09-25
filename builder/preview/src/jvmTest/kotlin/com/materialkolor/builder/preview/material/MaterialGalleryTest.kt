package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The phone and desktop frames the gallery is checked at, at the height of a first screen.
 */
private val GalleryFrames: List<IntSize> = listOf(IntSize(412, 900), IntSize(1280, 800))

/**
 * How many columns of 280 dp cards, 16 dp apart, fit across each frame's width.
 */
private val GalleryColumns: Map<Int, Int> = mapOf(412 to 1, 1280 to 4)

/**
 * Wide enough for four columns and tall enough that every card composes.
 */
private val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/**
 * The cards whose component Material 3 gives no disabled look, or that hold nothing to press.
 */
private val GalleryNoDisabled: Set<String> =
    setOf("Floating action button", "Extended FAB", "Tabs", "Progress indicators", "Snackbar", "Tooltips")

/**
 * Imports that open a popup or a window, which on the web take the accessibility mirror over (D40).
 */
private val GalleryPopupImports: List<String> = listOf(
    "androidx.compose.material3.DropdownMenu",
    "androidx.compose.material3.ExposedDropdownMenuBox",
    "androidx.compose.material3.AlertDialog",
    "androidx.compose.material3.BasicAlertDialog",
    "androidx.compose.material3.DatePickerDialog",
    "androidx.compose.material3.TimePickerDialog",
    "androidx.compose.material3.TooltipBox",
    "androidx.compose.material3.ModalBottomSheet",
    // b-228b
    // Their touch selection handles are popups too, so the gallery draws them from their parts (D45).
    "androidx.compose.material3.TextField",
    "androidx.compose.material3.OutlinedTextField",
)

/**
 * The sources every gallery card is drawn from, and the Trips screen that holds the note.
 */
private val GallerySources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/canvas/ComponentsTab.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/material/GalleryEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/material/MaterialGallery.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/material/GalleryFeedback.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/material/SampleFields.kt", // b-228b
    // b-228c
    // The Trips note is a sample field too, so the text field ban reaches it.
    "src/commonMain/kotlin/com/materialkolor/builder/preview/material/TripDetail.kt",
)

@OptIn(ExperimentalTestApi::class)
class MaterialGalleryTest {
    @Test
    fun cards_everyGroup_holdAboutThirtyUniquelyNamedComponents() {
        MaterialCards.size shouldBeInRange 28..36
        MaterialCards.map { card -> card.title }.distinct().size shouldBe MaterialCards.size
        GalleryGroup.entries.filter { group -> MaterialCards.none { card -> card.group == group } }.shouldBeEmpty()
    }

    @Test
    fun controls_everyCard_declareTheirOwnRoles() =
        runComposeUiTest {
            val composed = mutableSetOf<String>()
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole, composed) }
            waitForIdle()
            composed shouldBe MaterialCards.map { card -> card.title }.toSet()

            // Unmerged, since a merged node also carries the roles its children declared.
            val controls = onAllNodes(GalleryInteractive, useUnmergedTree = true).fetchSemanticsNodes()
            controls.shouldNotBeEmpty()
            controls
                .filterNot { node -> node.galleryDeclaresRoles() }
                .map { node -> node.config.toString() }
                .shouldBeEmpty()
            MaterialCards
                .filterNot { card -> galleryCardDeclaresRoles(card.title) }
                .map { card -> card.title }
                .shouldBeEmpty()
        }

    @Test
    fun cards_everyComponentWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            for (card in MaterialCards) {
                withClue(card.title) {
                    val nodes = galleryFrame(card.title).galleryDescendants()
                    val disabled = nodes.count { node -> SemanticsProperties.Disabled in node.config }
                    val enabled = nodes.count { node ->
                        node.galleryInteractive() && SemanticsProperties.Disabled !in node.config
                    }
                    if (card.title in GalleryNoDisabled) {
                        disabled shouldBe 0
                    } else {
                        (enabled > 0 && disabled > 0) shouldBe true
                    }
                }
            }
        }

    @Test
    fun gallery_everyFirstScreen_composesOnlyTheCardsInViewAndTheirRoles() {
        for (frame in GalleryFrames) {
            withClue(frame) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    val composed = mutableSetOf<String>()
                    setContent { GalleryHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize(), composed) }
                    waitForIdle()

                    // The rows on screen and the one the list prefetches below them, and no more.
                    val bound = (galleryRowsOnScreen(composed) + 1) * GalleryColumns.getValue(frame.width)
                    composed.size shouldBeLessThanOrEqual bound
                    composed shouldNotContain MaterialCards.last().title
                    composed
                        .filterNot { title -> galleryCardDeclaresRoles(title) }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun gallery_everyControlPressed_opensNoPopupOrWindow() =
        runComposeUiTest {
            val toolbar = GalleryToolbarProbe()
            setContent {
                CompositionLocalProvider(LocalTextToolbar provides toolbar) {
                    GalleryHarness(LightSpec, DemoAppState(), GalleryWhole)
                }
            }
            waitForIdle()

            val pressable = onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
            pressable.shouldNotBeEmpty()
            runOnIdle {
                for (node in pressable) {
                    if (SemanticsProperties.Disabled !in node.config) {
                        node.config
                            .getOrNull(SemanticsActions.OnClick)
                            ?.action
                            ?.invoke()
                    }
                }
            }
            waitForIdle()
            onAllNodes(isRoot()).assertCountEquals(1)

            // A word to select, so a text field has a context menu and a text toolbar to open.
            val destinations = onAllNodes(hasSetTextAction() and hasText("Destination")).assertCountEquals(2)
            destinations[0].performTextReplacement("Lisbon")
            for (index in 0..1) {
                withClue("Destination field $index") {
                    destinations[index].performMouseInput { rightClick() }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                    destinations[index].performTouchInput { longClick() }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                }
            }
            toolbar.shown shouldBe 0
        }

    @Test
    fun gallerySources_importNothingThatOpensAPopup() {
        for (path in GallerySources) {
            withClue(path) {
                val source = File(path)
                source.isFile shouldBe true
                source
                    .readLines()
                    .map { line -> line.trim() }
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported ->
                        imported in GalleryPopupImports ||
                            imported.startsWith("androidx.compose.ui.window.")
                    }.shouldBeEmpty()
            }
        }
    }

    @Test
    fun segmentedButton_pickedInOneCopyOfASplit_showsPickedInBoth() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome {
                        SplitPreview(LightSpec, DarkSpec, SplitState(0.5f), GalleryWhole) { spec ->
                            ComponentsTab(spec, state)
                        }
                    }
                }
            }
            val bothWeeks = onAllNodes(isSelectable() and hasAnyDescendant(hasText("Week")), useUnmergedTree = true)

            onNode(hasClickAction() and hasText("Week")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            bothWeeks.assertCountEquals(2).assertAll(isSelected())
        }
}

/**
 * Anything a user can press, type into or drag.
 */
private val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

private fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its roles. A range slider's thumbs are nodes of their own under the
 * slider, so a node that is only dragged may leave its roles to its parent.
 */
private fun SemanticsNode.galleryDeclaresRoles(): Boolean {
    if (PreviewRoles in config) return true
    val onlyDragged = SemanticsActions.OnClick !in config && SemanticsActions.SetText !in config
    return onlyDragged && parent?.let { slider -> PreviewRoles in slider.config } == true
}

/**
 * Every node under this one in the unmerged tree.
 */
private fun SemanticsNode.galleryDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.galleryDescendants() }

/**
 * The frame of the card called [title], the node its title text sits in.
 */
private fun SemanticsNodeInteractionsProvider.galleryFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/**
 * How many rows of the [composed] cards show at least partly on screen. The cards of a row share
 * their top edge, so each distinct top is a row.
 */
private fun SemanticsNodeInteractionsProvider.galleryRowsOnScreen(composed: Set<String>): Int {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return composed
        .map { title -> galleryFrame(title) }
        .filter { frame -> frame.layoutInfo.isPlaced && frame.boundsInRoot.overlaps(screen) }
        .map { frame -> frame.boundsInRoot.top }
        .distinct()
        .size
}

/**
 * Whether anything in the card called [title] declares roles, its frame aside.
 */
private fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

/**
 * A text toolbar that counts how often it is asked to show, where the web's would open a popup.
 */
private class GalleryToolbarProbe : TextToolbar {
    var shown: Int = 0
        private set

    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        shown++
    }

    override fun hide() = Unit
}

/**
 * The Material 3 gallery in a pane of [spec], under the chrome, with motion frozen.
 */
@Composable
private fun GalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
) {
    val probe: ((String) -> Unit)? = composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides probe) {
        Chrome {
            PreviewPane(spec, modifier) { ComponentsTab(spec, state) }
        }
    }
}
