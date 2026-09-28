package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.Argb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyColorsTest {
    private val color = Argb(0xFFD9653B.toInt())

    @Test
    fun keyColors_fresh_leavesEveryPaletteOnTheSeed() {
        val colors = KeyColors()

        assertTrue(colors.isEmpty())
        KeyColor.entries.forEach { slot ->
            assertNull(colors[slot], "${slot.name} came back set")
        }
    }

    @Test
    fun keyColors_with_setsOnlyTheSlotItWasGiven() {
        KeyColor.entries.forEach { slot ->
            val colors = KeyColors().with(slot, color)

            assertEquals(color, colors[slot])
            assertEquals(1, KeyColor.entries.count { other -> colors[other] != null }, "${slot.name} spilled")
        }
    }

    @Test
    fun keyColors_withNull_putsThePaletteBackOnTheSeed() {
        val colors = KeyColors().with(KeyColor.Tertiary, color).with(KeyColor.Tertiary, null)

        assertTrue(colors.isEmpty())
    }
}
