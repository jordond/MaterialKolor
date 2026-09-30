package com.materialkolor.builder.codegen.symbol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbolTest {
    @Test
    fun isImportable_defaultAndLocalPackages_isFalse() {
        assertFalse(Symbols.Boolean.isImportable)
        assertFalse(Symbol("kotlin.collections", "List", SymbolKind.Class).isImportable)
        assertFalse(Symbol.local("SeedColor", SymbolKind.Property).isImportable)
        assertTrue(Symbol("kotlin.time", "Duration", SymbolKind.Class).isImportable)
        assertTrue(Symbols.Composable.isImportable)
    }
}
