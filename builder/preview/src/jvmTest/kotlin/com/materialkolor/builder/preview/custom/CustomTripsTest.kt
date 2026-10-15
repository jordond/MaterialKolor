package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
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
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DeviceFrames
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.TripsHarness
import com.materialkolor.builder.preview.TripsNaming
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.checkTripsControlsDeclareRoles
import com.materialkolor.builder.preview.inspect.PreviewRoles
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.opensAWindow
import com.materialkolor.builder.preview.refsOnScreen
import com.materialkolor.builder.preview.screenColors
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.trips.Trips
import com.materialkolor.builder.preview.walkTripsControls
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The Trips app in a Custom pane, under the red chrome, with motion frozen.
 */
private val CustomTrips: TripsHarness = { spec, state, width, modifier ->
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome {
            ProvideBuilderLayout(modifier = modifier) {
                PreviewPane(spec, Modifier.fillMaxSize()) { CustomAppEntry(spec, state, width) }
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
class CustomTripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnSlots() =
        checkTripsControlsDeclareRoles(CustomTrips, LightSpec) { node -> node.tripsDeclaresSlots() }

    @Test
    fun slots_everyDeviceWidthFirstScreen_showWhatF20AsksWithOrWithoutAccents() {
        for ((width, frame) in DeviceFrames) {
            for (spec in listOf(LightSpec, AccentLightSpec)) {
                withClue("$width ${spec.accentCount} accents") {
                    runDesktopComposeUiTest(frame.width, frame.height) {
                        setContent { CustomTrips(spec, DemoAppState(), width, Modifier.fillMaxSize()) }

                        val refs = refsOnScreen()
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
                    setContent { CustomTrips(spec, DemoAppState(), DeviceWidth.Desktop, Modifier.fillMaxSize()) }

                    val accents = refsOnScreen().filterIsInstance<ColorRef.OfAccent>().map { ref -> ref.slot }
                    accents.map { slot -> slot.index }.toSet() shouldBe Trips.indices.toSet()
                    val pixels = screenColors()
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
            walkTripsControls(CustomTrips, LightSpec, TripsNaming.Text, height = 900, checklist = hasClickAction())
        }

    @Test
    fun tripsSources_importNothingThatOpensAPopupOrLoops() {
        val sources = TripsSources
        sources.size shouldBeGreaterThanOrEqual 4
        checkSourcesOpenNothingAndNeverLoop(sources) { imported ->
            imported in TripsPopupImports || imported.opensAWindow()
        }
    }
}

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
 * The sources the app is drawn from, its entry, its colors and every CustomTrip file.
 */
private val TripsSources: List<File>
    get() {
        val folder = moduleSource("commonMain/kotlin/com/materialkolor/builder/preview/custom")
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
