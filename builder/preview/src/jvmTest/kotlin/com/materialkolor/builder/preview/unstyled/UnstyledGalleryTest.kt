package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The phone and desktop frames the gallery is checked at, at the height of a first screen.
 */
private val GalleryFrames: List<IntSize> = listOf(IntSize(412, 900), IntSize(1280, 800))

/**
 * The cards with nothing to press, or whose only control has no disabled look.
 */
private val GalleryNoDisabled: Set<String> = setOf("Separators", "Scroll area", "Progress", "Tooltip", "Badges")

/**
 * The sources every Unstyled gallery card is drawn from.
 */
private val GallerySources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryInputs.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryPanels.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/GalleryRoles.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledGallery.kt",
)

/**
 * Words in the names of what opens a popup, a window or a portal, which on the web take the mirror over.
 */
private val GalleryPopupWords: List<String> =
    listOf("Popup", "Dialog", "Modal", "BottomSheet", "DropdownMenu", "Tooltip", "Portal")

/**
 * What the gallery may take from the kit, its motion, the fold modifiers, the value node names and
 * `InnerTextWithoutHandles`.
 */
private val GalleryKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.a11y.foldsValueIntoName",
    "com.materialkolor.builder.kit.a11y.valueNodeName",
    "com.materialkolor.builder.kit.a11y.sliderRoleWord",
    "com.materialkolor.builder.kit.a11y.progressRoleWord",
    "com.materialkolor.builder.kit.headless.InnerTextWithoutHandles",
)

/**
 * The start of the names of the endless animation APIs. Written out whole they would trip the
 * builder's own architecture scan of this file.
 */
private val GalleryEndlessMotion: List<String> = listOf("rememberInfinite", "infiniteRepeat")

/**
 * The switches the gallery keeps in [DemoAppState].
 */
private val GallerySwitches: List<String> = listOf(
    GalleryKeys.Favourite,
    GalleryKeys.Starred,
    GalleryKeys.Wifi,
    GalleryKeys.SortShut,
    GalleryKeys.MenuShut,
    GalleryKeys.Details,
)

/**
 * The single choices the gallery keeps in [DemoAppState], each a switch per option.
 */
private val GalleryChoices: List<String> =
    listOf(GalleryKeys.Volume, GalleryKeys.Plan, GalleryKeys.Sort, GalleryKeys.Tab, GalleryKeys.Destination)

@OptIn(ExperimentalTestApi::class)
class UnstyledGalleryTest {
    @Test
    fun cards_everyGroup_holdUniquelyNamedCards() {
        UnstyledCards.map { card -> card.title }.distinct().size shouldBe UnstyledCards.size
        GalleryGroup.entries.filter { group -> UnstyledCards.none { card -> card.group == group } }.shouldBeEmpty()
    }

    @Test
    fun controls_bothFramesBothModes_declareTheirOwnRoles() {
        for (frame in GalleryFrames) {
            for (spec in listOf(LightSpec, DarkSpec)) {
                withClue("${frame.width} ${spec.label}") {
                    runComposeUiTest {
                        val composed = mutableSetOf<String>()
                        // As wide as the frame, and tall enough that every card composes.
                        val whole = Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(frame.width.dp, 9000.dp)
                        setContent { GalleryHarness(spec, DemoAppState(), whole, composed) }
                        waitForIdle()
                        composed shouldBe UnstyledCards.map { card -> card.title }.toSet()

                        val frames = UnstyledCards.map { card -> galleryFrame(card.title).id }.toSet()
                        // Unmerged, since a merged node also carries the roles its children declared.
                        val controls = onAllNodes(GalleryInteractive, useUnmergedTree = true).fetchSemanticsNodes()
                        controls.shouldNotBeEmpty()
                        controls
                            .filterNot { node -> node.galleryDeclaresRoles(frames) }
                            .map { node -> node.config.toString() }
                            .shouldBeEmpty()
                        UnstyledCards
                            .filterNot { card -> galleryCardDeclaresRoles(card.title) }
                            .map { card -> card.title }
                            .shouldBeEmpty()
                    }
                }
            }
        }
    }

