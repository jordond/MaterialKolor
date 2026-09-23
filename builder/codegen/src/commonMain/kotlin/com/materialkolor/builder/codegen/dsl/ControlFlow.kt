package com.materialkolor.builder.codegen.dsl

import com.materialkolor.builder.codegen.symbol.Symbol

/**
 * `if (condition) whenTrue else whenFalse`.
 *
 * It stays on one line while it fits. Past that, or when a branch cannot be written on one line, it
 * breaks into braces the way ktlint's multiline if rule asks. A [whenFalse] that is itself an
 * [ifElse] comes out as an `else if` chain, which is never kept on one line.
 */
public fun ifElse(
    condition: Expression,
    whenTrue: Expression,
    whenFalse: Expression,
): Expression {
    require(!condition.breaksOnItsOwn) { "The condition of an if has to fit on one line" }
    val shape = IfElseShape(condition, whenTrue, whenFalse)

    return Expression(
        tokens = buildList {
            addAll(shape.keyword)
            addAll(whenTrue.tokens)
            add(spaceToken)
            add(keywordToken("else"))
            add(spaceToken)
            addAll(whenFalse.tokens)
        },
        symbols = condition.symbols + whenTrue.symbols + whenFalse.symbols,
        shape = shape,
        breaksOnItsOwn = whenTrue.breaksOnItsOwn ||
            whenFalse.breaksOnItsOwn ||
            whenTrue.shape is IfElseShape ||
            whenFalse.shape is IfElseShape,
    )
}

/**
 * A `when`, with a [subject] or without one.
 *
 * It always takes a line per branch, which is how anyone would write it by hand.
 */
public fun whenExpression(
    subject: Expression? = null,
    build: WhenScope.() -> Unit,
): Expression {
    require(subject?.breaksOnItsOwn != true) { "The subject of a when has to fit on one line" }
    val scope = WhenScope().apply(build)
    val branches = scope.branches.toList()
    val otherwise = scope.otherwise
    require(branches.isNotEmpty() || otherwise != null) { "A when needs at least one branch" }
    val shape = WhenShape(subject, branches, otherwise)

    return Expression(
        tokens = shape.open + punctuationToken("}"),
        symbols = subject?.symbols.orEmpty() +
            branches.flatMap { it.condition.symbols + it.value.symbols } +
            otherwise?.symbols.orEmpty(),
        shape = shape,
        breaksOnItsOwn = true,
    )
}

/** The branches of one [whenExpression]. */
@CodegenDsl
public class WhenScope internal constructor() {
    internal val branches: MutableList<WhenBranch> = mutableListOf()
    internal var otherwise: Expression? = null

    /** `condition -> value`. */
    public fun branch(
        condition: Expression,
        value: Expression,
    ) {
        require(!condition.breaksOnItsOwn) { "A when condition has to fit on one line" }
        branches += WhenBranch(condition, value)
    }

    /** The `else ->` branch, which is written last wherever it was added. */
    public fun otherwise(value: Expression) {
        require(otherwise == null) { "A when has one else branch" }
        otherwise = value
    }

    /** A call used as a branch value, offered here for the same reason [ArgumentsScope.call] is. */
    public fun call(
        symbol: Symbol,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(symbol.simpleName)), listOf(symbol), multiline, build)

    /** The same, for a function that is already in scope. */
    public fun call(
        name: String,
        multiline: Boolean = false,
        build: ArgumentsScope.() -> Unit = {},
    ): Expression = buildCall(listOf(functionToken(name)), emptyList(), multiline, build)
}

/**
 * A lambda, as in `{ scheme -> scheme.copy(primary = SeedColor) }`.
 *
 * The body is an ordinary list of statements and the last one is the lambda's value, as in Kotlin.
 * A body that is a single expression stays on the line with the braces while it fits. Anything more
 * puts the parameter and arrow on the opening line and the statements below it.
 */
public fun lambda(
    parameter: String? = null,
    build: BodyScope.() -> Unit,
): Expression = LambdaShape(parameter, BodyScope().apply(build)).expression()

/** `left name right`, for infix calls such as `LocalExtendedColors provides colors`. */
public fun infix(
    left: Expression,
    name: String,
    right: Expression,
): Expression {
    require(!left.breaksOnItsOwn) { "The left side of an infix call has to fit on one line" }
    val head = left.tokens + listOf(spaceToken, functionToken(name), spaceToken)

    return Expression(
        tokens = head + right.tokens,
        symbols = left.symbols + right.symbols,
        shape = right.shape?.let { JoinedShape(head, right, emptyList()) },
        breaksOnItsOwn = right.breaksOnItsOwn,
    )
}

/** `Foo::class`, which imports `Foo`. */
public fun classLiteral(symbol: Symbol): Expression =
    Expression(
        tokens = listOf(typeToken(symbol.simpleName), punctuationToken("::"), keywordToken("class")),
        symbols = listOf(symbol),
    )

