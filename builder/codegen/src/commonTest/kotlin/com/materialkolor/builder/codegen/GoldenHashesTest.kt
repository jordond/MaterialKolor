package com.materialkolor.builder.codegen

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
    private val sources: Map<String, Map<String, ExportInput>> = mapOf(
        "Material3DynamicCases" to Material3DynamicCases.all,
        "Material3FrozenCases" to Material3FrozenCases.all,
        "UnstyledDynamicCases" to UnstyledDynamicCases.all,
        "UnstyledFrozenCases" to UnstyledFrozenCases.all,
        "FluentDynamicCases" to FluentDynamicCases.all,
        "FluentFrozenCases" to FluentFrozenCases.all,
        "CustomDynamicCases" to CustomDynamicCases.all,
        "CustomFrozenCases" to CustomFrozenCases.all,
        "SnippetsCases" to SnippetsCases.all,
    )

    /**
     * Every export case, by case name.
     */
    val exports: Map<String, ExportInput> = buildMap {
        sources.forEach { (source, cases) ->
            cases.forEach { (case, input) ->
                require(put(case, input) == null) { "Golden case $case appears twice, the second time in $source" }
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
}
