package com.materialkolor.builder.core.platform

import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.QuarantineReason
import com.materialkolor.builder.domain.persist.StorageKey
import com.materialkolor.builder.domain.persist.StorageKeys
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryStoreFactoryTest {
    private val factory = InMemoryStoreFactory(now = { MOVED_AT })
    private val store = factory.create(StorageKeys.PREFS, Preferences.Codec, Preferences())

    @Test
    fun get_unreadableText_movesItAsideAndReportsItOnce() =
        runTest {
            val reported = mutableListOf<Quarantined>()
            backgroundScope.launch { factory.quarantined.toList(reported) }
            factory.seed(StorageKeys.PREFS, BROKEN)

            store.get() shouldBe Preferences()
            store.get() shouldBe Preferences()
            runCurrent()

            reported shouldBe listOf(Quarantined(StorageKeys.PREFS, QuarantineReason.Unreadable))
            factory.textAt(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT)) shouldBe BROKEN
            factory.textAt(StorageKeys.PREFS) shouldBe null
        }

    @Test
    fun get_sameKeyUnreadableTwiceInOneMillisecond_keepsBothTextsAside() =
        runTest {
            factory.seed(StorageKeys.PREFS, BROKEN)
            store.get()
            factory.seed(StorageKeys.PREFS, ALSO_BROKEN)

            store.get() shouldBe Preferences()

            factory.textAt(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT)) shouldBe BROKEN
            factory.textAt(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT + 1)) shouldBe ALSO_BROKEN
        }

    @Test
    fun data_unreadableText_emitsTheDefault() =
        runTest {
            factory.seed(StorageKeys.PREFS, BROKEN)

            store.data.first() shouldBe Preferences()
            factory.textAt(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT)) shouldBe BROKEN
        }

    @Test
    fun update_overUnreadableText_keepsTheOldTextAside() =
        runTest {
            factory.seed(StorageKeys.PREFS, BROKEN)

            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe null

            store.get().hueLock shouldBe true
            factory.textAt(StorageKeys.quarantine(StorageKeys.PREFS, MOVED_AT)) shouldBe BROKEN
        }

    // b-301a
    @Test
    fun get_newerSchemaText_leavesItInPlaceAndReportsItOnce() =
        runTest {
            val reported = mutableListOf<Quarantined>()
            backgroundScope.launch { factory.quarantined.toList(reported) }
            factory.seed(StorageKeys.PREFS, NEWER)
            val sameKey = factory.create(StorageKeys.PREFS, Preferences.Codec, Preferences())

            store.get() shouldBe Preferences()
            store.get() shouldBe Preferences()
            sameKey.data.first() shouldBe Preferences()
            runCurrent()

            reported shouldBe listOf(Quarantined(StorageKeys.PREFS, QuarantineReason.NewerSchema))
            factory.textAt(StorageKeys.PREFS) shouldBe NEWER
            factory.keys shouldBe setOf(StorageKeys.PREFS)
        }

    // b-301a
    @Test
    fun update_overNewerSchemaText_isRefusedAndLeavesTheText() =
        runTest {
            val reported = mutableListOf<Quarantined>()
            backgroundScope.launch { factory.quarantined.toList(reported) }
            factory.seed(StorageKeys.PREFS, NEWER)

            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe StoreError.Unavailable
            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe StoreError.Unavailable
            runCurrent()

            store.get().hueLock shouldBe false
            factory.textAt(StorageKeys.PREFS) shouldBe NEWER
            factory.keys shouldBe setOf(StorageKeys.PREFS)
            reported shouldBe listOf(Quarantined(StorageKeys.PREFS, QuarantineReason.NewerSchema))
        }

    @Test
    fun update_failuresQueued_failsThatManyTimesThenWrites() =
        runTest {
            factory.failNextUpdates(2, StoreError.QuotaExceeded)

            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe StoreError.QuotaExceeded
            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe StoreError.QuotaExceeded
            store.get().hueLock shouldBe false
            store.update { prefs -> prefs.copy(hueLock = true) } shouldBe null
            store.get().hueLock shouldBe true
        }

    @Test
    fun delete_writtenRecord_readsAsTheDefaultAgain() =
        runTest {
            store.update { prefs -> prefs.copy(seedLock = true) }

            store.delete() shouldBe null

            store.get() shouldBe Preferences()
            factory.keys shouldBe emptySet()
        }

    @Test
    fun delete_failureQueued_failsOnceThenRemoves() =
        runTest {
            store.update { prefs -> prefs.copy(seedLock = true) }
            factory.failNextDeletes(1, StoreError.Unavailable)

            store.delete() shouldBe StoreError.Unavailable
            store.get().seedLock shouldBe true
            store.delete() shouldBe null
            factory.keys shouldBe emptySet()
        }

    @Test
    fun writeFromAnotherTab_ownAndForeignKeys_reportsOnlyTheOwnKey() =
        runTest {
            val changed = mutableListOf<StorageKey>()
            backgroundScope.launch { factory.externalChanges.toList(changed) }
            runCurrent()

            factory.writeFromAnotherTab(StorageKeys.PREFS, Preferences.Codec.encode(Preferences(hueLock = true)))
            factory.writeFromAnotherTab("someone:else", "{}")
            runCurrent()

            changed shouldBe listOf(StorageKey.Prefs)
            store.get().hueLock shouldBe true
        }
}

private const val MOVED_AT = 1_700_000_000_000

private const val BROKEN = "not json"

private const val ALSO_BROKEN = "still not json"

private const val NEWER = """{"schema":999,"data":{"hueLock":true}}"""
