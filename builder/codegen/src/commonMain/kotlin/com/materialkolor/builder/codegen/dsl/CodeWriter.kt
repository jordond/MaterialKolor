package com.materialkolor.builder.codegen.dsl

internal const val MAX_LINE_LENGTH = 120
internal const val INDENT = "    "

/** Lays tokens out into lines, breaking whatever runs past the column limit. */
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

    /**
     * Writes [value] between [prefix] and [suffix], on one line when it fits and nothing inside it
     * insists on breaking.
     */
    fun expression(
        value: Expression,
        prefix: List<Token> = emptyList(),
        suffix: List<Token> = emptyList(),
    ) {
        val flat = prefix + value.tokens + suffix
        val shape = value.shape
        if (shape == null || (!value.breaksOnItsOwn && fits(flat))) {
            line(flat)
            return
        }

        when (shape) {
            is CallShape -> call(shape, flat, prefix, suffix)
            is LambdaShape -> lambda(shape, prefix, suffix)
            is IfElseShape -> ifElse(shape, prefix, suffix)
            is WhenShape -> whenBlock(shape, prefix, suffix)
            is JoinedShape -> expression(shape.inner, prefix + shape.head, shape.tail + suffix)
        }
    }

    /**
     * A call past the column limit.
     *
     * Without a trailing lambda the arguments go one per line. With one, the lambda breaks first and
     * the arguments stay on the opening line for as long as they fit there.
     */
    private fun call(
        shape: CallShape,
        flat: List<Token>,
        prefix: List<Token>,
        suffix: List<Token>,
    ) {
        val trailing = shape.trailing
        if (trailing == null) {
            if (shape.arguments.isEmpty()) {
                line(flat)
            } else {
                line(prefix + shape.callee + punctuationToken("("))
                arguments(shape.arguments)
                line(listOf(punctuationToken(")")) + suffix)
            }
            return
        }

        val opening = prefix + shape.callee + shape.flatArguments + spaceToken
        if (shape.arguments.isEmpty() || (!shape.argumentsBreak && fits(opening + trailing.open))) {
            lambda(trailing, opening, suffix)
        } else {
            line(prefix + shape.callee + punctuationToken("("))
            arguments(shape.arguments)
            lambda(trailing, listOf(punctuationToken(")"), spaceToken), suffix)
        }
    }

    private fun arguments(arguments: List<RenderedArgument>) {
        indented {
            arguments.forEach { argument ->
                expression(argument.value, argument.prefix, listOf(punctuationToken(",")))
            }
        }
    }

    /** A lambda with its statements below the opening brace, or `{}` when there are none. */
    private fun lambda(
        shape: LambdaShape,
        prefix: List<Token>,
        suffix: List<Token>,
    ) {
        if (shape.isEmpty) {
            line(prefix + shape.tokens + suffix)
            return
        }

        line(prefix + shape.open)
        indented { shape.body.render(this) }
        line(listOf(punctuationToken("}")) + suffix)
    }

    private fun ifElse(
        shape: IfElseShape,
        prefix: List<Token>,
        suffix: List<Token>,
    ) {
        line(prefix + shape.open)
        indented { expression(shape.whenTrue) }
        elseBranch(shape.whenFalse, suffix)
    }

    /** The `else` side of a broken if, following an `else if` chain down to its last branch. */
    private fun elseBranch(
        value: Expression,
        suffix: List<Token>,
    ) {
        val closing = listOf(punctuationToken("}"), spaceToken, keywordToken("else"), spaceToken)
        val chained = value.shape
        if (chained is IfElseShape) {
            line(closing + chained.open)
            indented { expression(chained.whenTrue) }
            elseBranch(chained.whenFalse, suffix)
        } else {
            line(closing + punctuationToken("{"))
            indented { expression(value) }
            line(listOf(punctuationToken("}")) + suffix)
        }
    }

    private fun whenBlock(
        shape: WhenShape,
        prefix: List<Token>,
        suffix: List<Token>,
    ) {
        val arrow = listOf(spaceToken, punctuationToken("->"), spaceToken)
        line(prefix + shape.open)
        indented {
            shape.branches.forEach { branch -> expression(branch.value, branch.condition.tokens + arrow) }
            shape.otherwise?.let { otherwise -> expression(otherwise, listOf(keywordToken("else")) + arrow) }
        }
        line(listOf(punctuationToken("}")) + suffix)
    }
}
