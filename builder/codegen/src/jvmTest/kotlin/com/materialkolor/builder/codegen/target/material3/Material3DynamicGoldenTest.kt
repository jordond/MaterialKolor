package com.materialkolor.builder.codegen.target.material3

import com.materialkolor.builder.codegen.GoldenHarness
import kotlin.test.Test

class Material3DynamicGoldenTest {
    @Test
    fun material3Dynamic_everyCase_matchesTheCheckedInFiles() {
        Material3DynamicCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, Material3DynamicCases.files(case))
        }
    }
}
