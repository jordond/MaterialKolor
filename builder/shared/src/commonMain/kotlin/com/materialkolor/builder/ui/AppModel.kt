package com.materialkolor.builder.ui

import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.DI
import com.materialkolor.builder.core.UrlLauncher
import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.SettingsRepo
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.version.MaterialKolorVersionService
import dev.stateholder.extensions.viewmodel.StateViewModel
import kotlinx.coroutines.launch

class AppModel(
    private val settingsRepo: SettingsRepo = DI.settingsRepo,
    private val darkModeProvider: DarkModeProvider = DI.darkModeProvider,
    private val versionService: MaterialKolorVersionService = DI.versionService,
    val urlLauncher: UrlLauncher = DI.urlLauncher,
    isDarkMode: Boolean,
) : StateViewModel<AppModel.State>(State(isDarkMode)) {

    init {
        settingsRepo.settings.mergeState { state, value ->
            state.copy(settings = value)
        }

        viewModelScope.launch {
            versionService.fetchVersions()
        }
    }

    fun updateDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(isDarkMode = isDarkMode) }
            darkModeProvider.initialize(isDarkMode)
        }
    }

    data class State(
        val settings: Settings,
    ) {
        constructor(isDarkMode: Boolean) : this(Settings.Default.copy(isDarkMode = isDarkMode))
    }
}
