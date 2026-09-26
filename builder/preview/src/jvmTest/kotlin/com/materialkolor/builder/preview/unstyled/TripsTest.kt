package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.ShellChrome
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.TripFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The frame the dock shows each device in, the kit's screen widths at the height of a first screen.
 */
private val TripsFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/**
 * The four families every screen has to use.
 */
private val TripsFamilies: Map<String, Set<Role>> = mapOf(
    "primary" to setOf(Role.Primary, Role.OnPrimary, Role.PrimaryContainer, Role.OnPrimaryContainer),
    "secondary" to setOf(Role.Secondary, Role.OnSecondary, Role.SecondaryContainer, Role.OnSecondaryContainer),
    "tertiary" to setOf(Role.Tertiary, Role.OnTertiary, Role.TertiaryContainer, Role.OnTertiaryContainer),
    "error" to setOf(Role.Error, Role.OnError, Role.ErrorContainer, Role.OnErrorContainer),
)

private val TripsContainerLevels: Set<Role> = setOf(
    Role.SurfaceContainerLowest,
    Role.SurfaceContainerLow,
    Role.SurfaceContainer,
    Role.SurfaceContainerHigh,
    Role.SurfaceContainerHighest,
)

/**
 * The Trips app's sources, from the module the tests run in.
 */
private val TripsSources: List<String> = listOf(
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/AppEntry.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledTrips.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledTripDetail.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledParts.kt",
    "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledRoles.kt",
)

/**
 * Words in the names of what opens a popup, a window or a portal, which on the web take the mirror
 * over, and Compose Unstyled's own field, which keeps its inner text to itself.
 */
private val TripsPopupWords: List<String> =
    listOf("Popup", "Dialog", "Modal", "BottomSheet", "DropdownMenu", "Tooltip", "Portal")

/**
 * What the app takes from the kit, its motion, the fold modifiers and `InnerTextWithoutHandles`.
 */
private val TripsKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.headless.InnerTextWithoutHandles",
)

/**
 * The start of the names of the endless animation APIs. Written out whole they would trip the
 * builder's own architecture scan of this file.
 */
private val TripsEndlessMotion: List<String> = listOf("rememberInfinite", "infiniteRepeat")

@OptIn(ExperimentalTestApi::class)
class TripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() {
        for ((width, frame) in TripsFrames) {
            withClue(width) {
                runComposeUiTest {
                    setContent {
                        TripsHarness(
                            spec = LightSpec,
                            state = DemoAppState(),
                            width = width,
                            // Tall enough that every lazy item composes, wider than the window on desktop.
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(frame.width.dp, 2400.dp),
                        )
                    }

                    // Unmerged, since a merged node also carries the roles its children declared.
                    val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                    controls.shouldNotBeEmpty()
                    controls
                        .filter { node -> PreviewRoles !in node.config }
                        .map { node -> node.config.toString() }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun roles_everyDeviceWidthFirstScreen_showTheSchemeF20Asks() {
        for ((width, frame) in TripsFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    setContent { TripsHarness(LightSpec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    val screen = onRoot().fetchSemanticsNode().boundsInRoot
                    val used = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .filter { node -> node.boundsInRoot.overlaps(screen) }
                        .flatMap { node -> node.config[PreviewRoles] }
                        .filterIsInstance<ColorRef.OfRole>()
                        .map { ref -> ref.role }
                        .toSet()
                    TripsFamilies.filterValues { family -> family.none { role -> role in used } }.keys.shouldBeEmpty()
                    (TripsContainerLevels intersect used).size shouldBeGreaterThanOrEqual 4
                    used shouldContain Role.Outline
                }
            }
        }
    }

    @Test
    fun controls_clicked_openTheTripFilterTheListAndTickTheChecklist() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { TripsHarness(LightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 760.dp)) }

            onNode(hasClickAction() and hasText("Kyoto, Japan")).performClick()
            waitForIdle()
            state.selectedItem shouldBe 1
            onAllNodesWithText("Kyoto, Japan").assertCountEquals(2)
            onNodeWithText("Nothing planned yet").assertExists()

            onNode(hasClickAction() and hasText("Shared")).performClick()
            waitForIdle()
            state.tabIndex shouldBe TripFilter.Shared.ordinal
            onAllNodes(hasClickAction() and hasText("Kyoto, Japan")).assertCountEquals(0)
            onAllNodes(hasClickAction() and hasText("Lisbon, Portugal")).assertCountEquals(1)

            onNode(hasClickAction() and hasText("Past")).performClick()
            waitForIdle()
            onNodeWithText("No past trips yet").assertExists()

            onNode(
                isToggleable() and hasText(PackingItem.Passports.label),
            ).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isChecked(PackingItem.Passports.key) shouldBe true
            onNodeWithText("1 of 3").assertExists()

            onNode(hasClickAction() and hasText("Turn on")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(OfflineMapsSwitch) shouldBe true
            onAllNodesWithText("Turn on").assertCountEquals(0)
        }

    @Test
    fun tripsSources_openNoPopupWindowOrPortalAndNeverLoop() {
        for (path in TripsSources) {
            withClue(path) {
                val source = File(path)
                source.isFile shouldBe true
                val lines = source.readLines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported -> imported.isBannedInTrips() }
                    .shouldBeEmpty()
                lines
                    .filter { line -> TripsEndlessMotion.any { stem -> stem in line } }
                    .shouldBeEmpty()
            }
        }
    }
}

private fun String.isBannedInTrips(): Boolean {
    val name = substringAfterLast('.')
    return startsWith("androidx.compose.ui.window.") ||
        TripsPopupWords.any { word -> word in name } ||
        (startsWith("com.composeunstyled.") && "TextField" in name) ||
        (startsWith("com.materialkolor.builder.kit.") && TripsKitImports.none { allowed -> startsWith(allowed) })
}

/**
 * The Trips app in an Unstyled pane of [spec]'s document, under the shell chrome, with motion frozen.
 */
@Composable
private fun TripsHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(ShellChrome) {
            val unstyled = remember(spec) { spec.on(Library.Unstyled) }
            PreviewPane(unstyled, modifier) { UnstyledAppEntry(unstyled, state, width) }
        }
    }
}
