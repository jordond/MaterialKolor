package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.web.interop.browserFile
import com.materialkolor.builder.web.interop.downloadBytes
import com.materialkolor.builder.web.interop.pageCanShareFiles
import com.materialkolor.builder.web.interop.shareBrowserFiles
import org.khronos.webgl.toInt8Array

/**
 * Downloads through a Blob and an anchor, and the share sheet through the Web Share API.
 *
 * Both only work while the page handles a click in Safari, and sharing needs one everywhere. Each
 * starts before its function first suspends, so start them undispatched from the click handler.
 */
internal object WebFileSaver : FileSaver {
    override val canShareFiles: Boolean = pageCanShareFiles()

    /** Start a download. Success means the browser took it, since a page never learns where it went. */
    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = outcome(downloadBytes(bytes.toInt8Array(), name, mime, REVOKE_AFTER_MILLIS))

    /**
     * Open the share sheet with [files], or fail when `navigator.canShare` turns them down. Chromium
     * only shares images, text and a few media types, so a zip goes to [save] instead.
     */
    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> {
        val shared = files.map { file -> browserFile(file.bytes.toInt8Array(), file.name, file.mime) }
        return outcome(shareBrowserFiles(shared))
    }
}

// Long enough for Safari to finish reading a large zip, short enough not to hold it for the session.
private const val REVOKE_AFTER_MILLIS = 30_000
