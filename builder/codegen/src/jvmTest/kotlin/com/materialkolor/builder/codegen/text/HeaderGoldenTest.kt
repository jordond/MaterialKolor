package com.materialkolor.builder.codegen.text

import com.materialkolor.builder.codegen.Fixtures
import com.materialkolor.builder.codegen.GoldenHarness
import kotlin.test.Test

class HeaderGoldenTest {
    @Test
    fun headerDefault_golden_matchesTheCheckedInFile() {
        GoldenHarness.Default.verify("header-default", headerCaseFiles(Fixtures.Default.input))
    }
}
