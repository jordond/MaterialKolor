package com.materialkolor.builder.feature.export

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.export.ExportResolver
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val VERSIONS =
    ExportVersions(
        builder = "2.0.0",
        materialKolor = "6.0.0",
        fluent = "v0.1.0",
        composeUnstyled = "1.0.0",
        composeMaterial3 = "1.12.0-alpha03",
        androidxMaterial3 = "1.5.0-alpha28",
        inklet = "0.3.0",
    )

class ExportTreeTest {
    @Test
    fun material3DynamicMultiplatformWithACatalog_groupsTheSourcesThenGradleSnippetsAndTheReadme() {
        val files = filesOf(ThemeDocument.Default, ExportPrefs(multiplatform = true, versionCatalog = true))

        val rows = exportTree(files)

        rows.labels() shouldBe listOf(
            "src/commonMain/kotlin@0",
            "com/example/theme@1",
            "Color.kt@2",
            "Theme.kt@2",
            "gradle@0",
            "libs.versions.toml@1",
            "snippets@0",
            "build.gradle.kts@1",
            "README.md@0",
        )
    }

    @Test
    fun androidOnly_putsTheSourcesUnderSrcMainKotlin() {
        val files = filesOf(ThemeDocument.Default, ExportPrefs(multiplatform = false))

        val folders = exportTree(files).filterIsInstance<TreeRow.Folder>()

        folders.take(2) shouldBe listOf(TreeRow.Folder("src/main/kotlin", 0), TreeRow.Folder("com/example/theme", 1))
    }

    @Test
    fun customFrozen_hasNoGradleOrSnippetsRows_andEndsOnTheReadme() {
        val custom = DocumentChange.SetLibrary(Library.Custom, expressive = false).apply(ThemeDocument.Default)
        val files = filesOf(custom, ExportPrefs(mode = ExportMode.Frozen))

        val rows = exportTree(files)

        rows.filterIsInstance<TreeRow.Folder>().map { folder -> folder.label } shouldBe listOf(
            "src/commonMain/kotlin",
            "com/example/theme",
        )
        rows.last() shouldBe TreeRow.File("README.md", "README.md", files.last().lines.size, depth = 0)
    }

    @Test
    fun eachFileRow_countsTheFilesLines() {
        val files = filesOf(ThemeDocument.Default, ExportPrefs())

        val counts = exportTree(files).filterIsInstance<TreeRow.File>().map { row -> row.path to row.lines }

        counts shouldBe files.map { file -> file.path to file.lines.size }
    }

    private fun filesOf(
        document: ThemeDocument,
        prefs: ExportPrefs,
    ): List<GeneratedFile> {
        val targeted = document.forTarget(ExportTarget.of(document.library, document.expressive))
        val input = ExportInput(
            document = document,
            prefs = prefs,
            resolved = ExportResolver().resolve(targeted, prefs),
            versions = VERSIONS,
            shareUrl = "https://materialkolor.com/t/test",
        )
        return generate(input)
    }

    private fun List<TreeRow>.labels(): List<String> =
        map { row ->
            when (row) {
                is TreeRow.Folder -> "${row.label}@${row.depth}"
                is TreeRow.File -> "${row.name}@${row.depth}"
            }
        }
}
