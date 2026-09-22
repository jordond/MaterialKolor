package com.materialkolor.sample.customtheme.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeGoldenTest {
    @Test
    fun everySlot_matchesTheRecordedTable() {
        assertEquals(
            expected = appThemeGoldens.trim(),
            actual = renderAppThemeGoldens().trim(),
            message = "A tone choice moved. Review the diff, then regenerate on purpose, " +
                "see AppThemeGoldenWriter.",
        )
    }
}
