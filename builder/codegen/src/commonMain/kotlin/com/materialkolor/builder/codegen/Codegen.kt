package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.target.Readme
import com.materialkolor.builder.codegen.target.Snippets
import com.materialkolor.builder.codegen.target.custom.CustomDynamic
import com.materialkolor.builder.codegen.target.custom.CustomFrozen
import com.materialkolor.builder.codegen.target.fluent.FluentDynamic
import com.materialkolor.builder.codegen.target.fluent.FluentFrozen
import com.materialkolor.builder.codegen.target.material3.Material3Dynamic
import com.materialkolor.builder.codegen.target.material3.Material3Frozen
import com.materialkolor.builder.codegen.target.unstyled.UnstyledDynamic
import com.materialkolor.builder.codegen.target.unstyled.UnstyledFrozen
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.codegen.zip.ZipWriter
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * Every file the export of [input] writes, theme files first, then the dependency snippets, then
 * the `README.md`.
 *
 * The export works on the document as its target sees it, so a setting left over from another
 * target never reaches the files. The caller resolves [ExportInput.resolved] from that same view,
 * with `ExportResolver.resolve(document.forTarget(target), prefs)`, since the engine sits outside
 * codegen.
 *
 * A theme or accent name the target already uses cannot be exported, and this fails on the first
 * one. Callers such as the export sheet show [ReservedNames.clashes] for the document as the target
 * sees it before they let anyone export, so a user sees the name to change rather than an error.
 */
public fun generate(input: ExportInput): List<GeneratedFile> {
    val targeted = input.copy(document = input.document.forTarget(input.target))
    val clashes = ReservedNames.clashes(targeted.document)
    require(clashes.isEmpty()) {
        "The ${targeted.target} export already uses the name ${clashes.first().name}, so it has to be renamed first"
    }

    val files = themeFiles(targeted) + Snippets.files(targeted)

    return files + Readme.file(targeted, files)
}

/**
 * Every file in [files] as one text to copy, each opened by a `// <path>` line and set apart from
 * the next by a blank line.
 */
public fun copyAll(files: List<GeneratedFile>): String =
    files.joinToString(separator = "\n") { file -> "// ${file.path}\n${file.text}" }

/**
 * The zip a user downloads, with every one of [files] under a folder named [themeName], as in
 * `AppTheme/src/commonMain/kotlin/com/example/theme/Theme.kt` and `AppTheme/README.md`.
 *
 * The same files always give the same bytes, on every platform.
 */
public fun zipArchive(
    themeName: String,
    files: List<GeneratedFile>,
): ByteArray {
    require(themeName.isNotBlank() && '/' !in themeName && '\\' !in themeName) {
        "The zip folder is named after the theme, which cannot be '$themeName'"
    }

    return ZipWriter.write(
        files.map { file -> ZipWriter.Entry(path = "$themeName/${file.path}", bytes = file.text.encodeToByteArray()) },
    )
}

/** The theme files of [input], from the one export that writes its target in its mode. */
private fun themeFiles(input: ExportInput): List<GeneratedFile> =
    when (input.target) {
        ExportTarget.Material3, ExportTarget.Material3Expressive -> {
            when (input.prefs.mode) {
                ExportMode.Dynamic -> Material3Dynamic.files(input)
                ExportMode.Frozen -> Material3Frozen.files(input)
            }
        }
        ExportTarget.Unstyled -> {
            when (input.prefs.mode) {
                ExportMode.Dynamic -> UnstyledDynamic.files(input)
                ExportMode.Frozen -> UnstyledFrozen.files(input)
            }
        }
        ExportTarget.Fluent -> {
            when (input.prefs.mode) {
                ExportMode.Dynamic -> FluentDynamic.files(input)
                ExportMode.Frozen -> FluentFrozen.files(input)
            }
        }
        ExportTarget.Custom -> {
            when (input.prefs.mode) {
                ExportMode.Dynamic -> CustomDynamic.files(input)
                ExportMode.Frozen -> CustomFrozen.files(input)
            }
        }
    }
