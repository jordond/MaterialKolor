package com.materialkolor.fluent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.materialkolor.ktx.DynamicScheme
import io.github.composefluent.Colors
import io.github.composefluent.Shades
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AnimateFluentColorsTest {
    private val seed = Color(0xFF4285F4)

    private val otherSeed = Color(0xFFB3261E)

    @Test
    fun animateFluentColors_isAPassThroughWhileNothingChanges() =
        runComposeUiTest {
            var latest: Colors? = null

            setContent {
                latest = animateFluentColors(rememberFluentColors(seedColor = seed, isDark = false))
            }

            waitForIdle()
            assertEquals(shadesFor(seed), latest?.shades)
        }

    @Test
    fun animateFluentColors_settlesOnTheNewRamp() =
        runComposeUiTest {
            var currentSeed by mutableStateOf(seed)
            var latest: Colors? = null

            setContent {
                latest = animateFluentColors(rememberFluentColors(seedColor = currentSeed, isDark = false))
            }

            waitForIdle()
            currentSeed = otherSeed
            waitForIdle()

            assertEquals(shadesFor(otherSeed), latest?.shades)
        }

    @Test
    fun animateFluentColors_movesThroughTheMiddleRatherThanCutting() =
        runComposeUiTest {
            var currentSeed by mutableStateOf(seed)
            var latest: Colors? = null

            setContent {
                latest = animateFluentColors(rememberFluentColors(seedColor = currentSeed, isDark = false))
            }

            waitForIdle()
            mainClock.autoAdvance = false
            currentSeed = otherSeed

            // One frame to let the composition pick up the new seed and retarget the transition,
            // then enough time for the spring to have moved without arriving. This is the assertion
            // that fails if animateFluentColors ever becomes a pass-through.
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(80)

            val midFlight = latest?.shades
            assertNotEquals(shadesFor(seed), midFlight)
            assertNotEquals(shadesFor(otherSeed), midFlight)

            mainClock.autoAdvance = true
            waitForIdle()
            assertEquals(shadesFor(otherSeed), latest?.shades)
        }

    @Test
    fun animateFluentColors_keepsTheDarkFlagAndTheConstantGroups() =
        runComposeUiTest {
            var latest: Colors? = null

            setContent {
                latest = animateFluentColors(rememberFluentColors(seedColor = seed, isDark = true))
            }

            waitForIdle()
            val colors = requireNotNull(latest)

            assertTrue(colors.darkMode)
            // system is built from Fluent's own constants, so it has to match an unanimated Colors.
            assertEquals(Colors(shadesFor(seed), darkMode = true).system, colors.system)
        }

    private fun shadesFor(seedColor: Color): Shades =
        DynamicScheme(seedColor = seedColor, isDark = false).primaryPalette.toFluentShades()
}
