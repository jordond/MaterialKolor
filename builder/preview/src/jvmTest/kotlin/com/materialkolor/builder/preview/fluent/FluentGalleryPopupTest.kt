package com.materialkolor.builder.preview.fluent

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.preview.GalleryWhole
import com.materialkolor.builder.preview.PaneKitImports
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.importedNames
import com.materialkolor.builder.preview.isKitImportBeyond
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.opensAWindow
import com.materialkolor.builder.preview.sweepEveryControl
import com.materialkolor.builder.preview.sweepWholeGallery
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The sources every Fluent gallery card is drawn from.
 */
private val GallerySources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryEntry.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/fluent/FluentGallery.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/fluent/GallerySelection.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryPanels.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryRoles.kt",
)

/**
 * Words in the names of what opens a popup, a window or a portal, which on the web take the mirror
 * over. Fluent's side and top navigation carry tooltips, and its menu items flyouts.
 */
private val GalleryPopupWords: List<String> = listOf(
    "Popup",
    "Dialog",
    "Modal",
    "Flyout",
    "Tooltip",
    "Portal",
    "ContextMenu",
    "ComboBox",
    "DropDown",
    "Dropdown",
    "CommandBar",
    "SideNav",
    "TopNav",
    "NavigationView",
    "MenuItem",
)

/**
 * Fluent's own parts the gallery must not use. Its slider's thumb opens a popup while dragged, its
 * text field keeps its inner text away from the kit's `InnerTextWithoutHandles`, and its expander
 * animates past frozen motion.
 */
private val GalleryBannedFluent: Set<String> = setOf(
    "io.github.composefluent.component.Slider",
    "io.github.composefluent.component.TextField",
    "io.github.composefluent.component.Expander",
)

/**
 * A progress bar or ring called without a value, the endless kind.
 */
private val EndlessProgress = Regex("""\bProgress(Bar|Ring)\((?!\s*progress\b)""")

/**
 * The Fluent gallery never opens a popup, a window or a portal, which on the web take the mirror
 * over, and never loops.
 */
@OptIn(ExperimentalTestApi::class)
class FluentGalleryPopupTest {
    @Test
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() {
        // A press and drag is how the slider's value tip opens, beside the tooltips of hover and focus.
        sweepWholeGallery(pressAndDrag = true) { FluentGalleryHarness(FluentLightSpec, DemoAppState(), GalleryWhole) }
    }

    @Test
    fun gallerySources_openNoPopupWindowOrPortalAndNeverLoop() {
        val sources = GallerySources.map { path -> moduleSource(path) }
        checkSourcesOpenNothingAndNeverLoop(sources) { imported -> imported.isBannedInTheGallery() }
        for (source in sources) {
            withClue(source.name) {
                val text = source.readText()
                EndlessProgress
                    .findAll(text)
                    .map { match -> match.value }
                    .toList()
                    .shouldBeEmpty()
                // Only the theme, never the one that adds a host and a backdrop round the screen.
                ("FluentTheme(" in text) shouldBe false
                ("SliderDefaults.Thumb" in text) shouldBe false
            }
        }
    }
}

private fun String.isBannedInTheGallery(): Boolean {
    val name = substringAfterLast('.')
    return opensAWindow() ||
        GalleryPopupWords.any { word -> word in name } ||
        this in GalleryBannedFluent ||
        isKitImportBeyond(PaneKitImports)
}
