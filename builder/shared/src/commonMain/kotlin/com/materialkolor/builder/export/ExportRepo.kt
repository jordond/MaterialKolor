package com.materialkolor.builder.export

import co.touchlab.kermit.Logger
import com.materialkolor.builder.core.exportSupported
import com.materialkolor.builder.export.model.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

interface ExportRepo {
    suspend fun export(options: ExportOptions): Boolean
}

class DefaultExportRepo : ExportRepo {

    override suspend fun export(options: ExportOptions): Boolean {
        if (!exportSupported) {
            Logger.e { "Export is not supported" }
            return false
        }

        try {
            Logger.d { "Exporting ${options.type.displayName} theme" }
            Logger.d { "Files to export: ${options.files.joinToString { it.name }}" }
            withContext(Dispatchers.Default) {
                exportFiles(options.files)
            }
            return true
        } catch (cause: Exception) {
            if (cause is CancellationException) throw cause
            Logger.e(cause) { "Export failed" }
            return false
        }
    }
}
