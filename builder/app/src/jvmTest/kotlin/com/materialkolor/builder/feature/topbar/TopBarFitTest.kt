package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.materialkolor.builder.BuilderRoot
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.di.AppGraph
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectViewState
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakePlatform
import com.materialkolor.builder.feature.command.InWorkspace
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.rememberDispatcher
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val HEIGHT = 800

/**
 * Wide enough for the segmented switcher in every skin.
 */
private const val ROOMY_WIDTH = 1600

/**
 * Room for the segmented switcher in every skin.
 */
private val ROW_ROOM: Dp = 1400.dp

/**
 * Room for the dropdown only.
 */
private val DROPDOWN_ROOM: Dp = 720.dp

/**
 * The names the segmented switcher shows, one per library.
 */
private val LIBRARY_NAMES = listOf("M3", "Expressive", "Unstyled", "Fluent", "Custom")

private val InBar: SemanticsMatcher = hasAnyAncestor(hasTestTag(TOP_BAR_TAG)) and InWorkspace
private val InSwitcher: SemanticsMatcher = hasAnyAncestor(hasTestTag(LIBRARY_SWITCHER_TAG))

/**
 * The top bar at the widths people use, in every skin. The actions keep their full size, More
 * options stays reachable, and the switcher is segmented only where the row fits.
 */
@OptIn(ExperimentalTestApi::class)
class TopBarFitTest {
    @Test
    fun at1280_everySkin_keepsMoreOptionsWholeAndNothingOverlaps() = checkEverySkin(width = 1280)

    @Test
    fun at1024_everySkin_keepsMoreOptionsWholeAndNothingOverlaps() = checkEverySkin(width = 1024)

    @Test
    fun at840_everySkin_keepsMoreOptionsWholeAndNothingOverlaps() = checkEverySkin(width = 840)

    @Test
    fun at600_everySkin_keepsMoreOptionsWholeAndNothingOverlaps() = checkEverySkin(width = 600)

    @Test
    fun at412_everySkin_keepsMoreOptionsWholeAndNothingOverlaps() = checkEverySkin(width = 412)

    @Test
    fun switcher_inAWideWindowWithoutRoomForTheRow_isTheDropdown() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            showBar(barWidth = DROPDOWN_ROOM)

