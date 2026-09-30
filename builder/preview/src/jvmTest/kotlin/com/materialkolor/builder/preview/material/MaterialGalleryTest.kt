package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.GalleryInteractive
import com.materialkolor.builder.preview.GalleryWhole
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.TextToolbarProbe
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.galleryCardDeclaresRoles
import com.materialkolor.builder.preview.galleryDescendants
import com.materialkolor.builder.preview.galleryFrame
import com.materialkolor.builder.preview.galleryInteractive
import com.materialkolor.builder.preview.galleryRowsOnScreen
import com.materialkolor.builder.preview.importedNames
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.pressEveryControl
import com.materialkolor.builder.preview.rightClickAndLongPressEveryField
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
 * The cards whose component Material 3 gives no disabled look, or that hold nothing to press.
 */
private val GalleryNoDisabled: Set<String> =
    setOf("Floating action button", "Extended FAB", "Tabs", "Progress indicators", "Snackbar", "Tooltips")

/**
 * Imports that open a popup or a window, which on the web take the accessibility mirror over.
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
    // Their touch selection handles are popups too, so the gallery draws them from their parts.
    "androidx.compose.material3.TextField",
    "androidx.compose.material3.OutlinedTextField",
)

/**
 * The sources every gallery card is drawn from, and the Trips screen that holds the note.
 */
private val GallerySources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/preview/canvas/ComponentsTab.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/material/GalleryEntry.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/material/MaterialGallery.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/material/GalleryFeedback.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/material/SampleFields.kt",
    // The Trips note is a sample field too, so the text field ban reaches it.
    "commonMain/kotlin/com/materialkolor/builder/preview/material/TripDetail.kt",
    "inkletMain/kotlin/com/materialkolor/builder/preview/inklet/InkletGallery.kt",
    "inkletMain/kotlin/com/materialkolor/builder/preview/inklet/InkletGalleryPanels.kt",
    "inkletMain/kotlin/com/materialkolor/builder/preview/inklet/InkletTripDetail.kt",
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
            val toolbar = TextToolbarProbe()
            setContent {
                CompositionLocalProvider(LocalTextToolbar provides toolbar) {
                    GalleryHarness(LightSpec, DemoAppState(), GalleryWhole)
                }
            }
            waitForIdle()

            pressEveryControl()
            // A word to select, so a text field has a context menu and a text toolbar to open.
            rightClickAndLongPressEveryField(
                onAllNodes(hasSetTextAction() and hasText("Destination")).assertCountEquals(2),
                "Lisbon",
            )
            toolbar.shown shouldBe 0
        }

    @Test
    fun gallerySources_importNothingThatOpensAPopup() {
        for (path in GallerySources) {
            withClue(path) {
                moduleSource(path)
                    .importedNames()
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
 * Whether the node declares its roles. A range slider's thumbs are nodes of their own under the
 * slider, so a node that is only dragged may leave its roles to its parent.
 */
private fun SemanticsNode.galleryDeclaresRoles(): Boolean {
    if (PreviewRoles in config) return true
    val onlyDragged = SemanticsActions.OnClick !in config && SemanticsActions.SetText !in config
    return onlyDragged && parent?.let { slider -> PreviewRoles in slider.config } == true
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
