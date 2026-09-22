package com.materialkolor.sample.customtheme.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class AppThemeTest {
    @Test
    fun rememberAppColors_matchesTheNonComposablePath() =
        runComposeUiTest {
            var result: AppColors? = null

            setContent {
                result = rememberAppColors(seed = Seed, isDark = false)
            }

            assertEquals(appColors(seed = Seed, isDark = false), assertNotNull(result))
        }

    @Test
    fun rememberAppColors_keepsTheSameRecord_untilAnInputMoves() =
        runComposeUiTest {
            var isDark by mutableStateOf(false)
            var tick by mutableStateOf(0)
            val compositions = mutableListOf<Int>()
            val observed = mutableListOf<AppColors>()

            setContent {
                compositions += tick
                val colors = rememberAppColors(seed = Seed, isDark = isDark)
                if (observed.lastOrNull() !== colors) observed += colors
            }

            tick = 1
            waitForIdle()
            assertEquals(2, compositions.size, "expected the unrelated state change to recompose")
            assertEquals(1, observed.size)

            isDark = true
            waitForIdle()
            assertEquals(2, observed.size)
            assertEquals(false, observed[1].isLight)
        }

    @Test
    fun appTheme_providesTheColorsToItsContent() =
        runComposeUiTest {
            var provided: AppColors? = null

            setContent {
                AppTheme(seed = Seed, mode = AppThemeMode.Dark) {
                    provided = LocalAppColors.current
                }
            }

            assertEquals(appColors(seed = Seed, isDark = true), assertNotNull(provided))
        }

    private companion object {
        val Seed = Color(0xFF6750A4)
    }
}
