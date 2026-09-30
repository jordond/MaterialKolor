package com.materialkolor.builder.preview.inklet

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DeviceFrames
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.AppTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.pressEveryControl
import com.materialkolor.builder.preview.tripsWhole
import io.kotest.assertions.withClue
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class InkletTripsTest {
    /**
     * The D40 guard. With the builder's motion frozen Trips settles at every device width, and a
     * click on every control opens no popup or window.
     */
    @Test
    fun trips_motionFrozen_settlesAndOpensNoPopupOrWindow() {
        for ((width, frame) in DeviceFrames) {
            withClue(width) {
                runComposeUiTest {
                    val spec = LightSpec.on(Library.Inklet)
                    val state = DemoAppState()
                    setContent {
                        CompositionLocalProvider(LocalMotionFrozen provides true) {
                            Chrome {
                                PreviewPane(spec, tripsWhole(frame)) { AppTab(spec, state, width) }
                            }
                        }
                    }
                    waitForIdle()

                    pressEveryControl()
                }
            }
        }
    }
}
