package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GalleryGroup
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.inspect.firstRated
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The cards with nothing to press, or whose only control has no disabled look.
 */
private val GalleryNoDisabled: Set<String> = setOf("Tabs", "Info bar", "Progress bar", "Progress ring", "Badges")

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
                    // Through the action, since some sit past the edge of the test window.
                    onNode(galleryNamed(name) and hasClickAction() and isEnabled(), useUnmergedTree = true)
                        .performSemanticsAction(SemanticsActions.OnClick)
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
                withClue(name) { onNode(galleryNamed(name) and isNotEnabled(), useUnmergedTree = true).assertExists() }
            }

            // Delete waits for the box, and puts it back.
            onNode(hasText("Delete") and hasClickAction()).assert(isNotEnabled())
            onNode(
                galleryNamed("I understand"),
                useUnmergedTree = true,
            ).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isChecked(FluentGalleryKeys.Understood) shouldBe true
            onNode(
                hasText("Delete") and hasClickAction() and isEnabled(),
            ).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isChecked(FluentGalleryKeys.Understood) shouldBe false
        }

    @Test
    fun slider_setProgressAndArrowKeys_moveTheSharedStop() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { GalleryHarness(FluentLightSpec, state, GalleryWhole) }
            waitForIdle()
            val volume = onNode(galleryNamed("Volume") and isEnabled(), useUnmergedTree = true)
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 6

            volume.performSemanticsAction(SemanticsActions.SetProgress) { set -> set(3f) }
            waitForIdle()
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 3

            volume.requestFocus()
            volume.performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            state.choice(FluentGalleryKeys.Volume, 11, default = 6) shouldBe 4

            val disabled = onNode(
                galleryNamed("Volume") and isNotEnabled(),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            (SemanticsActions.SetProgress in disabled.config) shouldBe false
            disabled.config[SemanticsProperties.ProgressBarRangeInfo].current shouldBe 4f
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

            for (name in WebNames) withClue(name) { onNode(galleryNamed(name), useUnmergedTree = true).assertExists() }

            onNode(galleryNamed("Shipping details, collapsed"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(FluentGalleryKeys.Details) shouldBe true
            onNode(galleryNamed("Shipping details, expanded"), useUnmergedTree = true).assertExists()
            onNodeWithText("12 Harbour Street").assertExists()

            onNode(
                galleryNamed("Wi-Fi, switch, off"),
                useUnmergedTree = true,
            ).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNode(galleryNamed("Wi-Fi, switch, on"), useUnmergedTree = true).assertExists()
        }

    @Test
    fun names_offTheWeb_areTheLabelsAlone() =
        runComposeUiTest {
            setContent { GalleryHarness(FluentLightSpec, DemoAppState(), GalleryWhole) }
            waitForIdle()

            val labels =
                listOf("Bold", "Email me updates", "Standard", "Wi-Fi", "Day", "Shipping details", "Recent", "Inbox")
            for (name in labels) {
                withClue(name) {
                    onAllNodes(galleryNamed(name), useUnmergedTree = true).fetchSemanticsNodes().shouldNotBeEmpty()
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
                    Inspecting(
                        shown = PreviewMode.Light,
                        split = remember { SplitState() },
                        expressive = ShellExpressive,
                    ) {
                        PreviewPane(FluentLightSpec, Modifier.fillMaxSize()) { ComponentsTab(FluentLightSpec, state) }
                    }
                }
            }
            waitForIdle()
            onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("Toggle switch"))
            waitForIdle()
            val before = state.gallerySnapshot()

            onNode(galleryNamed("Wi-Fi") and isEnabled(), useUnmergedTree = true).performClick()
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

/**
 * What the web mirror hears from one control of each kind, with the gallery as it first shows.
 */
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
