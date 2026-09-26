package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.trips.PackingItem
import com.materialkolor.builder.preview.trips.TripFilter
import com.materialkolor.builder.preview.trips.Trips
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnSlots() {
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

                    // Unmerged, since a merged node also carries the slots its children declared.
                    val controls = onAllNodes(hasClickAction() or hasSetTextAction(), useUnmergedTree = true)
                        .fetchSemanticsNodes()
                    controls.shouldNotBeEmpty()
                    controls
                        .filterNot { node -> node.tripsDeclaresSlots() }
                        .map { node -> node.config.toString() }
                        .shouldBeEmpty()
                }
            }
        }
    }

    @Test
    fun slots_everyDeviceWidthFirstScreen_showWhatF20AsksWithOrWithoutAccents() {
        for ((width, frame) in TripsFrames) {
            for (spec in listOf(LightSpec, AccentLightSpec)) {
                withClue("$width ${spec.accentCount} accents") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        setContent { TripsHarness(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                        val refs = tripsRefsOnScreen()
                        val slots = refs.filterIsInstance<ColorRef.OfSlot>().map { ref -> ref.slot }.toSet()
                        TripsFamilies
                            .filterValues { family -> family.none { slot -> slot in slots } }
                            .keys
                            .shouldBeEmpty()
                        (TripsSurfaces intersect slots).size shouldBeGreaterThanOrEqual 3
                        (TripsBorders intersect slots).size shouldBeGreaterThanOrEqual 1
                        if (spec.accentCount == 0) {
                            refs.filterIsInstance<ColorRef.OfAccent>().shouldBeEmpty()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun accents_eightInTheDocument_eachTripThumbPaintsItsOwnInTheColorItDeclares() {
        for (spec in listOf(AccentLightSpec, AccentDarkSpec)) {
            withClue(spec.label) {
                runDesktopComposeUiTest(1280, 800) {
                    setContent { TripsHarness(spec, DemoAppState(), DeviceWidth.Desktop, Modifier.fillMaxSize()) }

                    val accents = tripsRefsOnScreen().filterIsInstance<ColorRef.OfAccent>().map { ref -> ref.slot }
                    accents.map { slot -> slot.index }.toSet() shouldBe Trips.indices.toSet()
                    val pixels = onRoot().captureToImage().toPixelMap().let { map ->
                        buildSet { for (x in 0 until map.width) for (y in 0 until map.height) add(map[x, y].toArgb()) }
                    }
                    // The fills, since text and glyph edges blend into what they sit on.
                    for (slot in accents.filter { slot -> slot.part == AccentPart.Container }.toSet()) {
                        withClue(slot) {
                            pixels shouldContain spec.result.accents[slot, spec.isDark]
                                .toColor()
                                .toArgb()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun controls_clicked_openTheTripFilterTheListTickTheChecklistAndTurnOfflineMapsOn() =
        runComposeUiTest {
            val state = DemoAppState()
            setContent { TripsHarness(LightSpec, state, DeviceWidth.Tablet, Modifier.size(840.dp, 900.dp)) }

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

            onNode(hasClickAction() and hasText(PackingItem.Passports.label))
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isChecked(PackingItem.Passports.key) shouldBe true
            onNodeWithText("1 of 3").assertExists()

            onNode(hasClickAction() and hasText("Turn on")).performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            state.isOn(OfflineMapsSwitch) shouldBe true
            onAllNodesWithText("Turn on").assertCountEquals(0)
        }

    @Test
    fun tripsSources_importNothingThatOpensAPopupOrLoops() {
        val sources = TripsSources
        sources.size shouldBeGreaterThanOrEqual 4
        for (source in sources) {
            withClue(source.name) {
                val lines = source.readLines().map { line -> line.trim() }
                lines
                    .filter { line -> line.startsWith("import ") }
                    .map { line -> line.removePrefix("import ").substringBefore(" as ") }
                    .filter { imported ->
                        imported in TripsPopupImports || imported.startsWith("androidx.compose.ui.window.")
                    }.shouldBeEmpty()
                lines.filter { line -> TripsEndlessMotion.containsMatchIn(line) }.shouldBeEmpty()
            }
        }
    }
}

/**
 * The frame the dock shows each device in, the kit's screen widths at the height of a first screen.
 */
private val TripsFrames: Map<DeviceWidth, IntSize> = mapOf(
    DeviceWidth.Phone to IntSize(412, 900),
    DeviceWidth.Tablet to IntSize(840, 900),
    DeviceWidth.Desktop to IntSize(1280, 800),
)

/**
 * The chrome the app sits in, the Custom skin coloured from the red chrome document.
 */
private val TripsSkin: Skin = Skin(SkinLibrary.Custom, expressive = false)

/**
 * The blue preview document with eight accents, twice as many as there are trips.
 */
private val AccentDocument: ThemeDocument = ThemeDocument(
    seed = Argb(0x1E88E5),
    accents = listOf(
        Accent("Love", Argb(0xE05263)),
        Accent("Cold", Argb(0x3E92CC)),
        Accent("Warm", Argb(0xF0A202)),
        Accent("Coffee", Argb(0x6F4E37)),
        Accent("Matcha", Argb(0x8DB600)),
        Accent("Iced", Argb(0x7EC8E3)),
        Accent("Tea", Argb(0xC9A227)),
        Accent("Chocolate", Argb(0x3F2212)),
    ),
)

private val AccentLightSpec: PaneSpec =
    PaneSpec(ThemeResolver().resolve(AccentDocument), isDark = false, label = "Light")

private val AccentDarkSpec: PaneSpec = PaneSpec(ThemeResolver().resolve(AccentDocument), isDark = true, label = "Dark")

/**
 * How many accents the pane's document has.
 */
private val PaneSpec.accentCount: Int
    get() = result.accents.families.size

/**
 * The four families every first screen has to use, any slot of each.
 */
private val TripsFamilies: Map<String, Set<CustomSlot>> = mapOf(
    "primary" to setOf(
        CustomSlot.Primary,
        CustomSlot.OnPrimary,
        CustomSlot.PrimaryContainer,
        CustomSlot.OnPrimaryContainer,
        CustomSlot.PrimaryPressed,
        CustomSlot.PrimaryRaised,
    ),
    "secondary" to setOf(
        CustomSlot.Secondary,
        CustomSlot.OnSecondary,
        CustomSlot.SecondaryContainer,
        CustomSlot.OnSecondaryContainer,
    ),
    "tertiary" to setOf(
        CustomSlot.Tertiary,
        CustomSlot.OnTertiary,
        CustomSlot.TertiaryContainer,
        CustomSlot.OnTertiaryContainer,
    ),
    "error" to setOf(CustomSlot.Error, CustomSlot.OnError, CustomSlot.ErrorContainer, CustomSlot.OnErrorContainer),
)

/**
 * The surface levels of the Custom slots, three of which every first screen has to use, as the
 * Material 3 app has to use four of its five container levels.
 */
private val TripsSurfaces: Set<CustomSlot> =
    setOf(CustomSlot.Surface, CustomSlot.SurfaceRaised, CustomSlot.SurfaceSunken, CustomSlot.SurfaceInverse)

/**
 * The border slots, one of which every first screen has to use.
 */
private val TripsBorders: Set<CustomSlot> =
    setOf(CustomSlot.BorderFaint, CustomSlot.BorderSoft, CustomSlot.BorderStrong)

/**
 * Imports that open a popup, a window or an overlay, which on the web take the mirror over.
 */
private val TripsPopupImports: List<String> = listOf(
    "com.materialkolor.builder.kit.control.BuilderMenu",
    "com.materialkolor.builder.kit.control.BuilderTooltip",
    "com.materialkolor.builder.kit.control.BuilderDialog",
    "com.materialkolor.builder.kit.control.BuilderSheet",
    "com.materialkolor.builder.kit.control.BuilderSidePanel",
    "com.materialkolor.builder.kit.control.BuilderBottomSheet",
    "com.materialkolor.builder.kit.control.BuilderToastHost",
    "com.materialkolor.builder.kit.control.BuilderSelect",
    "com.materialkolor.builder.kit.control.BuilderPopover",
)

/**
 * A call that loops for ever, an infinite transition or an infinite repeat. The names are
 * split by a wildcard so the builder's architecture scan does not read this pattern as a call.
 */
private val TripsEndlessMotion: Regex = Regex("""\b(rememberInfinite\w*Transition|infinite\w*Repeatable)\b""")

/**
 * The sources the app is drawn from, its entry, its colors and every CustomTrip file.
 */
private val TripsSources: List<File>
    get() {
        val folder = File("src/commonMain/kotlin/com/materialkolor/builder/preview/custom")
        val trips = folder.listFiles().orEmpty().filter { file -> file.name.startsWith("CustomTrip") }
        return trips.sortedBy { file -> file.name } + File(folder, "AppEntry.kt") + File(folder, "CustomColors.kt")
    }

/**
 * The slot groups a kit control declares once on its outer node for the nodes inside it, the
 * options of a chip group and the editable text of a field.
 */
private val TripsGroupRefs: Set<List<ColorRef>> = setOf(CustomComponent.Chip.refs, CustomComponent.TextField.refs)

/**
 * Whether the node declares its slots itself, or sits inside a kit control that declares them once
 * for every node it holds.
 */
private fun SemanticsNode.tripsDeclaresSlots(): Boolean {
    if (PreviewRoles in config) return true
    val holder = generateSequence(parent) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null && holder.config[PreviewRoles] in TripsGroupRefs
}

/**
 * Every slot and accent declared by a node that shows at least partly on screen.
 */
private fun SemanticsNodeInteractionsProvider.tripsRefsOnScreen(): List<ColorRef> {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodes(SemanticsMatcher.keyIsDefined(PreviewRoles), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> node.boundsInRoot.overlaps(screen) }
        .flatMap { node -> node.config[PreviewRoles] }
}

/**
 * The Trips app in a Custom pane of [spec], under a red Custom chrome, with motion frozen.
 */
@Composable
private fun TripsHarness(
    spec: PaneSpec,
    state: DemoAppState,
    width: DeviceWidth,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(TripsSkin) {
            ProvideBuilderLayout(modifier = modifier) {
                PreviewPane(spec, Modifier.fillMaxSize()) { CustomAppEntry(spec, state, width) }
            }
        }
    }
}
