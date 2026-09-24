package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.ChromeResult
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.PreviewResult
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on.
 */
private const val GalleryScreenshotDir = "src/jvmTest/screenshots/gallery"

/** The chrome the gallery sits in, the Custom skin coloured from the red chrome document. */
private val GallerySkin: Skin = Skin(Library.Custom, expressive = false)

/** The phone and desktop frames the gallery is checked at, at the height of a first screen. */
private val GalleryFrames: List<IntSize> = listOf(IntSize(412, 900), IntSize(1280, 800))

/** How many columns of 280 dp cards, 16 dp apart, fit across each frame's width. */
private val GalleryColumns: Map<Int, Int> = mapOf(412 to 1, 1280 to 4)

/** Wide enough for four columns and tall enough that every card composes. */
private val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/** The cards whose kit control has no disabled look, or that hold nothing to press. */
private val GalleryNoDisabled: Set<String> = setOf("Tabs", "Badges", "Progress", "Tooltip", "Toast")

/** Imports that open a popup, a window or an overlay, which on the web take the mirror over (D40). */
private val GalleryPopupImports: List<String> = listOf(
    "com.materialkolor.builder.kit.control.BuilderMenu",
    "com.materialkolor.builder.kit.control.BuilderTooltip",
    "com.materialkolor.builder.kit.control.BuilderDialog",
    "com.materialkolor.builder.kit.control.BuilderSheet",
    "com.materialkolor.builder.kit.control.BuilderSidePanel",
    "com.materialkolor.builder.kit.control.BuilderBottomSheet",
    "com.materialkolor.builder.kit.control.BuilderToastHost",
    "com.materialkolor.builder.kit.widget.CodeView",
    "com.materialkolor.builder.kit.widget.SchemeChip",
    "com.materialkolor.builder.kit.widget.SwatchTile",
)

/** The sources every Custom gallery card is drawn from. */
private val GallerySources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/custom/GalleryEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/custom/CustomGallery.kt",
)

@OptIn(ExperimentalTestApi::class)
class CustomGalleryTest {
    @Test
    fun cards_everyGroup_holdUniquelyNamedKitControls() {
        CustomCards.map { card -> card.title }.distinct().size shouldBe CustomCards.size
        GalleryGroup.entries.filter { group -> CustomCards.none { card -> card.group == group } }.shouldBeEmpty()
    }

    @Test
    fun controls_everyCard_declareTheirOwnSlots() =
        runComposeUiTest {
            val composed = mutableSetOf<String>()
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole, composed) }
            waitForIdle()
            composed shouldBe CustomCards.map { card -> card.title }.toSet()

            val frames = CustomCards.map { card -> galleryFrame(card.title).id }.toSet()
            // Unmerged, since a merged node also carries the roles its children declared.
            val controls = onAllNodes(GalleryInteractive, useUnmergedTree = true).fetchSemanticsNodes()
            controls.shouldNotBeEmpty()
            controls
                .filterNot { node -> node.galleryDeclaresRoles(frames) }
                .map { node -> node.config.toString() }
                .shouldBeEmpty()
            CustomCards
                .filterNot { card -> galleryCardDeclaresRoles(card.title) }
                .map { card -> card.title }
                .shouldBeEmpty()
        }

    @Test
    fun cards_everyControlWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            for (card in CustomCards) {
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
    fun gallery_underARedCustomChrome_paintsThePanesSlots() =
        runDesktopComposeUiTest(1280, 800) {
            setContent { GalleryHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize()) }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap().let { map ->
                buildSet { for (x in 0 until map.width) for (y in 0 until map.height) add(map[x, y].toArgb()) }
            }
            for (slot in listOf(CustomSlot.Primary, CustomSlot.SurfaceSunken, CustomSlot.Surface)) {
                withClue(slot) {
                    val pane = PreviewResult.customSlots[slot, false].toColor().toArgb()
                    val chrome = ChromeResult.chromeCustomSlots[slot, false].toColor().toArgb()
                    (pane != chrome) shouldBe true
                    pixels shouldContain pane
                    pixels shouldNotContain chrome
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
                    composed.size shouldBeGreaterThan 0
                    composed.size shouldBeLessThanOrEqual bound
                    composed shouldNotContain CustomCards.last().title
                    composed
                        .filterNot { title -> galleryCardDeclaresRoles(title) }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun gallery_everyControlPressedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        runComposeUiTest {
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
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
            val fields = onAllNodes(hasSetTextAction())
            val count = fields.fetchSemanticsNodes().size
            count shouldBeGreaterThan 0
            fields[0].performTextReplacement("Harbour")
            for (index in 0 until count) {
                withClue("Text field $index") {
                    fields[index].performMouseInput { rightClick() }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                    fields[index].performTouchInput { longClick() }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                }
            }
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
    fun screens_bothFramesBothModes_render() {
        for (frame in GalleryFrames) {
            withClue(frame) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(LightSpec)
                    setContent { GalleryHarness(spec, DemoAppState(), Modifier.fillMaxSize()) }

                    for (mode in listOf(LightSpec, DarkSpec)) {
                        spec = mode
                        waitForIdle()
                        onNodeWithText(GalleryGroup.Actions.name).assertExists()
                        onRoot().captureRoboImage("$GalleryScreenshotDir/custom-${frame.width}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun screens_wholeGalleryBothModes_renderEveryCard() =
        runDesktopComposeUiTest(1280, 2400) {
            // Four columns, and tall enough that the last card is on screen too.
            var spec by mutableStateOf(LightSpec)
            setContent { GalleryHarness(spec, DemoAppState(), Modifier.fillMaxSize()) }

            for (mode in listOf(LightSpec, DarkSpec)) {
                spec = mode
                waitForIdle()
                onNodeWithText(CustomCards.last().title).assertExists()
                onRoot().captureRoboImage("$GalleryScreenshotDir/custom-whole-${mode.label}.png")
            }
        }
}

/** Anything a user can press, type into or drag. */
private val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

private fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its slots, itself or through the kit control it is part of. The
 * options of a segmented control or a tab row are nodes of their own under the control, so the
 * nearest node with roles may be an ancestor, as long as it sits inside a card and is not the
 * card's frame among [frames].
 */
private fun SemanticsNode.galleryDeclaresRoles(frames: Set<Int>): Boolean {
    val holder = generateSequence(this) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null &&
        holder.id !in frames &&
        generateSequence(holder.parent) { node -> node.parent }.any { node -> node.id in frames }
}

/** Every node under this one in the unmerged tree. */
private fun SemanticsNode.galleryDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.galleryDescendants() }

/** The frame of the card called [title], the node its title text sits in. */
private fun SemanticsNodeInteractionsProvider.galleryFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/** Whether anything in the card called [title] declares roles, its frame aside. */
private fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

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

/** The Custom gallery in a pane of [spec], under a red Custom chrome, with motion frozen. */
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
        Chrome(GallerySkin) {
            ProvideBuilderLayout(modifier = modifier) {
                PreviewPane(spec, Modifier.fillMaxSize()) { ComponentsTab(spec, state) }
            }
        }
    }
}
