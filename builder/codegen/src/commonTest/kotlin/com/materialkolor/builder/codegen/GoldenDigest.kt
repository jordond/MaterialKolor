package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.zip.Crc32

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

        return Crc32.of(bytes).toLong() and 0xFFFFFFFFL
    }
}
