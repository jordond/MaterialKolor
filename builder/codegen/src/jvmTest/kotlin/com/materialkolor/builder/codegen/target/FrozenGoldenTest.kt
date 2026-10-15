package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.GoldenHarness
import com.materialkolor.builder.codegen.target.custom.CustomFrozenCases
import com.materialkolor.builder.codegen.target.fluent.FluentFrozenCases
import com.materialkolor.builder.codegen.target.material3.Material3FrozenCases
import kotlin.test.Test

class FrozenGoldenTest {
    @Test
    fun material3Frozen_everyCase_matchesTheCheckedInFiles() {
        Material3FrozenCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, Material3FrozenCases.files(case))
        }
    }

    @Test
    fun fluentFrozen_everyCase_matchesTheCheckedInFiles() {
        FluentFrozenCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, FluentFrozenCases.files(case))
        }
    }

    @Test
    fun customFrozen_everyCase_matchesTheCheckedInFiles() {
        CustomFrozenCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, CustomFrozenCases.files(case))
        }
    }
}
