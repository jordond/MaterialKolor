package com.materialkolor.builder.domain.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CssColorsTest {
    @Test
    fun byName_list_hasAll148NamedColors() {
        assertEquals(148, CssColors.byName.size)
    }

    @Test
    fun byName_everyName_isLowercaseLetters() {
        CssColors.byName.keys.forEach { name -> assertTrue(name.all { it in 'a'..'z' }, name) }
    }

    @Test
    fun byName_greyAndGraySpellings_matchEachOther() {
        CssColors.byName.filterKeys { "gray" in it }.forEach { (name, value) ->
            assertEquals(value, CssColors.byName[name.replace("gray", "grey")], name)
        }
    }

    @Test
    fun find_knownName_givesAnOpaqueColor() {
        assertEquals(Argb.fromHex("#663399"), CssColors.find("rebeccapurple"))
        assertEquals(Argb.fromHex("#FAFAD2"), CssColors.find("lightgoldenrodyellow"))
        assertEquals(Argb.fromHex("#000000"), CssColors.find("black"))
    }

    @Test
    fun find_unknownName_givesNull() {
        assertNull(CssColors.find("transparent"))
        assertNull(CssColors.find("Red"))
    }
}
