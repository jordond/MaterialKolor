package com.materialkolor.builder.export.model.library

import com.materialkolor.builder.export.model.ExportFile
import com.materialkolor.builder.export.model.ExportOptions
import dev.snipme.highlights.model.SyntaxLanguage

fun ExportOptions.createMaterialKolorFiles(): List<ExportFile> {
    val libs = if (useVersionCatalog) libsVersionsToml() else null
    val gradle = gradleKts(isMultiplatform = multiplatform, useVersionCatalog = useVersionCatalog)
    val colors = mkColorsKt(packageName = packageName, colors = settings.colors)
    val theme = mkThemeKt(
        packageName = packageName,
        themeName = themeName,
        animate = animate,
        settings = settings,
    )

    return listOfNotNull(
        libs?.let { content ->
            ExportFile(
                name = "libs.versions.toml",
                content = content,
                language = SyntaxLanguage.DEFAULT,
            )
        },
        ExportFile(name = "build.gradle.kts", content = gradle),
        ExportFile(name = "Color.kt", content = colors),
        ExportFile(name = "Theme.kt", content = theme),
    )
}
