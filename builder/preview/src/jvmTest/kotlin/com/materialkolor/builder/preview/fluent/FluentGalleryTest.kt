package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.inspect.firstRated
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitState
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * Where a recording job would write the screenshots. Nothing is written unless a Roborazzi task
 * turns capture on, and the preview keeps no baselines.
 */
private const val GalleryScreenshotDir = "src/jvmTest/screenshots/fluent"

/** The cards with nothing to press, or whose only control has no disabled look. */
private val GalleryNoDisabled: Set<String> = setOf("Tabs", "Info bar", "Progress bar", "Progress ring", "Badges")

/** The sources every Fluent gallery card is drawn from. */
private val GallerySources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/FluentGallery.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryPanels.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent/GalleryRoles.kt",
)

/**
 * Words in the names of what opens a popup, a window or a portal, which on the web take the mirror
 * over (D40). Fluent's side and top navigation carry tooltips, and its menu items flyouts.
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
 * text field keeps its inner text out of the D45 lever, and its expander animates past frozen motion.
 */
private val GalleryBannedFluent: Set<String> = setOf(
    "io.github.composefluent.component.Slider",
    "io.github.composefluent.component.TextField",
    "io.github.composefluent.component.Expander",
)

/** What the gallery may take from the kit, its motion, the fold modifiers and the text lever of D45. */
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

/** A progress bar or ring called without a value, the endless kind. */
private val EndlessProgress = Regex("""\bProgress(Bar|Ring)\((?!\s*progress\b)""")

/** The switches and boxes the gallery keeps in [DemoAppState]. */
private val GallerySwitches: List<String> =
    listOf(FluentGalleryKeys.Bold, FluentGalleryKeys.Wifi, FluentGalleryKeys.Details)

/** The single choices the gallery keeps in [DemoAppState], each a switch per option. */
private val GalleryChoices: List<String> = listOf(
    FluentGalleryKeys.Volume,
    FluentGalleryKeys.Delivery,
    FluentGalleryKeys.View,
    FluentGalleryKeys.Folder,
    FluentGalleryKeys.Tab,
    FluentGalleryKeys.Page,
)

@OptIn(ExperimentalTestApi::class)
class FluentGalleryTest {
    @Test
    fun cards_everyGroup_holdUniquelyNamedCards() {
        FluentCards.map { card -> card.title }.distinct().size shouldBe FluentCards.size
        GalleryGroup.entries.filter { group -> FluentCards.none { card -> card.group == group } }.shouldBeEmpty()
    }

    @Test
    fun controls_everyDeviceWidthBothModes_declareTheirOwnRoles() {
        for ((width, frame) in FluentFrames) {
            for (spec in listOf(FluentLightSpec, FluentDarkSpec)) {
                withClue("$width ${spec.label}") {
                    runComposeUiTest {
                        val composed = mutableSetOf<String>()
                        // As wide as the frame, and tall enough that every card composes.
                        val whole = Modifier
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(frame.width.dp, 12000.dp)
                        setContent { GalleryHarness(spec, DemoAppState(), whole, composed) }
                        waitForIdle()
                        composed shouldBe FluentCards.map { card -> card.title }.toSet()

                        val frames = FluentCards.map { card -> galleryFrame(card.title).id }.toSet()
                        // Unmerged, since a merged node also carries the roles its children declared.
                        val controls = onAllNodes(GalleryInteractive, useUnmergedTree = true)
                            .fetchSemanticsNodes()
                            .filterNot { node -> UnderAnOverlay.matches(node) }
                        controls.shouldNotBeEmpty()
                        controls
                            .filterNot { node -> node.galleryDeclaresRoles(frames) }
                            .map { node -> node.config.toString() }
                            .shouldBeEmpty()
                        FluentCards
                            .filterNot { card -> galleryCardDeclaresRoles(card.title) }
                            .map { card -> card.title }
                            .shouldBeEmpty()
                    }
                }
            }
        }
    }

