package com.materialkolor.builder.domain.persist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StorageKeysTest {
    @Test
    fun storageKeys_everyKey_matchesTheArchitecture() {
        assertEquals("mk:index", StorageKeys.INDEX)
        assertEquals("mk:prefs", StorageKeys.PREFS)
        assertEquals("mk:splash", StorageKeys.SPLASH)
        assertEquals("mk:project:k3x9", StorageKeys.project("k3x9"))
        assertEquals("mk:history:k3x9", StorageKeys.history("k3x9"))
        assertEquals("mk:view:k3x9", StorageKeys.view("k3x9"))
    }

    @Test
    fun quarantine_key_keepsTheOriginalKeyAndTheTime() {
        assertEquals(
            "mk:quarantine:mk:project:k3x9:1758000000000",
            StorageKeys.quarantine(StorageKeys.project("k3x9"), time = 1_758_000_000_000L),
        )
    }

    @Test
    fun projectKeys_emptyOrColonId_areRefused() {
        listOf("", "a:b", ":").forEach { id ->
            assertFailsWith<IllegalArgumentException> { StorageKeys.project(id) }
            assertFailsWith<IllegalArgumentException> { StorageKeys.history(id) }
            assertFailsWith<IllegalArgumentException> { StorageKeys.view(id) }
        }
    }

    @Test
    fun parse_everyBuiltKey_readsBackWhatItNames() {
        val ids = listOf("k3x9", "p", "a-b_c.d")
        val expected = mapOf(
            StorageKeys.INDEX to StorageKey.Index,
            StorageKeys.PREFS to StorageKey.Prefs,
            StorageKeys.SPLASH to StorageKey.Splash,
        ) + ids.flatMap { id ->
            listOf(
                StorageKeys.project(id) to StorageKey.Project(id),
                StorageKeys.history(id) to StorageKey.History(id),
                StorageKeys.view(id) to StorageKey.View(id),
            )
        }

        expected.forEach { (key, named) -> assertEquals(named, StorageKeys.parse(key)) }
    }

    @Test
    fun parse_quarantineKey_isNull() {
        val keys = listOf(
            StorageKeys.INDEX,
            StorageKeys.PREFS,
            StorageKeys.SPLASH,
            StorageKeys.project("k3x9"),
            StorageKeys.history("k3x9"),
            StorageKeys.view("k3x9"),
        )

        keys.forEach { key -> assertNull(StorageKeys.parse(StorageKeys.quarantine(key, time = 1_758_000_000_000L))) }
    }

    @Test
    fun parse_keyTheBuilderDoesNotOwn_isNull() {
        val strangers = listOf(
            "",
            "mk:",
            "mk:index:k3x9",
            "mk:indexes",
            "mk:project:",
            "mk:project:a:b",
            "mk:history",
            "mk:unknown:k3x9",
            "k3x9",
            "theme",
            "MK:INDEX",
            "xmk:project:k3x9",
        )

        strangers.forEach { key -> assertNull(StorageKeys.parse(key), key) }
    }
}
