package com.materialkolor.builder.domain.persist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
