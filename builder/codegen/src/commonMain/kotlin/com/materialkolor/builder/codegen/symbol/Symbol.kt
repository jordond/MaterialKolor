package com.materialkolor.builder.codegen.symbol

/**
 * What a [Symbol] names, which decides how it is highlighted and how it is referred to in code.
 */
public enum class SymbolKind {
    Class,
    Function,
    Property,
    Annotation,
}

/**
 * Something a generated file can refer to by its short name once it has been imported.
 *
 * Every reference the code DSL writes goes through a symbol, which is how imports end up collected
 * instead of hand written. A symbol with an empty [packageName] is a local name and never imports.
 */
public class Symbol(
    public val packageName: String,
    public val simpleName: String,
    public val kind: SymbolKind,
) {
    init {
        require(simpleName.isNotBlank()) { "A symbol needs a simple name" }
    }

    /**
     * The full name, which is also the import path when this symbol is worth importing.
     */
    public val qualifiedName: String
        get() = if (packageName.isEmpty()) simpleName else "$packageName.$simpleName"

    /**
     * Whether this symbol is worth an import line at all.
     *
     * Local names and anything Kotlin imports by default are already in scope, and writing them out
     * would only give ktlint something to complain about.
     */
    public val isImportable: Boolean
        get() = packageName.isNotEmpty() && packageName !in DEFAULT_IMPORT_PACKAGES

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val symbol = other as? Symbol ?: return false

        return packageName == symbol.packageName &&
            simpleName == symbol.simpleName &&
            kind == symbol.kind
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + simpleName.hashCode()
        result = 31 * result + kind.hashCode()
        return result
    }

    override fun toString(): String = qualifiedName

    public companion object {
        /**
         * The packages Kotlin puts in scope on its own, so importing from them is redundant.
         */
        private val DEFAULT_IMPORT_PACKAGES = setOf(
            "kotlin",
            "kotlin.annotation",
            "kotlin.collections",
            "kotlin.comparisons",
            "kotlin.io",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text",
        )

        /**
         * A symbol read off a fully qualified name, which keeps call sites short.
         */
        public fun of(
            qualifiedName: String,
            kind: SymbolKind,
        ): Symbol {
            val separator = qualifiedName.lastIndexOf('.')

            return Symbol(
                packageName = if (separator < 0) "" else qualifiedName.substring(0, separator),
                simpleName = qualifiedName.substring(separator + 1),
                kind = kind,
            )
        }

        /**
         * A name that is declared in the file being generated, so it never imports.
         */
        public fun local(
            simpleName: String,
            kind: SymbolKind,
        ): Symbol = Symbol("", simpleName, kind)
    }
}
