package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.web.interop.browserFile
import com.materialkolor.builder.web.interop.downloadBytes
import com.materialkolor.builder.web.interop.pageCanShare
import com.materialkolor.builder.web.interop.pageCanShareFiles
import com.materialkolor.builder.web.interop.pageCanShareLink
import com.materialkolor.builder.web.interop.shareBrowserFiles
import com.materialkolor.builder.web.interop.shareBrowserLink
import com.materialkolor.builder.web.interop.shareProbe
import org.khronos.webgl.toInt8Array

/**
 * Downloads through a Blob and an anchor, and the share sheet through the Web Share API.
 */
internal object WebFileSaver : FileSaver {
    override val canShareFiles: Boolean = pageCanShareFiles()
    override val canShareLink: Boolean
        get() = pageCanShareLink()

    override fun canShare(files: List<OutgoingFile>): Boolean =
        pageCanShare(files.map { file -> shareProbe(file.name, file.mime) })

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = outcome(downloadBytes(bytes.toInt8Array(), name, mime, REVOKE_AFTER_MILLIS))

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> {
        val shared = files.map { file -> browserFile(file.bytes.toInt8Array(), file.name, file.mime) }
        return outcome(shareBrowserFiles(shared))
    }

    override suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit> = outcome(shareBrowserLink(url, title))
}

private const val REVOKE_AFTER_MILLIS = 30_000
