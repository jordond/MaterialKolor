package com.materialkolor.builder.codegen.symbol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbolTest {
    @Test
    fun of_qualifiedName_splitsPackageFromSimpleName() {
        val symbol = Symbol.of("com.materialkolor.dynamiccolor.ColorSpec", SymbolKind.Class)

        assertEquals("com.materialkolor.dynamiccolor", symbol.packageName)
        assertEquals("ColorSpec", symbol.simpleName)
        assertEquals("com.materialkolor.dynamiccolor.ColorSpec", symbol.qualifiedName)
    }

    @Test
    fun isImportable_defaultAndLocalPackages_isFalse() {
        assertFalse(TestSymbols.Boolean.isImportable)
        assertFalse(Symbol("kotlin.collections", "List", SymbolKind.Class).isImportable)
        assertFalse(Symbol.local("SeedColor", SymbolKind.Property).isImportable)
        assertTrue(Symbol("kotlin.time", "Duration", SymbolKind.Class).isImportable)
        assertTrue(TestSymbols.Composable.isImportable)
    }

    @Test
    fun local_simpleName_hasNoPackageInItsQualifiedName() {
        assertEquals("SeedColor", Symbol.local("SeedColor", SymbolKind.Property).qualifiedName)
    }

    @Test
    fun equals_samePackageAndName_treatsSymbolsAsOne() {
        val first = Symbol("com.materialkolor", "DynamicMaterialTheme", SymbolKind.Function)
        val second = Symbol.of("com.materialkolor.DynamicMaterialTheme", SymbolKind.Function)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }
}
