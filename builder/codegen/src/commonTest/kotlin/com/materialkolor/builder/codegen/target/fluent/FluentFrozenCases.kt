package com.materialkolor.builder.codegen.target.fluent

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.frozenPrefs
import com.materialkolor.builder.codegen.target.lintFailures
import com.materialkolor.builder.codegen.target.materialKolorImports
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.export.FluentShades
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The golden cases of the Fluent frozen export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object FluentFrozenCases {
    private val FluentDocument: ThemeDocument = Fixtures.Base.copy(library = Library.Fluent)

    val all: Map<String, ExportInput> = mapOf(
        "fluent-frozen-default" to Fixtures.input(FluentDocument, frozenPrefs()),
        "fluent-frozen-vibrant-primary-override" to Fixtures.input(
            document = FluentDocument.copy(
                style = Style.Vibrant,
                keyColors = Fixtures.PrimaryOverride.input.document.keyColors,
            ),
            prefs = frozenPrefs(),
        ),
        "fluent-frozen-all-contrasts" to Fixtures.input(FluentDocument, frozenPrefs(FrozenVariants.AllContrasts)),
    )

    fun files(case: String): List<GeneratedFile> = FluentFrozen.files(all.getValue(case))
}

class FluentFrozenTest {
    @Test
    fun fluentFrozen_everyShadeOfBothModes_isWrittenAsALiteral() {
        val input = FluentFrozenCases.all.getValue("fluent-frozen-default")
        val shades = checkNotNull(input.resolved.fluentShades)
        val theme = FluentFrozen.files(input).single().text

        listOf(LIGHT_THEME_SHADES to shades.light, DARK_THEME_SHADES to shades.dark).forEach { (property, values) ->
            val declared = theme.substringAfter("val $property = Shades(\n").substringBefore(")\n")
            named(values).forEach { (name, color) ->
                val expected = "    $name = Color(${Literals.hexText(color.value)}),"
                assertTrue(expected in declared, "$expected in $property")
            }
        }
        assertTrue("shades = if (isDark) DarkThemeShades else LightThemeShades," in theme, theme)
        assertTrue("darkMode = isDark," in theme, theme)
    }

    @Test
    fun fluentFrozen_sameShadesInBothModes_stillWritesBothSets() {
        val input = FluentFrozenCases.all.getValue("fluent-frozen-default")
        val light = checkNotNull(input.resolved.fluentShades).light
        val same = input.copy(resolved = input.resolved.copy(fluentShades = FluentShades(light = light, dark = light)))
        val theme = FluentFrozen.files(same).single().text

        assertTrue("val LightThemeShades = Shades(" in theme, theme)
        assertTrue("val DarkThemeShades = Shades(" in theme, theme)
        assertTrue("shades = if (isDark) DarkThemeShades else LightThemeShades," in theme, theme)
    }

    @Test
    fun fluentFrozen_allContrasts_writesTheSameShadesAsStandard() {
        val standard = codeOf(FluentFrozenCases.files("fluent-frozen-default"))
        val every = codeOf(FluentFrozenCases.files("fluent-frozen-all-contrasts"))

        assertEquals(standard, every)
    }

    private fun named(shades: FluentShadeValues): List<Pair<String, Argb>> =
        listOf(
            "base" to shades.base,
            "light1" to shades.light1,
            "light2" to shades.light2,
            "light3" to shades.light3,
            "dark1" to shades.dark1,
            "dark2" to shades.dark2,
            "dark3" to shades.dark3,
        )

    /**
     * The files without their header, which carries the link and so differs between cases.
     */
    private fun codeOf(files: List<GeneratedFile>): List<String> =
        files.map { file ->
            file.text
                .lines()
                .dropWhile { line -> line.startsWith("//") }
                .joinToString("\n")
        }
}
