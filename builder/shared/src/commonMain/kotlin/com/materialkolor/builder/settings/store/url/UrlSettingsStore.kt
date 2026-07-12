package com.materialkolor.builder.settings.store.url

import co.touchlab.kermit.Logger
import com.materialkolor.builder.core.DI
import com.materialkolor.builder.core.observePlatformQueryParams
import com.materialkolor.builder.core.readPlatformQueryParams
import com.materialkolor.builder.core.updatePlatformQueryParams
import com.materialkolor.builder.settings.DarkModeProvider
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.settings.store.SettingsStore
import com.materialkolor.builder.settings.store.entity.toEntity
import com.materialkolor.builder.settings.store.entity.toModel
import com.materialkolor.builder.settings.store.entity.toQueryParams
import com.materialkolor.builder.settings.store.entity.toSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UrlSettingsStore(
    private val darkModeProvider: DarkModeProvider = DI.darkModeProvider,
    scope: CoroutineScope = DI.defaultScope,
) : SettingsStore {

    private val store = MutableStateFlow<String?>(null)

    init {
        val params = readPlatformQueryParams()
        if (params != null) {
            Logger.d { "Setting initial params: $params" }
            store.update { params }
        }

        scope.launch {
            observePlatformQueryParams().collect { params ->
                Logger.d { "Detected new params: $params" }
                store.update { params }
            }
        }

        scope.launch {
            store.collect { params ->
                when {
                    params == readPlatformQueryParams() -> {
                        Logger.d { "Ignoring duplicate params" }
                    }
                    params != null -> {
                        Logger.d { "Setting params: $params" }
                        updatePlatformQueryParams(params)
                    }
                    else -> {
                        Logger.d { "Clearing params" }
                        updatePlatformQueryParams("")
                    }
                }
            }
        }
    }

    override fun get(): Flow<Settings> = store.mapNotNull { params ->
        val entity = params?.toSettingsEntity() ?: return@mapNotNull null
        entity.toModel(darkModeProvider.isDarkMode.value)
    }

    override suspend fun store(settings: Settings) {
        val entity = settings.toEntity()
        val params = entity.toQueryParams()
        store.update { params }
    }

    override fun clear() {
        store.update { null }
    }
}
