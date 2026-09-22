package com.materialkolor.sample.customtheme.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppColorsSymmetryTest {
    @Test
    fun lightAndDark_comeFromTheSameInputs() {
        for (seed in TestSeeds) {
            val light = appColors(seed = seed, isDark = false)
            val dark = appColors(seed = seed, isDark = true)

            assertTrue(light.isLight, "light theme should report isLight, seed=$seed")
            assertFalse(dark.isLight, "dark theme should not report isLight, seed=$seed")
            assertEquals(light.slots.keys, dark.slots.keys)
        }
    }

    @Test
    fun everySlotMoves_exceptTheOnesDeclaredModeIndependent() {
        for (seed in TestSeeds) {
            val light = appColors(seed = seed, isDark = false)
            val dark = appColors(seed = seed, isDark = true)

            val unchanged = light.slots
                .filterKeys { name -> light.slots.getValue(name) == dark.slots.getValue(name) }
                .keys

            assertEquals(
                expected = AppColors.ModeIndependentSlots,
                actual = unchanged,
                message = "seed=$seed",
            )
        }
    }
}
