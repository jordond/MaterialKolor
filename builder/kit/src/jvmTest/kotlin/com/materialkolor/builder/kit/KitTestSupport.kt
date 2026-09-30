package com.materialkolor.builder.kit

import androidx.compose.ui.input.key.Key
import java.io.File

/**
 * The key held with a letter for a desktop shortcut such as undo, copy or select all, Command on a
 * Mac and Control everywhere else, as the desktop reads it.
 */
internal val ShortcutKey: Key =
    if (System.getProperty("os.name").orEmpty().startsWith("Mac")) Key.MetaLeft else Key.CtrlLeft

/**
 * Runs [check] for every one of [cases], then fails once with every case that failed, each headed
 * by its [name], so the first failing case does not hide the rest.
 */
internal fun <T> checkEach(
    cases: Iterable<T>,
    name: (T) -> String,
    check: (T) -> Unit,
) {
    val failures = cases.mapNotNull { case ->
        try {
            check(case)
            null
        } catch (failure: Throwable) {
            name(case) to failure
        }
    }
    if (failures.isEmpty()) return
    val first = failures.first().second
    val report = failures.joinToString(separator = "\n\n") { (case, failure) -> "$case: ${failure.message}" }
    val error = AssertionError("${failures.size} of the cases failed\n\n$report", first)
    for ((_, failure) in failures.drop(1)) error.addSuppressed(failure)
    throw error
}

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
