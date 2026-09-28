package com.materialkolor.builder.domain.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorNamesTest {
    private val entries = ColorNames.entries

    @Test
    fun nameOf_burntOrangeSeed_readsAsAWarmOrange() {
        val seed = Argb.fromHex("#D9653B")
        val name = ColorNames.nameOf(seed)
        val matched = entries.single { it.name == name }

        assertEquals("Burnt Ember", name)
        assertTrue(matched.oklab.toOklch().h in 30.0..60.0, "hue ${matched.oklab.toOklch().h}")
    }

    @Test
    fun nameOf_everyPureGrey_takesAGreyName() {
        val greyNames = entries.filter { it.argb.isGrey() }.map { it.name }.toSet()

        (0..0xFF).forEach { level ->
            val grey = Argb((level shl 16) or (level shl 8) or level)
            val name = ColorNames.nameOf(grey)
            assertTrue(name in greyNames, "${grey.toHex()} is called $name")
        }
    }

    @Test
    fun nameOf_eachEntryColor_givesItsOwnName() {
        entries.forEach { entry -> assertEquals(entry.name, ColorNames.nameOf(entry.argb), entry.argb.toHex()) }
    }

    @Test
    fun entries_count_isAboutThreeHundred() {
        assertTrue(entries.size in 280..340, "${entries.size} names")
    }

    @Test
    fun entries_namesAndColors_areUnique() {
        assertEquals(entries.size, entries.map { it.name.lowercase() }.toSet().size)
        assertEquals(entries.size, entries.map { it.argb }.toSet().size)
    }

    @Test
    fun entries_everyName_isTwoTitleCaseWords() {
        entries.forEach { entry ->
            val words = entry.name.split(' ')
            assertEquals(2, words.size, entry.name)
            words.forEach { word ->
                assertTrue(word.first().isUpperCase() && word.drop(1).all { it in 'a'..'z' }, entry.name)
            }
        }
    }

    @Test
    fun entries_noName_isACssNamedColor() {
        entries.forEach { entry ->
            val asCss = entry.name.replace(" ", "").lowercase()
            assertTrue(asCss !in CssColors.byName, "${entry.name} reads as the CSS color $asCss")
        }
    }

    @Test
    fun colorNameData_sourceFile_staysUnderTenKilobytes() {
        // Each string sits on its own source line, indented, quoted and followed by a comma.
        val listBytes = ColorNameData.sumOf { line -> line.encodeToByteArray().size + SourceLineOverhead }
        val fileBytes = listBytes + SourceHeaderAllowance

        assertTrue(fileBytes < TenKilobytes, "about $fileBytes bytes")
    }

    private fun Argb.isGrey(): Boolean = red == green && green == blue

    private companion object {
        /**
         * Eight spaces of indent, two quotes, a comma and a newline.
         */
        const val SourceLineOverhead = 12

        /**
         * The package line, the KDoc and the `listOf(` around the strings, with room to spare.
         */
        const val SourceHeaderAllowance = 1_000

        const val TenKilobytes = 10_000
    }
}
