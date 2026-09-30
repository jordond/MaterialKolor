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
import com.materialkolor.builder.preview.checkCardsFillEveryGroup
import com.materialkolor.builder.preview.checkCardsShowEnabledAndDisabled
import com.materialkolor.builder.preview.checkFirstScreenComposesOnlyCardsInView
import com.materialkolor.builder.preview.checkGalleryControlsDeclareRoles
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.galleryCardDeclaresRoles
import com.materialkolor.builder.preview.galleryDeclaresRoles
import com.materialkolor.builder.preview.galleryDescendants
import com.materialkolor.builder.preview.galleryFrame
import com.materialkolor.builder.preview.galleryInteractive
import com.materialkolor.builder.preview.galleryProbe
import com.materialkolor.builder.preview.galleryRowsOnScreen
import com.materialkolor.builder.preview.importedNames
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.opensAWindow
import com.materialkolor.builder.preview.screenColors
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.sweepEveryControl
import com.materialkolor.builder.preview.sweepWholeGallery
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
    fun cards_everyGroup_holdUniquelyNamedKitControls() = checkCardsFillEveryGroup(CustomCards)

    @Test
    fun controls_everyCard_declareTheirOwnSlots() =
        runComposeUiTest {
            val composed = mutableSetOf<String>()
            setContent { CustomGalleryHarness(LightSpec, DemoAppState(), GalleryWhole, composed) }
            waitForIdle()

            checkGalleryControlsDeclareRoles(CustomCards, composed)
        }

    @Test
    fun cards_everyControlWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { CustomGalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            checkCardsShowEnabledAndDisabled(CustomCards, GalleryNoDisabled)
        }

    @Test
    fun gallery_underARedChrome_paintsThePanesSlots() =
        runDesktopComposeUiTest(1280, 800) {
            var chromeScheme: ColorScheme? = null
            setContent {
                CustomGalleryHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize()) { scheme ->
                    chromeScheme = scheme
                }
            }
            waitForIdle()

            val pixels = screenColors()
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
    fun gallery_everyFirstScreen_composesOnlyTheCardsInViewAndTheirRoles() =
        checkFirstScreenComposesOnlyCardsInView(CustomCards.last().title) { composed ->
            CustomGalleryHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize(), composed)
        }

    @Test
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        sweepWholeGallery { CustomGalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }

    @Test
    fun gallerySources_importNothingThatOpensAPopup() =
        checkSourcesOpenNothingAndNeverLoop(GallerySources.map { path -> moduleSource(path) }) { imported ->
            imported in GalleryPopupImports || imported.opensAWindow()
        }
}

/**
 * The Custom gallery in a pane of [spec], under the red chrome, with motion frozen. [onChrome] hears
 * the chrome's own colour scheme, read outside the pane.
 */
@Composable
private fun CustomGalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    onChrome: (ColorScheme) -> Unit = {},
) {
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides galleryProbe(composed)) {
        Chrome {
            onChrome(MaterialTheme.colorScheme)
            ProvideBuilderLayout(modifier = modifier) {
                val custom = remember(spec) { spec.on(Library.Custom) }
                PreviewPane(custom, Modifier.fillMaxSize()) { ComponentsTab(custom, state) }
            }
        }
    }
}
