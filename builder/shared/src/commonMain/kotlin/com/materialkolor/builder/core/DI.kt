package com.materialkolor.builder.core

import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.DefaultDarkModeProvider
import com.materialkolor.builder.settings.DefaultSettingsRepo
import com.materialkolor.builder.settings.SettingsRepo
import com.materialkolor.builder.settings.store.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object DI {

    val defaultScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    val urlLauncher: UrlLauncher = DefaultUrlLauncher()

    val clipboard: Clipboard = DefaultClipboard()

    val darkModeProvider: DarkModeProvider = DefaultDarkModeProvider()

    val settingsStore: SettingsStore by lazy {
        provideSettingsStore()
    }

    val settingsRepo: SettingsRepo by lazy {
        DefaultSettingsRepo(darkModeProvider, settingsStore, defaultScope)
    }
}

expect fun DI.provideSettingsStore(): SettingsStore