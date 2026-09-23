package com.materialkolor.builder.core.data

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.StorageKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The browser's [Preferences], shared by every project.
 *
 * The export options of every target live inside the one preferences record, so they load and save
 * together with everything else.
 *
 * @param[stores] Where the preferences are kept.
 * @param[scope] Keeps [preferences] up to date for as long as it runs.
 */
internal class PreferencesRepository(
    stores: StoreFactory,
    scope: CoroutineScope,
) {
    private val store = stores.create(StorageKeys.PREFS, Preferences.Codec, Preferences())

    /** The preferences now and after every change, including one made in another tab. */
    val preferences: StateFlow<Preferences> = store.data.stateIn(scope, SharingStarted.Eagerly, Preferences())

    /**
     * Replace the preferences with what [block] makes of them.
     *
     * Returns the reason the write did not land, or null when it did. Unlike a project write, a full
     * storage comes back as [StoreError.QuotaExceeded] straight away, with nothing pruned and no
     * second try. B-215 decides whether preference writes go through the project repository's prune.
     */
    suspend fun update(block: (Preferences) -> Preferences): StoreError? = store.update(block)

    /** The options [target] was last exported with, or the defaults when it never was. */
    suspend fun exportPrefs(target: ExportTarget): ExportPrefs = store.get().exportPrefsFor(target)

    /**
     * Remember what [block] makes of the export options of [target], leaving every other target
     * alone.
     *
     * Returns the reason the write did not land, or null when it did, the same way [update] does.
     */
    suspend fun updateExportPrefs(
        target: ExportTarget,
        block: (ExportPrefs) -> ExportPrefs,
    ): StoreError? = store.update { prefs -> prefs.withExportPrefs(target, block(prefs.exportPrefsFor(target))) }
}
