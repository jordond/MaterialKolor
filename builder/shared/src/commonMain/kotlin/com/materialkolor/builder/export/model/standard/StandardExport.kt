package com.materialkolor.builder.export.model.standard

import com.materialkolor.builder.export.model.ExportFile
import com.materialkolor.builder.export.model.ExportOptions

fun ExportOptions.createStandardFiles(): List<ExportFile> {
    val colors = standardColorsKt(settings = settings)
    val theme = standardThemeKt(
        themeName = themeName,
        multiplatform = multiplatform,
        settings = settings,
    )

    return listOf(
        ExportFile("Color.kt", colors),
        ExportFile("Theme.kt", theme),
    )
}
