package com.materialkolor.builder.codegen

/**
 * Stands in for the real `TokenKind` until B-108 lands the codegen DSL.
 *
 * Same trick as the domain stub next door. It sits in the codegen package so the kit imports the
 * final name today, and the merge that brings the real one in is a file delete.
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
