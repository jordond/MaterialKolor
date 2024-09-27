package com.materialkolor.builder.core

import co.touchlab.kermit.Logger

interface UrlLauncher {

    fun launch(url: String)

    fun launch(url: UrlLink) = launch(url.value)
}

class DefaultUrlLauncher : UrlLauncher {

    private val logger = Logger.withTag("LinkLauncher")

    override fun launch(url: String) {
        try {
            logger.d { "Launching link: $url" }
            launchUrl(url)
        } catch (cause: Exception) {
            logger.e(cause) { "Failed to launch link: $url" }
        }
    }
}

internal expect fun launchUrl(url: String)