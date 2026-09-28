package com.materialkolor.builder.kit.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LoopPhaseTest {
    @Test
    fun loopPhase_whenMotionIsFrozen_staysOnTheFrozenPhase() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var phase: State<Float>? = null
            setContent {
                CompositionLocalProvider(LocalMotionFrozen provides true) {
                    phase = rememberLoopPhase(periodMillis = 1_000, frozenPhase = 0.25f)
                }
            }

            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(5_000)

            assertNotNull(phase).value shouldBe 0.25f
        }

    @Test
    fun loopPhase_whenTheTabIsHidden_holdsWhereItWas() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var phase: State<Float>? = null
            setContent {
                CompositionLocalProvider(LocalTabVisible provides false) {
                    phase = rememberLoopPhase(periodMillis = 1_000)
                }
            }

            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(5_000)

            assertNotNull(phase).value shouldBe 0f
        }

    @Test
    fun loopPhase_fromAClockAtZero_walksFromTheFirstFrames() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var phase: State<Float>? = null
            setContent {
                phase = rememberLoopPhase(periodMillis = 1_000, frozenPhase = 0.7f)
            }

            // The clock has run for less than the 700 ms the loop picks up from.
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(100)
            val walked = assertNotNull(phase).value

            assertTrue(walked > 0.75f, "expected the phase to walk on from 0.7, got $walked")
            assertTrue(walked < 0.9f, "expected the phase to walk on from 0.7, got $walked")
        }

    @Test
    fun loopPhase_whileRunning_walksWithTheClock() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var phase: State<Float>? = null
            setContent {
                phase = rememberLoopPhase(periodMillis = 1_000)
            }

            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(500)
            val walked = assertNotNull(phase).value

            assertTrue(walked > 0.4f, "expected the phase to be about half way, got $walked")
            assertTrue(walked < 0.6f, "expected the phase to be about half way, got $walked")
        }
}
