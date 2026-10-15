package com.materialkolor.builder.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.TripsDestination
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * The Trips app of one library in a pane of `spec`, drawn at `width` into `modifier`, with motion
 * frozen.
 */
internal typealias TripsHarness =
    @Composable (spec: PaneSpec, state: DemoAppState, width: DeviceWidth, modifier: Modifier) -> Unit

/**
 * The frame the dock shows each device in, the kit's screen widths at the height of a first screen.
 */
internal val DeviceFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/**
 * As wide as [frame], and tall enough that every lazy item composes, wider than the window on
 * desktop.
 */
internal fun tripsWhole(frame: IntSize): Modifier =
    Modifier
        .wrapContentSize(Alignment.TopStart, unbounded = true)
        .requiredSize(frame.width.dp, 2400.dp)

/**
 * How a library's Trips names its rows, its filters and its checklist items.
 */
internal enum class TripsNaming {
    /**
     * By their text, so an open trip's name shows twice, on its row and as the title.
     */
    Text,

    /**
     * By a content description, with the words a row draws under its layer, so an open trip's
     * name shows as text once, as the title.
     */
    Description,
    ;

    /**
     * A node this naming calls [label].
     */
    fun named(label: String): SemanticsMatcher =
        when (this) {
            Text -> hasText(label)
            Description -> hasContentDescription(label)
        }

    /**
     * How many nodes show an open trip's name as text.
     */
    val openTripTexts: Int
        get() =
            when (this) {
                Text -> 2
                Description -> 1
            }
}

/**
 * The four families every screen of a role based library has to use.
 */
private val TripsFamilies: Map<String, Set<Role>> = mapOf(
    "primary" to setOf(Role.Primary, Role.OnPrimary, Role.PrimaryContainer, Role.OnPrimaryContainer),
    "secondary" to setOf(Role.Secondary, Role.OnSecondary, Role.SecondaryContainer, Role.OnSecondaryContainer),
    "tertiary" to setOf(Role.Tertiary, Role.OnTertiary, Role.TertiaryContainer, Role.OnTertiaryContainer),
    "error" to setOf(Role.Error, Role.OnError, Role.ErrorContainer, Role.OnErrorContainer),
)

/**
 * The surface container levels, four of which every screen of a role based library has to use.
 */
private val TripsContainerLevels: Set<Role> = setOf(
    Role.SurfaceContainerLowest,
    Role.SurfaceContainerLow,
    Role.SurfaceContainer,
    Role.SurfaceContainerHigh,
    Role.SurfaceContainerHighest,
)

/**
 * Every role, slot and accent declared by a node that shows at least partly on screen.
 */
internal fun SemanticsNodeInteractionsProvider.refsOnScreen(): List<ColorRef> {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> node.boundsInRoot.overlaps(screen) }
        .flatMap { node -> node.config[PreviewRoles] }
}

/**
 * Checks that at every device width every control of [harness]'s Trips in [spec] declares its
 * roles, as [declares] reads them, leaving out the nodes [ignored] matches.
 */
@OptIn(ExperimentalTestApi::class)
internal fun checkTripsControlsDeclareRoles(
    harness: TripsHarness,
    spec: PaneSpec,
    ignored: SemanticsMatcher? = null,
    declares: (SemanticsNode) -> Boolean = { node -> PreviewRoles in node.config },
) {
    for ((width, frame) in DeviceFrames) {
        withClue(width) {
            runComposeUiTest {
                setContent { harness(spec, DemoAppState(), width, tripsWhole(frame)) }

                onNode(hasText("Turn on"), useUnmergedTree = true).assertExists()
                // Unmerged, since a merged node also carries the roles its children declared.
                val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .filterNot { node -> ignored?.matches(node) == true }
                controls.shouldNotBeEmpty()
                controls
                    .filterNot { node -> declares(node) }
                    .map { node -> "${node.boundsInRoot} ${node.config}" }
                    .shouldBeEmpty()
            }
        }
    }
}

/**
 * Checks that the first screen of [harness]'s Trips in [spec] shows every family F20 asks for, four
 * container levels and the outline, at every device width.
 */
