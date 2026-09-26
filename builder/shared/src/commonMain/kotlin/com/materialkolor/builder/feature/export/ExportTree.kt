package com.materialkolor.builder.feature.export

import com.materialkolor.builder.codegen.dsl.GeneratedFile

/**
 * One row of the export's file tree, as the zip lays the files out.
 */
internal sealed interface TreeRow {
    /**
     * How far in the row sits, 0 at the zip's root.
     */
    val depth: Int

    /**
     * A folder, named by [label], which can be several path parts such as `com/example/theme`.
     */
    data class Folder(
        val label: String,
        override val depth: Int,
    ) : TreeRow

    /**
     * The file at [path], called [name], with [lines] lines.
     */
    data class File(
        val path: String,
        val name: String,
        val lines: Int,
        override val depth: Int,
    ) : TreeRow
}

/**
 * The rows of the tree [files] make, in codegen's own order, each folder row just before the first
 * file in it.
 *
 * A source file under `src/<set>/kotlin/<package path>/` sits under two folder rows, the source root
 * `src/<set>/kotlin` and the package path such as `com/example/theme`. A file in any other folder,
 * such as `gradle` or `snippets`, sits under one row naming that folder whole, and a file at the
 * root, such as the README, under none.
 */
internal fun exportTree(files: List<GeneratedFile>): List<TreeRow> {
    val rows = mutableListOf<TreeRow>()
    val shown = mutableSetOf<String>()
    for (file in files) {
        val parts = file.path.split('/')
        val folders = foldersOf(parts.dropLast(1))
        var path = ""
        folders.forEachIndexed { depth, label ->
            path = if (path.isEmpty()) label else "$path/$label"
            if (shown.add(path)) rows += TreeRow.Folder(label = label, depth = depth)
        }
        rows += TreeRow.File(path = file.path, name = parts.last(), lines = file.lines.size, depth = folders.size)
    }
    return rows
}

/**
 * The folder rows above a file in [folders], outermost first.
 */
private fun foldersOf(folders: List<String>): List<String> {
    if (folders.isEmpty()) return emptyList()
    val sourceRoot = folders.size >= SOURCE_ROOT_PARTS && folders[0] == "src" && folders[2] == "kotlin"
    if (!sourceRoot) return listOf(folders.joinToString("/"))
    val root = folders.take(SOURCE_ROOT_PARTS).joinToString("/")
    val packagePath = folders.drop(SOURCE_ROOT_PARTS).joinToString("/")
    return listOfNotNull(root, packagePath.ifEmpty { null })
}

/**
 * The parts of a source root, `src`, the source set and `kotlin`.
 */
private const val SOURCE_ROOT_PARTS = 3
