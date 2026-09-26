package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.inspect.firstRated
import com.materialkolor.builder.preview.split.SplitState
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.TripsDestination
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

private const val FluentSourceDir = "src/commonMain/kotlin/com/materialkolor/builder/preview/fluent"

/**
 * The files of Fluent's Trips, which the gallery's sources are not.
 */
private val TripsSources: Set<String> =
    setOf("TripsApp.kt", "TripDetail.kt", "TripsShades.kt", "FluentRoles.kt", "AppEntry.kt")

/**
 * What Trips' sources never name, anything that opens outside the layout or an endless clock.
 */
private val TripsBannedWords: List<String> = listOf(
    "androidx.compose.ui.window",
    "Popup",
    "Dialog",
    "Tooltip",
    // Only the theme, never the one that adds a host and a backdrop round the screen.
    "FluentTheme(",
    // Stems, so the architecture scan does not read this list as an endless animation.
    "rememberInfinite",
    "infiniteRepeat",
)

/**
 * What Trips may take from the kit, its motion, the fold modifiers and `InnerTextWithoutHandles`.
 */
private val TripsKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.headless.InnerTextWithoutHandles",
)

/**
 * A progress bar or ring called without a value, the endless kind.
 */
private val EndlessProgress = Regex("""\bProgress(Bar|Ring)\((?!\s*progress\b)""")

/**
 * What the web mirror hears from one control of each kind, with the app as it first shows on a tablet.
 */
private val TripsWebNames: List<String> = listOf(
    "Offline maps, switch, off",
    "Lisbon, Portugal, selected",
    "Kyoto, Japan, not selected",
    "Trips, selected",
    "Explore, not selected",
    "Passports, checkbox, not checked",
)

/**
 * The offline maps switch.
 */
private val OfflineSwitch: SemanticsMatcher = isToggleable() and hasContentDescription("Offline maps")

/**
 * A trip's row in the list, named for the trip.
 */
private fun tripRow(name: String): SemanticsMatcher = hasClickAction() and hasContentDescription(name)

