package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.command.InWorkspace
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideOverlaysInTreeForTest
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.WindowClass
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.test.Test

private const val HEIGHT = 800

/** How long a dropdown pick may take to reach the document, its menu's exit included. */
private const val PICK_TIMEOUT_MS = 5_000L

/** What the switcher calls each library, as `strings_topbar.xml` has them. */
private val NAMES = mapOf(
    LibraryChoice.M3 to "M3",
    LibraryChoice.Expressive to "Expressive",
    LibraryChoice.Unstyled to "Unstyled",
    LibraryChoice.Fluent to "Fluent",
    LibraryChoice.Custom to "Custom",
)

private val InBar: SemanticsMatcher = hasAnyAncestor(hasTestTag(TOP_BAR_TAG)) and InWorkspace
private val InChips: SemanticsMatcher = hasAnyAncestor(hasTestTag(LIBRARY_CHIP_ROW_TAG))
private val InSwitcher: SemanticsMatcher =
    hasAnyAncestor(hasTestTag(LIBRARY_SWITCHER_TAG)) or hasTestTag(LIBRARY_SWITCHER_TAG)

@OptIn(ExperimentalTestApi::class, KitTestApi::class)
class CompactMediumTopBarTest {
    @Test
    fun at360_everySkin_fitsAndMeetsTheTouchTargets() = checkEverySkin(width = 360)

    @Test
    fun at390_everySkin_fitsAndMeetsTheTouchTargets() = checkEverySkin(width = 390)

    @Test
    fun at600_everySkin_fitsAndShowsTheLibraryWhole() = checkEverySkin(width = 600)

    @Test
    fun at720_everySkin_fitsAndShowsTheLibraryWhole() = checkEverySkin(width = 720)

    @Test
    fun at840_everySkin_fitsAndShowsTheLibraryWhole() = checkEverySkin(width = 840)

    @Test
    fun at1024_everySkin_fitsAndShowsTheLibraryWhole() = checkEverySkin(width = 1024)

    @Test
    fun at1280_everySkin_fitsAndMeetsTheTouchTargets() = checkEverySkin(width = 1280)

    @Test
    fun compact_showsTheSwatchAndNameWithTheChipsUnderTheBar() =
        runDesktopComposeUiTest(width = 390, height = HEIGHT) {
            val graph = showRoot()
            val chosen = NAMES.getValue(LibraryChoice.of(graph.session.document.value))
            val project = runBlocking { graph.session.projectName.first() }

            val bar = onNodeWithTag(TOP_BAR_TAG).fetchSemanticsNode().boundsInRoot
            val chips = onNodeWithTag(LIBRARY_CHIP_ROW_TAG).fetchSemanticsNode().boundsInRoot
            chips.top shouldBeGreaterThanOrEqual bar.bottom - 1f
            // The project's name stands at the start of the bar, after its swatch, in place of a mark.
            onAllNodes(hasText(project) and InBar).fetchSemanticsNodes().size shouldBe 1
            onAllNodes(hasContentDescription("Export code") and hasClickAction() and InBar)
                .fetchSemanticsNodes()
                .size shouldBe 1
            onAllNodes(hasContentDescriptionExactly("$chosen, radio, selected") and InChips)
                .fetchSemanticsNodes()
                .size shouldBe 1
            // The palette, undo and redo wait in the overflow.
            listOf("Command palette", "Undo", "Redo").forEach { name ->
                onAllNodes(hasContentDescription(name, substring = true) and hasClickAction() and InBar)
                    .fetchSemanticsNodes()
                    .shouldBeEmpty()
            }
        }

    @Test
    fun medium_showsTheDropdownInTheBarAndNoChips() =
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            val graph = showRoot()
            val chosen = NAMES.getValue(LibraryChoice.of(graph.session.document.value))

