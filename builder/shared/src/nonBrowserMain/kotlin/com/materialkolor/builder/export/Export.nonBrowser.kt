package com.materialkolor.builder.export

import com.materialkolor.builder.export.model.ExportFile

actual suspend fun exportFiles(list: List<ExportFile>) {
    throw UnsupportedOperationException("Exporting files is not supported in non-browser environments")
}
