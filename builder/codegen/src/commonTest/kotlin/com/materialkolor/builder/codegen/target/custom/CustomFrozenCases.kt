package com.materialkolor.builder.codegen.target.custom

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.expectedVariants
import com.materialkolor.builder.codegen.target.frozenPrefs
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.codegen.target.materialKolorImports
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The golden cases of the Custom frozen export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object CustomFrozenCases {
    private val CustomDocument: ThemeDocument = Fixtures.Base.copy(library = Library.Custom)

    val Default: Fixture = Fixtures.Default.with(document = CustomDocument, prefs = frozenPrefs())

    val AccentsPinsAmoled: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document.copy(
            library = Library.Custom,
            accents = Fixtures.ThreeAccents.input.document.accents,
            amoled = true,
        ),
        prefs = frozenPrefs(),
    )

    val all: Map<String, ExportInput> = mapOf(
        "custom-frozen-default" to Default.input,
        "custom-frozen-accents-pins-amoled" to AccentsPinsAmoled.input,
        "custom-frozen-all-contrasts" to
            Fixtures.Default.with(document = CustomDocument, prefs = frozenPrefs(FrozenVariants.AllContrasts)).input,
    )

    fun files(case: String): List<GeneratedFile> = CustomFrozen.files(all.getValue(case))
}

class CustomFrozenTest {
    @Test
    fun customFrozen_everyCase_matchesTheGoldenHash() {
        CustomFrozenCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(CustomFrozenCases.files(case)), case)
        }
    }

    @Test
    fun customFrozen_everyCodeLine_passesTheLintLimits() {
        CustomFrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(CustomFrozenCases.files(case)), case)
        }
    }

    @Test
    fun customFrozen_everyCase_importsNothingFromMaterialKolor() {
        CustomFrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), materialKolorImports(CustomFrozenCases.files(case)), case)
        }
    }

    @Test
    fun customFrozen_everySlot_isWrittenForBothModesAtEveryVariant() {
        CustomFrozenCases.all.forEach { (case, input) ->
            val colors = themeColors(input)
            val blocks = colors.split("\n\n")

            assertEquals(input.resolved.customSlots.keys, input.prefs.frozenVariants.expectedVariants(), case)
            input.resolved.customSlots.forEach { (variant, slots) ->
                listOf("Light" to slots.light, "Dark" to slots.dark).forEach { (mode, values) ->
                    val name = variant.valueName(mode)
                    val block = blocks.single { it.startsWith("val $name = ThemeColors(") || "\nval $name =" in it }
                    CustomSlot.entries.forEach { slot ->
                        val hex = Literals.hexText(values.getValue(slot).value)
                        val expected = "    ${slot.propertyName} = Color($hex),"
                        assertTrue(expected in block, "$expected in $name of $case")
                    }
                }
            }
        }
    }

    @Test
    fun customFrozen_slotProperties_areNamedTheWaySlotsSerialize() {
        val descriptor = CustomSlot.serializer().descriptor

        CustomSlot.entries.forEach { slot -> assertEquals(descriptor.getElementName(slot.ordinal), slot.propertyName) }
    }

    @Test
    fun customFrozen_accents_areNestedFamilies() {
        val withAccents = themeColors(CustomFrozenCases.AccentsPinsAmoled.input)
        val without = themeColors(CustomFrozenCases.Default.input)

        assertTrue("data class ColorFamily(" in withAccents, withAccents)
        assertTrue("    val brand: ColorFamily," in withAccents, withAccents)
        assertTrue("    brand = ColorFamily(\n" in withAccents, withAccents)
        assertFalse("brandColor" in withAccents, withAccents)
        assertFalse("ColorFamily" in without, without)
    }

    @Test
    fun customFrozen_theme_providesTheStandardPair() {
        val theme = CustomFrozenCases.files("custom-frozen-all-contrasts").single { it.path.endsWith("/Theme.kt") }.text

        assertTrue("val LocalThemeColors = staticCompositionLocalOf<ThemeColors> {" in theme, theme)
        assertTrue("val colors = if (isDark) darkThemeColors else lightThemeColors" in theme, theme)
        assertTrue("CompositionLocalProvider(LocalThemeColors provides colors, content = content)" in theme, theme)
    }

    @Test
    fun customFrozen_otherLibrary_isRefused() {
        val material3 = Fixtures.input(document = Fixtures.Base, prefs = frozenPrefs())

        assertFailsWith<IllegalArgumentException> { CustomFrozen.files(material3) }
    }

    private fun themeColors(input: ExportInput): String =
        CustomFrozen.files(input).single { it.path.endsWith("/ThemeColors.kt") }.text

    private fun ContrastVariant.valueName(mode: String): String =
        when (this) {
            ContrastVariant.Standard -> "${mode.lowercase()}ThemeColors"
            ContrastVariant.Medium -> "mediumContrast${mode}ThemeColors"
            ContrastVariant.High -> "highContrast${mode}ThemeColors"
        }
}
