package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.web.interop.localStorageRead
import com.materialkolor.builder.web.interop.localStorageRemove
import com.materialkolor.builder.web.interop.localStorageWrite
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// b-310aa
class WebStoreFactoryTest {
    private val factory = WebStoreFactory(now = { MOVED_AT })
    private val store = factory.create(StorageKeys.PREFS, Preferences.Codec, Preferences())

    @AfterTest
    fun tearDown() {
        localStorageRemove(StorageKeys.PREFS)
        localStorageRemove(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT))
    }

    @Test
    fun fromNewerBuild_eachKindOfRecord_isTrueOnlyForANewerSchemaAndOnlyReads() {
        assertFalse(settled { store.fromNewerBuild() })
        assertNull(settled { store.update { prefs -> prefs.copy(hueLock = true) } })
        assertFalse(settled { store.fromNewerBuild() })

        localStorageWrite(StorageKeys.PREFS, BROKEN)
        assertFalse(settled { store.fromNewerBuild() })
        assertEquals(BROKEN, localStorageRead(StorageKeys.PREFS))
        assertNull(localStorageRead(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT)))

        localStorageWrite(StorageKeys.PREFS, NEWER)
        assertTrue(settled { store.fromNewerBuild() })
        assertEquals(NEWER, localStorageRead(StorageKeys.PREFS))
    }

    @Test
    fun update_overNewerSchemaText_neverRunsItsBlock() {
        localStorageWrite(StorageKeys.PREFS, NEWER)
        var ran = false

        val error = settled {
            store.update { prefs ->
                ran = true
                prefs
            }
        }

        assertEquals(StoreError.Unavailable, error)
        assertFalse(ran)
        assertEquals(NEWER, localStorageRead(StorageKeys.PREFS))
    }
}

/**
 * Run [block] to its end. Every call into localStorage is synchronous, so the store never suspends.
 */
private fun <T> settled(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result -> outcome = result })
    return checkNotNull(outcome) { "The store suspended" }.getOrThrow()
}

private const val MOVED_AT = 1_700_000_000_000

private const val BROKEN = "not json"

private const val NEWER = """{"schema":999,"data":{"hueLock":true}}"""
