package com.materialkolor.builder.codegen.target

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.MAX_LINE_LENGTH
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants

/** The prefs of a frozen export, at the standard contrast or at every contrast. */
internal fun frozenPrefs(variants: FrozenVariants = FrozenVariants.StandardOnly): ExportPrefs =
    ExportPrefs(mode = ExportMode.Frozen, frozenVariants = variants)

/** Every import line in [files] that pulls anything from MaterialKolor, which a frozen export never does. */
internal fun materialKolorImports(files: List<GeneratedFile>): List<String> =
    files.flatMap { file -> file.text.lines() }.filter { line -> line.startsWith("import com.materialkolor") }

/** Every code line of [files] past the column limit or ending in a space, which ktlint would reject. */
internal fun lintFailures(files: List<GeneratedFile>): List<String> =
    files.flatMap { file ->
        val lines = file.text.lines()
        // The header's share link grows with the theme. ktlint leaves a line that is only a comment alone.
        val tooLong = lines.filterNot { it.startsWith("//") }.filter { it.length > MAX_LINE_LENGTH }

        (tooLong + lines.filter { it.endsWith(" ") }).map { line -> "${file.path} $line" }
    }

/** The contrast variants a frozen export with these prefs resolves and writes. */
internal fun FrozenVariants.expectedVariants(): Set<ContrastVariant> =
    when (this) {
        FrozenVariants.StandardOnly -> setOf(ContrastVariant.Standard)
        FrozenVariants.AllContrasts -> ContrastVariant.entries.toSet()
    }
