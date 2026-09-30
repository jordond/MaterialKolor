package com.materialkolor.builder.preview

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
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

/**
 * The start of the names of the endless animation APIs. Written out whole they would trip the
 * builder's own architecture scan of this file.
 */
private val EndlessMotionStems: List<String> = listOf("rememberInfinite", "infiniteRepeat")

/**
 * What a library's Trips or gallery drawn with its own components may take from the kit, its motion,
 * the fold modifiers and `InnerTextWithoutHandles`.
 */
internal val PaneKitImports: List<String> = listOf(
    "com.materialkolor.builder.kit.motion.",
    "com.materialkolor.builder.kit.control.folded",
    "com.materialkolor.builder.kit.headless.InnerTextWithoutHandles",
)

/**
 * Whether this import opens a window.
 */
internal fun String.opensAWindow(): Boolean = startsWith("androidx.compose.ui.window.")

/**
 * Whether this import comes from the kit but from none of the [allowed] prefixes.
 */
internal fun String.isKitImportBeyond(allowed: List<String>): Boolean =
    startsWith("com.materialkolor.builder.kit.") && allowed.none { prefix -> startsWith(prefix) }

/**
 * Checks that none of [sources] imports anything [isBanned] matches or names an endless animation.
 */
internal fun checkSourcesOpenNothingAndNeverLoop(
    sources: List<File>,
    isBanned: (String) -> Boolean,
) {
    for (source in sources) {
        withClue(source.name) {
            source.importedNames().filter { imported -> isBanned(imported) }.shouldBeEmpty()
            source
                .readLines()
                .filter { line -> EndlessMotionStems.any { stem -> stem in line } }
                .shouldBeEmpty()
        }
    }
}
