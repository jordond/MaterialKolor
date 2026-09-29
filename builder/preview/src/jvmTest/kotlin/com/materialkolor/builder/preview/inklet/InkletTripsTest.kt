package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.AppTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.on
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlin.test.Test

/**
 * The width the dock frames each device at.
 */
private val InkletFrameWidths: Map<DeviceWidth, Int> = mapOf(
    DeviceWidth.Phone to 412,
    DeviceWidth.Tablet to 840,
    DeviceWidth.Desktop to 1280,
)

@OptIn(ExperimentalTestApi::class)
class InkletTripsTest {
    /**
     * The D40 guard. With the builder's motion frozen Trips settles at every device width, and a
     * click on every control opens no popup or window.
     */
    @Test
    fun trips_motionFrozen_settlesAndOpensNoPopupOrWindow() {
        for ((width, frameWidth) in InkletFrameWidths) {
            withClue(width) {
                runComposeUiTest {
                    val spec = LightSpec.on(Library.Inklet)
                    val state = DemoAppState()
                    setContent {
                        CompositionLocalProvider(LocalMotionFrozen provides true) {
                            Chrome {
                                PreviewPane(
                                    spec = spec,
                                    // Tall enough that every lazy item composes.
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                                        .requiredSize(frameWidth.dp, 2400.dp),
                                ) { AppTab(spec, state, width) }
                            }
                        }
                    }
                    waitForIdle()

                    val pressable = onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
                    pressable.shouldNotBeEmpty()
                    runOnIdle {
                        for (node in pressable) {
                            if (SemanticsProperties.Disabled !in node.config) {
                                node.config
                                    .getOrNull(SemanticsActions.OnClick)
                                    ?.action
                                    ?.invoke()
                            }
                        }
                    }
                    waitForIdle()
                    onAllNodes(isRoot()).assertCountEquals(1)
                }
            }
        }
    }
}
