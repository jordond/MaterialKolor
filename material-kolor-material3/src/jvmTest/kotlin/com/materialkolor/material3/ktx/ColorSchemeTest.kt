package com.materialkolor.material3.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.material3.fields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ColorSchemeTest {
    private val lightScheme = lightColorScheme()

    private val darkScheme = darkColorScheme()

    @Test
    fun animateColorScheme_isAPassThroughForEveryFieldWhileNothingChanges() =
        runComposeUiTest {
            var result: ColorScheme? = null

            setContent {
                result = animateColorScheme(colorScheme = lightScheme)
            }

            waitForIdle()
            assertEquals(lightScheme.fields(), requireNotNull(result).fields())
        }

    @Test
    fun animateColorScheme_movesThroughTheMiddleThenSettlesOnTheNewScheme() =
        runComposeUiTest {
            var target by mutableStateOf(lightScheme)
            var result: ColorScheme? = null

            setContent {
                result = animateColorScheme(colorScheme = target)
            }

            waitForIdle()
            mainClock.autoAdvance = false
            target = darkScheme

            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(80)

            val light = lightScheme.fields()
            val dark = darkScheme.fields()
            val midFlight = requireNotNull(result).fields()
            val moving = light.keys.filter { name -> light[name] != dark[name] }
            assertTrue(moving.size > 1)
            for (name in moving) {
                assertNotEquals(light[name], midFlight[name], "$name is still light mid-flight")
                assertNotEquals(dark[name], midFlight[name], "$name is already dark mid-flight")
            }

            mainClock.autoAdvance = true
            waitForIdle()
            assertEquals(darkScheme.fields(), requireNotNull(result).fields())
        }
}
