package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.GoldenHarness
import com.materialkolor.builder.codegen.target.fluent.FluentDynamicCases
import com.materialkolor.builder.codegen.target.unstyled.UnstyledDynamicCases
import kotlin.test.Test

class DynamicTargetsGoldenTest {
    @Test
    fun unstyledDynamic_everyCase_matchesTheCheckedInFiles() {
        UnstyledDynamicCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, UnstyledDynamicCases.files(case))
        }
    }

    @Test
    fun fluentDynamic_everyCase_matchesTheCheckedInFiles() {
        FluentDynamicCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, FluentDynamicCases.files(case))
        }
    }
}
