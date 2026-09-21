package com.materialkolor.transformer

import java.io.File
import java.nio.file.Files

internal fun requiredProperty(name: String): String =
    checkNotNull(System.getProperty(name)) {
        "Required test input $name was not configured"
    }

/**
 * Runs [block] against a fresh temporary directory and removes the whole tree afterwards.
 */
internal fun temporaryDirectory(
    prefix: String,
    block: (File) -> Unit,
) {
    val directory = Files.createTempDirectory(prefix).toFile()
    try {
        block(directory)
    } finally {
        directory.deleteRecursively()
    }
}
