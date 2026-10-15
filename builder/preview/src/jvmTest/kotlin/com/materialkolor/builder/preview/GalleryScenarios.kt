package com.materialkolor.builder.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryCard
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * The phone and desktop frames a gallery is checked at, at the height of a first screen.
 */
internal val GalleryFrames: List<IntSize> = listOf(IntSize(412, 900), IntSize(1280, 800))

/**
 * How many columns of 280 dp cards, 16 dp apart, fit across each of [GalleryFrames]' widths.
 */
private val GalleryColumns: Map<Int, Int> = mapOf(412 to 1, 1280 to 4)

/**
 * A node named [name] exactly.
 */
internal fun galleryNamed(name: String): SemanticsMatcher = hasContentDescription(name)

/**
 * A probe that adds to [composed] the title of every gallery card that composes, or none without
 * a set to add to.
 */
internal fun galleryProbe(composed: MutableSet<String>?): ((String) -> Unit)? =
    composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }

/**
 * The gallery of [spec]'s library in a pane, under the chrome, Expressive when [expressive], with
 * motion frozen. [composed] hears the title of every card that composes. With [webFolds] the kit's
 * fold modifiers fold state into names as they do on the web.
 */
@OptIn(KitTestApi::class)
@Composable
internal fun GalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    expressive: Boolean = false,
    webFolds: Boolean = false,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides galleryProbe(composed)) {
        Chrome(expressive) {
            if (webFolds) {
                ProvideWebFoldsForTest { PreviewPane(spec, modifier) { ComponentsTab(spec, state) } }
            } else {
                PreviewPane(spec, modifier) { ComponentsTab(spec, state) }
            }
        }
    }
}

/**
 * Checks that every card of [cards] has a title of its own and that every group holds one.
 */
internal fun checkCardsFillEveryGroup(cards: List<GalleryCard>) {
    cards.map { card -> card.title }.distinct().size shouldBe cards.size
    GalleryGroup.entries.filter { group -> cards.none { card -> card.group == group } }.shouldBeEmpty()
}

/**
 * Checks that every one of [cards] composed into [composed], that every interactive node declares
 * its roles as [declares] reads them against the cards' frames, leaving out the nodes [ignored]
 * matches, and that every card declares roles inside its frame.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkGalleryControlsDeclareRoles(
    cards: List<GalleryCard>,
    composed: Set<String>,
    ignored: SemanticsMatcher? = null,
    declares: SemanticsNode.(frames: Set<Int>) -> Boolean = { frames -> galleryDeclaresRoles(frames) },
) {
    composed shouldBe cards.map { card -> card.title }.toSet()

    val frames = cards.map { card -> galleryFrame(card.title).id }.toSet()
    // Unmerged, since a merged node also carries the roles its children declared.
    val controls = onAllNodes(GalleryInteractive, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filterNot { node -> ignored?.matches(node) == true }
    controls.shouldNotBeEmpty()
    controls
        .filterNot { node -> node.declares(frames) }
        .map { node -> node.config.toString() }
        .shouldBeEmpty()
    cards
        .filterNot { card -> galleryCardDeclaresRoles(card.title) }
        .map { card -> card.title }
        .shouldBeEmpty()
}

/**
 * Checks that every one of [cards] shows its control both enabled and disabled, except those in
 * [noDisabled], which show nothing disabled. The nodes [ignored] matches do not count.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkCardsShowEnabledAndDisabled(
    cards: List<GalleryCard>,
    noDisabled: Set<String>,
    ignored: SemanticsMatcher? = null,
) {
    for (card in cards) {
        withClue(card.title) {
            val nodes = galleryFrame(card.title).galleryDescendants().filterNot { node ->
                ignored?.matches(node) == true
            }
            val disabled = nodes.count { node -> SemanticsProperties.Disabled in node.config }
            val enabled = nodes.count { node ->
                node.galleryInteractive() && SemanticsProperties.Disabled !in node.config
            }
            if (card.title in noDisabled) {
                disabled shouldBe 0
            } else {
                (enabled > 0 && disabled > 0) shouldBe true
            }
        }
    }
}

/**
 * Checks that on the first screen of every one of [GalleryFrames] the gallery [content] draws into
 * its modifier composes only the rows in view and the one below, never [lastTitle], and that every
 * card it composed declares roles.
 */