@OptIn(ExperimentalTestApi::class)
internal fun checkTripsFirstScreenShowsTheScheme(
    harness: TripsHarness,
    spec: PaneSpec,
) {
    for ((width, frame) in DeviceFrames) {
        withClue(width) {
            runDesktopComposeUiTest(frame.width, frame.height) {
                setContent { harness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                val used = refsOnScreen()
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

/**
 * Checks that at every device width, in [light] and then [dark], [harness]'s Trips opens the trip
 * below its row with no rail on a phone, and beside its row with a rail on anything wider.
 */
@OptIn(ExperimentalTestApi::class)
internal fun checkTripsLayOutForEveryWidth(
    harness: TripsHarness,
    naming: TripsNaming,
    light: PaneSpec,
    dark: PaneSpec,
) {
    for ((width, frame) in DeviceFrames) {
        withClue(width) {
            runDesktopComposeUiTest(frame.width, frame.height) {
                var spec by mutableStateOf(light)
                setContent { harness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                for (mode in listOf(light, dark)) {
                    spec = mode
                    waitForIdle()
                    val (row, title) = when (naming) {
                        TripsNaming.Text -> {
                            val (row, title) = onAllNodesWithText("Lisbon, Portugal")
                                .fetchSemanticsNodes()
                                .map { node -> node.boundsInRoot }
                            row to title
                        }
                        TripsNaming.Description -> {
                            val row = onNode(hasClickAction() and naming.named("Lisbon, Portugal"))
                            val title = onNodeWithText("Lisbon, Portugal")
                            row.fetchSemanticsNode().boundsInRoot to title.fetchSemanticsNode().boundsInRoot
                        }
                    }
                    val rail = onAllNodes(naming.named(TripsDestination.Explore.label))
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

/**
 * Sets [harness]'s Trips on a tablet [height] dp tall, then opens Kyoto, filters the list to the
 * shared and the past trips, ticks the passports through [checklist] and turns offline maps on,
 * checking the state and the screen after each. Returns the state, for a library's own checks.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.walkTripsControls(
    harness: TripsHarness,
    spec: PaneSpec,
    naming: TripsNaming,
    height: Int = 760,
    checklist: SemanticsMatcher = isToggleable(),
): DemoAppState {
    val state = DemoAppState()
    setContent { harness(spec, state, DeviceWidth.Tablet, Modifier.size(840.dp, height.dp)) }

    onNode(hasClickAction() and naming.named("Kyoto, Japan")).performClick()
    waitForIdle()
    state.selectedItem shouldBe 1
    onAllNodesWithText("Kyoto, Japan").assertCountEquals(naming.openTripTexts)
    onNodeWithText("Nothing planned yet").assertExists()

    onNode(hasClickAction() and naming.named(TripFilter.Shared.label)).performClick()
    waitForIdle()
    state.tabIndex shouldBe TripFilter.Shared.ordinal
    onAllNodes(hasClickAction() and naming.named("Kyoto, Japan")).assertCountEquals(0)
    onAllNodes(hasClickAction() and naming.named("Lisbon, Portugal")).assertCountEquals(1)

    onNode(hasClickAction() and naming.named(TripFilter.Past.label)).performClick()
    waitForIdle()
    onNodeWithText("No past trips yet").assertExists()

    onNode(checklist and naming.named(PackingItem.Passports.label))
        .performSemanticsAction(SemanticsActions.OnClick)
    waitForIdle()
    state.isChecked(PackingItem.Passports.key) shouldBe true
    onNodeWithText("1 of 3").assertExists()

    onNode(hasClickAction() and hasText("Turn on")).performSemanticsAction(SemanticsActions.OnClick)
    waitForIdle()
    state.isOn(OfflineMapsSwitch) shouldBe true
    onAllNodesWithText("Turn on").assertCountEquals(0)
    return state
}

/**
 * Everything Trips keeps in [DemoAppState], to tell whether anything changed.
 */
internal fun DemoAppState.tripsSnapshot(): List<Any> =
    listOf(selectedItem, tabIndex, text, isOn(OfflineMapsSwitch)) +
        PackingItem.entries.map { item -> isChecked(item.key) }
