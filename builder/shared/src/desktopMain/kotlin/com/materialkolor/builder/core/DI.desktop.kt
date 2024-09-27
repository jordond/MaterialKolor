package com.materialkolor.builder.core

import com.materialkolor.builder.settings.store.SettingsStore
import com.materialkolor.builder.settings.store.url.UrlSettingsStore

actual fun DI.provideSettingsStore(): SettingsStore {
    return UrlSettingsStore()
}