package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * Where a recording job would write the screenshots. Nothing is written unless capture is on.
 */
private const val ExpressiveScreenshotDir = "src/jvmTest/screenshots/gallery"

private const val ExpressiveSourceDir = "src/commonMain/kotlin/com/materialkolor/builder/preview/material"

/**
 * The sources the Expressive cards are drawn from.
 */
private val ExpressiveSources: List<String> = listOf("ExpressiveGallery.kt", "ExpressiveFeedback.kt")

/**
 * The Expressive cards whose components Material gives no disabled look, or that hold nothing to press.
 */
private val ExpressiveNoDisabled: Set<String> =
    setOf("FAB menu", "Material shapes", "Loading indicators", "Wavy progress indicators")

/**
 * Wide enough for four columns and tall enough that every card composes.
 */
private val ExpressiveWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 9000.dp)

/**
 * Words the Expressive sources never name, what opens outside the layout or runs an endless clock.
 * The endless ones are stems, so the architecture scan does not read this list as an animation.
 */
private val ExpressiveBannedWords: List<String> = listOf(
    "androidx.compose.ui.window",
    "Popup",
    "Dialog",
    "Tooltip",
    "ModalWideNavigationRail",
    "rememberInfinite",
    "infiniteRepeat",
)

/**
 * What the Expressive sources may take from the kit, its motion, the folds and the value node names.
 */
private val ExpressiveKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.a11y.foldsValueIntoName",
    "com.materialkolor.builder.kit.a11y.progressRoleWord",
    "com.materialkolor.builder.kit.a11y.valueNodeName",
)

/**
 * What the web mirror hears from each Expressive control as the gallery first shows.
 */
private val ExpressiveWebNames: List<String> = listOf(
    "Wi-Fi, checkbox, not checked",
    "Pets, checkbox, not checked, disabled",
    "Booking options, collapsed",
    "Booking options, collapsed, disabled",
    "Create, collapsed",
    "Train, radio, selected",
    "Bus, radio, not selected",
    "Ferry, radio, not selected, disabled",
    "Cookie, radio, selected",
    "Gem, radio, not selected",
)

/**
 * What the web mirror hears from the progress samples, which read their name as text.
 */
private val ExpressiveWebValues: List<String> = listOf(
    "Loading trips, progress bar",
    "Saving, progress bar, 60%",
    "Uploading, progress bar, 60%",
    "Syncing, progress bar, 60%",
)

@OptIn(ExperimentalTestApi::class)
class ExpressiveGalleryTest {
    @Test
    fun cards_expressive_followTheMaterialOnesUnderUniqueNames() {
        ExpressiveCards.size shouldBe 9
        ExpressiveGalleryCards.take(MaterialCards.size) shouldBe MaterialCards
        ExpressiveGalleryCards.map { card -> card.title }.distinct().size shouldBe ExpressiveGalleryCards.size
    }

    @Test
    fun gallery_expressiveOrNot_showsTheExpressiveCardsOnlyWhenExpressive() {
        for (expressive in listOf(true, false)) {
            withClue("expressive $expressive") {
                runComposeUiTest {
                    val composed = mutableSetOf<String>()
                    setContent {
                        ExpressiveHarness(LightSpec, DemoAppState(), ExpressiveWhole, composed, expressive = expressive)
                    }
                    waitForIdle()

                    val expected = if (expressive) ExpressiveGalleryCards else MaterialCards
                    composed shouldBe expected.map { card -> card.title }.toSet()
                }
            }
        }
    }