            onAllNodes(hasTestTag(LIBRARY_CHIP_ROW_TAG)).fetchSemanticsNodes().shouldBeEmpty()
            trigger().performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onAllNodes(hasContentDescriptionExactly("$chosen, option, selected")).fetchSemanticsNodes().size shouldBe 1
        }

    @Test
    fun chipRow_tapPicksTheLibrary() =
        runDesktopComposeUiTest(width = 390, height = HEIGHT) {
            val graph = showRoot(folds = false)
            val target = LibraryChoice.Fluent.takeUnless { choice ->
                choice == LibraryChoice.of(graph.session.document.value)
            } ?: LibraryChoice.Unstyled

            onAllNodes(hasText(NAMES.getValue(target)) and InChips and hasClickAction()).onFirst().performClick()
            waitForIdle()

            LibraryChoice.of(graph.session.document.value) shouldBe target
        }

    @Test
    fun dropdown_tapOpensAndTapPicks() =
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            val graph = showRoot(folds = false, inTree = true)
            val target = LibraryChoice.Fluent.takeUnless { choice ->
                choice == LibraryChoice.of(graph.session.document.value)
            } ?: LibraryChoice.Unstyled

            trigger().performClick()
            waitForIdle()
            option(NAMES.getValue(target)).performClick()
            awaitLibrary(graph, target)

            LibraryChoice.of(graph.session.document.value) shouldBe target
        }

    /**
     * The keys the kit's own select tests drive. The list opens on the chosen library, Tab walks the
     * options and Enter picks. Up and Down walk them too since P-M4b, which the kit's
     * `HeadlessDropdownKeysTest` covers.
     */
    @Test
    fun dropdown_enterOpensOnTheChosenAndTabThenEnterPicks() =
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            val graph = showRoot(folds = false, inTree = true)
            val start = LibraryChoice.of(graph.session.document.value)

            val trigger = trigger()
            trigger.requestFocus()
            trigger.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            val options = hasClickAction() and !InSwitcher
            val opened = LibraryChoice.entries.count { choice ->
                onAllNodes(options and named(choice)).fetchSemanticsNodes().isNotEmpty()
            }
            opened shouldBe LibraryChoice.entries.size
            val focusedOption = options and isFocused()
            onAllNodes(focusedOption and named(start)).fetchSemanticsNodes().size shouldBe 1
            onNode(focusedOption).performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            onNode(focusedOption).performKeyInput { pressKey(Key.Enter) }
            val next = LibraryChoice.entries[(start.ordinal + 1) % LibraryChoice.entries.size]
            awaitLibrary(graph, next)

            LibraryChoice.of(graph.session.document.value) shouldBe next
        }

    /** Material's own exposed dropdown, opened in a desktop window of its own, still picks by tap. */
    @Test
    fun dropdown_inADesktopWindow_tapPicks() =
        runDesktopComposeUiTest(width = 720, height = HEIGHT) {
            val graph = showRoot(folds = false)
            val start = LibraryChoice.of(graph.session.document.value)
            (start == LibraryChoice.M3 || start == LibraryChoice.Expressive) shouldBe true
            val target = LibraryChoice.Fluent

            trigger().performClick()
            waitForIdle()
            option(NAMES.getValue(target)).performClick()
            awaitLibrary(graph, target)

            LibraryChoice.of(graph.session.document.value) shouldBe target
        }

    /** The phone layout at 390 by 844, written to `build/screenshots` for a person to look over. */
    @Test
    fun screenshot_at390_showsThePhoneLayout() =
        runDesktopComposeUiTest(width = 390, height = 844) {
            showRoot(folds = false)

            val image = onRoot().captureToImage()
            val file = File("build/screenshots/compact-390.png")
            file.parentFile.mkdirs()
            ImageIO.write(image.toAwtImage(), "png", file)

            image.width shouldBe (390 * density.density).roundToInt()
            file.length() shouldBeGreaterThan 0L
        }

    /** Boots the builder [width] wide and checks the top bar and the chips in each library's skin. */
    private fun checkEverySkin(width: Int) =
        runDesktopComposeUiTest(width = width, height = HEIGHT) {
            val graph = showRoot(folds = false)
            val layout = LayoutInfo(width.dp, HEIGHT.dp)
            val misfits = LibraryChoice.entries.flatMap { choice ->
                runOnIdle { graph.session.edit(choice.change, EditPhase.Discrete) }
                waitForIdle()
                misfits(width, layout).map { misfit -> "$width dp, ${choice.name}: $misfit" }
            }

            misfits.joinToString("\n") shouldBe ""
        }

    /** Everything wrong with the chrome in a window [width] wide, or nothing. */
    private fun ComposeUiTest.misfits(
        width: Int,
        layout: LayoutInfo,
    ): List<String> {
        val found = mutableListOf<String>()
        val px = density.density
        val controls = onAllNodes(hasClickAction() and (InBar or InChips)).fetchSemanticsNodes()
        controls.forEach { node ->
            val name = node.name()
            val shown = node.boundsInRoot
            if (shown.isEmpty) return@forEach
            if (shown.left < -1f || shown.right > width * px + 1f) found += "$name at $shown overflows the window"
            val primary = hasText("Export code").matches(node)
            val target = (if (primary) layout.primaryTouchTarget else layout.minTouchTarget).value * px
            val reach = node.reach()
            // A chip scrolled part way out of the row is cut by the row's edge, not too small.
            val cut = InChips.matches(node) && shown.width < node.size.width - 1f
            if (!cut && (reach.width < target - 0.5f || reach.height < target - 0.5f)) {
                found += "$name reaches ${reach.width / px} by ${reach.height / px} dp, under ${target / px} dp"
            }
        }
        if (layout.windowClass == WindowClass.Medium) found += cutLibraryName()
        return found
    }

    /** The dropdown's shown name when it is narrower than its own line. */
    private fun ComposeUiTest.cutLibraryName(): List<String> =
        onAllNodes(InSwitcher, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.textLayout()?.let { text -> node to text } }
            .filter { (_, text) -> text.multiParagraph.intrinsics.maxIntrinsicWidth > text.size.width + 1f }
            .map { (node, text) ->
                val wants = text.multiParagraph.intrinsics.maxIntrinsicWidth / density.density
                val gets = text.size.width / density.density
                "the dropdown cuts ${node.name()} short, $gets of $wants dp, in a bar holding ${barButtons()}"
            }

    /** The names of the top bar's buttons and the switcher's width, for a misfit to say what the bar held. */
    private fun ComposeUiTest.barButtons(): String {
        val switcher = onNodeWithTag(LIBRARY_SWITCHER_TAG).fetchSemanticsNode().size.width / density.density
        val names = onAllNodes(hasClickAction() and InBar).fetchSemanticsNodes().map { node -> node.name() }
        return "$names beside a switcher $switcher dp wide"
    }

    /**
     * Waits for the document to reach [choice]. A pick from the dropdown lands once its menu has left,
     * which an idle wait does not always cover while a desktop window closes.
     */
    private fun ComposeUiTest.awaitLibrary(
        graph: AppGraph,
        choice: LibraryChoice,
    ) {
        waitUntil(timeoutMillis = PICK_TIMEOUT_MS) { LibraryChoice.of(graph.session.document.value) == choice }
        waitForIdle()
    }

    /** The open dropdown's option called [name]. */
    private fun ComposeUiTest.option(name: String) =
        onAllNodes((hasText(name) or hasContentDescription(name)) and hasClickAction()).onLast()

    private fun named(choice: LibraryChoice): SemanticsMatcher =
        hasText(NAMES.getValue(choice)) or hasContentDescription(NAMES.getValue(choice))

    /** The dropdown's trigger, the control inside the switcher that opens it. */
    private fun ComposeUiTest.trigger() = onAllNodes(hasClickAction() and InSwitcher).onFirst()

    private fun SemanticsNode.name(): String =
        config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: config.getOrNull(SemanticsProperties.Text)?.joinToString()
            ?: "node $id"

    private fun SemanticsNode.textLayout(): TextLayoutResult? {
        val layouts = mutableListOf<TextLayoutResult>()
        val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        return if (action(layouts)) layouts.firstOrNull() else null
    }

    /** Where a press still lands on this control, its bounds or its larger touch area. */
    private fun SemanticsNode.reach(): Rect {
        val touch = touchBoundsInRoot
        return if (touch.width * touch.height > boundsInRoot.width * boundsInRoot.height) touch else boundsInRoot
    }

    /**
     * The whole builder on fakes, booted, read the way the web reads it when [folds] holds, and with
     * its overlays drawn in the page, as the web draws them, when [inTree] holds.
     */
    private fun ComposeUiTest.showRoot(
        folds: Boolean = true,
        inTree: Boolean = false,
    ): AppGraph {
        val platform = FakePlatform()
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
            ) {
                val root = @Composable { BuilderRoot(graph) }
                val named = @Composable { if (folds) ProvideWebFoldsForTest(root) else root() }
                if (inTree) ProvideOverlaysInTreeForTest(named) else named()
            }
        }
        waitUntil { platform.environment.splashHidden }
        waitForIdle()
        return graph
    }

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
