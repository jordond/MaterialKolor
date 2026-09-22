package com.materialkolor.ktx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

@OptIn(ExperimentalTestApi::class)
class RememberTonalPaletteTest {
    @Test
    fun rememberTonalPalette_keepsTheSameInstance_whenNothingChanges() =
        runComposeUiTest {
            var seed by mutableStateOf(Color(0xFF6750A4))
            var tick by mutableStateOf(0)
            val compositions = mutableListOf<Int>()
            val instances = mutableListOf<TonalPalette>()

            setContent {
                compositions += tick
                val palette = rememberTonalPalette(seed = seed)
                if (instances.lastOrNull() !== palette) instances += palette
            }

            tick = 1
            waitForIdle()

            assertEquals(2, compositions.size, "expected the unrelated state change to recompose")
            assertEquals(1, instances.size)

            seed = Color(0xFFB3261E)
            waitForIdle()

            assertEquals(2, instances.size)
            assertNotSame(instances[0], instances[1])
        }

    @Test
    fun rememberTonalPalette_recomputes_whenHarmonizeWithChanges() =
        runComposeUiTest {
            var harmonizeWith by mutableStateOf<Color?>(null)
            var result: TonalPalette? = null

            setContent {
                result = rememberTonalPalette(seed = Color(0xFF00A3FF), harmonizeWith = harmonizeWith)
            }

            val plain = assertNotNull(result).toneColor(50)

            harmonizeWith = Color(0xFFB3261E)
            waitForIdle()

            val harmonized = assertNotNull(result).toneColor(50)
            assertNotEquals(plain, harmonized)
        }

    @Test
    fun rememberTonalPalette_leavesTheSeedAlone_whenHarmonizeWithIsNull() =
        runComposeUiTest {
            val seed = Color(0xFF00A3FF)
            var result: TonalPalette? = null

            setContent {
                result = rememberTonalPalette(seed = seed, harmonizeWith = null)
            }

            val expected = TonalPalette.from(seed)
            assertEquals(expected.toneColor(40), assertNotNull(result).toneColor(40))
        }

    @Test
    fun rememberTonalPalette_matchesTheHarmonizedSeed() =
        runComposeUiTest {
            val seed = Color(0xFF00A3FF)
            val toward = Color(0xFFB3261E)
            var result: TonalPalette? = null

            setContent {
                result = rememberTonalPalette(seed = seed, harmonizeWith = toward)
            }

            val expected = TonalPalette.from(seed.harmonize(toward))
            assertEquals(expected.toneColor(40), assertNotNull(result).toneColor(40))
        }
}
