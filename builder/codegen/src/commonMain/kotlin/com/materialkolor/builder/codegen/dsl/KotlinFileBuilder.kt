package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.SymbolKind
import com.materialkolor.builder.codegen.symbol.Symbols

/** Keeps the nested builder scopes from reaching into each other by accident. */
@DslMarker
public annotation class CodegenDsl

internal const val MAX_LINE_LENGTH = 120
internal const val INDENT = "    "

/**
 * The parameter count at which ktlint_official puts every parameter on a line of its own.
 *
 * The emitted files are linted with this repo's own `.editorconfig`, so the writer has to reach the
 * same answer the formatter would.
 */
internal const val FORCE_MULTILINE_PARAMETERS = 2

/**
 * A type as it is written in generated source, along with the symbols it needs imported.
 *
 * Build one with [type] or [lambdaType] rather than calling the constructor.
 */
public class TypeRef internal constructor(
    internal val tokens: List<Token>,
    internal val symbols: List<Symbol>,
)

/** A type written as a symbol, optionally with type arguments, as in `List<Color>`. */
public fun type(
    symbol: Symbol,
    vararg arguments: TypeRef,
): TypeRef {
    val tokens = buildList {
        add(typeToken(symbol.simpleName))
        if (arguments.isNotEmpty()) {
            add(punctuationToken("<"))
            arguments.forEachIndexed { index, argument ->
                if (index > 0) {
                    add(punctuationToken(","))
                    add(spaceToken)
                }
                addAll(argument.tokens)
            }
            add(punctuationToken(">"))
        }
    }

    return TypeRef(tokens, listOf(symbol) + arguments.flatMap { it.symbols })
}

/** A type declared by the file being generated, so it carries no import. */
public fun type(simpleName: String): TypeRef = TypeRef(listOf(typeToken(simpleName)), emptyList())

/** The same type with a question mark on it. */
public fun TypeRef.orNull(): TypeRef = TypeRef(tokens + punctuationToken("?"), symbols)

/** A function type such as `@Composable () -> Unit`. */
public fun lambdaType(
    parameters: List<TypeRef> = emptyList(),
    returns: TypeRef = type(Symbols.Unit),
    annotations: List<Symbol> = emptyList(),
): TypeRef {
    val tokens = buildList {
        annotations.forEach { annotationSymbol ->
            add(annotationToken("@${annotationSymbol.simpleName}"))
            add(spaceToken)
        }
        add(punctuationToken("("))
        parameters.forEachIndexed { index, parameter ->
            if (index > 0) {
                add(punctuationToken(","))
                add(spaceToken)
            }
            addAll(parameter.tokens)
        }
        add(punctuationToken(")"))
        add(spaceToken)
        add(punctuationToken("->"))
        add(spaceToken)
        addAll(returns.tokens)
    }

    val symbols = annotations + parameters.flatMap { it.symbols } + returns.symbols
    return TypeRef(tokens, symbols)
}

/**
 * A piece of generated code that sits on the right of an `=` or stands alone as a statement.
 *
 * A call keeps its own shape so the writer can break it over several lines when it grows past the
 * column limit, rather than guessing from the flattened text.
 */
public class Expression internal constructor(
    internal val tokens: List<Token>,
    internal val symbols: List<Symbol>,
    internal val call: CallShape? = null,
)

internal class CallShape(
    val callee: List<Token>,
    val arguments: List<RenderedArgument>,
    val alwaysMultiline: Boolean,
)

internal class RenderedArgument(
    val prefix: List<Token>,
    val value: Expression,
)

/** A name that is already in scope, such as a parameter or a value declared in the same file. */
public fun ref(name: String): Expression = Expression(listOf(plainToken(name)), emptyList())

/** A reference to a symbol, which imports it. */
public fun ref(symbol: Symbol): Expression = Expression(listOf(symbolToken(symbol)), listOf(symbol))

/** An expression assembled from tokens directly, for the cases the DSL has no shape for. */
public fun raw(
    tokens: List<Token>,
    symbols: List<Symbol> = emptyList(),
): Expression = Expression(tokens, symbols)

/**
 * A call of an imported function, as in `DynamicMaterialTheme(seedColor = SeedColor)`.
 *
 * Set [multiline] to give the call one argument per line even when it would fit, which is how a
 * person would write a call that is the whole point of the file.
 */
public fun call(
    symbol: Symbol,
    multiline: Boolean = false,
    build: ArgumentsScope.() -> Unit = {},
): Expression = buildCall(listOf(functionToken(symbol.simpleName)), listOf(symbol), multiline, build)

/** A call of a function that is already in scope. */
public fun call(
    name: String,
    multiline: Boolean = false,
    build: ArgumentsScope.() -> Unit = {},
): Expression = buildCall(listOf(functionToken(name)), emptyList(), multiline, build)

