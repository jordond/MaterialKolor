package com.materialkolor.builder.codegen.symbol

/**
 * The handful of symbols the DSL tests write their examples with.
 *
 * Production code names the one or two symbols it needs in the file that needs them, and B-109's
 * generated library symbol table takes over from there. This fixture only has to keep tests short.
 */
internal object TestSymbols {
    val Boolean: Symbol = Symbol("kotlin", "Boolean", SymbolKind.Class)
    val Int: Symbol = Symbol("kotlin", "Int", SymbolKind.Class)
    val String: Symbol = Symbol("kotlin", "String", SymbolKind.Class)
    val Unit: Symbol = Symbol("kotlin", "Unit", SymbolKind.Class)

    val Composable: Symbol = Symbol("androidx.compose.runtime", "Composable", SymbolKind.Annotation)
    val Color: Symbol = Symbol("androidx.compose.ui.graphics", "Color", SymbolKind.Class)
    val IsSystemInDarkTheme: Symbol =
        Symbol("androidx.compose.foundation", "isSystemInDarkTheme", SymbolKind.Function)
    val DynamicMaterialTheme: Symbol = Symbol("com.materialkolor", "DynamicMaterialTheme", SymbolKind.Function)
}
