package com.materialkolor.builder.codegen.check

import com.materialkolor.builder.codegen.ExportInput
import com.materialkolor.builder.codegen.FluentBinding
import com.materialkolor.builder.codegen.GoldenCases
import com.materialkolor.builder.codegen.dsl.GeneratedFile
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import java.io.File

/**
 * Lays every golden export case into the fixture projects of `builder/codegen-check`, which the
 * `:builder:codegen:writeCompileFixtures` task runs with the fixtures directory as its argument.
 *
 * Each target and mode gets one project, and each case its own package in it, so a target compiles
 * in one compilation. Dynamic Fluent gets a project per binding, since the module and the inline
 * shades need different dependencies. A project's plainest case, `<target>-<mode>-default` or for
 * inline Fluent `fluent-dynamic-inline`, sits in `commonMain` and is the one the wasm compile covers,
 * or the first case by name when the project has no such case. The other multiplatform cases sit in
 * `jvmMain`, and the Android only cases sit in `androidMain`. Every project takes its dependencies
 * from the snippets its cases generate, and the build takes its catalog from their generated
 * catalogs, so the check proves the snippets too.
 */
fun main(args: Array<String>) {
    val root = File(args.single())
    root.deleteRecursively()
    root.mkdirs()

    val cases = GoldenCases.exports.entries
        .sortedBy { it.key }
        .map { (name, input) -> FixtureCase.of(name, input) }
    val catalog = Catalog()
    cases.groupBy { case -> case.fixture }.forEach { (fixture, members) ->
        members.forEach { case -> catalog.merge(case) }
        FixtureProject(fixture, members).write(File(root, fixture))
    }
    File(root, "libs.versions.toml").writeText(catalog.render())
}

/** One golden case as the compile check sees it, generated into a package of its own. */
private class FixtureCase(
    val name: String,
    val input: ExportInput,
    val files: List<GeneratedFile>,
) {
    /** The project this case compiles in, as in `material3-dynamic` or `fluent-dynamic-inline`. */
    val fixture: String
        get() = listOfNotNull(targetAndMode, fluentBinding).joinToString("-")

    /** The name of the plainest case of this case's project, the one its wasm compile covers. */
    val plainCase: String
        get() = if (fluentBinding == INLINE) fixture else "$targetAndMode-default"

    private val targetAndMode: String
        get() {
            val target = when (input.target) {
                ExportTarget.Material3, ExportTarget.Material3Expressive -> "material3"
                ExportTarget.Unstyled -> "unstyled"
                ExportTarget.Fluent -> "fluent"
                ExportTarget.Custom -> "custom"
            }
            return "$target-${input.prefs.mode.name.lowercase()}"
        }

    /** How a dynamic Fluent case gets its shades, `module` or `inline`, which splits its project in two. */
    private val fluentBinding: String?
        get() = input.versions.fluentBinding.name
            .lowercase()
            .takeIf { input.target == ExportTarget.Fluent && input.prefs.mode == ExportMode.Dynamic }

    val androidOnly: Boolean
        get() = !input.prefs.multiplatform

    val sources: List<GeneratedFile>
        get() = files.filter { file -> file.path.startsWith("src/") }

    val snippet: String?
        get() = files.singleOrNull { file -> file.path == "snippets/build.gradle.kts" }?.text

    val catalog: String?
        get() = files.singleOrNull { file -> file.path == "gradle/libs.versions.toml" }?.text

    companion object {
        fun of(
            name: String,
            input: ExportInput,
        ): FixtureCase {
            val packaged = input.copy(prefs = input.prefs.copy(packageName = "check.${name.replace('-', '_')}"))
            return FixtureCase(name, packaged, generate(packaged))
        }

        private val INLINE = FluentBinding.Inline.name.lowercase()
    }
}

/** The version catalog every fixture shares, merged from the catalogs the cases generate. */
private class Catalog {
    private val sections = sortedMapOf<String, MutableMap<String, String>>()

    fun merge(case: FixtureCase) {
        val entries = case.catalog?.let(::parse).orEmpty()
        val aliases = entries["libraries"].orEmpty().keys.map { alias -> alias.replace('-', '.').replace('_', '.') }
        LIBS_ACCESSOR.findAll(case.snippet.orEmpty()).forEach { match ->
            require(match.groupValues[1] in aliases) {
                "The snippet of ${case.name} names ${match.value}, which its own catalog does not define"
            }
        }
        // The merged catalog would find a version another case defines, so each case has to define its own.
        val versions = entries["versions"].orEmpty().keys
        entries.filterKeys { section -> section != "versions" }.forEach { (section, lines) ->
            lines.forEach { (key, value) ->
                VERSION_REF.findAll(value).forEach { match ->
                    require(match.groupValues[1] in versions) {
                        "The catalog of ${case.name} has $section.$key take version.ref ${match.groupValues[1]}, " +
                            "which its own [versions] does not define"
                    }
                }
            }
        }
        entries.forEach { (section, lines) ->
            val merged = sections.getOrPut(section) { sortedMapOf() }
            lines.forEach { (key, value) ->
                val previous = merged.putIfAbsent(key, value)
                require(previous == null || previous == value) {
                    "The catalog of ${case.name} sets $section.$key to $value where another case has $previous"
                }
            }
        }
    }

    fun render(): String =
        sections.entries.joinToString(separator = "\n", prefix = "$WRITTEN_BY\n\n") { (section, lines) ->
            "[$section]\n" + lines.entries.joinToString(separator = "") { (key, value) -> "$key = $value\n" }
        }

