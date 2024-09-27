package com.materialkolor.builder.settings.store

import com.materialkolor.builder.core.DI
import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.model.ColorSettings
import com.materialkolor.builder.settings.model.ImagePresets
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.settings.store.SettingsStore.Companion.defaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface SettingsStore {

    fun get(): Flow<Settings>

    suspend fun store(settings: Settings)

    fun clear()

    companion object {

        fun defaults(isDarkMode: Boolean): Settings {
            val image = ImagePresets.all.first()
            return Settings(
                selectedImage = image,
                colors = ColorSettings(seed = image.color),
                isDarkMode = isDarkMode,
            )
        }
    }
}

class DefaultSettingsStore(
    private val darkModeProvider: DarkModeProvider = DI.darkModeProvider,
    scope: CoroutineScope = DI.defaultScope,
) : SettingsStore {

    private val _settings = MutableStateFlow<Settings?>(null)

    init {
        scope.launch {
            darkModeProvider.isDarkMode.collect { isDarkMode ->
                _settings.update { settings ->
                    settings?.copy(isDarkMode = isDarkMode)
                }
            }
        }
    }

    override fun get(): Flow<Settings> {
        return _settings.mapNotNull { it }
    }

    override suspend fun store(settings: Settings) {
        _settings.update { settings }
    }

    override fun clear() {
        _settings.update { defaults(darkModeProvider.isDarkMode.value) }
    }
}