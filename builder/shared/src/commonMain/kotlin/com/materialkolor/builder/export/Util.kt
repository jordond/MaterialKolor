package com.materialkolor.builder.export

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.materialkolor.ktx.toHex

fun Color?.variable(name: String): String? {
    if (this == null) return null
    return "val ${name.capitalize(Locale("EN"))} = ${string()}"
}

private fun Color.string(): String {
    val hex = toHex(alwaysIncludeAlpha = true, includePrefix = false)
    return "Color(0x$hex)"
}
