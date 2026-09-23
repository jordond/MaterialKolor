package com.materialkolor.builder.codegen.validate

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.target.material3.Material3Dynamic
import com.materialkolor.builder.codegen.target.material3.Material3DynamicCases
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
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
}
