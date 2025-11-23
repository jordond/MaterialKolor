package com.materialkolor.builder.export.model

import com.materialkolor.builder.export.model.library.createMaterialKolorFiles
import com.materialkolor.builder.export.model.standard.createStandardFiles
import com.materialkolor.builder.settings.model.Settings
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

enum class ExportType(val displayName: String) {
    MaterialKolor("Material Kolor"),
    Standard("Standard"),
}

data class ExportOptions(
    val type: ExportType,
    val settings: Settings,
    val multiplatform: Boolean = DEFAULT_MULTIPLATFORM,
    val themeName: String = DEFAULT_THEME_NAME,
    val useVersionCatalog: Boolean = DEFAULT_USE_VERSION_CATALOG,
    val animate: Boolean = DEFAULT_ANIMATE,
) {

    fun createFiles(materialKolorVersion: String): PersistentList<ExportFile> = when (type) {
        ExportType.MaterialKolor -> createMaterialKolorFiles(materialKolorVersion)
        ExportType.Standard -> createStandardFiles()
    }.toPersistentList()

    fun toggleType(): ExportOptions = copy(
        type = when (type) {
            ExportType.MaterialKolor -> ExportType.Standard
            ExportType.Standard -> ExportType.MaterialKolor
        },
    )

    companion object {

        const val DEFAULT_THEME_NAME = "AppTheme"
        const val DEFAULT_MULTIPLATFORM = true
        const val DEFAULT_USE_VERSION_CATALOG = true
        const val DEFAULT_ANIMATE = true

        fun default(settings: Settings) = ExportOptions(
            type = ExportType.MaterialKolor,
            settings = settings,
        )
    }
}