    private fun parse(text: String): Map<String, Map<String, String>> {
        val parsed = mutableMapOf<String, MutableMap<String, String>>()
        var section: MutableMap<String, String>? = null
        text.lines().map(String::trim).filter { line -> line.isNotEmpty() && !line.startsWith("#") }.forEach { line ->
            if (line.startsWith("[")) {
                section = parsed.getOrPut(line.removeSurrounding("[", "]")) { mutableMapOf() }
            } else {
                val (key, value) = line.split(" = ", limit = 2)
                checkNotNull(section) { "Catalog line outside a section: $line" }[key] = value
            }
        }
        return parsed
    }

    private companion object {
        val LIBS_ACCESSOR = Regex("""\blibs\.([A-Za-z0-9.]+)""")
        val VERSION_REF = Regex("""\bversion\.ref = "([^"]+)"""")
    }
}

/** One fixture project, the cases of one target and mode. */
private class FixtureProject(
    private val name: String,
    private val cases: List<FixtureCase>,
) {
    private val multiplatform = cases.filterNot { case -> case.androidOnly }
    private val android = cases.filter { case -> case.androidOnly }
    private val wasmCase = multiplatform.firstOrNull { case -> case.name == case.plainCase } ?: multiplatform.first()

    fun write(dir: File) {
        cases.forEach { case ->
            val sourceSet = when {
                case.androidOnly -> "androidMain"
                case === wasmCase -> "commonMain"
                else -> "jvmMain"
            }
            case.sources.forEach { file ->
                val relative = file.path.substringAfter("/kotlin/")
                File(dir, "src/$sourceSet/kotlin/$relative").apply { parentFile.mkdirs() }.writeText(file.text)
            }
        }
        File(dir, "build.gradle.kts").writeText(buildFile())

        val androidNote = if (android.isEmpty()) "" else android.joinToString(prefix = " (", postfix = ")") { it.name }
        println(
            "Compile check $name: ${multiplatform.size} cases on JVM, ${wasmCase.name} on wasm, " +
                "${android.size} Android only cases on Android$androidNote",
        )
    }

    private fun buildFile(): String =
        buildString {
            appendLine(WRITTEN_BY.replace("#", "//"))
            appendLine("plugins {")
            appendLine("    id(\"org.jetbrains.kotlin.multiplatform\")")
            appendLine("    id(\"org.jetbrains.kotlin.plugin.compose\")")
            if (android.isNotEmpty()) appendLine("    id(\"com.android.kotlin.multiplatform.library\")")
            appendLine("}")
            appendLine()
            appendLine("kotlin {")
            appendLine("    jvm()")
            appendLine("    wasmJs { browser() }")
            if (android.isNotEmpty()) {
                appendLine("    android {")
                appendLine("        namespace = \"check.${name.replace('-', '_')}\"")
                appendLine("        compileSdk = repo.versions.sdk.compile.get().toInt()")
                appendLine("        minSdk = repo.versions.sdk.min.app.get().toInt()")
                appendLine("    }")
            }
            appendLine("    jvmToolchain(repo.versions.jvmTarget.get().toInt())")
            appendLine()
            appendLine("    sourceSets {")
            appendLine("        commonMain.dependencies {")
            appendLine("            // Every Compose project already has these, so no snippet names them.")
            appendLine("            implementation(repo.compose.runtime)")
            appendLine("            implementation(repo.compose.foundation)")
            appendLine("            implementation(repo.compose.ui)")
            baseDependency()?.let { (comment, dependency) ->
                appendLine("            // $comment")
                appendLine("            implementation($dependency)")
            }
            appendLine("        }")
            if (android.isNotEmpty()) {
                appendLine("        // The Android snippets add to the module's dependencies, androidMain here.")
                appendLine("        androidMain.dependencies {")
                androidDependencies().forEach { line -> appendLine("            $line") }
                appendLine("        }")
            }
            appendLine("    }")
            appendLine("}")
            multiplatform
                .filter { case -> case.snippet != null }
                .groupBy { case -> checkNotNull(case.snippet) }
                .forEach { (snippet, owners) ->
                    appendLine()
                    appendLine("// The snippet of ${owners.joinToString { it.name }}")
                    append(snippet)
                }
        }

    /**
     * What a fixture adds past its snippets, with the reason. Frozen Custom writes no snippet either
     * but needs nothing past Compose foundation, which every fixture has.
     */
    private fun baseDependency(): Pair<String, String>? =
        when (name) {
            "material3-frozen" -> {
                "Frozen Material 3 writes no snippet, its README names Compose Material 3." to "repo.compose.material3"
            }
            "material3-dynamic" -> {
                "Dynamic Material 3 and Expressive import androidx.compose.material3 themselves, which " +
                    "material-kolor-material3 keeps off the compile classpath, so their README names Compose " +
                    "Material 3." to "repo.compose.material3"
            }
            else -> {
                null
            }
        }

    /**
     * The dependency lines of every Android snippet, which add to a plain `dependencies` block. A
     * line that is none of a comment, the block itself or an `implementation(...)` fails, rather than
     * the check dropping what the snippet asks for.
     */
    private fun androidDependencies(): List<String> =
        android
            .flatMap { case ->
                case.snippet
                    .orEmpty()
                    .lines()
                    .map(String::trim)
                    .filterNot { line -> line.isEmpty() || line.startsWith("//") || line in ANDROID_BLOCK }
                    .onEach { line ->
                        require(line.startsWith("implementation(")) {
                            "The Android snippet of ${case.name} has the line $line, which the compile check cannot add"
                        }
                    }
            }.distinct()

    private companion object {
        val ANDROID_BLOCK = setOf("dependencies {", "}")
    }
}

private const val WRITTEN_BY = "# Written by :builder:codegen:writeCompileFixtures, which rewrites it on every run."
