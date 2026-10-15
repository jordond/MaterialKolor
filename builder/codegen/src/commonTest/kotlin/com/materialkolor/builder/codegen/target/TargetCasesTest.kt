package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenCases
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.formsCaseFiles
import com.materialkolor.builder.codegen.target.custom.CustomDynamic
import com.materialkolor.builder.codegen.target.custom.CustomFrozen
import com.materialkolor.builder.codegen.target.fluent.FluentDynamic
import com.materialkolor.builder.codegen.target.fluent.FluentFrozen
import com.materialkolor.builder.codegen.target.material3.Material3Dynamic
import com.materialkolor.builder.codegen.target.material3.Material3Frozen
import com.materialkolor.builder.codegen.target.unstyled.UnstyledDynamic
import com.materialkolor.builder.codegen.target.unstyled.UnstyledFrozen
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.persist.ExportMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The checks every target export shares, run over the golden cases of each target at once.
 *
 * The golden hashes themselves are held in `GoldenHashesTest`.
 */
class TargetCasesTest {
    @Test
    fun targets_everyCodeLine_passesTheLintLimits() {
        val cases = targetSources.flatMap { source ->
            source.cases.keys.map { case -> case to source.files(case) }
        } + ("dsl-forms" to formsCaseFiles())

        val failures = cases.flatMap { (case, files) -> lintFailures(files).map { line -> "$case $line" } }

        assertEquals(emptyList(), failures)
    }

    @Test
    fun frozenTargets_everyCase_importsNothingFromMaterialKolor() {
        val frozen = targetSources.filter { source -> source.cases.values.all { it.prefs.mode == ExportMode.Frozen } }
        val imports = frozen.flatMap { source ->
            source.cases.keys.flatMap { case -> materialKolorImports(source.files(case)).map { line -> "$case $line" } }
        }

        assertEquals(4, frozen.size)
        assertEquals(emptyList(), imports)
    }

    @Test
    fun targets_otherLibrary_isRefused() {
        val fluent = Fixtures.Base.copy(library = Library.Fluent)
        val material3 = Fixtures.Base
        val refusals: List<Pair<String, () -> List<GeneratedFile>>> = listOf(
            "Material3Dynamic" to { Material3Dynamic.files(Fixtures.input(fluent)) },
            "Material3Frozen" to { Material3Frozen.files(Fixtures.input(fluent, frozenPrefs())) },
            "UnstyledDynamic" to { UnstyledDynamic.files(Fixtures.input(fluent)) },
            "UnstyledFrozen" to { UnstyledFrozen.files(Fixtures.input(material3, frozenPrefs())) },
            "FluentDynamic" to { FluentDynamic.files(Fixtures.input(Fixtures.Base.copy(library = Library.Unstyled))) },
            "FluentFrozen" to { FluentFrozen.files(Fixtures.input(material3, frozenPrefs())) },
            "CustomDynamic" to { CustomDynamic.files(Fixtures.input(material3)) },
            "CustomFrozen" to { CustomFrozen.files(Fixtures.input(material3, frozenPrefs())) },
        )

        refusals.forEach { (target, write) ->
            assertFailsWith<IllegalArgumentException>(target) { write() }
        }
    }

    private val targetSources = GoldenCases.sources.filter { source -> source.name != "SnippetsCases" }
}