    @Test
    fun accentControls_everyOneDeclared_resolveToARowOfTheAudit() =
        runComposeUiTest {
            val state = DemoAppState().apply { setOn(FluentGalleryKeys.Wifi, true) }
            setContent { GalleryHarness(FluentLightSpec, state, GalleryWhole) }
            waitForIdle()

            val declared = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .map { node -> node.config[PreviewRoles] }
            declared.filter { refs -> refs.isNotEmpty() }.distinct() shouldBe listOf(FluentAccentRefs)
            for (isDark in listOf(false, true)) {
                FluentLightSpec.result.audit
                    .firstRated(FluentAccentRefs, isDark)
                    .shouldNotBeNull()
            }
            val accent = SemanticsMatcher("declares the accent fill") { node ->
                node.config.getOrNull(PreviewRoles) == FluentAccentRefs
            }
            for (name in listOf("Wi-Fi", "Standard", "Day", "Recent", "Inbox", "Volume")) {
                withClue(name) {
                    onAllNodes(accent and hasContentDescription(name), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .shouldNotBeEmpty()
                }
            }
            // A button's colors sit on the part round the row that takes its click and holds its label.
            onAllNodes(accent and hasAnyDescendant(hasText("Send")), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .shouldNotBeEmpty()
            onAllNodes(accent and hasAnyDescendant(hasText("Save")), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .shouldBeEmpty()
            onAllNodes(accent, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size shouldBeGreaterThan 8
        }

    @Test
    fun cards_everyControlWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { GalleryHarness(FluentLightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            for (card in FluentCards) {
                withClue(card.title) {
                    val nodes = galleryFrame(card.title).galleryDescendants().filterNot { node ->
                        UnderAnOverlay.matches(node)
                    }
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
    fun controls_clickedOnce_changeTheSharedState() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { GalleryHarness(FluentLightSpec, state, GalleryWhole) }
            waitForIdle()

            for (name in listOf("Bold", "Email me updates", "Express", "Wi-Fi", "Week", "Shared", "Photos", "Sent")) {
                withClue(name) {
                    onNode(named(name) and hasClickAction() and isEnabled(), useUnmergedTree = true).performClick()
                    waitForIdle()
                }
            }
            state.isOn(FluentGalleryKeys.Bold) shouldBe true
            state.isChecked(FluentGalleryKeys.Updates) shouldBe true
            state.choice(FluentGalleryKeys.Delivery, 3) shouldBe 1
            state.isOn(FluentGalleryKeys.Wifi) shouldBe true
            state.choice(FluentGalleryKeys.View, 3) shouldBe 1
            state.choice(FluentGalleryKeys.Folder, 3) shouldBe 1
            state.choice(FluentGalleryKeys.Tab, 3) shouldBe 1
            state.choice(FluentGalleryKeys.Page, 3) shouldBe 1

            // The disabled options stay out of reach.
            for (name in listOf("Overnight", "Month", "Favorites", "Archive")) {
                withClue(name) { onNode(named(name) and isNotEnabled(), useUnmergedTree = true).assertExists() }
            }

            // Delete waits for the box, and puts it back.
            onNode(hasText("Delete") and hasClickAction()).assert(isNotEnabled())
            onNode(named("I understand"), useUnmergedTree = true).performClick()
            waitForIdle()
            state.isChecked(FluentGalleryKeys.Understood) shouldBe true
            onNode(hasText("Delete") and hasClickAction() and isEnabled()).performClick()
            waitForIdle()
            state.isChecked(FluentGalleryKeys.Understood) shouldBe false
        }

    @Test
    fun slider_setProgressAndArrowKeys_moveTheSharedStop() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { GalleryHarness(FluentLightSpec, state, GalleryWhole) }
            waitForIdle()
            val volume = onNode(named("Volume") and isEnabled(), useUnmergedTree = true)
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 6

            volume.performSemanticsAction(SemanticsActions.SetProgress) { set -> set(3f) }
            waitForIdle()
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 3

            volume.requestFocus()
            volume.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 4

            val disabled = onNode(named("Volume") and isNotEnabled(), useUnmergedTree = true).fetchSemanticsNode()
            (SemanticsActions.SetProgress in disabled.config) shouldBe false
            disabled.config[SemanticsProperties.ProgressBarRangeInfo].current shouldBe 4f
        }

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
                EndlessProgress.findAll(text).map { match -> match.value }.toList().shouldBeEmpty()
                // Only the theme, never the one that adds a host and a backdrop round the screen.
                ("FluentTheme(" in text) shouldBe false
                ("SliderDefaults.Thumb" in text) shouldBe false
            }
        }
    }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutTheirColumnsAndRender() {
        for ((width, frame) in FluentFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(FluentLightSpec)
                    setContent { GalleryHarness(spec, DemoAppState(), Modifier.fillMaxSize()) }

                    for (mode in listOf(FluentLightSpec, FluentDarkSpec)) {
                        spec = mode
                        waitForIdle()
                        onNodeWithText(GalleryGroup.Actions.name).assertExists()
                        val first = onNodeWithText("Button").fetchSemanticsNode().boundsInRoot
                        val second = onNodeWithText("Accent button").fetchSemanticsNode().boundsInRoot
                        if (width == DeviceWidth.Phone) {
                            second.top shouldBeGreaterThan first.bottom
                        } else {
                            second.left shouldBeGreaterThan first.right
                            second.top shouldBeLessThan first.bottom
                        }
                        onRoot().captureRoboImage("$GalleryScreenshotDir/gallery-${width.name}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun names_onTheWeb_foldTheStateOfEveryKindOfControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { GalleryHarness(FluentLightSpec, state, GalleryWhole, webFolds = true) }
            waitForIdle()

            for (name in WebNames) withClue(name) { onNode(named(name), useUnmergedTree = true).assertExists() }

            onNode(named("Shipping details, collapsed"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(FluentGalleryKeys.Details) shouldBe true
            onNode(named("Shipping details, expanded"), useUnmergedTree = true).assertExists()
            onNodeWithText("12 Harbour Street").assertExists()

            onNode(named("Wi-Fi, switch, off"), useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNode(named("Wi-Fi, switch, on"), useUnmergedTree = true).assertExists()
        }

    @Test
    fun names_offTheWeb_areTheLabelsAlone() =
        runComposeUiTest {
            setContent { GalleryHarness(FluentLightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            val labels = listOf("Bold", "Email me updates", "Standard", "Wi-Fi", "Day", "Shipping details", "Recent", "Inbox")
            for (name in labels) {
                withClue(name) {
                    onAllNodes(named(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
                }
            }
            onAllNodes(hasContentDescription(", ", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .map { node -> node.config.getOrNull(SemanticsProperties.ContentDescription) }
                .shouldBeEmpty()
        }

    @Test
    fun inspect_clickOnASwitchThatIsOn_pinsItsColorsAndNeverReachesTheGallery() =
        runComposeUiTest {
            val state = DemoAppState().apply { setOn(FluentGalleryKeys.Wifi, true) }
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(shown = PreviewMode.Light, split = remember { SplitState() }, skin = FluentSkin) {
                        PreviewPane(FluentLightSpec, Modifier.fillMaxSize()) { ComponentsTab(FluentLightSpec, state) }
                    }
                }
            }
            waitForIdle()
            onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Toggle switch"))
            waitForIdle()
            val before = state.gallerySnapshot()

            onNode(named("Wi-Fi") and isEnabled(), useUnmergedTree = true).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText("onAccentPrimary", substring = true)).assertExists()
            onNode(OnCard and hasText("primary", substring = true)).assertExists()
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

/** What the web mirror hears from one control of each kind, with the gallery as it first shows. */
private val WebNames: List<String> = listOf(
    "Bold, checkbox, not checked",
    "Bold, checkbox, not checked, disabled",
    "Email me updates, checkbox, not checked",
    "Keep a copy on this device, checkbox, checked, disabled",
    "Standard, radio, selected",
    "Express, radio, not selected",
    "Overnight, radio, not selected, disabled",
    "Wi-Fi, switch, off",
    "Airplane mode, switch, off, disabled",
    "Day, radio, selected",
    "Month, radio, not selected, disabled",
    "Shipping details, collapsed",
    "Payment, collapsed, disabled",
    "I understand, checkbox, not checked",
    "Recent, tab, selected",
    "Favorites, tab, not selected, disabled",
    "Home, tab, selected",
    "Photos, tab, not selected",
    "Inbox, selected",
    "Archive, not selected, disabled",
)

/** Wide enough for four columns and tall enough that every card composes. */
private val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/** Anything a user can press, type into or drag. */
private val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

/** Anything that takes keyboard focus. */
private val GalleryFocusable: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus)

/** A node named [name] exactly. */
private fun named(name: String): SemanticsMatcher = hasContentDescription(name)

private fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its colors, itself or through the control it is part of. The nearest
 * node that declares them may be an ancestor, as long as it sits inside a card and is not the card's
 * frame among [frames]. A Fluent button keeps its clickable on a row inside the part its modifier
 * reaches.
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

/** Whether anything in the card called [title] declares its colors, its frame aside. */
private fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

private fun String.isBannedInTheGallery(): Boolean {
    val name = substringAfterLast('.')
    return startsWith("androidx.compose.ui.window.") ||
        GalleryPopupWords.any { word -> word in name } ||
        this in GalleryBannedFluent ||
        (startsWith("com.materialkolor.builder.kit.") && GalleryKitImports.none { allowed -> startsWith(allowed) })
}

/** Everything the gallery keeps in [DemoAppState], to tell whether anything changed. */
private fun DemoAppState.gallerySnapshot(): List<Any> {
    val picks = GalleryChoices.flatMap { group -> (0 until 12).map { option -> isOn("$group.$option") } }
    return picks + GallerySwitches.map { switch -> isOn(switch) } +
        isChecked(FluentGalleryKeys.Updates) + isChecked(FluentGalleryKeys.Understood) + text
}

/**
 * The Fluent gallery in a Fluent pane of [spec], under the Fluent chrome, with motion frozen. With
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
        Chrome(FluentSkin) {
            if (webFolds) {
                ProvideWebFoldsForTest { PreviewPane(spec, modifier) { ComponentsTab(spec, state) } }
            } else {
                PreviewPane(spec, modifier) { ComponentsTab(spec, state) }
            }
        }
    }
}
