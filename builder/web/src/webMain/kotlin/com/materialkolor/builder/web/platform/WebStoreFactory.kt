package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Quarantined
import com.materialkolor.builder.core.platform.Store
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.persist.DecodeOutcome
import com.materialkolor.builder.domain.persist.QuarantineReason
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.web.interop.localStorageRead
import com.materialkolor.builder.web.interop.localStorageRemove
import com.materialkolor.builder.web.interop.localStorageWrite
import com.materialkolor.builder.web.interop.onLocalStorageChange
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * Stores kept in localStorage, one key per record, as the text their codec writes.
 *
 * Nothing is cached, so every read sees what another tab wrote a moment ago. Another tab's write
 * arrives as one `storage` event, which lands on [externalChanges] and makes every [Store.data]
 * read its key again.
 *
 * This talks to localStorage itself rather than through kstore-storage. With its cache off, kstore
 * adds a lock this single thread does not need, lets a full storage escape as a bare JS exception,
 * and its update flow never hears about another tab's write.
 *
 * Text that no longer decodes is moved to its quarantine key and reported once, the same way the
 * in-memory stores do it. When there is no room to move it, it stays where it is and every update to
 * that key is turned down with the storage error, so a write never lands on top of it.
 *
 * A record a newer build wrote is never moved, since that build may still be open in another tab.
 * It reads as the default, every update to its key is turned down with [StoreError.Unavailable], and
 * it is reported once per key so the app can ask for a reload.
 *
 * @param[now] The time in milliseconds since the epoch, stamped on quarantine keys.
 */
internal class WebStoreFactory(
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : StoreFactory {
    // Bumped on every write this tab makes and every storage event, so each store rereads its key.
    private val writes = MutableStateFlow(0L)
    private val otherTabs = MutableSharedFlow<StorageKey>(extraBufferCapacity = CHANGE_BUFFER)
    private val quarantines = Channel<Quarantined>(Channel.UNLIMITED)
    private val newerReported = mutableSetOf<String>()

    override val externalChanges: Flow<StorageKey> = otherTabs.asSharedFlow()

    override val quarantined: Flow<Quarantined> = quarantines.receiveAsFlow()

    init {
        onLocalStorageChange { key ->
            writes.update { count -> count + 1 }
            key?.let(StorageKeys::parse)?.let(otherTabs::tryEmit)
        }
        exposeToE2e()
    }

    override fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T> = LocalStorageStore(key, codec, default)

    private fun wrote() = writes.update { count -> count + 1 }

    private inner class LocalStorageStore<T>(
        private val key: String,
        private val codec: RecordCodec<T>,
        private val default: T,
    ) : Store<T> {
        override val data: Flow<T> =
            writes
                .map { localStorageRead(key) }
                .distinctUntilChanged()
                .map { settle().value }
                .distinctUntilChanged()

        override suspend fun get(): T = settle().value

        // Nothing suspends between the read and the write, so no other write from this tab lands in between.
        override suspend fun update(block: (T) -> T): StoreError? {
            val reading = settle()
            reading.blocked?.let { error -> return error }
            val error = localStorageWrite(key, codec.encode(block(reading.value)))
            if (error == null) wrote()
            return error
        }

        override suspend fun fromNewerBuild(): Boolean {
            val text = localStorageRead(key) ?: return false
            val outcome = codec.decode(text)
            return outcome is DecodeOutcome.Quarantine && outcome.reason == QuarantineReason.NewerSchema
        }

        override suspend fun delete(): StoreError? {
            val error = localStorageRemove(key)
            if (error == null) wrote()
            return error
        }

        private fun settle(): Reading<T> {
            val text = localStorageRead(key) ?: return Reading(default, blocked = null)
            return when (val outcome = codec.decode(text)) {
                is DecodeOutcome.Ok -> {
                    Reading(outcome.value, blocked = null)
                }
                is DecodeOutcome.Quarantine -> {
                    val blocked = when (outcome.reason) {
                        QuarantineReason.NewerSchema -> leaveForNewerBuild()
                        QuarantineReason.Unreadable,
                        QuarantineReason.MigrationFailed,
                        QuarantineReason.WrongShape,
                        -> setAside(text, outcome.reason)
                    }
                    Reading(default, blocked)
                }
            }
        }

        private fun leaveForNewerBuild(): StoreError {
            if (newerReported.add(key)) quarantines.trySend(Quarantined(key, QuarantineReason.NewerSchema))
            return StoreError.Unavailable
        }

        // Copy first and remove after, so the text is never in neither place.
        private fun setAside(
            text: String,
            reason: QuarantineReason,
        ): StoreError? {
            localStorageWrite(freeQuarantineKey(), text)?.let { error -> return error }
            localStorageRemove(key)?.let { error -> return error }
            wrote()
            quarantines.trySend(Quarantined(key, reason))
            return null
        }

        private fun freeQuarantineKey(): String {
            var time = now()
            while (localStorageRead(StorageKeys.quarantine(key, time)) != null) time++
            return StorageKeys.quarantine(key, time)
        }
    }
}

/**
 * What a store holds, and the error that keeps it from being written when a newer build wrote it or
 * unreadable text could not be moved aside.
 */
private class Reading<T>(
    val value: T,
    val blocked: StoreError?,
)

private const val CHANGE_BUFFER = 64
