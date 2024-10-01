package com.materialkolor.builder.core

import com.materialkolor.builder.settings.store.DefaultSettingsStore
import com.materialkolor.builder.settings.store.SettingsStore

actual fun DI.provideSettingsStore(): SettingsStore {
    return DefaultSettingsStore()
}
