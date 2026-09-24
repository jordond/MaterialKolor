package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.inspect.INSPECT_CARD_TAG
import com.materialkolor.builder.preview.inspect.Inspecting
import com.materialkolor.builder.preview.inspect.OnCard
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
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test
import androidx.compose.ui.semantics.Role as SemanticsRole

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on.
 */
private const val DashboardScreenshotDir = "src/jvmTest/screenshots/dashboard"

/** Where the dashboard's sources live, from the module the tests run in. */
private const val DashboardSourceDir = "src/commonMain/kotlin/com/materialkolor/builder/preview/unstyled"

/** The frame the dock shows each device in, the kit's screen widths at the height of a first screen. */
private val DashboardFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/** The four families F-20 wants on every screen. */
private val DashboardFamilies: Map<String, Set<Role>> = mapOf(
    "primary" to setOf(Role.Primary, Role.OnPrimary, Role.PrimaryContainer, Role.OnPrimaryContainer),
    "secondary" to setOf(Role.Secondary, Role.OnSecondary, Role.SecondaryContainer, Role.OnSecondaryContainer),
    "tertiary" to setOf(Role.Tertiary, Role.OnTertiary, Role.TertiaryContainer, Role.OnTertiaryContainer),
    "error" to setOf(Role.Error, Role.OnError, Role.ErrorContainer, Role.OnErrorContainer),
)

private val DashboardContainerLevels: Set<Role> = setOf(
    Role.SurfaceContainerLowest,
    Role.SurfaceContainerLow,
    Role.SurfaceContainer,
    Role.SurfaceContainerHigh,
    Role.SurfaceContainerHighest,
)

/** The dashboard's sources whose names do not start with Dashboard. */
private val DashboardSideSources: Set<String> = setOf("AppEntry.kt", "UnstyledRoles.kt")

/** Compose Unstyled parts that open a window, a portal or a text field, by a word in their name. */
private val DashboardBannedUnstyled: List<String> =
    listOf("Dialog", "Modal", "BottomSheet", "DropdownMenu", "Tooltip", "Portal", "TextField")

/**
 * The start of the names of the endless animation APIs. Written out whole they would trip the
 * builder's own architecture scan of this file.
 */
private val DashboardEndlessMotion: List<String> = listOf("rememberInfinite", "infiniteRepeat")

/** The order status button, which is the only dropdown on the page. */
private val StatusButton: SemanticsMatcher =
    hasClickAction() and SemanticsMatcher.expectValue(SemanticsProperties.Role, SemanticsRole.DropdownList)

/** The pick of the status menu that keeps [filter], in any copy. */
private fun menuItem(filter: OrderFilter): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, SemanticsRole.RadioButton) and
        hasAnyDescendant(hasText(filter.label))

