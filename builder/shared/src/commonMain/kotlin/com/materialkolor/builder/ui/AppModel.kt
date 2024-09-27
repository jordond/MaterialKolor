package com.materialkolor.builder.ui

import com.materialkolor.builder.core.DI
import com.materialkolor.builder.core.UrlLauncher
import com.materialkolor.builder.settings.SettingsRepo
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.ktx.StateViewModel

class AppModel(
    settingsRepo: SettingsRepo = DI.settingsRepo,
    val urlLauncher: UrlLauncher = DI.urlLauncher,
) : StateViewModel<AppModel.State>(State(settingsRepo.settings.value)) {

    init {
        settingsRepo.settings.collectToState { state, value ->
            state.copy(settings = value)
        }
    }

    data class State(
        val settings: Settings,
    )
}