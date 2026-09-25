package com.materialkolor.builder.codegen.dsl

/**
 * Writes a TOML file, which in practice means a version catalog snippet.
 *
 * Tables come out in the order they are added with a blank line between them, and the very first
 * table gets no leading blank so the file starts on something readable.
 */
public fun tomlFile(
    path: String,
    build: TomlScope.() -> Unit,
): GeneratedFile {
    val scope = TomlScope().apply(build)

    return GeneratedFile(path = path, language = Language.Toml, lines = scope.result())
}

/**
 * The top level of a generated TOML file.
 */
@CodegenDsl
public class TomlScope internal constructor() {
    private val lines = mutableListOf<List<Token>>()

    /**
     * A comment line.
     */
    public fun comment(text: String) {
        lines += listOf(commentToken("# $text"))
    }

    /**
     * A blank line.
     */
    public fun blankLine() {
        lines.add(emptyList())
    }

    /**
     * A `[name]` table header followed by whatever the block adds.
     */
    public fun table(
        name: String,
        build: TomlTableScope.() -> Unit,
    ) {
        if (lines.isNotEmpty()) blankLine()
        lines += listOf(
            punctuationToken("["),
            Token(TokenKind.TomlTable, keyText(name)),
            punctuationToken("]"),
        )
        lines += TomlTableScope().apply(build).result()
    }

    internal fun result(): List<List<Token>> = lines.toList()
}

/**
 * The entries under one TOML table.
 */
@CodegenDsl
public class TomlTableScope internal constructor() {
    private val lines = mutableListOf<List<Token>>()

    /**
     * A comment line.
     */
    public fun comment(text: String) {
        lines += listOf(commentToken("# $text"))
    }

    /**
     * A plain `key = "value"` entry.
     */
    public fun key(
        name: String,
        value: String,
    ) {
        lines += keyTokens(name) + stringToken(value)
    }

    /**
     * An entry whose value is an inline table, as most catalog libraries are.
     */
    public fun inlineTable(
        name: String,
        build: TomlInlineScope.() -> Unit,
    ) {
        val entries = TomlInlineScope().apply(build).result()
        require(entries.isNotEmpty()) { "An inline table needs at least one entry" }

        val tokens = buildList {
            addAll(keyTokens(name))
            add(punctuationToken("{"))
            add(spaceToken)
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    add(punctuationToken(","))
                    add(spaceToken)
                }
                addAll(keyTokens(entry.key))
                add(stringToken(entry.value))
            }
            add(spaceToken)
            add(punctuationToken("}"))
        }

        lines += tokens
    }

    internal fun result(): List<List<Token>> = lines.toList()
}

/**
 * The entries inside one inline table.
 */
@CodegenDsl
public class TomlInlineScope internal constructor() {
    private val entries = mutableListOf<TomlEntry>()

    /**
     * One `key = "value"` pair. The key may be dotted, as in `version.ref`.
     */
    public fun entry(
        key: String,
        value: String,
    ) {
        entries += TomlEntry(key, value)
    }

    internal fun result(): List<TomlEntry> = entries.toList()
}

internal class TomlEntry(
    val key: String,
    val value: String,
)

private const val TOML_HEX_DIGITS = "0123456789ABCDEF"

private fun keyTokens(name: String): List<Token> =
    listOf(
        Token(TokenKind.TomlKey, keyText(name)),
        spaceToken,
        punctuationToken("="),
        spaceToken,
    )

/**
 * A key as TOML wants to read it.
 *
 * Dots separate the parts of a path, and any part that is not made of letters, digits, dashes and
 * underscores is quoted, because TOML only takes those bare.
 */
private fun keyText(name: String): String {
    require(name.isNotBlank()) { "A TOML key needs a name" }
    val parts = name.split('.')
    require(parts.none { it.isEmpty() }) { "A TOML key cannot have an empty part, $name" }

    return parts.joinToString(".") { part ->
        if (part.all { it.isBareKeyCharacter() }) part else "\"${escapeToml(part)}\""
    }
}

private fun Char.isBareKeyCharacter(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '_' || this == '-'

private fun stringToken(value: String): Token = Token(TokenKind.StringLiteral, "\"${escapeToml(value)}\"")

/**
 * The body of a TOML basic string, without the quotes.
 *
 * TOML bans raw control characters the same way Kotlin does, so anything below a space comes out as
 * an escape, either one of the named ones or a `\u` sequence.
 */
private fun escapeToml(value: String): String =
    buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20 || character.code == 0x7F) {
                    append("\\u")
                    for (shift in 12 downTo 0 step 4) {
                        append(TOML_HEX_DIGITS[(character.code ushr shift) and 0xF])
                    }
                } else {
                    append(character)
                }
            }
        }
    }
