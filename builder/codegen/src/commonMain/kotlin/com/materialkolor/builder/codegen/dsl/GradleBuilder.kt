package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.text.Literals

/**
 * Writes a Kotlin DSL Gradle snippet, such as the dependency block that goes with an export.
 *
 * It is Kotlin, so it reuses the same expressions and the same line breaking as a generated source
 * file. What it does not reuse is imports, because a snippet is meant to be pasted into a build file
 * that already has its own.
 */
public fun gradleFile(
    path: String,
    build: GradleScope.() -> Unit,
): GeneratedFile {
    val scope = GradleScope().apply(build)
    val writer = CodeWriter()
    scope.render(writer)

    return GeneratedFile(path = path, language = Language.Kotlin, lines = writer.result)
}

/** One level of a Gradle Kotlin DSL snippet. */
@CodegenDsl
public class GradleScope internal constructor() {
    private val entries = mutableListOf<GradleEntry>()

    /** A nested block, as in `dependencies { }` or `commonMain.dependencies { }`. */
    public fun block(
        name: String,
        build: GradleScope.() -> Unit,
    ) {
        entries += GradleEntry.Block(name, GradleScope().apply(build))
    }

    /** A call written on its own line. */
    public fun call(
        name: String,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ) {
        statement(buildCall(listOf(functionToken(name)), emptyList(), multiline, build))
    }

    /** A dependency on a literal coordinate, as in `implementation("group:name:version")`. */
    public fun dependency(
        configuration: String,
        coordinate: String,
    ) {
        dependency(configuration, Literals.string(coordinate))
    }

    /** A dependency on anything else, such as a version catalog accessor. */
    public fun dependency(
        configuration: String,
        notation: Expression,
    ) {
        val call = buildCall(
            callee = listOf(functionToken(configuration)),
            calleeSymbols = emptyList(),
            multiline = false,
        ) { argument(notation) }

        statement(call)
    }

    /** Any expression on its own line. */
    public fun statement(value: Expression) {
        entries += GradleEntry.Line(emptyList(), value)
    }

    /** A comment line. */
    public fun comment(text: String) {
        entries += GradleEntry.Line(listOf(commentToken("// $text")), null)
    }

    /** A blank line. */
    public fun blankLine() {
        entries += GradleEntry.Line(emptyList(), null)
    }

    internal fun render(writer: CodeWriter) {
        entries.forEach { entry ->
            when (entry) {
                is GradleEntry.Block -> {
                    writer.line(listOf(functionToken(entry.name), spaceToken, punctuationToken("{")))
                    writer.indented { entry.scope.render(writer) }
                    writer.line(listOf(punctuationToken("}")))
                }
                is GradleEntry.Line -> {
                    val value = entry.value
                    if (value == null) {
                        writer.line(entry.prefix)
                    } else {
                        writer.expression(value, entry.prefix)
                    }
                }
            }
        }
    }
}

internal sealed interface GradleEntry {
    class Block(
        val name: String,
        val scope: GradleScope,
    ) : GradleEntry

    class Line(
        val prefix: List<Token>,
        val value: Expression?,
    ) : GradleEntry
}
