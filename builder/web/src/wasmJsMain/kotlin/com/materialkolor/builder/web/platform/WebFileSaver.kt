package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile

// stub
// B-301 downloads through FileKit and shares through the Web Share API.
internal object WebFileSaver : FileSaver {
    override val canShareFiles: Boolean = false

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = notInBrowserYet()

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> = notInBrowserYet()
}
