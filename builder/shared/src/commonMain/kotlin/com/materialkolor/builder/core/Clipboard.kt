package com.materialkolor.builder.core

import co.touchlab.kermit.Logger

interface Clipboard {

    fun copy(text: String): Boolean
}

class DefaultClipboard : Clipboard {

    private val logger = Logger.withTag("LinkLauncher")

    override fun copy(text: String): Boolean {
        return try {
            logger.d { "Copying text to clipboard: $text" }
            copyTextToClipboard(text)
            true
        } catch (cause: Exception) {
            logger.e(cause) { "Failed copying text to clipboard" }
            false
        }
    }
}

internal expect fun copyTextToClipboard(text: String)