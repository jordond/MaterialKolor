package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile

/**
 * A [FileSaver] that keeps every file it was handed.
 */
internal class FakeFileSaver(
    override var canShareFiles: Boolean = false,
    override var canShareLink: Boolean = false,
) : FileSaver {
    /**
     * Every file saved, oldest first.
     */
    val saved: MutableList<OutgoingFile> = mutableListOf()

    /**
     * Every batch handed to the share sheet, oldest first.
     */
    val shared: MutableList<List<OutgoingFile>> = mutableListOf()

    /**
     * When set, every save and share fails with it and nothing is kept.
     */
    var failure: Throwable? = null

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = attempt { saved += OutgoingFile(name, bytes, mime) }

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> = attempt { shared += files }

    /**
     * Every link handed to the share sheet, with its title, oldest first.
     */
    val sharedLinks: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit> = attempt { sharedLinks += url to title }

    /**
     * Follows [canShareFiles], so a test that turns sharing on gets it for every batch.
     */
    override fun canShare(files: List<OutgoingFile>): Boolean = canShareFiles

    private inline fun attempt(keep: () -> Unit): Result<Unit> {
        failure?.let { error -> return Result.failure(error) }
        keep()
        return Result.success(Unit)
    }
}
