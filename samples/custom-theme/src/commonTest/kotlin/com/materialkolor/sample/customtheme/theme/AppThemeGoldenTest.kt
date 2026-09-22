package com.materialkolor.sample.customtheme.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeGoldenTest {
    @Test
    fun everySlot_matchesTheRecordedTable() {
        assertEquals(
            expected = appThemeGoldens.trim(),
            actual = renderAppThemeGoldens().trim(),
            message = "A tone choice moved. Read the diff, and if the change was intended, " +
                "regenerate the table with AppThemeGoldenWriter.",
        )
    }
}