internal fun buildCall(
    callee: List<Token>,
    calleeSymbols: List<Symbol>,
    multiline: Boolean,
    build: ArgumentsScope.() -> Unit,
): Expression {
    val arguments = ArgumentsScope().apply(build).arguments
    val flat = buildList {
        addAll(callee)
        add(punctuationToken("("))
        arguments.forEachIndexed { index, argument ->
            if (index > 0) {
                add(punctuationToken(","))
                add(spaceToken)
            }
            addAll(argument.prefix)
            addAll(argument.value.tokens)
        }
        add(punctuationToken(")"))
    }

    return Expression(
        tokens = flat,
        symbols = calleeSymbols + arguments.flatMap { it.value.symbols },
        call = CallShape(callee, arguments, multiline),
    )
}

/** Collects the arguments of one call. */
@CodegenDsl
public class ArgumentsScope internal constructor() {
    internal val arguments: MutableList<RenderedArgument> = mutableListOf()

    /** A named argument, which is how generated code always writes them. */
    public fun argument(
        name: String,
        value: Expression,
    ) {
        val prefix = listOf(parameterToken(name), spaceToken, punctuationToken("="), spaceToken)
        arguments += RenderedArgument(prefix, value)
    }

    /** A positional argument, for calls where a name would only add noise. */
    public fun argument(value: Expression) {
        arguments += RenderedArgument(emptyList(), value)
    }

    /** A named argument that disappears when there is nothing to say. */
    public fun optionalArgument(
        name: String,
        value: Expression?,
    ) {
        if (value != null) argument(name, value)
    }

    /**
     * A nested call used as an argument value.
     *
     * This repeats the top level [call] on purpose. Inside a body the statement form of `call` is
     * the nearer candidate, and the DSL marker turns that into a compile error rather than a stray
     * statement, so the argument scope has to offer the expression form itself.
     */
    public fun call(
        symbol: Symbol,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(symbol.simpleName)), listOf(symbol), multiline, build)

    /** A nested call of a function that is already in scope, used as an argument value. */
    public fun call(
        name: String,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(name)), emptyList(), multiline, build)
}

/** Lays tokens out into lines, breaking calls that run past the column limit. */
internal class CodeWriter {
    private val lines = mutableListOf<List<Token>>()
    private var depth = 0

    val result: List<List<Token>>
        get() = lines.toList()

    fun blankLine() {
        lines.add(emptyList())
    }

    fun line(tokens: List<Token>) {
        if (tokens.isEmpty()) {
            blankLine()
        } else {
            lines += if (depth == 0) tokens else listOf(plainToken(INDENT.repeat(depth))) + tokens
        }
    }

    fun indented(body: () -> Unit) {
        depth++
        body()
        depth--
    }

    fun fits(tokens: List<Token>): Boolean = depth * INDENT.length + tokens.sumOf { it.text.length } <= MAX_LINE_LENGTH

    fun expression(
        value: Expression,
        prefix: List<Token> = emptyList(),
        suffix: List<Token> = emptyList(),
    ) {
        val flat = prefix + value.tokens + suffix
        val shape = value.call
        val broken = shape != null &&
            shape.arguments.isNotEmpty() &&
            (shape.alwaysMultiline || !fits(flat))

        if (shape == null || !broken) {
            line(flat)
            return
        }

        line(prefix + shape.callee + punctuationToken("("))
        indented {
            shape.arguments.forEach { argument ->
                expression(argument.value, argument.prefix, listOf(punctuationToken(",")))
            }
        }
        line(listOf(punctuationToken(")")) + suffix)
    }
}

internal fun symbolToken(symbol: Symbol): Token =
    when (symbol.kind) {
        SymbolKind.Class -> typeToken(symbol.simpleName)
        SymbolKind.Function -> functionToken(symbol.simpleName)
        SymbolKind.Property -> plainToken(symbol.simpleName)
        SymbolKind.Annotation -> annotationToken("@${symbol.simpleName}")
    }

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
        blocks += Block(lines.map { listOf(commentToken("// $it")) }, isComment = true)
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
                headerLines.forEach { headerLine -> add(listOf(commentToken("// $headerLine"))) }
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

    private fun importLines(): List<List<Token>> =
        symbols
            .asSequence()
            .filter { it.isImportable && it.packageName != packageName }
            .map { it.qualifiedName }
            .distinct()
            .sortedWith(IMPORT_ORDER)
            .map { path -> listOf(keywordToken("import"), spaceToken, plainToken(path)) }
            .toList()
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

        val onOneLine = parameters.size < FORCE_MULTILINE_PARAMETERS && writer.fits(flat)
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
        statements += Statement(listOf(commentToken("// $text")), null)
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
 * The ktlint_official import order, which is the layout `*`, `java.**`, `javax.**`, `kotlin.**`.
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
