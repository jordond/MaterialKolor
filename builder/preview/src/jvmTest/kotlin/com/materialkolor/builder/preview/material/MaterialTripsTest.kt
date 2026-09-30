package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.TripsHarness
import com.materialkolor.builder.preview.TripsNaming
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.checkTripsControlsDeclareRoles
import com.materialkolor.builder.preview.checkTripsFirstScreenShowsTheScheme
import com.materialkolor.builder.preview.checkTripsLayOutForEveryWidth
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import com.materialkolor.builder.preview.trips.OfflineMapsSwitch
import com.materialkolor.builder.preview.walkTripsControls
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The Trips app in a Material 3 pane, under the chrome, with motion frozen.
 */
private val MaterialTrips: TripsHarness = { spec, state, width, modifier ->
    CompositionLocalProvider(LocalMotionFrozen provides true) {
        Chrome {
            PreviewPane(spec, modifier) { MaterialAppEntry(spec, state, width) }
        }
    }
}

/**
 * The offline maps row as the merged tree shows it.
 */
private val TripsOfflineRow: SemanticsMatcher = isToggleable() and hasText("Offline maps")

@OptIn(ExperimentalTestApi::class)
class MaterialTripsTest {
    @Test
    fun controls_everyDeviceWidth_declareTheirOwnRoles() = checkTripsControlsDeclareRoles(MaterialTrips, LightSpec)

    @Test
    fun roles_everyDeviceWidthFirstScreen_showTheSchemeF20Asks() =
        checkTripsFirstScreenShowsTheScheme(MaterialTrips, LightSpec)

    @Test
    fun screens_everyDeviceWidthBothModes_layOutForTheWidthAndRender() =
        checkTripsLayOutForEveryWidth(MaterialTrips, TripsNaming.Text, LightSpec, DarkSpec)

    @Test
    fun controls_clicked_openTheTripFilterTheListAndTickTheChecklist() =
        runComposeUiTest {
            walkTripsControls(MaterialTrips, LightSpec, TripsNaming.Text)
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
                            MaterialAppEntry(spec, state, DeviceWidth.Tablet)
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
}
