package com.materialkolor.builder.preview

import java.io.File

/**
 * The file or folder at [path] under this module's `src` folder, found through the folder the
 * Gradle test task hands over, so it resolves whichever folder the tests run from.
 */
internal fun moduleSource(path: String): File {
    val root = System.getProperty("builder.sourceDir")
    require(root != null) { "builder.sourceDir is unset, run the tests through Gradle" }
    val source = File(root, path)
    require(source.exists()) { "No source at $source" }
    return source
}

/**
 * What this source file imports, each name without its alias.
 */
internal fun File.importedNames(): List<String> =
    readLines()
        .map { line -> line.trim() }
        .filter { line -> line.startsWith("import ") }
        .map { line -> line.removePrefix("import ").substringBefore(" as ") }
