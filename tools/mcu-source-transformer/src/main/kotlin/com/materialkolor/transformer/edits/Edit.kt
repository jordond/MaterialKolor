package com.materialkolor.transformer.edits

import com.materialkolor.transformer.rules.Rule

internal data class Edit(
    val start: Int,
    val end: Int,
    val replacement: String,
    val rule: Rule,
)
