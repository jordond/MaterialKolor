package com.materialkolor.builder.ktx

import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale

fun String.snakeToCamelCase(): String =
    this.split("_").joinToString("") { string ->
        string.capitalize(Locale("EN"))
    }
