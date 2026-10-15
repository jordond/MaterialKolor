package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.GoldenHarness
import kotlin.test.Test

class SnippetsGoldenTest {
    @Test
    fun snippets_everyCase_matchesTheCheckedInFiles() {
        SnippetsCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, SnippetsCases.files(case))
        }
    }
}
