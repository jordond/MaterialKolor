package com.materialkolor.builder.codegen

import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.dsl.Language
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.codegen.validate.ReservedNames
import com.materialkolor.builder.codegen.zip.Crc32
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CodegenTest {
    @Test
    fun generate_everyTargetModeLayoutAndCatalog_writesTheExpectedFileSet() {
        ExportTarget.entries.forEach { target ->
            ExportMode.entries.forEach { mode ->
                listOf(true, false).forEach { multiplatform ->
                    listOf(true, false).forEach { versionCatalog ->
                        val prefs = ExportPrefs(
                            multiplatform = multiplatform,
                            versionCatalog = versionCatalog,
                            mode = mode,
                        )
                        val case = "$target $mode multiplatform=$multiplatform catalog=$versionCatalog"
                        val sourceDir = if (multiplatform) "src/commonMain/kotlin" else "src/main/kotlin"
                        val needsDependencies = mode == ExportMode.Dynamic ||
                            target == ExportTarget.Unstyled ||
                            target == ExportTarget.Fluent
                        val expected = buildList {
                            themeFileNames(target, mode).forEach { name -> add("$sourceDir/com/example/theme/$name") }
                            if (needsDependencies && versionCatalog) add("gradle/libs.versions.toml")
                            if (needsDependencies) add("snippets/build.gradle.kts")
                            add("README.md")
                        }

                        val paths = generate(Fixtures.input(documentFor(target), prefs)).map { it.path }

                        assertEquals(expected.sorted(), paths.sorted(), case)
                        assertEquals("README.md", paths.last(), case)
                    }
                }
            }
        }
    }

    @Test
    fun generate_frozenExport_namesNoMaterialKolorDependency() {
        ExportTarget.entries.forEach { target ->
            val files = generate(Fixtures.input(documentFor(target), ExportPrefs(mode = ExportMode.Frozen)))

            val naming = files
                .filter { file -> file.path == "gradle/libs.versions.toml" || file.path == "snippets/build.gradle.kts" }
                .filter { file -> "com.materialkolor" in file.text }
            assertEquals(emptyList(), naming.map { it.path }, "$target")
        }
    }

    @Test
    fun generate_fluentWithoutTheModule_dependsOnCoreAndFluent() {
        val input = Fixtures.input(
            document = documentFor(ExportTarget.Fluent),
            prefs = ExportPrefs(versionCatalog = false),
            versions = Fixtures.Versions.copy(fluentModuleAvailable = false),
        )

        val snippet = generate(input).single { it.path == "snippets/build.gradle.kts" }.text

        assertTrue("implementation(\"com.materialkolor:material-kolor-core:6.0.0\")" in snippet, snippet)
        assertTrue("implementation(\"io.github.compose-fluent:fluent:v0.1.0\")" in snippet, snippet)
        assertTrue("material-kolor-fluent" !in snippet, snippet)
    }

    @Test
    fun generate_reservedThemeName_failsNamingIt() {
        val input = Fixtures.input(ThemeDocument.Default.copy(themeName = "MaterialTheme"))

        val error = assertFailsWith<IllegalArgumentException> { generate(input) }

        assertTrue("MaterialTheme" in error.message.orEmpty(), error.message)
    }

    @Test
    fun generate_accentTheTargetDrops_isNotHeldToItsReservedNames() {
        val reserved = ReservedNames.of(ExportTarget.Fluent).first { name -> name.first().isUpperCase() }
        val document = documentFor(ExportTarget.Fluent).copy(
            accents = listOf(Accent(name = reserved, seed = ThemeDocument.Default.seed)),
        )

        val files = generate(Fixtures.input(document))

        assertTrue(
            ReservedNames.clashes(document).isNotEmpty(),
            "The accent should clash before the Fluent view drops it",
        )
        assertEquals(generate(Fixtures.input(documentFor(ExportTarget.Fluent))).size, files.size)
    }

    @Test
    fun copyAll_files_joinsThemUnderTheirPaths() {
        val text = copyAll(FixedFiles)

        val expected =
            """
            |// src/commonMain/kotlin/com/example/theme/Theme.kt
            |package com.example.theme
            |
            |// README.md
            |# Thème
            |
            """.trimMargin()
        assertEquals(expected, text)
    }

    @Test
    fun zipArchive_fixedFiles_writesTheSameBytesOnEveryPlatform() {
        val bytes = zipArchive("AppTheme", FixedFiles)

        // Worked out once by an independent writer. Every platform the tests run on has to agree.
        assertEquals(359, bytes.size)
        assertEquals(0x1A44EADC, Crc32.of(bytes))
    }

    @Test
    fun zipArchive_themeNameWithASlash_isRejected() {
        assertFailsWith<IllegalArgumentException> { zipArchive("App/Theme", FixedFiles) }
    }

    private fun documentFor(target: ExportTarget): ThemeDocument {
        val library = when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive -> Library.Material3
            ExportTarget.Unstyled -> Library.Unstyled
            ExportTarget.Fluent -> Library.Fluent
            ExportTarget.Custom -> Library.Custom
        }

        return ThemeDocument.Default.copy(library = library, expressive = target == ExportTarget.Material3Expressive)
    }

    /** The Kotlin files each export writes for a default theme, which has no accents. */
    private fun themeFileNames(
        target: ExportTarget,
        mode: ExportMode,
    ): List<String> =
        when (target) {
            ExportTarget.Material3, ExportTarget.Material3Expressive -> {
                listOf("Color.kt", "Theme.kt")
            }
            ExportTarget.Unstyled -> {
                when (mode) {
                    ExportMode.Dynamic -> listOf("Color.kt", "Theme.kt")
                    ExportMode.Frozen -> listOf("Tokens.kt", "Color.kt", "Theme.kt")
                }
            }
            ExportTarget.Fluent -> {
                when (mode) {
                    ExportMode.Dynamic -> listOf("Color.kt", "Theme.kt")
                    ExportMode.Frozen -> listOf("Theme.kt")
                }
            }
            ExportTarget.Custom -> {
                when (mode) {
                    ExportMode.Dynamic -> listOf("ThemeSeeds.kt", "ThemeColors.kt", "Theme.kt")
                    ExportMode.Frozen -> listOf("ThemeColors.kt", "Theme.kt")
                }
            }
        }

    private companion object {
        val FixedFiles: List<GeneratedFile> = listOf(
            GeneratedFile(
                path = "src/commonMain/kotlin/com/example/theme/Theme.kt",
                language = Language.Kotlin,
                lines = listOf(listOf(Token(TokenKind.Plain, "package com.example.theme"))),
            ),
            GeneratedFile(
                path = "README.md",
                language = Language.Markdown,
                lines = listOf(listOf(Token(TokenKind.Plain, "# Thème"))),
            ),
        )
    }
}
