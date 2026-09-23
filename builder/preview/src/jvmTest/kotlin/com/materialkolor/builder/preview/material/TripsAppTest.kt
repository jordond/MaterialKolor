package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on.
 */
private const val TripsScreenshotDir = "src/jvmTest/screenshots/trips"

/** The width the dock frames each device at, the kit's screen widths. */
private val TripsFrames: Map<DeviceWidth, Dp> =
    mapOf(DeviceWidth.Phone to 412.dp, DeviceWidth.Tablet to 840.dp, DeviceWidth.Desktop to 1280.dp)

private const val TripsFrameHeight = 900

/** The four families F-20 wants on every screen. */
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

/** The offline maps row as the merged tree shows it. */
private val TripsOfflineRow: SemanticsMatcher = isToggleable() and hasText("Offline maps")

@OptIn(ExperimentalTestApi::class)
class TripsAppTest {
    @Test
    fun roles_everyDeviceWidth_coverEveryControlAndTheSchemeF20Asks() {
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
                                .requiredSize(frame, 2400.dp),
                        )
                    }

                    val controls = onAllNodes(hasClickAction() or hasSetTextAction()).fetchSemanticsNodes()
                    controls.shouldNotBeEmpty()
                    controls
                        .filter { node -> PreviewRoles !in node.config }
                        .map { node -> node.config.toString() }
                        .shouldBeEmpty()

                    val used = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                        .fetchSemanticsNodes()
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
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        for ((width, frame) in TripsFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.value.toInt(), TripsFrameHeight) {
                    var spec by mutableStateOf(LightSpec)
                    setContent { TripsHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(LightSpec, DarkSpec)) {
                        spec = mode
                        waitForIdle()
                        val (row, title) = onAllNodesWithText("Lisbon, Portugal")
                            .fetchSemanticsNodes()
                            .map { node -> node.boundsInRoot }
                        if (width == DeviceWidth.Phone) {
                            title.top shouldBeGreaterThan row.bottom
                            onAllNodesWithText("Explore").assertCountEquals(0)
                        } else {
                            title.left shouldBeGreaterThan row.right
                            onNodeWithText("Explore").assertExists()
                        }
                        onRoot().captureRoboImage("$TripsScreenshotDir/${width.name}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun offlineMaps_toggledInEitherCopyOfASplit_flipsBoth() =
        runComposeUiTest {
            val state = DemoAppState()
            // With the handle at the start edge the dark copy shows everywhere, so it takes the click.
            val split = SplitState(0f)
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome {
                        SplitPreview(LightSpec, DarkSpec, split, Modifier.size(840.dp, 760.dp)) { spec ->
                            MaterialAppEntry(spec, state, DeviceWidth.Tablet, expressive = false)
                        }
                    }
                }
            }
            val bothRows =
                onAllNodes(isToggleable() and hasAnyDescendant(hasText("Offline maps")), useUnmergedTree = true)

            onNode(TripsOfflineRow).performClick()
            waitForIdle()
            state.isOn(OfflineMapsSwitch) shouldBe true
            bothRows.assertCountEquals(2).assertAll(isOn())

            split.fraction = 1f
            waitForIdle()
            onNode(TripsOfflineRow).performClick()
            waitForIdle()
            state.isOn(OfflineMapsSwitch) shouldBe false
            bothRows.assertCountEquals(2).assertAll(isOff())
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
}

/** The Trips app in a Material 3 pane of [spec], under the chrome, with motion frozen. */
@Composable
private fun TripsHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome {
            PreviewPane(spec, modifier) { MaterialAppEntry(spec, state, width, expressive = false) }
        }
    }
}
