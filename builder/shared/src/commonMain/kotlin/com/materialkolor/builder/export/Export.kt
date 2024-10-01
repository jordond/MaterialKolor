package com.materialkolor.builder.export

import com.materialkolor.builder.export.model.ExportFile

expect suspend fun exportFiles(list: List<ExportFile>)
