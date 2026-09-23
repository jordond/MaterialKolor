package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol

/** Keeps the nested builder scopes from reaching into each other by accident. */
@DslMarker
public annotation class CodegenDsl

/**
 * The parameter count at which ktlint_official puts every parameter on a line of its own.
 *
 * The emitted files are linted with this repo's own `.editorconfig`, so the writer has to reach the
 * same answer the formatter would.
 */
internal const val FORCE_MULTILINE_PARAMETERS = 2

internal class Block(
    val lines: List<List<Token>>,
    val isComment: Boolean,
)

/**
 * Writes one Kotlin file.
 *
 * Declarations come out in the order they are added, separated by a blank line, and the imports are
 * whatever the declarations turned out to reference.
 */
public fun kotlinFile(
    path: String,
    packageName: String,
    build: KotlinFileScope.() -> Unit,
): GeneratedFile {
    require(packageName.isNotBlank()) { "A generated Kotlin file needs a package" }
    val scope = KotlinFileScope(packageName).apply(build)

    return GeneratedFile(path = path, language = Language.Kotlin, lines = scope.render())
}

/** The top level of a generated Kotlin file. */
@CodegenDsl
public class KotlinFileScope internal constructor(
    private val packageName: String,
) {
    private val headerLines = mutableListOf<String>()
    private val blocks = mutableListOf<Block>()
    private val symbols = mutableListOf<Symbol>()

    /** The comment lines that sit above the package declaration. */
    public fun header(lines: List<String>) {
        headerLines += lines
    }

    /** A standalone comment, which stays glued to whatever is declared after it. */
    public fun comment(vararg lines: String) {
        blocks += Block(lines.map { listOf(lineCommentToken(it)) }, isComment = true)
    }

    /** A top level `val`, as in `val SeedColor = Color(0xFF6750A4)`. */
    public fun property(
        name: String,
        value: Expression,
        type: TypeRef? = null,
        const: Boolean = false,
    ) {
        symbols += value.symbols
        type?.let { symbols += it.symbols }

        val prefix = buildList {
            if (const) {
                add(keywordToken("const"))
                add(spaceToken)
            }
            add(keywordToken("val"))
            add(spaceToken)
            add(plainToken(name))
            if (type != null) {
                add(punctuationToken(":"))
                add(spaceToken)
                addAll(type.tokens)
            }
            add(spaceToken)
            add(punctuationToken("="))
            add(spaceToken)
        }

        val writer = CodeWriter()
        writer.expression(value, prefix)
        blocks += Block(writer.result, isComment = false)
    }

    /** A top level function. */
    public fun function(
        name: String,
        annotations: List<Symbol> = emptyList(),
        returns: TypeRef? = null,
        build: FunctionScope.() -> Unit,
    ) {
        val scope = FunctionScope().apply(build)
        symbols += annotations
        symbols += scope.collectSymbols()
        returns?.let { symbols += it.symbols }

        val writer = CodeWriter()
        annotations.forEach { annotationSymbol -> writer.line(listOf(symbolToken(annotationSymbol))) }
        scope.render(writer, name, returns)
        blocks += Block(writer.result, isComment = false)
    }

    internal fun render(): List<List<Token>> =
        buildList {
            if (headerLines.isNotEmpty()) {
                headerLines.forEach { headerLine -> add(listOf(lineCommentToken(headerLine))) }
                add(emptyList())
            }

            add(listOf(keywordToken("package"), spaceToken, plainToken(packageName)))

            val imports = importLines()
            if (imports.isNotEmpty()) {
                add(emptyList())
                addAll(imports)
            }

            blocks.forEachIndexed { index, block ->
                val glued = index > 0 && blocks[index - 1].isComment
                if (!glued) add(emptyList())
                addAll(block.lines)
            }
        }

    /**
     * The import block, in the order ktlint asks for.
     *
     * The body only ever writes simple names, so two symbols that share a simple name would give a
     * file that does not compile. There is no aliasing yet, so that clash is an error here instead.
     */
    private fun importLines(): List<List<Token>> {
        val paths = symbols
            .asSequence()
            .filter { it.isImportable && it.packageName != packageName }
            .map { it.qualifiedName }
            .distinct()
            .sortedWith(IMPORT_ORDER)
            .toList()

        paths.groupBy { it.substringAfterLast('.') }.forEach { (simpleName, clashing) ->
            require(clashing.size == 1) {
                "Cannot import two symbols named $simpleName into $packageName, " +
                    "${clashing.joinToString(" and ")}. Generated code writes simple names and the " +
                    "DSL has no import alias yet."
            }
        }

        return paths.map { path -> listOf(keywordToken("import"), spaceToken, plainToken(path)) }
    }
}

/** The parameters and body of one generated function. */
@CodegenDsl
public class FunctionScope internal constructor() {
    private val parameters = mutableListOf<ParameterSpec>()
    private var body: BodyScope? = null

    /** A parameter with an explicit type and an optional default. */
    public fun parameter(
        name: String,
        type: TypeRef,
        default: Expression? = null,
    ) {
        parameters += ParameterSpec(name, type, default)
    }

    /** The same, for the common case where the type is just a symbol. */
    public fun parameter(
        name: String,
        symbol: Symbol,
        default: Expression? = null,
    ) {
        parameter(name, type(symbol), default)
    }

    /** The statements between the braces. */
    public fun body(build: BodyScope.() -> Unit) {
        body = BodyScope().apply(build)
    }

