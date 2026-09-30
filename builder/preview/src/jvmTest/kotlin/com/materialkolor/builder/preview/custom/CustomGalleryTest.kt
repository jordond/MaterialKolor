package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.GalleryInteractive
import com.materialkolor.builder.preview.GalleryWhole
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.PreviewResult
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.galleryCardDeclaresRoles
import com.materialkolor.builder.preview.galleryDeclaresRoles
import com.materialkolor.builder.preview.galleryDescendants
import com.materialkolor.builder.preview.galleryFrame
import com.materialkolor.builder.preview.galleryInteractive
import com.materialkolor.builder.preview.galleryRowsOnScreen
import com.materialkolor.builder.preview.importedNames
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.sweepEveryControl
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
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
 * The cards whose kit control has no disabled look, or that hold nothing to press.
 */
private val GalleryNoDisabled: Set<String> = setOf("Tabs", "Badges", "Progress", "Tooltip", "Toast")

/**
 * Imports that open a popup, a window or an overlay, which on the web take the mirror over.
 */
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

/**
 * The sources every Custom gallery card is drawn from.
 */
private val GallerySources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/preview/custom/GalleryEntry.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/custom/CustomGallery.kt",
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
    fun gallery_underARedChrome_paintsThePanesSlots() =
        runDesktopComposeUiTest(1280, 800) {
            var chromeScheme: ColorScheme? = null
            setContent {
                GalleryHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize(), onChrome = { chromeScheme = it })
            }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap().let { map ->
                buildSet { for (x in 0 until map.width) for (y in 0 until map.height) add(map[x, y].toArgb()) }
            }
            val scheme = checkNotNull(chromeScheme)
            // Each pane slot beside the chrome role it would have leaked from.
            val pairs = listOf(
                CustomSlot.Primary to scheme.primary,
                CustomSlot.SurfaceSunken to scheme.surfaceContainerLow,
                CustomSlot.Surface to scheme.surface,
            )
            for ((slot, chromeRole) in pairs) {
                withClue(slot) {
                    val pane = PreviewResult.customSlots[slot, false].toColor().toArgb()
                    val chrome = chromeRole.toArgb()
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
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        runDesktopComposeUiTest(1280, 8000) {
            // A window the size of the whole gallery, so the pointer reaches every card and not just the first screen.
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            sweepEveryControl()
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
}

/**
 * The Custom gallery in a pane of [spec], under the red chrome, with motion frozen. [onChrome] hears
 * the chrome's own colour scheme, read outside the pane.
 */
@Composable
private fun GalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    onChrome: (ColorScheme) -> Unit = {},
) {
    val probe: ((String) -> Unit)? = composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides probe) {
        Chrome {
            onChrome(MaterialTheme.colorScheme)
            ProvideBuilderLayout(modifier = modifier) {
                val custom = remember(spec) { spec.on(Library.Custom) }
                PreviewPane(custom, Modifier.fillMaxSize()) { ComponentsTab(custom, state) }
            }
        }
    }
}
