package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.Fixture
import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenDigest
import com.materialkolor.builder.codegen.GoldenHashes
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.MAX_LINE_LENGTH
import com.materialkolor.builder.codegen.text.Literals
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The prefs of a frozen export, at the standard contrast or at every contrast. */
internal fun frozenPrefs(variants: FrozenVariants = FrozenVariants.StandardOnly): ExportPrefs =
    ExportPrefs(mode = ExportMode.Frozen, frozenVariants = variants)

/** Every file in [files] that imports anything from MaterialKolor, which a frozen export never does. */
internal fun materialKolorImports(files: List<GeneratedFile>): List<String> =
    files.flatMap { file -> file.text.lines() }.filter { line -> line.startsWith("import com.materialkolor") }

/** Every code line of [files] past the column limit or ending in a space, which ktlint would reject. */
internal fun lintFailures(files: List<GeneratedFile>): List<String> =
    files.flatMap { file ->
        val lines = file.text.lines()
        // The header's share link grows with the theme. ktlint leaves a line that is only a comment alone.
        val tooLong = lines.filterNot { it.startsWith("//") }.filter { it.length > MAX_LINE_LENGTH }

        (tooLong + lines.filter { it.endsWith(" ") }).map { line -> "${file.path} $line" }
    }

/**
 * The golden cases of the Material 3 frozen export, by case name.
 *
 * They live in common code so the wasm tests regenerate the same files and hold them to the hashes
 * the JVM goldens were written with.
 */
internal object Material3FrozenCases {
    private val ExpressiveDocument: ThemeDocument =
        ThemeDocument.Default.copy(expressive = true, style = Style.Expressive, spec = SpecVersion.Spec2025)

    val AccentsPinsAmoled: Fixture = Fixtures.Pins.with(
        document = Fixtures.Pins.input.document.copy(
            accents = Fixtures.ThreeAccents.input.document.accents,
            amoled = true,
        ),
        prefs = frozenPrefs(),
    )

    val all: Map<String, ExportInput> = mapOf(
        "material3-frozen-default" to Fixtures.Default.with(prefs = frozenPrefs()).input,
        "material3-frozen-accents-pins-amoled" to AccentsPinsAmoled.input,
        "material3-frozen-all-contrasts" to
            Fixtures.Default.with(prefs = frozenPrefs(FrozenVariants.AllContrasts)).input,
        "expressive-frozen-default" to
            Fixtures.Default.with(document = ExpressiveDocument, prefs = frozenPrefs()).input,
        "expressive-frozen-all-contrasts" to Fixtures.Default
            .with(
                document = ExpressiveDocument.copy(motionScheme = MotionSchemeChoice.Standard),
                prefs = frozenPrefs(FrozenVariants.AllContrasts),
            ).input,
    )

    fun files(case: String): List<GeneratedFile> = Material3Frozen.files(all.getValue(case))
}

