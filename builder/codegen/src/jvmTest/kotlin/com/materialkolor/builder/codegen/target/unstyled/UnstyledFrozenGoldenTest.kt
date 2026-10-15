package com.materialkolor.builder.codegen.target.unstyled

import com.materialkolor.builder.codegen.GoldenHarness
import kotlin.test.Test

class UnstyledFrozenGoldenTest {
    @Test
    fun unstyledFrozen_everyCase_matchesTheCheckedInFiles() {
        UnstyledFrozenCases.all.keys.forEach { case ->
            GoldenHarness.Default.verify(case, UnstyledFrozenCases.files(case))
        }
    }
}
