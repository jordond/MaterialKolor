package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AccentSlotTest {
    /**
     * The number that names each part of an accent family.
     *
     * Written out rather than read off the declaration order, the same way every coded enum is
     * pinned, so a part cannot quietly change what it means to the engine or the generator.
     */
    private val partCodes = mapOf(
        "Color" to 0,
        "OnColor" to 1,
        "Container" to 2,
        "OnContainer" to 3,
    )

    @Test
    fun accentPart_everyEntry_keepsTheCodeItWasGiven() {
        assertEquals(partCodes.keys.toList(), AccentPart.entries.map { part -> part.name })

        AccentPart.entries.forEach { part ->
            val expected = assertNotNull(partCodes[part.name], "${part.name} is missing from the table")
            assertEquals(expected, part.code, "${part.name} names another part of the family now")
        }
    }

    @Test
    fun accentSlot_negativeIndex_isRejected() {
        assertFailsWith<IllegalArgumentException> { AccentSlot(index = -1, part = AccentPart.Color) }
    }

    @Test
    fun accentSlot_firstAccent_isIndexZero() {
        val slot = AccentSlot(index = 0, part = AccentPart.OnContainer)

        assertEquals(0, slot.index)
        assertEquals(AccentPart.OnContainer, slot.part)
    }

    @Test
    fun accentSlot_sameAccentAndPart_areTheSameAddress() {
        val one = AccentSlot(index = 2, part = AccentPart.Container)
        val other = AccentSlot(index = 2, part = AccentPart.Container)

        assertEquals(one, other)
    }

    @Test
    fun accentSlot_everyPart_addressesOneColorOfTheFamily() {
        val family = AccentPart.entries.map { part -> AccentSlot(index = 1, part = part) }

        assertEquals(AccentPart.entries.size, family.toSet().size, "Two parts of a family share an address")
    }
}
