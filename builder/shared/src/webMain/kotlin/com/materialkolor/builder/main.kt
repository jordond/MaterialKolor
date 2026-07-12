package com.materialkolor.builder

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.materialkolor.builder.core.readPlatformQueryParams
import com.materialkolor.builder.settings.DESTINATION_QUERY_PARAM
import com.materialkolor.builder.settings.store.entity.splitQueryParams
import com.materialkolor.builder.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        val initialDestination = extractInitialDestination()
        App(initialDestination)
    }
}

private fun extractInitialDestination(): String? {
    val query = readPlatformQueryParams() ?: return null
    return query.splitQueryParams()[DESTINATION_QUERY_PARAM]
}
