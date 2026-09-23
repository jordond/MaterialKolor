package com.materialkolor.builder.codegen.validate

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.target.custom.CustomFrozen
import com.materialkolor.builder.codegen.target.custom.CustomFrozenCases
import com.materialkolor.builder.codegen.target.fluent.FluentFrozen
import com.materialkolor.builder.codegen.target.fluent.FluentFrozenCases
import com.materialkolor.builder.codegen.target.frozenPrefs
import com.materialkolor.builder.codegen.target.material3.Material3Dynamic
import com.materialkolor.builder.codegen.target.material3.Material3DynamicCases
import com.materialkolor.builder.codegen.target.material3.Material3Frozen
import com.materialkolor.builder.codegen.target.material3.Material3FrozenCases
import com.materialkolor.builder.codegen.target.unstyled.UnstyledFrozen
import com.materialkolor.builder.codegen.target.unstyled.UnstyledFrozenCases
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReservedNamesTest {
    private val seed = Argb(0xFF1E88E5.toInt())

    @Test
    fun clashes_themeNamedMaterialTheme_isReported() {
        val document = ThemeDocument.Default.copy(themeName = "MaterialTheme")

        assertEquals(listOf(ReservedNameClash.ThemeName("MaterialTheme")), ReservedNames.clashes(document))
    }

    @Test
    fun clashes_accentNamedAfterAnImport_isReportedWhateverTheCaseOfItsFirstLetter() {
        val document = ThemeDocument.Default.copy(
            accents = listOf(
                Accent(name = "brand", seed = seed),
                Accent(name = "remember", seed = seed),
                Accent(name = "Harmonize", seed = seed),
            ),
        )

        assertEquals(
            listOf(ReservedNameClash.AccentName(1, "remember"), ReservedNameClash.AccentName(2, "Harmonize")),
            ReservedNames.clashes(document),
        )
    }

    @Test
    fun clashes_nameTheMaterial3ExportDeclares_isReported() {
        val document = ThemeDocument.Default.copy(
            themeName = "ExtendedColors",
            accents = listOf(
                Accent(name = "colorFamily", seed = seed),
                Accent(name = "LocalExtendedColors", seed = seed),
                Accent(name = "RememberExtendedColors", seed = seed),
            ),
        )

        assertEquals(
            listOf(
                ReservedNameClash.ThemeName("ExtendedColors"),
                ReservedNameClash.AccentName(0, "colorFamily"),
                ReservedNameClash.AccentName(1, "LocalExtendedColors"),
                ReservedNameClash.AccentName(2, "RememberExtendedColors"),
            ),
            ReservedNames.clashes(document),
        )
    }

    @Test
    fun clashes_everyFixture_hasNone() {
        Fixtures.all.forEach { fixture ->
            assertEquals(emptyList(), ReservedNames.clashes(fixture.input.document), fixture.name)
        }
    }

    @Test
    fun clashes_reportedName_isOneTheGeneratorWouldRefuse() {
        val document = ThemeDocument.Default.copy(themeName = "DynamicMaterialTheme")

        assertEquals(listOf(ReservedNameClash.ThemeName("DynamicMaterialTheme")), ReservedNames.clashes(document))
        assertFailsWith<IllegalArgumentException> { Material3Dynamic.files(Fixtures.input(document)) }
    }

    @Test
    fun of_material3_coversEveryNameTheDynamicExportImports() {
        Material3DynamicCases.all.forEach { (case, input) ->
            val reserved = ReservedNames.of(input.target)
            val imported = Material3Dynamic
                .files(input)
                .flatMap { file -> file.text.lines() }
                .filter { line -> line.startsWith("import ") }
                .map { line -> line.substringAfterLast('.') }

            assertTrue(reserved.containsAll(imported), "$case imports ${imported - reserved}")
        }
    }

    @Test
    fun of_bothMaterial3Targets_reserveTheSameNames() {
        assertEquals(ReservedNames.of(ExportTarget.Material3), ReservedNames.of(ExportTarget.Material3Expressive))
    }

    // b-111
    @Test
    fun clashes_customAccentNamedLikeASlotOrADeclaration_isReported() {
        val document = ThemeDocument.Default.copy(
            library = Library.Custom,
            themeName = "ThemeColors",
            accents = listOf(
                Accent(name = "TextStrong", seed = seed),
                Accent(name = "focusRing", seed = seed),
                Accent(name = "brand", seed = seed),
                Accent(name = "LocalThemeColors", seed = seed),
                Accent(name = "ColorFamily", seed = seed),
            ),
        )

        assertEquals(
            listOf(
                ReservedNameClash.ThemeName("ThemeColors"),
                ReservedNameClash.AccentName(0, "TextStrong"),
                ReservedNameClash.AccentName(1, "focusRing"),
                ReservedNameClash.AccentName(3, "LocalThemeColors"),
                ReservedNameClash.AccentName(4, "ColorFamily"),
            ),
            ReservedNames.clashes(document),
        )
    }

    @Test
    fun clashes_customThemeNamedLikeASlot_passesWhileAnAccentNamedLikeOneIsReported() {
        listOf("Surface", "TextStrong").forEach { themeName ->
            val document = ThemeDocument.Default.copy(
                library = Library.Custom,
                themeName = themeName,
                accents = listOf(Accent(name = "TextStrong", seed = seed)),
            )

            val expected = listOf(ReservedNameClash.AccentName(0, "TextStrong"))

            assertEquals(expected, ReservedNames.clashes(document), themeName)
            CustomFrozen.files(Fixtures.input(document.copy(accents = emptyList()), frozenPrefs()))
        }
    }

    // b-111
    @Test
    fun clashes_fluentThemeNamedThemeShades_isReported() {
        val document = ThemeDocument.Default.copy(library = Library.Fluent, themeName = "ThemeShades")

        assertEquals(listOf(ReservedNameClash.ThemeName("ThemeShades")), ReservedNames.clashes(document))
    }

    // b-111
    @Test
    fun of_frozenTargets_coverEveryNameTheFrozenExportsImport() {
        val exports = Material3FrozenCases.all.mapValues { (_, input) -> input to Material3Frozen.files(input) } +
            FluentFrozenCases.all.mapValues { (_, input) -> input to FluentFrozen.files(input) } +
            CustomFrozenCases.all.mapValues { (_, input) -> input to CustomFrozen.files(input) } +
            UnstyledFrozenCases.all.mapValues { (_, input) -> input to UnstyledFrozen.files(input) } // b-111b

        exports.forEach { (case, export) ->
            val (input, files) = export
            val reserved = ReservedNames.of(input.target)
            val imported = files
                .flatMap { file -> file.text.lines() }
                .filter { line -> line.startsWith("import ") }
                .map { line -> line.substringAfterLast('.') }

            assertTrue(reserved.containsAll(imported), "$case imports ${imported - reserved}")
        }
    }

    // b-111b
    @Test
    fun clashes_unstyledAccentsWhoseFlattenedTokensMeet_reportTheLaterOne() {
        listOf("OnBrand", "BrandContainer").forEach { other ->
            val document = ThemeDocument.Default.copy(
                library = Library.Unstyled,
                accents = listOf(Accent(name = "Brand", seed = seed), Accent(name = other, seed = seed)),
            )

            assertEquals(listOf(ReservedNameClash.AccentName(1, other)), ReservedNames.clashes(document), other)
            assertEquals(emptyList(), ReservedNames.clashes(document.copy(library = Library.Material3)), other)
        }
    }

    @Test
    fun clashes_unstyledAccentFlattenedOntoALibraryToken_isReported() {
        val document = ThemeDocument.Default.copy(
            library = Library.Unstyled,
            accents = listOf(
                Accent(name = "PrimaryFixedVariant", seed = seed),
                Accent(name = "Brand", seed = seed),
                Accent(name = "Shadow", seed = seed),
            ),
        )

        assertEquals(
            listOf(
                ReservedNameClash.AccentName(0, "PrimaryFixedVariant"),
                ReservedNameClash.AccentName(2, "Shadow"),
            ),
            ReservedNames.clashes(document),
        )
        assertEquals(emptyList(), ReservedNames.clashes(document.copy(library = Library.Material3)))
    }

    @Test
    fun clashes_unstyledNamesTheFrozenExportDeclaresOrImports_areReported() {
        val document = ThemeDocument.Default.copy(
            library = Library.Unstyled,
            themeName = "ThemeTokens",
            accents = listOf(
                Accent(name = "LightColors", seed = seed),
                Accent(name = "highContrastDarkColors", seed = seed),
                Accent(name = "ThemeProperty", seed = seed),
            ),
        )

        assertEquals(
            listOf(
                ReservedNameClash.ThemeName("ThemeTokens"),
                ReservedNameClash.AccentName(0, "LightColors"),
                ReservedNameClash.AccentName(1, "highContrastDarkColors"),
                ReservedNameClash.AccentName(2, "ThemeProperty"),
            ),
            ReservedNames.clashes(document),
        )
        val colorScheme = ThemeDocument.Default.copy(library = Library.Unstyled, themeName = "ColorScheme")
        assertEquals(listOf(ReservedNameClash.ThemeName("ColorScheme")), ReservedNames.clashes(colorScheme))
        assertFailsWith<IllegalArgumentException> { UnstyledFrozen.files(Fixtures.input(colorScheme, frozenPrefs())) }
    }
}
