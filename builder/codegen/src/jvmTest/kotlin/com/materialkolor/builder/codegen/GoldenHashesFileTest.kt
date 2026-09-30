package com.materialkolor.builder.codegen

import kotlin.test.Test

class GoldenHashesFileTest {
    @Test
    fun goldenHashes_checkedInFile_matchesTheGoldenTree() {
        GoldenHarness.Default.verifyHashes()
    }
}
