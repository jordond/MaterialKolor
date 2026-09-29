package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.Language
import com.materialkolor.builder.codegen.dsl.plainToken
import com.materialkolor.builder.domain.persist.ExportMode

/**
 * Where the readme sits in the export.
 */
internal const val README_PATH: String = "README.md"

/**
 * The `README.md` that goes with an export, which tells a reader where each file goes, what to add
 * to their build, and how to get back to the theme.
 *
 * The dependency lines are copied out of the build file snippet itself, so the two never disagree.
 */
internal object Readme {
    /**
     * The readme for [input], given every other file the export writes.
     */
    fun file(
        input: ExportInput,
        files: List<GeneratedFile>,
    ): GeneratedFile {
        val sources = files.filter { file -> file.language == Language.Kotlin && file.path.startsWith("src/") }
        val catalog = files.firstOrNull { file -> file.path == CATALOG_PATH }
        val build = files.firstOrNull { file -> file.path == BUILD_SNIPPET_PATH }
        val versions = input.versions

        val text = buildList {
            add("# ${input.document.themeName}")
            add("")
            add("Made with MaterialKolor Builder ${versions.builder}: ${input.shareUrl}")
            when (input.prefs.mode) {
                ExportMode.Dynamic -> {
                    add(
                        "The theme builds its colors from the seed at runtime with MaterialKolor " +
                            versions.materialKolor,
                    )
                }
                ExportMode.Frozen -> {
                    add(
                        "The colors come from MaterialKolor ${versions.materialKolor}.",
                    )
                }
            }
            add("")
            add("## Add the theme")
            add("")
            add(
                "The `src` folder is laid out like a module, so copy it into the module that holds your theme. " +
                    "That puts these files in `${input.sourcePath("").removeSuffix("/")}`, " +
                    "in the package `${input.prefs.packageName}`.",
            )
            add("")
            sources.forEach { file -> add("- `${file.path.substringAfterLast('/')}`") }
            add("")
            add("## Add the dependencies")
            add("")
            if (build == null) {
                add("These files need nothing beyond Compose, so there is nothing to add.")
            } else {
                val addLines = "these lines to the build file of the same module. They are also in `${build.path}`."
                if (catalog == null) {
                    add("Add $addLines")
                } else {
                    add("Merge `${catalog.path}` into the version catalog of your project, then add $addLines")
                }
                add("")
                add("```kotlin")
                addAll(build.text.lines().filterNot { line -> line.startsWith("//") || line.isEmpty() })
                add("```")
                Snippets.platformNote(input.target)?.let { note ->
                    add("")
                    add(note)
                }
            }
        }

        return GeneratedFile(
            path = README_PATH,
            language = Language.Markdown,
            lines = text.map { line -> if (line.isEmpty()) emptyList() else listOf(plainToken(line)) },
        )
    }
}