/** A member of this value, as in `MaterialTheme.colorScheme` or `LocalExtendedColors.current`. */
public fun Expression.member(name: String): Expression = access(this, plainToken(name), emptyList())

/** An imported extension property read off this value, which imports it. */
public fun Expression.member(symbol: Symbol): Expression = access(this, plainToken(symbol.simpleName), listOf(symbol))

// b-111b

/** `receiver[key]`, as in `properties[ThemeTokens.colors]`, written on one line. */
public fun Expression.index(key: Expression): Expression {
    require(!breaksOnItsOwn) { "The receiver of an index has to fit on one line" }
    require(!key.breaksOnItsOwn) { "The key of an index has to fit on one line" }

    return Expression(
        tokens = tokens + punctuationToken("[") + key.tokens + punctuationToken("]"),
        symbols = symbols + key.symbols,
    )
}

/**
 * A member function called on this value, as in `scheme.copy(primary = SeedColor)`.
 *
 * The receiver is written on one line and only the arguments break.
 */
public fun Expression.call(
    name: String,
    multiline: Boolean = false,
    build: ArgumentsScope.() -> Unit = {},
): Expression = memberCall(this, functionToken(name), emptyList(), multiline, build)

/** An imported extension function called on this value, as in `palette.onTone(40)`, which imports it. */
public fun Expression.call(
    symbol: Symbol,
    multiline: Boolean = false,
    build: ArgumentsScope.() -> Unit = {},
): Expression = memberCall(this, functionToken(symbol.simpleName), listOf(symbol), multiline, build)

private fun access(
    receiver: Expression,
    name: Token,
    symbols: List<Symbol>,
): Expression {
    val tail = listOf(punctuationToken("."), name)

    return Expression(
        tokens = receiver.tokens + tail,
        symbols = receiver.symbols + symbols,
        shape = receiver.shape?.let { JoinedShape(emptyList(), receiver, tail) },
        breaksOnItsOwn = receiver.breaksOnItsOwn,
    )
}

private fun memberCall(
    receiver: Expression,
    name: Token,
    symbols: List<Symbol>,
    multiline: Boolean,
    build: ArgumentsScope.() -> Unit,
): Expression {
    require(!receiver.breaksOnItsOwn) { "The receiver of a member call has to fit on one line" }

    return buildCall(receiver.tokens + punctuationToken(".") + name, receiver.symbols + symbols, multiline, build)
}

internal class IfElseShape(
    val condition: Expression,
    val whenTrue: Expression,
    val whenFalse: Expression,
) : Shape {
    /** `if (condition) `, which starts the one line form. */
    val keyword: List<Token> =
        listOf(keywordToken("if"), spaceToken, punctuationToken("(")) +
            condition.tokens +
            listOf(punctuationToken(")"), spaceToken)

    /** `if (condition) {`, which opens the broken form. */
    val open: List<Token> = keyword + punctuationToken("{")
}

internal class WhenBranch(
    val condition: Expression,
    val value: Expression,
)

internal class WhenShape(
    val subject: Expression?,
    val branches: List<WhenBranch>,
    val otherwise: Expression?,
) : Shape {
    /** `when (subject) {` or `when {`. */
    val open: List<Token> = buildList {
        add(keywordToken("when"))
        add(spaceToken)
        if (subject != null) {
            add(punctuationToken("("))
            addAll(subject.tokens)
            add(punctuationToken(")"))
            add(spaceToken)
        }
        add(punctuationToken("{"))
    }
}

internal class LambdaShape(
    val parameter: String?,
    val body: BodyScope,
) : Shape {
    /** `{` with the parameter and arrow after it, which is how the lambda opens when it breaks. */
    val open: List<Token> = buildList {
        add(punctuationToken("{"))
        if (parameter != null) {
            add(spaceToken)
            add(parameterToken(parameter))
            add(spaceToken)
            add(punctuationToken("->"))
        }
    }

    val isEmpty: Boolean = body.statements.isEmpty()

    /** The value of a body that is nothing but one expression, the only kind that fits on one line. */
    private val single: Expression? = body.statements
        .singleOrNull()
        ?.takeIf { it.prefix.isEmpty() }
        ?.value

    val breaksOnItsOwn: Boolean = !isEmpty && (single == null || single.breaksOnItsOwn)

    val tokens: List<Token> =
        when {
            isEmpty && parameter == null -> listOf(punctuationToken("{}"))
            single == null -> open + listOf(spaceToken, punctuationToken("}"))
            else -> open + spaceToken + single.tokens + listOf(spaceToken, punctuationToken("}"))
        }

    val symbols: List<Symbol> = body.collectSymbols()

    fun expression(): Expression = Expression(tokens, symbols, this, breaksOnItsOwn)
}

/**
 * Fixed tokens either side of an expression that can break, such as the left side and operator of
 * an infix call or the member read off a receiver. The inner expression breaks as it would alone.
 */
internal class JoinedShape(
    val head: List<Token>,
    val inner: Expression,
    val tail: List<Token>,
) : Shape
