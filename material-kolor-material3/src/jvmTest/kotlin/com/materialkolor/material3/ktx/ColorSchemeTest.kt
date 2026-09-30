package com.materialkolor.material3.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.material3.fields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class ColorSchemeTest {
    private val lightScheme = dynamicColorScheme(seedColor = Color(0xFF6750A4), isDark = false)

    private val darkScheme = dynamicColorScheme(seedColor = Color(0xFFB3261E), isDark = true)

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

            val midFlight = requireNotNull(result)
            assertNotEquals(lightScheme.primary, midFlight.primary)
            assertNotEquals(darkScheme.primary, midFlight.primary)
            assertNotEquals(lightScheme.surface, midFlight.surface)
            assertNotEquals(darkScheme.surface, midFlight.surface)

            mainClock.autoAdvance = true
            waitForIdle()
            assertEquals(darkScheme.fields(), requireNotNull(result).fields())
        }
}