    @Test
    fun controls_everyExpressiveCard_declareTheirOwnRoles() =
        runComposeUiTest {
            // The menus open, so their items are there to check too.
            val state = DemoAppState().apply {
                setOn("gallery.split.open", true)
                setOn("gallery.fabMenu.open", true)
            }
            setContent { ExpressiveHarness(LightSpec, state, ExpressiveWhole) }
            waitForIdle()
            onNodeWithText("Book with points").assertExists()
            onNodeWithText("New trip").assertExists()

            // Unmerged, since a merged node also carries the roles its children declared.
            onAllNodes(hasClickAction(), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .filter { node -> PreviewRoles !in node.config }
                .map { node -> node.config.toString() }
                .shouldBeEmpty()
            ExpressiveCards
                .filterNot { card ->
                    expressiveFrame(card.title).expressiveDescendants().any { node -> PreviewRoles in node.config }
                }.map { card -> card.title }
                .shouldBeEmpty()
        }

    @Test
    fun cards_everyExpressiveComponentWithADisabledLook_showItEnabledAndDisabled() =
        runComposeUiTest {
            setContent { ExpressiveHarness(LightSpec, DemoAppState(), ExpressiveWhole) }
            waitForIdle()

            for (card in ExpressiveCards) {
                withClue(card.title) {
                    val nodes = expressiveFrame(card.title).expressiveDescendants()
                    val disabled = nodes.count { node -> SemanticsProperties.Disabled in node.config }
                    val enabled = nodes.count { node ->
                        SemanticsActions.OnClick in node.config && SemanticsProperties.Disabled !in node.config
                    }
                    if (card.title in ExpressiveNoDisabled) {
                        disabled shouldBe 0
                    } else {
                        (enabled > 0 && disabled > 0) shouldBe true
                    }
                }
            }
        }

    @Test
    fun gallery_everyExpressiveControlPressedTwice_opensNoPopupOrWindow() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { ExpressiveHarness(LightSpec, state, ExpressiveWhole) }
            waitForIdle()

            // Twice over, so what the first pass opens gets pressed too.
            repeat(2) {
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
            }
        }

    @OptIn(KitTestApi::class)
    @Test
    fun webNames_carryTheStateOfEveryExpressiveControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { ExpressiveHarness(LightSpec, state, ExpressiveWhole, webFolds = true) }
            waitForIdle()

            for (name in ExpressiveWebNames) {
                withClue(name) { onNode(hasContentDescription(name), useUnmergedTree = true).assertExists() }
            }
            for (name in ExpressiveWebValues) {
                withClue(name) { onNode(hasText(name), useUnmergedTree = true).assertExists() }
            }

            pressNamed("Wi-Fi, checkbox, not checked")
            state.isOn("gallery.amenity.Wi-Fi") shouldBe true
            onNode(hasContentDescription("Wi-Fi, checkbox, checked"), useUnmergedTree = true).assertExists()

            pressNamed("Booking options, collapsed")
            onNode(hasContentDescription("Booking options, expanded"), useUnmergedTree = true).assertExists()
            onNode(hasClickAction() and hasText("Book with points")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn("gallery.split.open") shouldBe false

            pressNamed("Create, collapsed")
            onNode(hasContentDescription("Create, expanded"), useUnmergedTree = true).assertExists()
            onNodeWithText("New trip").assertExists()
        }

