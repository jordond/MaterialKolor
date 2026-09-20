package com.materialkolor.transformer.edits

internal data class Result(
    val text: String,
    val edits: List<Edit>,
)
