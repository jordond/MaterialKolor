package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.SnippetsCases
import com.materialkolor.builder.codegen.target.custom.CustomDynamicCases
import com.materialkolor.builder.codegen.target.custom.CustomFrozenCases
import com.materialkolor.builder.codegen.target.fluent.FluentDynamicCases
import com.materialkolor.builder.codegen.target.fluent.FluentFrozenCases
import com.materialkolor.builder.codegen.target.material3.Material3DynamicCases
import com.materialkolor.builder.codegen.target.material3.Material3FrozenCases
import com.materialkolor.builder.codegen.target.unstyled.UnstyledDynamicCases
import com.materialkolor.builder.codegen.target.unstyled.UnstyledFrozenCases
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * One cases object, its cases by case name and the files it writes for each.
 */
internal class CaseSource(
    val name: String,
    val cases: Map<String, ExportInput>,
    val files: (case: String) -> List<GeneratedFile>,
)

/**
 * Every golden case that is a whole export, by case name, gathered from the cases object of each
 * target and from the snippets.
 *
 * The compile check lays each of these into a project of its own target, and [GoldenHashesTest]
 * holds the list to `GoldenHashes`.
 */
internal object GoldenCases {
    /**
     * The cases objects, each by the name the test failures use.
     */
    val sources: List<CaseSource> = listOf(
        CaseSource("Material3DynamicCases", Material3DynamicCases.all, Material3DynamicCases::files),
        CaseSource("Material3FrozenCases", Material3FrozenCases.all, Material3FrozenCases::files),
        CaseSource("UnstyledDynamicCases", UnstyledDynamicCases.all, UnstyledDynamicCases::files),
        CaseSource("UnstyledFrozenCases", UnstyledFrozenCases.all, UnstyledFrozenCases::files),
        CaseSource("FluentDynamicCases", FluentDynamicCases.all, FluentDynamicCases::files),
        CaseSource("FluentFrozenCases", FluentFrozenCases.all, FluentFrozenCases::files),
        CaseSource("CustomDynamicCases", CustomDynamicCases.all, CustomDynamicCases::files),
        CaseSource("CustomFrozenCases", CustomFrozenCases.all, CustomFrozenCases::files),
        CaseSource("SnippetsCases", SnippetsCases.all, SnippetsCases::files),
    )

    /**
     * Every export case, by case name.
     */
    val exports: Map<String, ExportInput> = buildMap {
        sources.forEach { source ->
            source.cases.forEach { (case, input) ->
                require(
                    put(case, input) == null,
                ) { "Golden case $case appears twice, the second time in ${source.name}" }
            }
        }
    }

    /**
     * The cases that are not a whole export, each hashed by the test that owns it.
     */
    val parts: Set<String> = setOf(
        // HeaderTest
        "header-default",
        // FormsCase
        "dsl-forms",
    )
}

class GoldenHashesTest {
    @Test
    fun goldenHashes_everyHash_belongsToACase() {
        val known = GoldenCases.exports.keys + GoldenCases.parts

        assertEquals(emptySet(), GoldenHashes.cases.keys - known, "hashes no cases object regenerates")
    }

    @Test
    fun goldenHashes_everyCase_hasAHash() {
        val known = GoldenCases.exports.keys + GoldenCases.parts

        assertEquals(emptySet(), known - GoldenHashes.cases.keys, "cases with no golden hash")
    }

    @Test
    fun goldenHashes_everyExportCase_matchesItsHash() {
        val mismatched = GoldenCases.sources.flatMap { source ->
            source.cases.keys
                .filter { case -> GoldenHashes.cases[case] != GoldenDigest.of(source.files(case)) }
                .map { case -> "$case in ${source.name}" }
        }

        assertEquals(emptyList(), mismatched, "cases whose files no longer match their golden hash")
    }
}