@OptIn(ExperimentalTestApi::class)
class DashboardAppTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() {
        for ((width, frame) in DashboardFrames) {
            withClue(width) {
                runComposeUiTest {
                    tallDashboard(width, frame)

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
    fun tokenPanel_everyDeviceWidth_listsEveryRoleTheScreenDeclares() {
        val listed = DashboardToken.entries.map { entry -> entry.role }.toSet()
        for ((width, frame) in DashboardFrames) {
            withClue(width) {
                runComposeUiTest {
                    tallDashboard(width, frame)

                    val declared = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .flatMap { node -> node.config[PreviewRoles] }
                        .filterIsInstance<ColorRef.OfRole>()
                        .map { ref -> ref.role }
                        .toSet()
                    (declared - listed).shouldBeEmpty()
                    onAllNodesWithText(DashboardToken.Scrim.token.name).fetchSemanticsNodes().shouldNotBeEmpty()
                }
            }
        }
    }

    @Test
    fun roles_everyDeviceWidthFirstScreen_showTheSchemeF20Asks() {
        for ((width, frame) in DashboardFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    setContent { DashboardHarness(LightSpec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    val screen = onRoot().fetchSemanticsNode().boundsInRoot
                    val used = onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .filter { node -> node.boundsInRoot.overlaps(screen) }
                        .flatMap { node -> node.config[PreviewRoles] }
                        .filterIsInstance<ColorRef.OfRole>()
                        .map { ref -> ref.role }
                        .toSet()
                    DashboardFamilies
                        .filterValues { family -> family.none { role -> role in used } }
                        .keys
                        .shouldBeEmpty()
                    (DashboardContainerLevels intersect used).size shouldBeGreaterThanOrEqual 4
                    used shouldContain Role.Outline
                }
            }
        }
    }

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() {
        val destination = DashboardDestination.Customers.label
        for ((width, frame) in DashboardFrames) {
            withClue(width) {
                runDesktopComposeUiTest(frame.width, frame.height) {
                    var spec by mutableStateOf(LightSpec)
                    setContent { DashboardHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                    for (mode in listOf(LightSpec, DarkSpec)) {
                        spec = mode
                        waitForIdle()
                        val customers = onNodeWithText(MetricKind.Customers.label).fetchSemanticsNode().boundsInRoot
                        val refunds = onNodeWithText(MetricKind.Refunds.label).fetchSemanticsNode().boundsInRoot
                        when (width) {
                            DeviceWidth.Phone -> {
                                refunds.top shouldBeGreaterThan customers.bottom
                                onAllNodesWithText(destination).assertCountEquals(0)
                                onAllNodesWithContentDescription(destination).assertCountEquals(0)
                                onNodeWithContentDescription(DashboardCopy.Navigation).assertExists()
                            }
                            DeviceWidth.Tablet -> {
                                refunds.top shouldBeLessThan customers.bottom
                                onAllNodesWithText(destination).assertCountEquals(0)
                                onNodeWithContentDescription(destination).assertExists()
                            }
                            DeviceWidth.Desktop -> {
                                refunds.top shouldBeLessThan customers.bottom
                                onNodeWithText(destination).assertExists()
                            }
                        }
                        onRoot().captureRoboImage("$DashboardScreenshotDir/${width.name}-${mode.label}.png")
                    }
                }
            }
        }
    }

    @Test
    fun menuAndDrawer_openedInOneCopyOfASplit_showInBoth() =
        runDesktopComposeUiTest(840, 1400) {
            val state = DemoAppState()
            // With the handle at the start edge the dark copy shows everywhere, so it takes the clicks.
            val split = SplitState(0f)
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Chrome(Skin(Library.Unstyled, expressive = false)) {
                        // Frame and window both tall enough that the orders and their menu take clicks.
                        SplitPreview(LightSpec, DarkSpec, split, Modifier.size(840.dp, 1400.dp)) { spec ->
                            UnstyledAppEntry(spec, state, DeviceWidth.Tablet)
                        }
                    }
                }
            }
            val failedPicks = onAllNodes(menuItem(OrderFilter.Failed), useUnmergedTree = true)
            failedPicks.assertCountEquals(0)

            onNode(StatusButton).performClick()
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe true
            failedPicks.assertCountEquals(2)

            failedPicks[0].performClick()
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe false
            state.choice(DashboardFilterChoice, OrderFilter.entries.size) shouldBe OrderFilter.Failed.ordinal
            failedPicks.assertCountEquals(0)
            onAllNodesWithText("Ines Duarte", useUnmergedTree = true).assertCountEquals(2)
            onAllNodesWithText("Amara Okafor", useUnmergedTree = true).assertCountEquals(0)

            // A second press on the button puts the menu away without a pick.
            onNode(StatusButton).performClick()
            waitForIdle()
            failedPicks.assertCountEquals(2)
            onNode(StatusButton).performClick()
            waitForIdle()
            state.isOn(DashboardMenuSwitch) shouldBe false
            failedPicks.assertCountEquals(0)

            val swatchName = onAllNodesWithText(DashboardToken.SurfaceContainerLow.token.name, useUnmergedTree = true)
            swatchName.assertCountEquals(0)
            onNode(hasClickAction() and hasContentDescription(DashboardCopy.ShowTokens)).performClick()
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe true
            swatchName.assertCountEquals(2)

            onNode(hasClickAction() and hasContentDescription(DashboardCopy.CloseTokens)).performClick()
            waitForIdle()
            state.isOn(DashboardDrawerSwitch) shouldBe false
            swatchName.assertCountEquals(0)
        }

    @Test
    fun tooltips_showOnHoverAndOnKeyboardFocus() =
        runComposeUiTest {
            setContent {
                DashboardHarness(LightSpec, DemoAppState(), DeviceWidth.Tablet, Modifier.size(840.dp, 900.dp))
            }
            val destination = DashboardDestination.Customers.label
            val railItem = onNode(hasClickAction() and hasContentDescription(destination))
            onAllNodesWithText(destination).assertCountEquals(0)

            railItem.performMouseInput { enter(center) }
            waitForIdle()
            onNodeWithText(destination).assertExists()
            railItem.performMouseInput { exit() }
            waitForIdle()
            onAllNodesWithText(destination).assertCountEquals(0)

            onAllNodesWithText(DashboardCopy.ShowTokens).assertCountEquals(0)
            onNode(hasClickAction() and hasContentDescription(DashboardCopy.ShowTokens)).requestFocus()
            waitForIdle()
            onNodeWithText(DashboardCopy.ShowTokens).assertExists()
        }

    @Test
    fun inspect_clickOnASidebarItem_pinsItsTokensAndNeverReachesTheApp() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    Inspecting(
                        shown = PreviewMode.Light,
                        split = remember { SplitState() },
                        skin = Skin(Library.Unstyled, expressive = false),
                    ) {
                        PreviewPane(LightSpec, Modifier.fillMaxSize()) {
                            UnstyledAppEntry(LightSpec, state, DeviceWidth.Desktop)
                        }
                    }
                }
            }
            waitForIdle()
            val before = state.dashboardSnapshot()

