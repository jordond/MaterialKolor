package com.materialkolor.builder.codegen.dsl

internal const val MAX_LINE_LENGTH = 120
internal const val INDENT = "    "

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
            (shape.breaksOnItsOwn || !fits(flat))

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
