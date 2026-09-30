package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.Volatile

/**
 * A [FileSaver] that keeps every file it was handed.
 */
internal class FakeFileSaver(
    canShareFiles: Boolean = false,
    canShareLink: Boolean = false,
) : FileSaver {
    @Volatile
    override var canShareFiles: Boolean = canShareFiles

    @Volatile
    override var canShareLink: Boolean = canShareLink

    /**
     * Every file saved, oldest first.
     */
    val saved: List<OutgoingFile>
        get() = savedFiles.value

    private val savedFiles = MutableStateFlow<List<OutgoingFile>>(emptyList())

    /**
     * Every batch handed to the share sheet, oldest first.
     */
    val shared: List<List<OutgoingFile>>
        get() = sharedFiles.value

    private val sharedFiles = MutableStateFlow<List<List<OutgoingFile>>>(emptyList())

    /**
     * When set, every save and share fails with it and nothing is kept.
     */
    @Volatile
    var failure: Throwable? = null

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = attempt { savedFiles.update { saved -> saved + OutgoingFile(name, bytes, mime) } }

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> =
        attempt { sharedFiles.update { shared -> shared.plusElement(files) } }

    /**
     * Every link handed to the share sheet, with its title, oldest first.
     */
    val sharedLinks: List<Pair<String, String>>
        get() = links.value

    private val links = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    override suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit> = attempt { links.update { sharedLinks -> sharedLinks + (url to title) } }

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
