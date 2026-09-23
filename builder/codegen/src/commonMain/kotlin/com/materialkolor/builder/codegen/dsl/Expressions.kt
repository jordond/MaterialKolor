package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.SymbolKind

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

/**
 * `kotlin.Unit`, which is what a lambda returns when nothing else is said.
 *
 * B-109's generated library symbol table replaces this.
 */
private val UnitSymbol: Symbol = Symbol("kotlin", "Unit", SymbolKind.Class)

/** A function type such as `@Composable () -> Unit`. */
public fun lambdaType(
    parameters: List<TypeRef> = emptyList(),
    returns: TypeRef = type(UnitSymbol),
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

/**
 * Whether this call breaks over several lines however much room is left on the line.
 *
 * A call with no arguments has nothing to break on, so asking it to go multiline changes nothing.
 */
internal val CallShape.breaksOnItsOwn: Boolean
    get() = alwaysMultiline && arguments.isNotEmpty()

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
    val forced = multiline || arguments.any { argument -> argument.value.call?.breaksOnItsOwn == true }
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
        call = CallShape(callee, arguments, forced),
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

internal fun symbolToken(symbol: Symbol): Token =
    when (symbol.kind) {
        SymbolKind.Class -> typeToken(symbol.simpleName)
        SymbolKind.Function -> functionToken(symbol.simpleName)
        SymbolKind.Property -> plainToken(symbol.simpleName)
        SymbolKind.Annotation -> annotationToken("@${symbol.simpleName}")
    }
