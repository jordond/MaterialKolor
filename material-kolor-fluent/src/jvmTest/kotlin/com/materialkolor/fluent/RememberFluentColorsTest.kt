package com.materialkolor.fluent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.materialkolor.ktx.DynamicScheme
import io.github.composefluent.Colors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class RememberFluentColorsTest {
    private val seed = Color(0xFF4285F4)

    private val otherSeed = Color(0xFFB3261E)

    @Test
    fun rememberFluentColors_keepsTheSameInstanceWhileNothingChanges() =
        runComposeUiTest {
            var unrelated by mutableStateOf(0)
            val seen = mutableListOf<Colors>()

            setContent {
                val colors = rememberFluentColors(seedColor = seed, isDark = false)
                // Read the unrelated state so this composable recomposes when it moves.
                unrelated.toString()
                seen += colors
            }

            waitForIdle()
            unrelated = 1
            waitForIdle()

            assertEquals(2, seen.size, "expected a second composition")
            assertSame(seen.first(), seen.last())
        }

    @Test
    fun rememberFluentColors_rebuildsWhenTheSeedChanges() =
        runComposeUiTest {
            var currentSeed by mutableStateOf(seed)
            val seen = mutableListOf<Colors>()

            setContent {
                seen += rememberFluentColors(seedColor = currentSeed, isDark = false)
            }

            waitForIdle()
            currentSeed = otherSeed
            waitForIdle()

            assertNotSame(seen.first(), seen.last())
            assertNotEquals(seen.first().shades, seen.last().shades)

            // The scheme's primary ramp, not the raw seed ramp. PaletteStyle reshapes chroma on the
            // way through, so TonalPalette.from(otherSeed) is a different ramp and deliberately so.
            val scheme = DynamicScheme(seedColor = otherSeed, isDark = false)
            assertEquals(scheme.primaryPalette.toFluentShades(), seen.last().shades)
        }

    @Test
    fun rememberFluentColors_rebuildsWhenTheModeChanges() =
        runComposeUiTest {
            var isDark by mutableStateOf(false)
            val seen = mutableListOf<Colors>()

            setContent {
                seen += rememberFluentColors(seedColor = seed, isDark = isDark)
            }

            waitForIdle()
            isDark = true
            waitForIdle()

            assertNotSame(seen.first(), seen.last())
            assertEquals(false, seen.first().darkMode)
            assertEquals(true, seen.last().darkMode)

            // Fluent derives light and dark from the same seven shades, so only the flag moves.
            assertEquals(seen.first().shades, seen.last().shades)
        }
}
