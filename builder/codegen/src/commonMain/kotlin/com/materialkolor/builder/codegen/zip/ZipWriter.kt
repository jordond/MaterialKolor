package com.materialkolor.builder.codegen.zip

/**
 * Writes a zip archive whose entries are stored as they are, without compression.
 *
 * An export is a handful of small text files, so compressing them saves next to nothing and would
 * need a deflate that behaves the same on every platform. Every entry carries the same fixed
 * timestamp and its name is marked as UTF-8, so the same entries always give the same bytes.
 * Folders are implied by the entry paths rather than written as entries of their own, which every
 * common unzip tool accepts.
 */
internal object ZipWriter {
    /**
     * One file in the archive.
     *
     * @property[path] Where the file sits in the archive, with forward slashes, as in `AppTheme/README.md`.
     * @property[bytes] What the file holds.
     */
    class Entry(
        val path: String,
        val bytes: ByteArray,
    ) {
        init {
            require(path.isNotEmpty() && !path.startsWith("/") && !path.endsWith("/")) {
                "A zip entry needs a relative file path, got '$path'"
            }
            require('\\' !in path && path.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
                "A zip entry path cannot step outside the archive or leave a part empty, got '$path'"
            }
        }
    }

    /**
     * The archive holding [entries] in the order given.
     */
    fun write(entries: List<Entry>): ByteArray {
        require(entries.size < ZIP64_ENTRY_COUNT) {
            "A zip without the 64 bit extension holds fewer than $ZIP64_ENTRY_COUNT entries"
        }
        require(entries.map { it.path }.distinct().size == entries.size) { "A zip entry path appears twice" }

        val stored = entries.map { entry -> Stored(entry.path.encodeToByteArray(), entry.bytes, Crc32.of(entry.bytes)) }
        stored.forEach { entry ->
            require(entry.name.size <= MAX_SHORT) { "A zip entry path is longer than $MAX_SHORT bytes" }
        }
        val localSize = stored.sumOf { entry -> LOCAL_HEADER_SIZE.toLong() + entry.name.size + entry.data.size }
        val centralSize = stored.sumOf { entry -> CENTRAL_HEADER_SIZE.toLong() + entry.name.size }
        val totalSize = localSize + centralSize + END_RECORD_SIZE
        require(totalSize <= Int.MAX_VALUE) { "A zip this large needs the 64 bit extension" }

        val out = ByteWriter(ByteArray(totalSize.toInt()))
        val offsets = stored.map { entry ->
            val offset = out.position
            out.localHeader(entry)
            out.bytes(entry.name)
            out.bytes(entry.data)
            offset
        }
        val centralStart = out.position
        stored.forEachIndexed { index, entry ->
            out.centralHeader(entry, offsets[index])
            out.bytes(entry.name)
        }
        out.endRecord(count = stored.size, centralSize = out.position - centralStart, centralStart = centralStart)

        return out.result()
    }

    private class Stored(
        val name: ByteArray,
        val data: ByteArray,
        val crc: Int,
    )

    private fun ByteWriter.localHeader(entry: Stored) {
        int(LOCAL_HEADER_SIGNATURE)
        short(VERSION_NEEDED)
        short(UTF8_NAMES_FLAG)
        short(METHOD_STORED)
        short(DOS_TIME)
        short(DOS_DATE)
        int(entry.crc)
        int(entry.data.size)
        int(entry.data.size)
        short(entry.name.size)
        short(0)
    }

    private fun ByteWriter.centralHeader(
        entry: Stored,
        offset: Int,
    ) {
        int(CENTRAL_HEADER_SIGNATURE)
        short(VERSION_NEEDED)
        short(VERSION_NEEDED)
        short(UTF8_NAMES_FLAG)
        short(METHOD_STORED)
        short(DOS_TIME)
        short(DOS_DATE)
        int(entry.crc)
        int(entry.data.size)
        int(entry.data.size)
        short(entry.name.size)
        // The extra field and comment lengths, then the disk number and the internal attributes.
        short(0)
        short(0)
        short(0)
        short(0)
        // The external attributes.
        int(0)
        int(offset)
    }

    private fun ByteWriter.endRecord(
        count: Int,
        centralSize: Int,
        centralStart: Int,
    ) {
        int(END_RECORD_SIGNATURE)
        // This disk and the disk the central directory starts on, both the only one.
        short(0)
        short(0)
        short(count)
        short(count)
        int(centralSize)
        int(centralStart)
        // No archive comment.
        short(0)
    }

    private const val LOCAL_HEADER_SIGNATURE = 0x04034B50
    private const val CENTRAL_HEADER_SIGNATURE = 0x02014B50
    private const val END_RECORD_SIGNATURE = 0x06054B50
    private const val LOCAL_HEADER_SIZE = 30
    private const val CENTRAL_HEADER_SIZE = 46
    private const val END_RECORD_SIZE = 22

    /**
     * Version 2.0, the first that knows folders, which is all a stored archive needs.
     */
    private const val VERSION_NEEDED = 20

    /**
     * General purpose bit 11, the names are UTF-8.
     */
    private const val UTF8_NAMES_FLAG = 0x0800
    private const val METHOD_STORED = 0

    /**
     * Midnight, the earliest time a zip can hold.
     */
    private const val DOS_TIME = 0

    /**
     * The 1st of January 1980, the earliest date a zip can hold.
     */
    private const val DOS_DATE = (1 shl 5) or 1

    private const val MAX_SHORT = 0xFFFF

    /**
     * The entry count that tells a reader the real count sits in the 64 bit extension.
     */
    private const val ZIP64_ENTRY_COUNT = 0xFFFF
}

/**
 * Little endian writes into a buffer sized up front.
 */
private class ByteWriter(
    private val buffer: ByteArray,
) {
    var position: Int = 0
        private set

    fun short(value: Int) {
        buffer[position++] = value.toByte()
        buffer[position++] = (value ushr 8).toByte()
    }

    fun int(value: Int) {
        short(value and 0xFFFF)
        short(value ushr 16)
    }

    fun bytes(value: ByteArray) {
        value.copyInto(buffer, destinationOffset = position)
        position += value.size
    }

    fun result(): ByteArray {
        check(position == buffer.size) { "The zip was sized for ${buffer.size} bytes but holds $position" }

        return buffer
    }
}
