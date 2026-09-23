package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CodedEnumTest {
    /**
     * Every enum in the model that carries codes, listed by hand.
     *
     * Common code cannot go looking for them, so a slice that adds a [CodedEnum] adds it here in
     * the same change. An enum that is missing from this list has its codes checked by nothing.
     */
    private val codedEnums: Map<String, List<CodedEnum>> = mapOf(
        "Style" to Style.entries,
        "SpecVersion" to SpecVersion.entries,
        "SchemePlatform" to SchemePlatform.entries,
        "Library" to Library.entries,
        "MotionSchemeChoice" to MotionSchemeChoice.entries,
        "KeyColor" to KeyColor.entries,
        "OnColorThreshold" to OnColorThreshold.entries,
        "Role" to Role.entries,
        "RoleGroup" to RoleGroup.entries,
        "CustomSlot" to CustomSlot.entries,
        "AccentPart" to AccentPart.entries,
    )

    @Test
    fun codedEnum_everyEnum_givesEachEntryItsOwnCode() {
        codedEnums.forEach { (name, entries) ->
            val codes = entries.map { entry -> entry.code }
            assertEquals(codes.size, codes.toSet().size, "$name repeats a code in $codes")
        }
    }

    @Test
    fun codedEnum_everyCode_findsItsOwnEntry() {
        codedEnums.forEach { (name, entries) ->
            entries.forEach { entry ->
                assertEquals(entry, entries.withCode(entry.code), "$name lost the entry with code ${entry.code}")
            }
        }
    }

    @Test
    fun codedEnum_codeNothingCarries_isNotFound() {
        assertNull(Style.entries.withCodeOrNull(code = 99))
        assertFailsWith<IllegalArgumentException> { Style.entries.withCode(code = 99) }
    }

    @Test
    fun style_everyEntry_keepsTheCodeItWasGiven() {
        val styleCodes = mapOf(
            "TonalSpot" to 0,
            "Neutral" to 1,
            "Vibrant" to 2,
            "Expressive" to 3,
            "Rainbow" to 4,
            "FruitSalad" to 5,
            "Monochrome" to 6,
            "Fidelity" to 7,
            "Content" to 8,
            "Cmf" to 9,
        )

        assertEquals(styleCodes.size, Style.entries.size, "A style was added or dropped without touching the table")

        Style.entries.forEach { style ->
            val expected = assertNotNull(styleCodes[style.name], "${style.name} is missing from the table")
            assertEquals(expected, style.code, "${style.name} carries a code no shared link knows about")
        }
    }

    @Test
    fun keyColor_everyEntry_keepsItsBitOfTheCodecMask() {
        val maskBits = mapOf(
            "Primary" to 0,
            "Secondary" to 1,
            "Tertiary" to 2,
            "Error" to 3,
            "Neutral" to 4,
            "NeutralVariant" to 5,
        )

        assertEquals(maskBits.keys.toList(), KeyColor.entries.map { slot -> slot.name })

        KeyColor.entries.forEach { slot ->
            val expected = assertNotNull(maskBits[slot.name], "${slot.name} is missing from the table")
            assertEquals(expected, slot.code, "${slot.name} moved to another bit of the presence mask")
        }
    }
}
