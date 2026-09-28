package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile

/**
 * The checksum of one golden case, worked out the same way on every platform.
 *
 * The JVM golden harness writes these into `GoldenHashes`, and a wasm test that regenerates the same
 * cases can compare against them without reading a single file from disk.
 */
internal object GoldenDigest {
    /**
     * The CRC-32 of [files], see the other overload.
     */
    fun of(files: List<GeneratedFile>): Long {
        require(files.map { it.path }.distinct().size == files.size) { "A golden case has one file per path" }

        return of(files.associate { file -> file.path to file.text })
    }

    /**
     * The CRC-32 of a case given as path to text.
     *
     * The files go in path order, each as its path, a newline, then its text, all as UTF-8. Paths are
     * relative to the case and use forward slashes whatever the platform.
     */
    fun of(files: Map<String, String>): Long {
        val bytes = files.entries
            .sortedBy { it.key }
            .joinToString(separator = "") { (path, text) -> "$path\n$text" }
            .encodeToByteArray()

        return crc32(bytes)
    }

    /**
     * The standard CRC-32, the one zip and `java.util.zip.CRC32` use.
     */
    fun crc32(bytes: ByteArray): Long {
        var crc = -1
        bytes.forEach { byte ->
            crc = Table[(crc xor byte.toInt()) and 0xFF] xor (crc ushr 8)
        }

        return crc.inv().toLong() and 0xFFFFFFFFL
    }

    private val Table: IntArray = IntArray(256) { index ->
        var value = index
        repeat(8) {
            value = if (value and 1 != 0) (value ushr 1) xor REVERSED_POLYNOMIAL else value ushr 1
        }
        value
    }

    private const val REVERSED_POLYNOMIAL = 0xEDB88320.toInt()
}
