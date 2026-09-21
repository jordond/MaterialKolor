package com.materialkolor.transformer

internal enum class Mode(
    val namespace: String,
) {
    Library("com.materialkolor"),
    Reference("upstream.kotlin"),
}
