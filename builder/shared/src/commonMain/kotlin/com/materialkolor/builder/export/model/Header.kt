package com.materialkolor.builder.export.model

import com.materialkolor.builder.BuildKonfig
import com.materialkolor.builder.core.baseUrl
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.settings.store.entity.toEntity
import com.materialkolor.builder.settings.store.entity.toQueryParams

fun header(settings: Settings) = """
// Generated using MaterialKolor Builder version ${BuildKonfig.VERSION_NAME} (${BuildKonfig.VERSION_CODE})
// ${settings.url()}

""".trimIndent()

private fun Settings.url(): String {
    return "$baseUrl/${toEntity().toQueryParams()}"
}
