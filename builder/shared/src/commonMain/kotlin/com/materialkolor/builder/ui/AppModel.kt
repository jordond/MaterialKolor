package com.materialkolor.builder.ui

import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.DI
import com.materialkolor.builder.core.UrlLauncher
import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.SettingsRepo
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.ktx.StateViewModel
import kotlinx.coroutines.launch

class AppModel(
    private val settingsRepo: SettingsRepo = DI.settingsRepo,
    private val darkModeProvider: DarkModeProvider = DI.darkModeProvider,
    val urlLauncher: UrlLauncher = DI.urlLauncher,
    isDarkMode: Boolean,
) : StateViewModel<AppModel.State>(State(isDarkMode)) {

    init {
        settingsRepo.settings.collectToState { state, value ->
            state.copy(settings = value)
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
