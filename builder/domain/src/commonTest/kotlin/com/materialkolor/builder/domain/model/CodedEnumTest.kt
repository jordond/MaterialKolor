package com.materialkolor.builder.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CodedEnumTest {
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
        assertEquals(0, Style.TonalSpot.code)
        assertEquals(9, Style.Cmf.code)
        assertEquals(Style.entries.indices.toList(), Style.entries.map { style -> style.code })
    }

    @Test
    fun keyColor_declarationOrder_isTheCodecMaskOrder() {
        val expected = listOf("Primary", "Secondary", "Tertiary", "Error", "Neutral", "NeutralVariant")

        assertEquals(expected, KeyColor.entries.map { slot -> slot.name })
        assertEquals(KeyColor.entries.indices.toList(), KeyColor.entries.map { slot -> slot.code })
    }
}
