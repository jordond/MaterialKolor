package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.a11y.KitTestApi
import com.materialkolor.builder.kit.a11y.ProvideWebFoldsForTest
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DeviceFrames
import com.materialkolor.builder.preview.InspectingPane
import com.materialkolor.builder.preview.PaneKitImports
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.TripsNaming
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.checkInspectPins
import com.materialkolor.builder.preview.checkNameFlips
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.checkTripsControlsDeclareRoles
import com.materialkolor.builder.preview.checkTripsLayOutForEveryWidth
import com.materialkolor.builder.preview.checkWebNames
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.inspect.firstRated
import com.materialkolor.builder.preview.isKitImportBeyond
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.opensAWindow
import com.materialkolor.builder.preview.refsOnScreen
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.TripsDestination
import com.materialkolor.builder.preview.tripsSnapshot
import com.materialkolor.builder.preview.walkTripsControls
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val FluentSourceDir = "commonMain/kotlin/com/materialkolor/builder/preview/fluent"

/**
 * The files of Fluent's Trips, which the gallery's sources are not.
 */
private val TripsSources: Set<String> =
    setOf("TripsApp.kt", "TripDetail.kt", "TripsShades.kt", "FluentRoles.kt", "AppEntry.kt")

/**
 * What Trips' sources never name, anything that opens outside the layout.
 */
private val TripsBannedWords: List<String> = listOf(
    "androidx.compose.ui.window",
    "Popup",
    "Dialog",
    "Tooltip",
    // Only the theme, never the one that adds a host and a backdrop round the screen.
    "FluentTheme(",
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
class FluentTripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() =
        checkTripsControlsDeclareRoles(
            harness = FluentTrips,
            spec = FluentLightSpec,
            // The navigation layer of compose-fluent 0.1.0 keeps its click action. It wraps the
            // destinations, so nothing outside it can clear its semantics and keep theirs.
            ignored = NavigationShield or ScrollbarArrow or UnderAnOverlay,
        ) { node -> node.declaresItsRoles() }

    @Test
    fun shades_everyDeviceWidthFirstScreen_showTheAccentFillAndTheShadesTripsPaints() {
        for ((width, frame) in DeviceFrames) {
            for (spec in listOf(FluentLightSpec, FluentDarkSpec)) {
                withClue("$width ${spec.label}") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        setContent { FluentTrips(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

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
            setContent { FluentTrips(FluentLightSpec, state, DeviceWidth.Desktop, Modifier.size(1280.dp, 800.dp)) }

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
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() =
        checkTripsLayOutForEveryWidth(FluentTrips, TripsNaming.Description, FluentLightSpec, FluentDarkSpec)

    @Test
    fun controls_clicked_openTheTripFilterTheListTickTheChecklistAndTurnMapsOn() =
        runComposeUiTest {
            walkTripsControls(FluentTrips, FluentLightSpec, TripsNaming.Description)
            onNode(OfflineSwitch and isOn()).assertExists()
        }

    @Test
    fun inspect_clickOnASwitch_pinsItsColorsAndNeverReachesTheApp() =
        runDesktopComposeUiTest(412, 900) {
            val state = DemoAppState().apply { setOn(OfflineMapsSwitch, true) }
            setContent {
                InspectingPane {
                    PreviewPane(FluentLightSpec, Modifier.fillMaxSize()) {
                        FluentAppEntry(FluentLightSpec, state, DeviceWidth.Phone)
                    }
                }
            }
            waitForIdle()
            // The inspector keeps the pane short, so the list scrolls the switch into view first.
            onNode(hasScrollToKeyAction()).performScrollToKey("offline")
            waitForIdle()

            checkInspectPins(
                target = onNode(OfflineSwitch),
                tokens = listOf(hasText("onAccentPrimary", substring = true), hasText("primary", substring = true)),
                snapshot = { state.tripsSnapshot() },
            )
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

            checkWebNames(TripsWebNames, useUnmergedTree = false)
            checkNameFlips("Offline maps, switch, off", "Offline maps, switch, on", useUnmergedTree = false)
        }

    @Test
    fun rail_everyDestinationHoveredOnATablet_opensNoTooltip() =
        runDesktopComposeUiTest(840, 900) {
            setContent { FluentTrips(FluentLightSpec, DemoAppState(), DeviceWidth.Tablet, Modifier.fillMaxSize()) }
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
        val sources = moduleSource(FluentSourceDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.name in TripsSources }
        sources.size shouldBe TripsSources.size
        checkSourcesOpenNothingAndNeverLoop(sources) { imported ->
            imported.opensAWindow() || imported.isKitImportBeyond(PaneKitImports)
        }
        for (source in sources) {
            withClue(source.name) {
                val text = source.readText()
                TripsBannedWords.filter { word -> word in text }.shouldBeEmpty()
                EndlessProgress
                    .findAll(text)
                    .map { match -> match.value }
                    .toList()
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
private fun ComposeUiTest.shadesOnScreen(isDark: Boolean): Set<FluentShade> =
    refsOnScreen().mapNotNull { ref -> ref.paintedShade(isDark) }.toSet()

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