    internal fun collectSymbols(): List<Symbol> =
        buildList {
            parameters.forEach { parameter ->
                addAll(parameter.type.symbols)
                parameter.default?.let { addAll(it.symbols) }
            }
            body?.let { addAll(it.collectSymbols()) }
        }

    internal fun render(
        writer: CodeWriter,
        name: String,
        returns: TypeRef?,
    ) {
        val head = listOf(keywordToken("fun"), spaceToken, functionToken(name))
        val tail = buildList {
            add(punctuationToken(")"))
            if (returns != null) {
                add(punctuationToken(":"))
                add(spaceToken)
                addAll(returns.tokens)
            }
            add(spaceToken)
            add(punctuationToken("{"))
        }

        val flat = buildList {
            addAll(head)
            add(punctuationToken("("))
            parameters.forEachIndexed { index, parameter ->
                if (index > 0) {
                    add(punctuationToken(","))
                    add(spaceToken)
                }
                addAll(parameter.flatTokens())
            }
            addAll(tail)
        }

        val onOneLine = parameters.isEmpty() ||
            (parameters.size < FORCE_MULTILINE_PARAMETERS && writer.fits(flat))
        if (onOneLine) {
            writer.line(flat)
        } else {
            writer.line(head + punctuationToken("("))
            writer.indented {
                parameters.forEach { parameter -> parameter.render(writer) }
            }
            writer.line(tail)
        }

        writer.indented { body?.render(writer) }
        writer.line(listOf(punctuationToken("}")))
    }
}

internal class ParameterSpec(
    val name: String,
    val type: TypeRef,
    val default: Expression?,
) {
    fun headTokens(): List<Token> = listOf(parameterToken(name), punctuationToken(":"), spaceToken) + type.tokens

    fun flatTokens(): List<Token> {
        val head = headTokens()

        return if (default == null) {
            head
        } else {
            head + listOf(spaceToken, punctuationToken("="), spaceToken) + default.tokens
        }
    }

    fun render(writer: CodeWriter) {
        val comma = listOf(punctuationToken(","))
        if (default == null) {
            writer.line(headTokens() + comma)
        } else {
            val prefix = headTokens() + listOf(spaceToken, punctuationToken("="), spaceToken)
            writer.expression(default, prefix, comma)
        }
    }
}

/** The statements inside a generated function. */
@CodegenDsl
public class BodyScope internal constructor() {
    private val statements = mutableListOf<Statement>()

    /** A call written as a statement of its own. */
    public fun call(
        symbol: Symbol,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ) {
        statement(buildCall(listOf(functionToken(symbol.simpleName)), listOf(symbol), multiline, build))
    }

    /** A call of a function that is already in scope, written as a statement. */
    public fun call(
        name: String,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ) {
        statement(buildCall(listOf(functionToken(name)), emptyList(), multiline, build))
    }

    /**
     * A call used as a value, as in `assign("theme", callOf(DynamicMaterialTheme))`.
     *
     * The statement form of [call] hands back nothing and is the nearer candidate inside a body, so
     * the expression form needs a name of its own.
     */
    public fun callOf(
        symbol: Symbol,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(symbol.simpleName)), listOf(symbol), multiline, build)

    /** The same, for a call of a function that is already in scope. */
    public fun callOf(
        name: String,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(name)), emptyList(), multiline, build)

    /** Any expression, written as a statement. */
    public fun statement(value: Expression) {
        statements += Statement(emptyList(), value)
    }

    /** A local `val`. */
    public fun assign(
        name: String,
        value: Expression,
    ) {
        val prefix = listOf(
            keywordToken("val"),
            spaceToken,
            plainToken(name),
            spaceToken,
            punctuationToken("="),
            spaceToken,
        )
        statements += Statement(prefix, value)
    }

    /** An explicit `return`. */
    public fun returns(value: Expression) {
        statements += Statement(listOf(keywordToken("return"), spaceToken), value)
    }

    /** A comment line. */
    public fun comment(text: String) {
        statements += Statement(listOf(lineCommentToken(text)), null)
    }

    /** A blank line between statements. */
    public fun blankLine() {
        statements += Statement(emptyList(), null)
    }

    internal fun collectSymbols(): List<Symbol> = statements.flatMap { it.value?.symbols.orEmpty() }

    internal fun render(writer: CodeWriter) {
        statements.forEach { statement ->
            val value = statement.value
            if (value == null) {
                writer.line(statement.prefix)
            } else {
                writer.expression(value, statement.prefix)
            }
        }
    }
}

internal class Statement(
    val prefix: List<Token>,
    val value: Expression?,
)

/**
 * The import layout `*`, `java.**`, `javax.**`, `kotlin.**`.
 *
 * Aliased imports are out of scope, since a [Symbol] carries no alias, so the `^` group that
 * ktlint_official puts last never comes up.
 *
 * Within a group the paths are compared as plain strings, so a class sitting straight in a package
 * lands above that package's subpackages. That falls out of upper case sorting before lower case and
 * is exactly what the repo's own sources look like.
 */
internal val IMPORT_ORDER: Comparator<String> = compareBy<String> { path ->
    when {
        path == "java" || path.startsWith("java.") -> 1
        path == "javax" || path.startsWith("javax.") -> 2
        path == "kotlin" || path.startsWith("kotlin.") -> 3
        else -> 0
    }
}.thenBy { it }
