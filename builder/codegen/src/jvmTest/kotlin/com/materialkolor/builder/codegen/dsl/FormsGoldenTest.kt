package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.GoldenHarness
import kotlin.test.Test

class FormsGoldenTest {
    @Test
    fun dslForms_golden_matchesTheCheckedInFile() {
        GoldenHarness.Default.verify("dsl-forms", formsCaseFiles())
    }
}