class Material3FrozenTest {
    @Test
    fun material3Frozen_everyCase_matchesTheGoldenHash() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(GoldenHashes.cases[case], GoldenDigest.of(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyCodeLine_passesTheLintLimits() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), lintFailures(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyCase_importsNothingFromMaterialKolor() {
        Material3FrozenCases.all.keys.forEach { case ->
            assertEquals(emptyList(), materialKolorImports(Material3FrozenCases.files(case)), case)
        }
    }

    @Test
    fun material3Frozen_everyRole_isWrittenForBothModesAtEveryVariant() {
        Material3FrozenCases.all.forEach { (case, input) ->
            val files = Material3Frozen.files(input)
            val colors = files.single { it.path.endsWith("/Color.kt") }.text
            val theme = files.single { it.path.endsWith("/Theme.kt") }.text

            assertEquals(input.resolved.roles.keys, input.prefs.frozenVariants.expected(), case)
            input.resolved.roles.forEach { (variant, table) ->
                Role.entries.forEach { role ->
                    val parameter = role.name.replaceFirstChar { it.lowercaseChar() }
                    val suffix = variant.suffix
                    listOf("Light" to table.light, "Dark" to table.dark).forEach { (mode, values) ->
                        val name = "$parameter$mode$suffix"
                        val literal = Literals.hexText(values.getValue(role).value)
                        assertTrue("val $name = Color($literal)" in colors, "$name in $case")
                        assertTrue("    $parameter = $name,\n" in theme, "$name in $case")
                    }
                }
            }
        }
    }

    @Test
    fun material3Frozen_allContrasts_namesEverySchemeAndUsesTheStandardPair() {
        val theme = theme(Material3FrozenCases.all.getValue("material3-frozen-all-contrasts"))

        listOf(
            "val lightScheme = lightColorScheme(",
            "val darkScheme = darkColorScheme(",
            "val mediumContrastLightColorScheme = lightColorScheme(",
            "val mediumContrastDarkColorScheme = darkColorScheme(",
            "val highContrastLightColorScheme = lightColorScheme(",
            "val highContrastDarkColorScheme = darkColorScheme(",
            "colorScheme = if (isDark) darkScheme else lightScheme,",
        ).forEach { expected -> assertTrue(expected in theme, expected) }
    }

    @Test
    fun material3Frozen_expressive_writesTheMotionSchemeWithoutOptingIn() {
        val default = theme(Material3FrozenCases.all.getValue("expressive-frozen-default"))
        val standard = theme(Material3FrozenCases.all.getValue("expressive-frozen-all-contrasts"))

        assertTrue("MaterialExpressiveTheme(" in default, default)
        assertTrue("motionScheme = MotionScheme.expressive()," in default, default)
        assertTrue("motionScheme = MotionScheme.standard()," in standard, standard)
        assertFalse("@OptIn" in default, default)
    }

    @Test
    fun material3Frozen_accents_writeLiteralFamiliesOnlyWhenThereAreAny() {
        val withAccents = Material3Frozen.files(Material3FrozenCases.AccentsPinsAmoled.input)
        val without = Material3FrozenCases.files("material3-frozen-default")
        val extended = withAccents.last().text
        val brand = Material3FrozenCases.AccentsPinsAmoled.input.resolved.accents
            .first()
            .light

        assertEquals(listOf("Color.kt", "Theme.kt", "ExtendedColors.kt"), withAccents.map { it.fileName })
        assertEquals(listOf("Color.kt", "Theme.kt"), without.map { it.fileName })
        assertTrue("val extendedLight = ExtendedColors(" in extended, extended)
        assertTrue("val extendedDark = ExtendedColors(" in extended, extended)
        assertTrue("colorContainer = Color(${Literals.hexText(brand.container.value)})," in extended, extended)
        assertTrue("onColorContainer = Color(${Literals.hexText(brand.onContainer.value)})," in extended, extended)
        assertTrue("LocalExtendedColors provides extendedColors" in withAccents[1].text)
    }

    @Test
    fun material3Frozen_otherLibrary_isRefused() {
        val fluent = Fixtures.input(
            document = ThemeDocument.Default.copy(library = Library.Fluent),
            prefs = frozenPrefs(),
        )

        assertFailsWith<IllegalArgumentException> { Material3Frozen.files(fluent) }
    }

    private fun theme(input: ExportInput): String =
        Material3Frozen.files(input).single { it.fileName == "Theme.kt" }.text

    private val GeneratedFile.fileName: String
        get() = path.substringAfterLast('/')

    private val ContrastVariant.suffix: String
        get() = when (this) {
            ContrastVariant.Standard -> ""
            ContrastVariant.Medium -> "MediumContrast"
            ContrastVariant.High -> "HighContrast"
        }

    private fun FrozenVariants.expected(): Set<ContrastVariant> =
        when (this) {
            FrozenVariants.StandardOnly -> setOf(ContrastVariant.Standard)
            FrozenVariants.AllContrasts -> ContrastVariant.entries.toSet()
        }
}
