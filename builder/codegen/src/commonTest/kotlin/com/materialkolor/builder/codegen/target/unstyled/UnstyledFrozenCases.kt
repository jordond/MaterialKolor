package com.materialkolor.builder.codegen.target.unstyled

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
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The golden cases of the Unstyled frozen export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object UnstyledFrozenCases {
    private val UnstyledDocument: ThemeDocument = Fixtures.Base.copy(library = Library.Unstyled)

    val AccentsPinsAmoled: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document.copy(
            library = Library.Unstyled,
            accents = Fixtures.ThreeAccents.input.document.accents,
            amoled = true,
        ),
        prefs = frozenPrefs(),
    )

    val all: Map<String, ExportInput> = mapOf(
        "unstyled-frozen-default" to Fixtures.Default.with(document = UnstyledDocument, prefs = frozenPrefs()).input,
        "unstyled-frozen-accents-pins-amoled" to AccentsPinsAmoled.input,
        "unstyled-frozen-all-contrasts" to Fixtures.Default
            .with(document = UnstyledDocument, prefs = frozenPrefs(FrozenVariants.AllContrasts))
            .input,
    )

    fun files(case: String): List<GeneratedFile> = UnstyledFrozen.files(all.getValue(case))
}

class UnstyledFrozenTest {
    @Test
    fun unstyledFrozen_everyRole_isWrittenForBothModesAtEveryVariant() {
        UnstyledFrozenCases.all.forEach { (case, input) ->
            val files = UnstyledFrozen.files(input)
            val tokens = files.text("Tokens.kt")
            val colors = files.text("Color.kt")

            assertEquals(input.resolved.roles.keys, input.prefs.frozenVariants.expectedVariants(), case)
            input.resolved.roles.forEach { (variant, table) ->
                listOf(true to table.light, false to table.dark).forEach { (isLight, values) ->
                    val map = colors.mapBlock(mapName(variant, isLight))
                    Role.entries.forEach { role ->
                        val name = role.name.replaceFirstChar { it.lowercaseChar() }
                        assertTrue("    val $name = ThemeToken<Color>(\"$name\")\n" in tokens, "$name in $case")
                        assertTrue(entry(name, values.getValue(role)) in map, "$name in $case $variant")
                    }
                }
            }
        }
    }

    @Test
    fun unstyledFrozen_accents_flattenIntoFourTokensAtTheirOneSetOfValues() {
        val files = UnstyledFrozen.files(UnstyledFrozenCases.AccentsPinsAmoled.input)
        val tokens = files.text("Tokens.kt")
        val colors = files.text("Color.kt")
        val brand = UnstyledFrozenCases.AccentsPinsAmoled.input.resolved.accents
            .first()

        listOf("brand", "onBrand", "brandContainer", "onBrandContainer").forEach { name ->
            assertTrue("    val $name = ThemeToken<Color>(\"$name\")\n" in tokens, name)
        }
        assertTrue(entry("brandContainer", brand.light.container) in colors.mapBlock("lightColors"), colors)
        assertTrue(entry("onBrandContainer", brand.dark.onContainer) in colors.mapBlock("darkColors"), colors)
    }

    @Test
    fun unstyledFrozen_allContrastsWithAccents_carriesEveryAccentInEveryMap() {
        val input = UnstyledFrozenCases.AccentsPinsAmoled.with(prefs = frozenPrefs(FrozenVariants.AllContrasts)).input
        val colors = UnstyledFrozen.files(input).text("Color.kt")

        assertEquals(ContrastVariant.entries.toSet(), input.resolved.roles.keys)
        assertEquals(3, input.resolved.accents.size)
        input.resolved.roles.keys.forEach { variant ->
            listOf(true, false).forEach { isLight ->
                val map = colors.mapBlock(mapName(variant, isLight))
                input.resolved.accents.forEach { accent ->
                    val values = if (isLight) accent.light else accent.dark
                    val upper = accent.name.replaceFirstChar { it.uppercaseChar() }
                    val lower = accent.name.replaceFirstChar { it.lowercaseChar() }
                    val entries = listOf(
                        entry(lower, values.color),
                        entry("on$upper", values.onColor),
                        entry("${lower}Container", values.container),
                        entry("on${upper}Container", values.onContainer),
                    )
                    entries.forEach { expected -> assertTrue(expected in map, "$expected in $variant") }
                }
            }
        }
    }

    @Test
    fun unstyledFrozen_animate_changesNothing() {
        val animated = Fixtures.Default.with(
            document = Fixtures.Base.copy(library = Library.Unstyled),
            prefs = frozenPrefs().copy(animate = true),
        )

        assertEquals(
            UnstyledFrozenCases.files("unstyled-frozen-default").map { it.text },
            UnstyledFrozen.files(animated.input).map { it.text },
        )
    }

    private fun List<GeneratedFile>.text(fileName: String): String = single { it.path.endsWith("/$fileName") }.text

    /**
     * The lines of one map in `Color.kt`, from its declaration to its closing parenthesis.
     */
    private fun String.mapBlock(name: String): String =
        substringAfter("val $name: Map<ThemeToken<Color>, Color> = mapOf(\n", missingDelimiterValue = "")
            .substringBefore("\n)")

    private fun entry(
        name: String,
        color: Argb,
    ): String = "    ThemeTokens.$name to Color(${Literals.hexText(color.value)}),"

    private fun mapName(
        variant: ContrastVariant,
        isLight: Boolean,
    ): String {
        val mode = if (isLight) "Light" else "Dark"

        return when (variant) {
            ContrastVariant.Standard -> "${mode.lowercase()}Colors"
            ContrastVariant.Medium -> "mediumContrast${mode}Colors"
            ContrastVariant.High -> "highContrast${mode}Colors"
        }
    }
}
