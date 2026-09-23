package com.materialkolor.builder.domain.persist

import com.materialkolor.builder.domain.model.Library
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferencesTest {
    @Test
    fun preferences_defaults_matchTheSpec() {
        val prefs = Preferences()

        assertEquals(Appearance.System, prefs.appearance)
        assertEquals(MotionOverride.System, prefs.motion)
        assertEquals(false, prefs.hueLock)
        assertEquals(true, prefs.styleLock)
        assertEquals(false, prefs.seedLock)
        assertEquals(emptySet(), prefs.dismissedHints)
        assertEquals(false, prefs.firstExportDone)
        assertEquals(false, prefs.posterCollapsed)
        assertNull(prefs.lastProjectId)
        assertEquals(false, prefs.persistRequested)
        assertEquals(emptyMap(), prefs.exportPrefs)
    }

    @Test
    fun exportPrefs_defaults_matchTheSpec() {
        val prefs = ExportPrefs()

        assertEquals("com.example.theme", prefs.packageName)
        assertEquals(true, prefs.multiplatform)
        assertEquals(true, prefs.versionCatalog)
        assertEquals(ExportMode.Dynamic, prefs.mode)
        assertEquals(false, prefs.animate)
        assertEquals(300, prefs.animationDurationMs)
        assertEquals(FrozenVariants.StandardOnly, prefs.frozenVariants)
        assertEquals(false, prefs.androidDynamicColor)
    }

    @Test
    fun exportTarget_of_coversEveryLibraryAndFlag() {
        val expected = mapOf(
            (Library.Material3 to false) to ExportTarget.Material3,
            (Library.Material3 to true) to ExportTarget.Material3Expressive,
            (Library.Unstyled to false) to ExportTarget.Unstyled,
            (Library.Unstyled to true) to ExportTarget.Unstyled,
            (Library.Fluent to false) to ExportTarget.Fluent,
            (Library.Fluent to true) to ExportTarget.Fluent,
            (Library.Custom to false) to ExportTarget.Custom,
            (Library.Custom to true) to ExportTarget.Custom,
        )
        val actual = Library.entries
            .flatMap { library -> listOf(library to false, library to true) }
            .associateWith { (library, expressive) -> ExportTarget.of(library, expressive) }

        assertEquals(expected, actual)
        assertEquals(ExportTarget.entries.toSet(), actual.values.toSet())
    }

    @Test
    fun exportPrefsFor_targetNeverExported_givesTheDefaults() {
        val prefs = Preferences().withExportPrefs(ExportTarget.Fluent, ExportPrefs(packageName = "com.fluent"))

        assertEquals(ExportPrefs(), prefs.exportPrefsFor(ExportTarget.Material3))
        assertEquals(ExportPrefs(packageName = "com.fluent"), prefs.exportPrefsFor(ExportTarget.Fluent))
    }

    @Test
    fun withExportPrefs_oneTarget_leavesTheOthersAlone() {
        val fixtures = PersistFixtures()
        val before = fixtures.preferences()
        val changed = ExportPrefs(packageName = "com.changed")

        val after = before.withExportPrefs(ExportTarget.Custom, changed)

        ExportTarget.entries.forEach { target ->
            val expected = if (target == ExportTarget.Custom) changed else before.exportPrefsFor(target)
            assertEquals(expected, after.exportPrefsFor(target))
        }
    }
}