@OptIn(ExperimentalTestApi::class)
class TripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() {
        for ((width, frame) in FluentFrames) {
            withClue(width) {
                runComposeUiTest {
                    setContent {
                        FluentHarness(
                            spec = FluentLightSpec,
                            state = DemoAppState(),
                            width = width,
                            // Tall enough that every lazy item composes, wider than the window on desktop.
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(frame.width.dp, 2400.dp),
                        )
                    }

                    onNode(hasText("Turn on"), useUnmergedTree = true).assertExists()
                    // Unmerged, since a merged node also carries the roles its children declared. The
                    // navigation layer of compose-fluent 0.1.0 keeps its click action. It wraps the
                    // destinations, so nothing outside it can clear its semantics and keep theirs.
                    val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .filterNot { node -> (NavigationShield or ScrollbarArrow or UnderAnOverlay).matches(node) }
                    controls.shouldNotBeEmpty()
                    controls
                        .filterNot { node -> node.declaresItsRoles() }
                        .map { node -> "${node.boundsInRoot} ${node.config}" }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun shades_everyDeviceWidthFirstScreen_showTheAccentFillAndTheShadesTripsPaints() {
        for ((width, frame) in FluentFrames) {
            for (spec in listOf(FluentLightSpec, FluentDarkSpec)) {
                withClue("$width ${spec.label}") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        setContent { FluentHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                        val shades = TripsShades(spec.isDark)
                        val painted = setOf(shades.accentFill, shades.tint, shades.accentText) + shades.scene
                        shadesOnScreen(spec.isDark) shouldBe painted
                    }
                }
            }
        }
    }

    @Test
    fun accentControls_switchOnAndOpenTrip_resolveToARowOfTheAudit() =
        runComposeUiTest {
            val state = DemoAppState().apply { setOn(OfflineMapsSwitch, true) }
            setContent { FluentHarness(FluentLightSpec, state, DeviceWidth.Desktop, Modifier.size(1280.dp, 800.dp)) }

            for (control in listOf(OfflineSwitch and isOn(), tripRow("Lisbon, Portugal"))) {
                val refs = onNode(control, useUnmergedTree = true).fetchSemanticsNode().config[PreviewRoles]
                refs shouldBe FluentAccentRefs
                for (isDark in listOf(false, true)) {
                    withClue("$refs dark $isDark") {
                        FluentLightSpec.result.audit
                            .firstRated(refs, isDark)
                            .shouldNotBeNull()
                    }
                }
            }
        }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        for ((width, frame) in FluentFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(FluentLightSpec)
                    setContent { FluentHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(FluentLightSpec, FluentDarkSpec)) {
                        spec = mode
                        waitForIdle()
                        // The row's own words sit under its layer, so the text is the open trip's title.
                        val title = onNodeWithText("Lisbon, Portugal").fetchSemanticsNode().boundsInRoot
                        val row = onNode(tripRow("Lisbon, Portugal")).fetchSemanticsNode().boundsInRoot
                        val rail = onAllNodes(hasContentDescription(TripsDestination.Explore.label))
                        if (width == DeviceWidth.Phone) {
                            title.top shouldBeGreaterThan row.bottom
                            rail.assertCountEquals(0)
                        } else {
                            title.left shouldBeGreaterThan row.right
                            rail.assertCountEquals(1)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun controls_clicked_openTheTripFilterTheListTickTheChecklistAndTurnMapsOn() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { FluentHarness(FluentLightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 760.dp)) }

            onNode(tripRow("Kyoto, Japan")).performClick()
            waitForIdle()
            state.selectedItem shouldBe 1
            onNodeWithText("Kyoto, Japan").assertExists()
            onNodeWithText("Nothing planned yet").assertExists()

            onNode(hasClickAction() and hasContentDescription(TripFilter.Shared.label)).performClick()
            waitForIdle()
            state.tabIndex shouldBe TripFilter.Shared.ordinal
            onAllNodes(tripRow("Kyoto, Japan")).assertCountEquals(0)
            onAllNodes(tripRow("Lisbon, Portugal")).assertCountEquals(1)

            onNode(hasClickAction() and hasContentDescription(TripFilter.Past.label)).performClick()
            waitForIdle()
            onNodeWithText("No past trips yet").assertExists()

            onNode(isToggleable() and hasContentDescription(PackingItem.Passports.label))
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isChecked(PackingItem.Passports.key) shouldBe true
            onNodeWithText("1 of 3").assertExists()

            onNode(hasClickAction() and hasText("Turn on")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(OfflineMapsSwitch) shouldBe true
            onAllNodesWithText("Turn on").assertCountEquals(0)
            onNode(OfflineSwitch and isOn()).assertExists()
        }

    @Test
    fun inspect_clickOnASwitch_pinsItsColorsAndNeverReachesTheApp() =
        runDesktopComposeUiTest(412, 900) {
            val state = DemoAppState().apply { setOn(OfflineMapsSwitch, true) }
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(shown = PreviewMode.Light, split = remember { SplitState() }, expressive = ShellExpressive) {
                        PreviewPane(FluentLightSpec, Modifier.fillMaxSize()) {
                            FluentAppEntry(FluentLightSpec, state, DeviceWidth.Phone)
                        }
                    }
                }
            }
            waitForIdle()
            // The inspector keeps the pane short, so the list scrolls the switch into view first.
            onNode(hasScrollToKeyAction()).performScrollToKey("offline")
            waitForIdle()
            val before = state.tripsSnapshot()

            onNode(OfflineSwitch).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText("onAccentPrimary", substring = true)).assertExists()
            onNode(OnCard and hasText("primary", substring = true)).assertExists()
            state.tripsSnapshot() shouldBe before
        }

    @OptIn(KitTestApi::class)
    @Test
    fun names_onTheWeb_foldTheStateOfEveryKindOfControl() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome(ShellExpressive) {
                        ProvideWebFoldsForTest {
                            PreviewPane(FluentLightSpec, Modifier.size(840.dp, 1400.dp)) {
                                FluentAppEntry(FluentLightSpec, state, DeviceWidth.Tablet)
                            }
                        }
                    }
                }
            }
            waitForIdle()

            for (name in TripsWebNames) withClue(name) { onNode(hasContentDescription(name)).assertExists() }

            onNode(hasContentDescription("Offline maps, switch, off")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            onNode(hasContentDescription("Offline maps, switch, on")).assertExists()
        }

    @Test
    fun rail_everyDestinationHoveredOnATablet_opensNoTooltip() =
        runDesktopComposeUiTest(840, 900) {
            setContent { FluentHarness(FluentLightSpec, DemoAppState(), DeviceWidth.Tablet, Modifier.fillMaxSize()) }
            waitForIdle()

            for (destination in TripsDestination.entries) {
                withClue(destination) {
                    onNode(hasClickAction() and hasContentDescription(destination.label)).performMouseInput {
                        moveTo(center)
                    }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                }
            }
        }

    @Test
    fun tripsSources_openNothingOutsideTheLayoutAndNeverLoop() {
        val sources = File(FluentSourceDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.name in TripsSources }
        sources.size shouldBe TripsSources.size
        for (source in sources) {
            withClue(source.name) {
                val text = source.readText()
                TripsBannedWords.filter { word -> word in text }.shouldBeEmpty()
                EndlessProgress
                    .findAll(text)
                    .map { match -> match.value }
                    .toList()
                    .shouldBeEmpty()
                text
                    .lines()
                    .map { line -> line.trim() }
                    .filter { line -> line.startsWith("import com.materialkolor.builder.kit.") }
                    .map { line -> line.removePrefix("import ") }
                    .filterNot { imported -> TripsKitImports.any { allowed -> imported.startsWith(allowed) } }
                    .shouldBeEmpty()
            }
        }
        sources.sumOf { source -> source.readLines().size } shouldBeGreaterThanOrEqual 300
    }
}

/**
 * Every shade the elements declared on the first screen paint, in the mode [isDark] picks.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.shadesOnScreen(isDark: Boolean): Set<FluentShade> {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> node.boundsInRoot.overlaps(screen) }
        .flatMap { node -> node.config[PreviewRoles] }
        .mapNotNull { ref: ColorRef -> ref.paintedShade(isDark) }
        .toSet()
}

/**
 * How far a control's clickable may sit inside the part that declares its colors, in pixels.
 */
private const val DeclaredPartSlack = 4f

/**
 * Whether the node declares its colors, itself or through the part of the same control that its
 * modifier reaches. A Fluent button keeps its clickable on a row inside that part, which has the
 * row's bounds, where a card or a pane round the control is larger.
 */
private fun SemanticsNode.declaresItsRoles(): Boolean {
    val holder = generateSequence(this) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null &&
        holder.boundsInRoot.width - boundsInRoot.width <= DeclaredPartSlack &&
        holder.boundsInRoot.height - boundsInRoot.height <= DeclaredPartSlack
}

/**
 * Everything Trips keeps in [DemoAppState], to tell whether anything changed.
 */
private fun DemoAppState.tripsSnapshot(): List<Any> =
    listOf(selectedItem, tabIndex, text, isOn(OfflineMapsSwitch)) +
        PackingItem.entries.map { item -> isChecked(item.key) }
