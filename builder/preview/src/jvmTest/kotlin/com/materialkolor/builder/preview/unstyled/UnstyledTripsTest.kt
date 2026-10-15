package com.materialkolor.builder.preview.unstyled

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.PaneKitImports
import com.materialkolor.builder.preview.ShellExpressive
import com.materialkolor.builder.preview.TripsHarness
import com.materialkolor.builder.preview.TripsNaming
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.checkSourcesOpenNothingAndNeverLoop
import com.materialkolor.builder.preview.checkTripsControlsDeclareRoles
import com.materialkolor.builder.preview.checkTripsFirstScreenShowsTheScheme
import com.materialkolor.builder.preview.moduleSource
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.walkTripsControls
import kotlin.test.Test

/**
 * The Trips app in an Unstyled pane of the spec's document, under the shell chrome, with motion
 * frozen.
 */
private val UnstyledTrips: TripsHarness = { spec, state, width, modifier ->
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome(ShellExpressive) {
            val unstyled = remember(spec) { spec.on(Library.Unstyled) }
            PreviewPane(unstyled, modifier) { UnstyledAppEntry(unstyled, state, width) }
        }
    }
}

/**
 * The Trips app's sources, from the module the tests run in.
 */
private val TripsSources: List<String> = listOf(
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/AppEntry.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledTrips.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledTripDetail.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledParts.kt",
    "commonMain/kotlin/com/materialkolor/builder/preview/unstyled/UnstyledRoles.kt",
)

@OptIn(ExperimentalTestApi::class)
class UnstyledTripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() = checkTripsControlsDeclareRoles(UnstyledTrips, LightSpec)

    @Test
    fun roles_everyDeviceWidthFirstScreen_showTheSchemeF20Asks() =
        checkTripsFirstScreenShowsTheScheme(UnstyledTrips, LightSpec)

    @Test
    fun controls_clicked_openTheTripFilterTheListAndTickTheChecklist() =
        runComposeUiTest {
            walkTripsControls(UnstyledTrips, LightSpec, TripsNaming.Text)
        }

    @Test
    fun tripsSources_openNoPopupWindowOrPortalAndNeverLoop() =
        checkSourcesOpenNothingAndNeverLoop(TripsSources.map { path -> moduleSource(path) }) { imported ->
            imported.isBannedInUnstyled(PaneKitImports)
        }
}
