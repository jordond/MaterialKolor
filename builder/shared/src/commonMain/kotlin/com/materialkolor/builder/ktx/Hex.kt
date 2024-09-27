package com.materialkolor.builder.ktx

import androidx.compose.ui.graphics.Color

private val hexPattern = Regex("^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")

fun String.parseHexToColor(): Color? {
    val matchResult = hexPattern.find(this)
    return matchResult?.groupValues?.get(1)?.let { hex ->
        try {
            when (hex.length) {
                6 -> Color(hex.toLong(16) or 0xFF000000)
                8 -> Color(hex.toLong(16).toInt())
                else -> null
            }
        } catch (exception: NumberFormatException) {
            null
        }
    }
}