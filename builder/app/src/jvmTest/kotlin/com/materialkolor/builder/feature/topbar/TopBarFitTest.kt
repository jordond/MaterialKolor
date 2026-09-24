package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
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
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Library
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
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// b-231

private const val HEIGHT = 800

/** Wide enough for the segmented switcher in every skin. */
private const val ROOMY_WIDTH = 1600

/** The names the segmented switcher shows, one per library. */
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
            showBar(barWidth = 720.dp)

            segmentedShown() shouldBe false
            onAllNodes(hasTestTag(LIBRARY_SWITCHER_TAG)).fetchSemanticsNodes().size shouldBe 1
            // The row measured to see whether it fits never reaches the merged tree people hear.
            onAllNodes(hasText("Fluent")).fetchSemanticsNodes().size shouldBe 0
        }

    @Test
    fun switcher_inAWideWindowWithRoomForTheRow_isSegmented() =
        runDesktopComposeUiTest(width = ROOMY_WIDTH, height = HEIGHT) {
            showBar(barWidth = 1400.dp)

            segmentedShown() shouldBe true
            // Only the row that shows names the libraries, not the one measured beside it.
            onAllNodes(hasText("Fluent")).fetchSemanticsNodes().size shouldBe 1
            onAllNodes(hasText("Fluent") and InSwitcher).fetchSemanticsNodes().size shouldBe 1
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
            val focused = onAllNodes(isFocused() and (InSwitcher or hasTestTag(LIBRARY_SWITCHER_TAG)))
            focused.fetchSemanticsNodes().size shouldBe 1
        }

    /** Boots the builder [width] wide and checks the top bar in each library's skin. */
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

    /** Everything wrong with how the top bar sits in a window [width] wide, or nothing. */
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

    /** The switcher and each end-edge action that shows, by name, with their bounds. */
    private fun ComposeUiTest.controls(): List<Pair<String, Rect>> {
        val switcher = onAllNodes(hasTestTag(LIBRARY_SWITCHER_TAG)).fetchSemanticsNodes()
        val actions = listOf("Command palette", "Undo", "Redo", "Share", "More options").flatMap { name ->
            val matcher = hasContentDescription(name, substring = true) and hasClickAction() and InBar
            onAllNodes(matcher).fetchSemanticsNodes().map { node -> name to node.boundsInRoot }
        }
        val export = onAllNodes(hasText("Export code") and hasClickAction() and InBar)
            .fetchSemanticsNodes()
            .map { node -> "Export code" to node.boundsInRoot }
        return switcher.map { node -> "Library" to node.boundsInRoot } + actions + export
    }

    /** Whether the switcher shows as the segmented row, which names every library at once. */
    private fun ComposeUiTest.segmentedShown(): Boolean =
        LIBRARY_NAMES.all { name ->
            onAllNodes(hasText(name) and InSwitcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    /** The segmented options whose label is narrower than its own line, which a row that fits never has. */
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

    /** Where a press still lands on this control, its bounds or its larger touch area. */
    private fun SemanticsNode.reach(): Rect {
        val touch = touchBoundsInRoot
        return if (touch.width * touch.height > boundsInRoot.width * boundsInRoot.height) touch else boundsInRoot
    }

    private fun Rect.containsRect(other: Rect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    /** The top bar alone, [barWidth] wide, in a window wide enough to ask for the segmented row. */
    private fun ComposeUiTest.showBar(barWidth: Dp) {
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
                    Box(Modifier.width(barWidth)) {
                        TopBarContent(state = state, dispatcher = dispatcher)
                    }
                }
            }
        }
        waitForIdle()
    }

    /** The whole builder on fakes, booted, with its graph so a test can switch libraries. */
    private fun ComposeUiTest.showRoot(): AppGraph {
        val platform = FakePlatform()
        val graph = createGraphFactory<AppGraph.Factory>().create(platform)
        val owner = TestOwner()
        setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalMetroViewModelFactory provides graph.metroViewModelFactory,
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
