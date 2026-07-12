package com.materialkolor.builder.core

import com.materialkolor.builder.export.DefaultExportRepo
import com.materialkolor.builder.export.ExportRepo
import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.DefaultDarkModeProvider
import com.materialkolor.builder.settings.DefaultSettingsRepo
import com.materialkolor.builder.settings.SettingsRepo
import com.materialkolor.builder.settings.store.SettingsStore
import com.materialkolor.builder.version.DefaultMaterialKolorVersionService
import com.materialkolor.builder.version.DefaultVersionCache
import com.materialkolor.builder.version.MaterialKolorVersionService
import com.materialkolor.builder.version.VersionCache
import com.materialkolor.builder.version.createVersionCacheStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object DI {

    val defaultScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    val urlLauncher: UrlLauncher = DefaultUrlLauncher()

    val clipboard: Clipboard = DefaultClipboard()

    val darkModeProvider: DarkModeProvider by lazy {
        DefaultDarkModeProvider()
    }

    val settingsStore: SettingsStore by lazy {
        provideSettingsStore()
    }

    val settingsRepo: SettingsRepo by lazy {
        DefaultSettingsRepo(darkModeProvider, settingsStore, defaultScope)
    }

    val exportRepo: ExportRepo = DefaultExportRepo()

    val versionCache: VersionCache by lazy {
        DefaultVersionCache(createVersionCacheStore())
    }

    val versionService: MaterialKolorVersionService by lazy {
        DefaultMaterialKolorVersionService(versionCache)
    }
}

expect fun DI.provideSettingsStore(): SettingsStore
