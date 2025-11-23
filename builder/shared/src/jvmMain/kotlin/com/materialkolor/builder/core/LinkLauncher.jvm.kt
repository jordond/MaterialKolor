package com.materialkolor.builder.core

import java.awt.Desktop
import java.net.URI

internal actual fun launchUrl(url: String) {
    val uri = URI(url)
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
    when {
        desktop != null && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(uri)
        else -> Runtime.getRuntime().exec(arrayOf("xdg-open", uri.toString()))
    }
}