package com.materialkolor.builder.core.data

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {
    private val stores = InMemoryStoreFactory()

    @Test
    fun updateExportPrefs_oneTarget_leavesTheOthersAlone() =
        runTest {
            val repository = PreferencesRepository(stores, backgroundScope)

            val error = repository.updateExportPrefs(ExportTarget.Fluent) { prefs ->
                prefs.copy(packageName = "com.fluent.theme")
            }

            error shouldBe null
            repository.exportPrefs(ExportTarget.Fluent).packageName shouldBe "com.fluent.theme"
            repository.exportPrefs(ExportTarget.Material3) shouldBe ExportPrefs()
        }

    @Test
    fun exportPrefs_newRepositoryOverTheSameStores_readsWhatWasSaved() =
        runTest {
            PreferencesRepository(stores, backgroundScope).updateExportPrefs(ExportTarget.Custom) { prefs ->
                prefs.copy(mode = ExportMode.Frozen)
            }

            val reloaded = PreferencesRepository(stores, backgroundScope)
            runCurrent()

            reloaded.exportPrefs(ExportTarget.Custom).mode shouldBe ExportMode.Frozen
            reloaded.preferences.value
                .exportPrefsFor(ExportTarget.Custom)
                .mode shouldBe ExportMode.Frozen
        }

    @Test
    fun preferences_afterUpdate_holdTheNewValue() =
        runTest {
            val repository = PreferencesRepository(stores, backgroundScope)

            repository.update { prefs -> prefs.copy(seedLock = true) } shouldBe null
            runCurrent()

            repository.preferences.value.seedLock shouldBe true
        }
}
