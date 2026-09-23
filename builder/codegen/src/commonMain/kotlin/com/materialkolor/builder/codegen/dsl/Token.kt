package com.materialkolor.builder.codegen.dsl

/**
 * The languages the builder knows how to emit.
 *
 * The viewer picks a highlighter from this and the zip picks an icon, so a target never has to
 * describe its own files twice.
 */
public enum class Language {
    Kotlin,
    Toml,
    Markdown,
}

/**
 * What a run of generated text means, which is all the code viewer needs to colour it.
 *
 * [ColorLiteral] is the one kind that carries data of its own, because a swatch beside the hex is
 * worth more than another shade of syntax colour.
 */
public enum class TokenKind {
    Keyword,
    Type,
    Function,
    Parameter,
    StringLiteral,
    NumberLiteral,
    ColorLiteral,
    Comment,
    Annotation,
    Punctuation,
    Plain,
    TomlTable,
    TomlKey,
}

/**
 * One run of generated text together with the meaning the viewer highlights it by.
 *
 * [color] is set on [TokenKind.ColorLiteral] tokens and holds the ARGB the literal stands for.
 */
public data class Token(
    public val kind: TokenKind,
    public val text: String,
    public val color: Int? = null,
)

/**
 * A file the builder produced, held as lines of tokens rather than as a string.
 *
 * Copy, zip and the code viewer all read [text] or [lines] off this one value, so what a reader
 * sees on screen and what lands in the download can never drift apart.
 */
public class GeneratedFile(
    public val path: String,
    public val language: Language,
    public val lines: List<List<Token>>,
) {
    /** The whole file, ending on the newline every text file is supposed to end on. */
    public val text: String = renderText(lines)

    override fun toString(): String = "GeneratedFile(path=$path, language=$language)"

    private companion object {
        fun renderText(lines: List<List<Token>>): String {
            if (lines.isEmpty()) return ""

            return buildString {
                lines.forEach { line ->
                    line.forEach { token -> append(token.text) }
                    append('\n')
                }
            }
        }
    }
}

internal fun keywordToken(text: String): Token = Token(TokenKind.Keyword, text)

internal fun typeToken(text: String): Token = Token(TokenKind.Type, text)

internal fun functionToken(text: String): Token = Token(TokenKind.Function, text)

internal fun parameterToken(text: String): Token = Token(TokenKind.Parameter, text)

internal fun annotationToken(text: String): Token = Token(TokenKind.Annotation, text)

internal fun punctuationToken(text: String): Token = Token(TokenKind.Punctuation, text)

internal fun commentToken(text: String): Token = Token(TokenKind.Comment, text)

/**
 * A `//` comment, written without the space when there is nothing to say.
 *
 * ktlint has no patience for a trailing space, and the generated file is linted like any other.
 */
internal fun lineCommentToken(text: String): Token = commentToken(if (text.isBlank()) "//" else "// $text")

internal fun plainToken(text: String): Token = Token(TokenKind.Plain, text)

internal val spaceToken: Token = plainToken(" ")