    @Test
    fun cards_everyControlWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            for (card in UnstyledCards) {
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
    fun gallery_everyControlPressedHoveredFocusedRightClickedAndLongPressed_opensNoPopupOrWindow() =
        runDesktopComposeUiTest(1280, 8000) {
            // A window the size of the whole gallery, so the pointer reaches every card and not just the first screen.
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

            // Hover and focus are how a tooltip opens.
            val interactive = onAllNodes(GalleryInteractive, useUnmergedTree = true)
            for (index in interactive.fetchSemanticsNodes().indices) {
                withClue("Hovered control $index") {
                    interactive[index].performMouseInput { moveTo(center) }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
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

            // A word to select, so the text field has a context menu and a text toolbar to open.
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
    fun gallerySources_openNoPopupWindowOrPortalAndNeverLoop() {
        for (path in GallerySources) {
            withClue(path) {
                val source = File(path)
                source.isFile shouldBe true
                val lines = source.readLines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported -> imported.isBannedInTheGallery() }
                    .shouldBeEmpty()
                lines
                    .filter { line -> GalleryEndlessMotion.any { stem -> stem in line } }
                    .shouldBeEmpty()
            }
        }
    }

    @Test
    fun names_onTheWeb_foldTheStateOfEveryKindOfControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { GalleryHarness(LightSpec, state, GalleryWhole, webFolds = true) }
            waitForIdle()

            for (name in WebNames) withClue(name) { onNode(named(name), useUnmergedTree = true).assertExists() }
            for (name in WebValueNames) {
                withClue(name) {
                    onAllNodes(hasText(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
                }
            }

            onNode(named("Wi-Fi, switch, off"), useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(GalleryKeys.Wifi) shouldBe true
            onNode(named("Wi-Fi, switch, on"), useUnmergedTree = true).assertExists()

            onNode(named("Shipping details, collapsed"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(GalleryKeys.Details) shouldBe true
            onNode(named("Shipping details, expanded"), useUnmergedTree = true).assertExists()
        }

    @Test
    fun names_offTheWeb_areTheLabelsAlone() =
        runComposeUiTest {
            setContent { GalleryHarness(LightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            for (name in listOf("Overview", "Free", "Rename", "Newest", "Options", "Shipping details", "Favourite")) {
                withClue(name) {
                    onAllNodes(named(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
                }
            }
            onAllNodes(hasContentDescription(", ", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .map { node -> node.config.getOrNull(SemanticsProperties.ContentDescription) }
                .filterNot { names -> names == listOf("Sort by, Newest") || names == listOf("Group by, Folder") }
                .shouldBeEmpty()
        }

    @Test
    fun inspect_clickOnAToggle_pinsItsTokensAndNeverReachesTheGallery() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(
                        shown = PreviewMode.Light,
                        split = remember { SplitState() },
                        expressive = ShellExpressive,
                    ) {
                        val unstyled = remember { LightSpec.on(Library.Unstyled) }
                        PreviewPane(unstyled, Modifier.fillMaxSize()) { ComponentsTab(unstyled, state) }
                    }
                }
            }
            waitForIdle()
            onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Toggle button"))
            waitForIdle()
            val before = state.gallerySnapshot()

            onNode(named("Favourite") and isEnabled(), useUnmergedTree = true).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText(UnstyledToken.Outline.token.name)).assertExists()
            onNode(OnCard and hasText(UnstyledToken.OnSurfaceVariant.token.name)).assertExists()
            state.gallerySnapshot() shouldBe before

            // Press the card where a control of the gallery lies under it.
            val card = onNodeWithTag(INSPECT_CARD_TAG).fetchSemanticsNode().boundsInRoot
            val under = onAllNodes(hasClickAction() and SemanticsMatcher.keyIsDefined(PreviewRoles))
                .fetchSemanticsNodes()
                .map { node -> node.boundsInRoot }
                .filter { bounds -> bounds.overlaps(card) }
            under.shouldNotBeEmpty()
            val press = card.intersect(under.first()).center
            onRoot().performTouchInput { click(press) }
            waitForIdle()
            state.gallerySnapshot() shouldBe before
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
        }
}

/**
 * What the web mirror hears from one control of each kind, with the gallery as it first shows.
 */
private val WebNames: List<String> = listOf(
    "Overview, tab, selected",
    "Activity, tab, not selected",
    "Settings, tab, not selected, disabled",
    "Free, radio, selected",
    "Enterprise, radio, not selected, disabled",
    "Rename, menu item",
    "Archive, menu item, disabled",
    "Options, expanded",
    "Options, collapsed, disabled",
    "Sort by, Newest, expanded",
    "Newest, option, selected",
    "Size, option, not selected, disabled",
    "Favourite, checkbox, not checked",
    "Email me the newsletter, checkbox, not checked",
    "Keep a copy on this device, checkbox, checked, disabled",
    "Wi-Fi, switch, off",
    "Airplane mode, switch, on, disabled",
    "Home, selected",
    "Team, not selected, disabled",
    "Shipping details, collapsed",
)

/**
 * What the web mirror hears from the slider and the progress bars, which play no role of their own
 * and so read their name, role word and value as text.
 */
private val WebValueNames: List<String> = listOf(
    "Volume, slider, 6",
    "Uploading, progress bar, 40%",
    "Exporting, progress bar, 75%",
)

/**
 * Wide enough for four columns and tall enough that every card composes.
 */
private val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/**
 * Anything a user can press, type into or drag.
 */
private val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

/**
 * Anything that takes keyboard focus.
 */
private val GalleryFocusable: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus)

/**
 * A node named [name] exactly.
 */
private fun named(name: String): SemanticsMatcher = hasContentDescription(name)

private fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its roles, itself or through the control it is part of. The nearest
 * node with roles may be an ancestor, as long as it sits inside a card and is not the card's frame
 * among [frames].
 */
private fun SemanticsNode.galleryDeclaresRoles(frames: Set<Int>): Boolean {
    val holder = generateSequence(this) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null &&
        holder.id !in frames &&
        generateSequence(holder.parent) { node -> node.parent }.any { node -> node.id in frames }
}

/**
 * Every node under this one in the unmerged tree.
 */
private fun SemanticsNode.galleryDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.galleryDescendants() }

/**
 * The frame of the card called [title], the node its title text sits in.
 */
private fun SemanticsNodeInteractionsProvider.galleryFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/**
 * Whether anything in the card called [title] declares roles, its frame aside.
 */
private fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

private fun String.isBannedInTheGallery(): Boolean {
    val name = substringAfterLast('.')
    return startsWith("androidx.compose.ui.window.") ||
        GalleryPopupWords.any { word -> word in name } ||
        (startsWith("com.composeunstyled.") && "TextField" in name) ||
        (startsWith("com.materialkolor.builder.kit.") && GalleryKitImports.none { allowed -> startsWith(allowed) })
}

/**
 * Everything the gallery keeps in [DemoAppState], to tell whether anything changed.
 */
private fun DemoAppState.gallerySnapshot(): List<Any> {
    val picks = GalleryChoices.flatMap { group -> (0 until 12).map { option -> isOn("$group.$option") } }
    return picks + GallerySwitches.map { switch -> isOn(switch) } + isChecked(GalleryKeys.Newsletter) + text
}

/**
 * The Unstyled gallery in a pane of [spec], under the shell chrome, with motion frozen. With
 * [webFolds] the kit's fold modifiers fold state into names as they do on the web.
 */
@OptIn(KitTestApi::class)
@Composable
private fun GalleryHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    webFolds: Boolean = false,
) {
    val probe: ((String) -> Unit)? = composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }
    CompositionLocalProvider(LocalMotionFrozen provides true, LocalCompositionProbe provides probe) {
        Chrome(ShellExpressive) {
            val unstyled = remember(spec) { spec.on(Library.Unstyled) }
            if (webFolds) {
                ProvideWebFoldsForTest { PreviewPane(unstyled, modifier) { ComponentsTab(unstyled, state) } }
            } else {
                PreviewPane(unstyled, modifier) { ComponentsTab(unstyled, state) }
            }
        }
    }
}
