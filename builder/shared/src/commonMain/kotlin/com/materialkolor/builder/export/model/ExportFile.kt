package com.materialkolor.builder.export.model

import androidx.compose.runtime.Stable
import dev.snipme.highlights.model.SyntaxLanguage

@Stable
data class ExportFile(
    val name: String,
    val content: String,
    val language: SyntaxLanguage = SyntaxLanguage.KOTLIN,
)