@OptIn(ExperimentalTestApi::class)
internal fun checkFirstScreenComposesOnlyCardsInView(
    lastTitle: String,
    content: @Composable (composed: MutableSet<String>) -> Unit,
) {
    for (frame in GalleryFrames) {
        withClue(frame) {
            runDesktopComposeUiTest(frame.width, frame.height) {
                val composed = mutableSetOf<String>()
                setContent { content(composed) }
                waitForIdle()

                // The rows on screen and the one the list prefetches below them, and no more.
                val bound = (galleryRowsOnScreen(composed) + 1) * GalleryColumns.getValue(frame.width)
                composed.size shouldBeGreaterThan 0
                composed.size shouldBeLessThanOrEqual bound
                composed shouldNotContain lastTitle
                composed
                    .filterNot { title -> galleryCardDeclaresRoles(title) }
                    .shouldBeEmpty()
            }
        }
    }
}

/**
 * Sweeps every control of the gallery [content] draws, in a window the size of the whole gallery
 * so the pointer reaches every card and not just the first screen.
 */
@OptIn(ExperimentalTestApi::class)
internal fun sweepWholeGallery(
    pressAndDrag: Boolean = false,
    content: @Composable () -> Unit,
) {
    runDesktopComposeUiTest(1280, 8000) {
        setContent(content)
        waitForIdle()

        sweepEveryControl(pressAndDrag)
    }
}

/**
 * Checks every one of [names] is on a node, and none of the rest has more than a label, [allowed]
 * aside.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkNamesAreTheLabelsAlone(
    names: List<String>,
    allowed: Set<List<String>> = emptySet(),
) {
    for (name in names) {
        withClue(name) {
            onAllNodes(galleryNamed(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
        }
    }
    onAllNodes(hasContentDescription(", ", substring = true), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config.getOrNull(SemanticsProperties.ContentDescription) }
        .filterNot { found -> found in allowed }
        .shouldBeEmpty()
}

/**
 * Checks every one of [names] is on a node, as the web mirror hears them.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkWebNames(
    names: List<String>,
    useUnmergedTree: Boolean = true,
) {
    for (name in names) withClue(name) { onNode(galleryNamed(name), useUnmergedTree).assertExists() }
}

/**
 * Clicks the node named [from] through its action, then checks its name reads [to].
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkNameFlips(
    from: String,
    to: String,
    useUnmergedTree: Boolean = true,
) {
    onNode(galleryNamed(from), useUnmergedTree).performSemanticsAction(SemanticsActions.OnClick)
    waitForIdle()
    onNode(galleryNamed(to), useUnmergedTree).assertExists()
}

/**
 * Everything a gallery keeps in [DemoAppState], its [choices] as a switch per option, its
 * [switches], its [checks] and its text, to tell whether anything changed.
 */
internal fun DemoAppState.gallerySnapshot(
    choices: List<String>,
    switches: List<String>,
    checks: List<String>,
): List<Any> {
    val picks = choices.flatMap { group -> (0 until 12).map { option -> isOn("$group.$option") } }
    return picks + switches.map { switch -> isOn(switch) } + checks.map { check -> isChecked(check) } + text
}

/**
 * Inspect on over [content], showing the light copy, in the shell chrome, with motion frozen.
 */
@Composable
internal fun InspectingPane(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Inspecting(
            shown = PreviewMode.Light,
            split = remember { SplitState() },
            expressive = ShellExpressive,
            content = content,
        )
    }
}

/**
 * Clicks [target] with Inspect on, then checks the card opened showing each of [tokens] and that
 * [snapshot] did not change.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkInspectPins(
    target: SemanticsNodeInteraction,
    tokens: List<SemanticsMatcher>,
    snapshot: () -> List<Any>,
) {
    val before = snapshot()
    target.performClick()
    waitForIdle()
    onNodeWithTag(INSPECT_CARD_TAG).assertExists()
    for (token in tokens) onNode(OnCard and token).assertExists()
    snapshot() shouldBe before
}

/**
 * Presses the Inspect card where a control lies under it, then checks the card stays and
 * [snapshot] did not change.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.checkInspectCardSwallowsPress(snapshot: () -> List<Any>) {
    val before = snapshot()
    val card = onNodeWithTag(INSPECT_CARD_TAG).fetchSemanticsNode().boundsInRoot
    val under = onAllNodes(hasClickAction() and SemanticsMatcher.keyIsDefined(PreviewRoles))
        .fetchSemanticsNodes()
        .map { node -> node.boundsInRoot }
        .filter { bounds -> bounds.overlaps(card) }
    under.shouldNotBeEmpty()
    val press = card.intersect(under.first()).center
    onRoot().performTouchInput { click(press) }
    waitForIdle()
    snapshot() shouldBe before
    onNodeWithTag(INSPECT_CARD_TAG).assertExists()
}