    @Test
    fun connectedButton_pickedInOneCopyOfASplit_showsPickedInBoth() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome(PhotoSkin) {
                        SplitPreview(LightSpec, DarkSpec, SplitState(0.5f), ExpressiveWhole) { spec ->
                            ComponentsTab(spec, state)
                        }
                    }
                }
            }
            waitForIdle()

            onNode(hasClickAction() and hasText("Bus")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.choice("gallery.transport", 3) shouldBe 0
            onAllNodes(isToggleable() and hasAnyDescendant(hasText("Bus")), useUnmergedTree = true)
                .assertCountEquals(2)
                .assertAll(isOn())
        }

    @Test
    fun loadingIndicator_holdsStillWhileMotionIsFrozenAndLoopsOtherwise() {
        for (frozen in listOf(true, false)) {
            withClue("frozen $frozen") {
                runDesktopComposeUiTest(1280, 3600) {
                    mainClock.autoAdvance = false
                    setContent {
                        ExpressiveHarness(LightSpec, DemoAppState(), Modifier.fillMaxSize(), frozen = frozen)
                    }
                    mainClock.advanceTimeBy(100)
                    val loading = onNode(hasContentDescription("Loading trips"))
                    val before = loading.captureToImage().toPixelMap()
                    mainClock.advanceTimeBy(700)
                    val after = loading.captureToImage().toPixelMap()
                    val same = (0 until before.width).all { x ->
                        (0 until before.height).all { y -> before[x, y] == after[x, y] }
                    }
                    same shouldBe frozen
                }
            }
        }
    }

    @Test
    fun loadingShapes_walkOutToTheLastShapeAndBack() {
        loadingShapes(0f) shouldBe 0f
        loadingShapes(0.25f) shouldBe 0.5f
        loadingShapes(0.5f) shouldBe 1f
        loadingShapes(1f) shouldBe 0f
    }

    @Test
    fun screens_wholeExpressiveGalleryBothModes_renderEveryCard() =
        runDesktopComposeUiTest(1280, 3600) {
            var spec by mutableStateOf(LightSpec)
            setContent { ExpressiveHarness(spec, DemoAppState(), Modifier.fillMaxSize()) }

            for (mode in listOf(LightSpec, DarkSpec)) {
                spec = mode
                waitForIdle()
                onNodeWithText(ExpressiveCards.last().title).assertExists()
                onRoot().captureRoboImage("$ExpressiveScreenshotDir/expressive-whole-${mode.label}.png")
            }
        }

    @Test
    fun expressiveSources_openNothingOutsideTheLayoutAndNeverLoopOnTheirOwn() {
        for (name in ExpressiveSources) {
            withClue(name) {
                val source = File(ExpressiveSourceDir, name)
                source.isFile shouldBe true
                val text = source.readText()
                ExpressiveBannedWords.filter { word -> word in text }.shouldBeEmpty()
                val imports = text
                    .lines()
                    .map { line -> line.trim() }
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                imports.filter { imported -> imported == "androidx.compose.material3.DropdownMenu" }.shouldBeEmpty()
                imports
                    .filter { imported -> imported.startsWith("com.materialkolor.builder.kit.") }
                    .filterNot { imported -> ExpressiveKitImports.any { allowed -> imported.startsWith(allowed) } }
                    .shouldBeEmpty()
                // A loading indicator only with its progress, a wavy one only holding its wave still.
                Regex("""\b(Contained)?LoadingIndicator\(\s*(\S+)""")
                    .findAll(text)
                    .map { call -> call.groupValues[2] }
                    .filterNot { first -> first.startsWith("progress") }
                    .toList()
                    .shouldBeEmpty()
                val wavy = Regex("""\b(Linear|Circular)WavyProgressIndicator\(""").findAll(text).count()
                Regex("""waveSpeed = 0\.dp""").findAll(text).count() shouldBe wavy
            }
        }
        // The components the hazards above are about are really there to check.
        val all = ExpressiveSources.joinToString { name -> File(ExpressiveSourceDir, name).readText() }
        listOf("ButtonGroup(", "SplitButtonLayout(", "FloatingActionButtonMenu(", "LoadingIndicator(")
            .filterNot { call -> call in all }
            .shouldBeEmpty()
    }
}

/**
 * Presses the node the web mirror hears as [name], and lets the gallery settle.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pressNamed(name: String) {
    onNode(hasContentDescription(name), useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
    waitForIdle()
}

/**
 * Every node under this one in the unmerged tree.
 */
private fun SemanticsNode.expressiveDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.expressiveDescendants() }

/**
 * The frame of the card called [title], the node its title text sits in.
 */
private fun SemanticsNodeInteractionsProvider.expressiveFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/**
 * The Material 3 gallery in a pane of [spec] under the chrome, in the Expressive flavour unless
 * [expressive] says otherwise. Motion is frozen and reduced unless [frozen] is false, and the web
 * fold is on when [webFolds] asks for it.
 */
@OptIn(KitTestApi::class)
@Composable
private fun ExpressiveHarness(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier,
    composed: MutableSet<String>? = null,
    expressive: Boolean = true,
    webFolds: Boolean = false,
    frozen: Boolean = true,
) {
    val probe: ((String) -> Unit)? = composed?.let { titles ->
        { where: String -> if (where.startsWith(GALLERY_CARD)) titles += where.removePrefix(GALLERY_CARD) }
    }
    val skin = Skin(Library.Material3, expressive)
    CompositionLocalProvider(LocalMotionFrozen provides frozen, LocalCompositionProbe provides probe) {
        Chrome(skin) {
            CompositionLocalProvider(LocalReducedMotion provides frozen) {
                val pane = @Composable { PreviewPane(spec, modifier) { ComponentsTab(spec, state) } }
                if (webFolds) ProvideWebFoldsForTest(pane) else pane()
            }
        }
    }
}
