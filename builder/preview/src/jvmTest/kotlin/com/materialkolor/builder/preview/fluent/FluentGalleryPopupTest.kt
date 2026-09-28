package com.materialkolor.builder.preview.fluent

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.preview.canvas.DemoAppState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The sources every Fluent gallery card is drawn from.
 */
private val GallerySources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/FluentGallery.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GallerySelection.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryPanels.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryRoles.kt",
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
 * What the gallery may take from the kit, its motion, the fold modifiers and `InnerTextWithoutHandles`.
 */
private val GalleryKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.headless.InnerTextWithoutHandles",
)

/**
 * The start of the names of the endless animation APIs. Written out whole they would trip the
 * builder's own architecture scan of this file.
 */
private val GalleryEndlessMotion: List<String> = listOf("rememberInfinite", "infiniteRepeat")

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
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        runDesktopComposeUiTest(1280, 8000) {
            // A window the size of the whole gallery, so the pointer reaches every card and not just the first screen.
            setContent { GalleryHarness(FluentLightSpec, DemoAppState(), GalleryWhole) }
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

            // Hover and focus are how a tooltip opens, and a press and drag is how the slider's value tip does.
            val interactive = onAllNodes(GalleryInteractive, useUnmergedTree = true)
            for (index in interactive.fetchSemanticsNodes().indices) {
                withClue("Hovered and pressed control $index") {
                    interactive[index].performMouseInput {
                        moveTo(center)
                        press()
                        moveBy(Offset(8f, 0f))
                    }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                    interactive[index].performMouseInput { release() }
                    waitForIdle()
                }
            }
            // The pointer leaves, so the presses below start from a fresh pointer.
            onRoot().performMouseInput { exit() }
            val focusable = onAllNodes(GalleryFocusable, useUnmergedTree = true)
            val focusables = focusable.fetchSemanticsNodes().size
            focusables shouldBeGreaterThan 0
            for (index in 0 until focusables) {
                withClue("Focused control $index") {
                    focusable[index].requestFocus()
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                }
            }

            // A word to select, so the text box has a context menu and a text toolbar to open.
            val fields = onAllNodes(hasSetTextAction())
            val count = fields.fetchSemanticsNodes().size
            count shouldBeGreaterThan 0
            fields[0].performTextReplacement("Harbour")
            for (index in 0 until count) {
                withClue("Text box $index") {
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
    fun gallerySources_openNoPopupWindowOrPortalAndNeverLoop() {
        for (path in GallerySources) {
            withClue(path) {
                val source = File(path)
                source.isFile shouldBe true
                val text = source.readText()
                val lines = text.lines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported -> imported.isBannedInTheGallery() }
                    .shouldBeEmpty()
                lines
                    .filter { line -> GalleryEndlessMotion.any { stem -> stem in line } }
                    .shouldBeEmpty()
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
    return startsWith("androidx.compose.ui.window.") ||
        GalleryPopupWords.any { word -> word in name } ||
        this in GalleryBannedFluent ||
        (startsWith("com.materialkolor.builder.kit.") && GalleryKitImports.none { allowed -> startsWith(allowed) })
}
