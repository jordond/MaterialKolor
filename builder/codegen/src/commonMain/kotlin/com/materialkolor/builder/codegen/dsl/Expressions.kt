package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol
import com.materialkolor.builder.codegen.symbol.SymbolKind
import com.materialkolor.builder.codegen.symbol.Symbols
import kotlin.jvm.JvmName

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
): TypeRef = lambdaType(parameters, returns, annotations.map { AnnotationSpec(it) })

/** The same, for annotations that take arguments. */
@JvmName("lambdaTypeWithAnnotations")
public fun lambdaType(
    parameters: List<TypeRef> = emptyList(),
    returns: TypeRef = type(Symbols.Unit),
    annotations: List<AnnotationSpec>,
): TypeRef {
    val tokens = buildList {
        annotations.forEach { annotation ->
            addAll(annotation.expression.tokens)
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

    val symbols = annotations.flatMap { it.expression.symbols } + parameters.flatMap { it.symbols } + returns.symbols
    return TypeRef(tokens, symbols)
}

/**
 * A piece of generated code that sits on the right of an `=` or stands alone as a statement.
 *
 * [tokens] is the expression written on one line. Anything that can break over several lines keeps
 * its [shape] too, so the writer can lay it out properly when it grows past the column limit rather
 * than guessing from the flattened text. [breaksOnItsOwn] is set when one line is never an option,
 * such as a `when` or a lambda with more than one statement, and it spreads to whatever holds it.
 */
public class Expression internal constructor(
    internal val tokens: List<Token>,
    internal val symbols: List<Symbol>,
    internal val shape: Shape? = null,
    internal val breaksOnItsOwn: Boolean = false,
)

/** The ways an expression can break over several lines, one per form the DSL offers. */
internal sealed interface Shape

internal class CallShape(
    val callee: List<Token>,
    val arguments: List<RenderedArgument>,
    val argumentsBreak: Boolean,
    val trailing: LambdaShape?,
) : Shape {
    /** The arguments in their parentheses on one line, or nothing when a trailing lambda is all there is. */
    val flatArguments: List<Token> =
        if (arguments.isEmpty() && trailing != null) {
            emptyList()
        } else {
            buildList {
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
        }
}

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
    val scope = ArgumentsScope().apply(build)
    val arguments = scope.arguments.toList()
    val trailing = scope.trailing
    val head = callee + typeArgumentTokens(scope.typeArguments)
    val argumentsBreak = arguments.isNotEmpty() && (multiline || arguments.any { it.value.breaksOnItsOwn })
    val shape = CallShape(head, arguments, argumentsBreak, trailing)

    val flat = buildList {
        addAll(head)
        addAll(shape.flatArguments)
        if (trailing != null) {
            add(spaceToken)
            addAll(trailing.tokens)
        }
    }

    return Expression(
        tokens = flat,
        symbols = calleeSymbols +
            scope.typeArguments.flatMap { it.symbols } +
            arguments.flatMap { it.value.symbols } +
            trailing?.symbols.orEmpty(),
        shape = shape,
        breaksOnItsOwn = argumentsBreak || trailing?.breaksOnItsOwn == true,
    )
}

private fun typeArgumentTokens(arguments: List<TypeRef>): List<Token> =
    if (arguments.isEmpty()) {
        emptyList()
    } else {
        buildList {
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

/** Collects the arguments of one call. */
@CodegenDsl
public class ArgumentsScope internal constructor() {
    internal val arguments: MutableList<RenderedArgument> = mutableListOf()
    internal val typeArguments: MutableList<TypeRef> = mutableListOf()
    internal var trailing: LambdaShape? = null

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
     * A type argument, as in `staticCompositionLocalOf<AppColors> { ... }`.
     *
     * Only worth writing when the compiler cannot infer it, which for a lambda that only throws is
     * every time.
     */
    public fun typeArgument(type: TypeRef) {
        typeArguments += type
    }

    /**
     * The lambda written after the parentheses, as in `remember(seedColor) { ... }`.
     *
     * A call with nothing else to pass drops its parentheses, so `staticCompositionLocalOf { ... }`
     * comes out the way ktlint wants it.
     */
    public fun trailingLambda(
        parameter: String? = null,
        build: BodyScope.() -> Unit,
    ) {
        require(trailing == null) { "A call takes one trailing lambda" }
        trailing = LambdaShape(parameter, BodyScope().apply(build))
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