            segmentedShown() shouldBe false
            onAllNodes(hasTestTag(LIBRARY_SWITCHER_TAG)).fetchSemanticsNodes().size shouldBe 1
            // The row measured to see whether it fits never reaches the merged tree people hear.
            onAllNodes(hasText("Fluent")).fetchSemanticsNodes().size shouldBe 0
        }

    @Test
    fun switcher_inAWideWindowWithRoomForTheRow_isSegmented() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            showBar(barWidth = ROW_ROOM)

            segmentedShown() shouldBe true
            // Only the row that shows names the libraries, not the one measured beside it.
            onAllNodes(hasText("Fluent")).fetchSemanticsNodes().size shouldBe 1
            onAllNodes(hasText("Fluent") and InSwitcher).fetchSemanticsNodes().size shouldBe 1
        }

    @Test
    fun switcher_fluentAt1280BesideHistory_fitsWholeOrFallsBackToTheDropdown() =
        runDesktopComposeUiTest(width = 1280, height = HEIGHT) {
            val graph = showRoot()
            runOnIdle { graph.session.edit(LibraryChoice.Fluent.change, EditPhase.Discrete) }
            waitForIdle()

            onAllNodes(hasContentDescription("History") and hasClickAction() and InBar)
                .fetchSemanticsNodes()
                .size shouldBe 1
            // Fluent shares the row out evenly, so every option needs the widest one's room.
            val clipped = if (segmentedShown()) clippedOptions() else emptyList()
            clipped shouldBe emptyList()
        }

    @Test
    fun switcher_focusedAcrossASwitchToFluent_keepsFocus() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            val graph = showRoot()
            onNodeWithText("M3").requestFocus()
            waitForIdle()

            onNodeWithText("Fluent").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            graph.session.document.value.library shouldBe Library.Fluent
            switcherFocused() shouldBe true
        }

    @Test
    fun switcher_focusedAcrossTheFitBoundary_keepsFocusInEachForm() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            val barWidth = showBar(barWidth = ROW_ROOM)
            onNodeWithText("M3").requestFocus()
            waitForIdle()
            switcherFocused() shouldBe true

            barWidth.value = DROPDOWN_ROOM
            waitForIdle()
            segmentedShown() shouldBe false
            switcherFocused() shouldBe true

            barWidth.value = ROW_ROOM
            waitForIdle()
            segmentedShown() shouldBe true
            switcherFocused() shouldBe true
        }

    @Test
    fun switcher_unfocusedAcrossTheFitBoundary_takesNoFocus() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            val barWidth = showBar(barWidth = ROW_ROOM)
            // Share shows its label while the row fits and a glyph once it does not, and focus follows it.
            val share = (hasContentDescription("Share") or hasText("Share")) and hasClickAction() and InBar
            onNode(share).requestFocus()
            waitForIdle()

            listOf(DROPDOWN_ROOM, ROW_ROOM).forEach { width ->
                barWidth.value = width
                waitForIdle()
                switcherFocused() shouldBe false
                onAllNodes(share and isFocused()).fetchSemanticsNodes().size shouldBe 1
            }
        }

    @Test
    fun switcher_acrossASeedDrag_doesNotMeasureTheRowAgain() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            var checks = 0
            val graph = showRoot(fitProbe = { checks++ })
            val booted = checks
            booted shouldBeGreaterThan 0

            listOf(0x3366CC, 0x4477DD, 0x5588EE).forEach { seed ->
                runOnIdle { graph.session.edit(seedChange(seed), EditPhase.Dragging) }
                waitForIdle()
            }
            runOnIdle { graph.session.edit(seedChange(0x6699FF), EditPhase.Released) }
            waitForIdle()
            checks shouldBe booted

            // A library switch moves the skin, which can change the row's width, so it measures again.
            runOnIdle { graph.session.edit(LibraryChoice.Fluent.change, EditPhase.Discrete) }
            waitForIdle()
            checks shouldBeGreaterThan booted
        }

    /**
     * Boots the builder [width] wide and checks the top bar in each library's skin.
     */
    private fun checkEverySkin(width: Int) =
        runDesktopComposeUiTest(width = width, height = HEIGHT) {
            val graph = showRoot()
            val target = LayoutInfo.of(width.dp, HEIGHT.dp).minTouchTarget
            val misfits = LibraryChoice.entries.flatMap { choice ->
                runOnIdle { graph.session.edit(choice.change, EditPhase.Discrete) }
                waitForIdle()
                misfits(width, target).map { misfit -> "$width dp, ${choice.name}: $misfit" }
            }

            misfits.joinToString("\n") shouldBe ""
        }

    /**
     * Everything wrong with how the top bar sits in a window [width] wide, or nothing.
     */
    private fun ComposeUiTest.misfits(
        width: Int,
        target: Dp,
    ): List<String> {
        val found = mutableListOf<String>()
        val px = density.density
        val window = Rect(0f, 0f, width * px, HEIGHT * px)
        val more = onAllNodes(hasContentDescription("More options") and hasClickAction() and InBar)
            .fetchSemanticsNodes()
        if (more.size != 1) return listOf("${more.size} More options buttons")
        val reach = more.single().reach()
        if (reach.width < target.value * px || reach.height < target.value * px) {
            found += "More options reaches ${reach.width / px} by ${reach.height / px} dp, under $target"
        }
        val bounds = more.single().boundsInRoot
        if (bounds.isEmpty || !window.containsRect(bounds)) found += "More options at $bounds, outside $window"

        val controls = controls()
        controls.forEachIndexed { index, (name, rect) ->
            controls.drop(index + 1).forEach { (other, otherRect) ->
                if (rect.overlaps(otherRect)) found += "$name at $rect overlaps $other at $otherRect"
            }
            if (!window.containsRect(rect)) found += "$name at $rect is outside $window"
        }
        if (segmentedShown()) found += clippedOptions()
        return found
    }

    /**
     * The switcher and each end-edge action that shows, by name, with their bounds.
     */
    private fun ComposeUiTest.controls(): List<Pair<String, Rect>> {
        val switcher = onAllNodes(hasTestTag(LIBRARY_SWITCHER_TAG)).fetchSemanticsNodes()
        // Share and Export code show as labels or as glyphs, so either name counts.
        val actions = listOf("Command palette", "Undo", "Redo", "Share", "More options").flatMap { name ->
            val named = hasContentDescription(name, substring = true) or hasText(name)
            val nodes = onAllNodes(named and hasClickAction() and InBar).fetchSemanticsNodes()
            nodes.map { node -> name to node.boundsInRoot }
        }
        val exportNamed = hasText("Export code") or hasContentDescription("Export code")
        val export = onAllNodes(exportNamed and hasClickAction() and InBar)
            .fetchSemanticsNodes()
            .map { node -> "Export code" to node.boundsInRoot }
        return switcher.map { node -> "Library" to node.boundsInRoot } + actions + export
    }

    /**
     * Whether the switcher, in either form, holds keyboard focus.
     */
    private fun ComposeUiTest.switcherFocused(): Boolean =
        onAllNodes(isFocused() and (InSwitcher or hasTestTag(LIBRARY_SWITCHER_TAG))).fetchSemanticsNodes().size == 1

    private fun seedChange(rgb: Int): DocumentChange = DocumentChange.SetSeed(Argb(rgb), SeedSource.Typed)

    /**
     * Whether the switcher shows as the segmented row, which names every library at once.
     */
    private fun ComposeUiTest.segmentedShown(): Boolean =
        LIBRARY_NAMES.all { name ->
            onAllNodes(hasText(name) and InSwitcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    /**
     * The segmented options whose label is narrower than its own line, which a row that fits never has.
     */
    private fun ComposeUiTest.clippedOptions(): List<String> =
        LIBRARY_NAMES
            .filter { name ->
                onAllNodes(hasText(name) and InSwitcher, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .mapNotNull { node -> node.textLayout() }
                    .any { layout -> layout.multiParagraph.intrinsics.maxIntrinsicWidth > layout.size.width + 1f }
            }.map { name -> "the segmented option $name is cut short" }

    private fun SemanticsNode.textLayout(): TextLayoutResult? {
        val layouts = mutableListOf<TextLayoutResult>()
        val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        return if (action(layouts)) layouts.firstOrNull() else null
    }

    /**
     * Where a press still lands on this control, its bounds or its larger touch area.
     */
    private fun SemanticsNode.reach(): Rect {
        val touch = touchBoundsInRoot
        return if (touch.width * touch.height > boundsInRoot.width * boundsInRoot.height) touch else boundsInRoot
    }

    private fun Rect.containsRect(other: Rect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    /**
     * The top bar alone, [barWidth] wide, in a window wide enough to ask for the segmented row. Set
     * the width it hands back to resize the bar.
     */
    private fun ComposeUiTest.showBar(barWidth: Dp): MutableState<Dp> {
        val width = mutableStateOf(barWidth)
        val document = ThemeDocument.Default
        val state = WorkspaceModel.State(
            document = document,
            capabilities = capabilitiesOf(document),
            history = HistoryState(),
            view = ProjectViewState(),
            preferences = Preferences(),
        )
        setContent {
            val dispatcher = rememberDispatcher<WorkspaceAction> {}
            BuilderTheme(
                skin = skinOf(document),
                result = ThemeResolver().resolve(document),
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.width(width.value)) {
                        TopBarContent(state = state, dispatcher = dispatcher)
                    }
                }
            }
        }
        waitForIdle()
        return width
    }

    /**
     * The whole builder on fakes, booted, with its graph so a test can switch libraries. [fitProbe]
     * hears each time the switcher measures its segmented row.
     */
    private fun ComposeUiTest.showRoot(fitProbe: (() -> Unit)? = null): AppGraph {
        val platform = FakePlatform()
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
                LocalSwitcherFitProbe provides fitProbe,
            ) {
                BuilderRoot(graph)
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
