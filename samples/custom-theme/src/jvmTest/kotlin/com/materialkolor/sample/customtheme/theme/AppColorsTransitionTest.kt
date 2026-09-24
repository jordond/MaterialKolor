package com.materialkolor.sample.customtheme.theme

import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class AppColorsTransitionTest {
    @Test
    fun animateAppColors_startsAtTheTarget() =
        runComposeUiTest {
            val light = appColors(seed = Seed, isDark = false)
            var shown: AppColors? = null

            setContent {
                shown = animateAppColors(light)
            }

            assertSame(light, assertNotNull(shown))
        }

    @Test
    fun animateAppColors_fadesToANewTarget() =
        runComposeUiTest {
            val light = appColors(seed = Seed, isDark = false)
            val dark = appColors(seed = Seed, isDark = true)
            var target by mutableStateOf(light)
            var shown: AppColors? = null

            mainClock.autoAdvance = false
            setContent {
                shown = animateAppColors(target, animationSpec = tween(durationMillis = FADE_MILLIS.toInt()))
            }

            target = dark
            mainClock.advanceTimeBy(FADE_MILLIS / 2)
            val midway = assertNotNull(shown)
            assertNotEquals(light.surface, midway.surface)
            assertNotEquals(dark.surface, midway.surface)

            mainClock.advanceTimeBy(FADE_MILLIS * 2)
            assertEquals(dark, shown)
        }

    @Test
    fun lerp_returnsTheRecordsThemselvesAtTheEnds() {
        val light = appColors(seed = Seed, isDark = false)
        val dark = appColors(seed = Seed, isDark = true)

        assertSame(light, lerp(start = light, stop = dark, fraction = 0f))
        assertSame(dark, lerp(start = light, stop = dark, fraction = 1f))
    }

    private companion object {
        val Seed = Color(0xFF6750A4)
        const val FADE_MILLIS = 300L
    }
}
