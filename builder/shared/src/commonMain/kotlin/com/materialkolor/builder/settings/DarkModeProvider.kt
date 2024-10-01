package com.materialkolor.builder.settings

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface DarkModeProvider {

    val isDarkMode: StateFlow<Boolean>

    fun initialize(isDarkMode: Boolean)
}

class DefaultDarkModeProvider : DarkModeProvider {

    private val _isDarkMode = MutableStateFlow(false)
    override var isDarkMode = _isDarkMode.asStateFlow()

    override fun initialize(isDarkMode: Boolean) {
        Logger.i { "Initializing dark mode to $isDarkMode" }
        _isDarkMode.update { isDarkMode }
    }
}
