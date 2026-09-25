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
 *
 * Both only work while the page handles a click in Safari, and sharing needs one everywhere. Each
 * starts before its function first suspends, so start them undispatched from the click handler.
 */
internal object WebFileSaver : FileSaver {
    /**
     * Whether the share sheet takes a zip and a Kotlin file, the kinds an export makes.
     */
    override val canShareFiles: Boolean = pageCanShareFiles()

    override fun canShare(files: List<OutgoingFile>): Boolean =
        pageCanShare(files.map { file -> shareProbe(file.name, file.mime) })

    /**
     * Start a download. Success means the browser took it, since a page never learns where it went.
     */
    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = outcome(downloadBytes(bytes.toInt8Array(), name, mime, REVOKE_AFTER_MILLIS))

    /**
     * Open the share sheet with [files], or fail when `navigator.canShare` turns them down. Chromium
     * only shares images, text and a few media types, so the click asks [canShare] first and sends a
     * zip to [save] instead.
     */
    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> {
        val shared = files.map { file -> browserFile(file.bytes.toInt8Array(), file.name, file.mime) }
        return outcome(shareBrowserFiles(shared))
    }

    /**
     * Asked each time, since a convertible can swap its finger for a mouse while the page is open.
     */
    override val canShareLink: Boolean
        get() = pageCanShareLink()

    /**
     * Open the share sheet with the link, or fail when the page has none. A dismissed sheet is success.
     */
    override suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit> = outcome(shareBrowserLink(url, title))
}

// Long enough for Safari to finish reading a large zip, short enough not to hold it for the session.
private const val REVOKE_AFTER_MILLIS = 30_000