            onNode(hasClickAction() and hasText(DashboardDestination.Customers.label)).performClick()
            waitForIdle()
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
            onNode(OnCard and hasText(DashboardToken.SurfaceContainerLow.token.name)).assertExists()
            onNode(OnCard and hasText(DashboardToken.OnSurfaceVariant.token.name)).assertExists()
            state.dashboardSnapshot() shouldBe before

            // Press the card where a control of the dashboard lies under it.
            val card = onNodeWithTag(INSPECT_CARD_TAG).fetchSemanticsNode().boundsInRoot
            val under = onAllNodes(hasClickAction() and SemanticsMatcher.keyIsDefined(PreviewRoles))
                .fetchSemanticsNodes()
                .map { node -> node.boundsInRoot }
                .filter { bounds -> bounds.overlaps(card) }
            under.shouldNotBeEmpty()
            val press = card.intersect(under.first()).center
            onRoot().performTouchInput { click(press) }
            waitForIdle()
            state.dashboardSnapshot() shouldBe before
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
        }

    @Test
    fun dashboardSources_openNoWindowPortalOrFieldAndNeverLoop() {
        val sources = File(DashboardSourceDir)
            .listFiles()
            .orEmpty()
            .filter { file -> file.name.startsWith("Dashboard") || file.name in DashboardSideSources }
        sources.size shouldBeGreaterThanOrEqual 5
        for (source in sources) {
            withClue(source.name) {
                val lines = source.readLines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported -> imported.isBannedOnTheDashboard() }
                    .shouldBeEmpty()
                lines
                    .filter { line -> DashboardEndlessMotion.any { stem -> stem in line } }
                    .shouldBeEmpty()
            }
        }
    }
}

/** Every panel open, so the menu, the token panel and a phone's navigation are on screen too. */
private fun DemoAppState.openEverything() {
    setOn(DashboardDrawerSwitch, true)
    setOn(DashboardMenuSwitch, true)
    setOn(DashboardNavSwitch, true)
}

/** Everything the dashboard keeps in [DemoAppState], to tell whether anything changed. */
private fun DemoAppState.dashboardSnapshot(): List<Any> {
    val picks = listOf(
        DashboardNavChoice to DashboardDestination.entries.size,
        DashboardRangeChoice to DashboardRange.entries.size,
        DashboardFilterChoice to OrderFilter.entries.size,
    ).flatMap { (group, count) -> (0 until count).map { option -> isOn("$group.$option") } }
    val panels = listOf(DashboardDrawerSwitch, DashboardMenuSwitch, DashboardNavSwitch).map { switch -> isOn(switch) }
    return picks + panels + listOf(selectedItem, tabIndex, text)
}

private fun String.isBannedOnTheDashboard(): Boolean {
    val name = substringAfterLast('.')
    return startsWith("androidx.compose.ui.window.") ||
        "TextField" in name ||
        (startsWith("com.composeunstyled.") && DashboardBannedUnstyled.any { word -> word in name }) ||
        (startsWith("com.materialkolor.builder.kit.") && !startsWith("com.materialkolor.builder.kit.motion."))
}

/** The dashboard for [width] with every panel open, in a frame tall enough that every lazy item composes. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tallDashboard(
    width: DeviceWidth,
    frame: IntSize,
) {
    val state = DemoAppState()
    state.openEverything()
    setContent {
        DashboardHarness(
            spec = LightSpec,
            state = state,
            width = width,
            // Wider than the window on desktop, and tall enough for the whole page.
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(frame.width.dp, 2400.dp),
        )
    }
}

/** The dashboard in an Unstyled pane of [spec], under the Unstyled chrome, with motion frozen. */
@Composable
private fun DashboardHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(Skin(Library.Unstyled, expressive = false)) {
            PreviewPane(spec, modifier) { UnstyledAppEntry(spec, state, width) }
        }
    }
}
