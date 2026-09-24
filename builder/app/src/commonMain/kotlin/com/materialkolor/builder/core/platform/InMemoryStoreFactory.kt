package com.materialkolor.builder.core.platform

import com.materialkolor.builder.domain.persist.DecodeOutcome
import com.materialkolor.builder.domain.persist.QuarantineReason
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import com.materialkolor.builder.domain.persist.StorageKeys
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
 * Stores that live in memory for the session.
 *
 * Each record is kept as the text its codec writes, so anything using these goes through the same
 * encode and decode path as real storage. Desktop runs on it, and tests use it in place of the web
 * store. It was public for the web shell, which no longer uses it, so it can become internal.
 *
 * Text that no longer decodes is moved to its quarantine key the first time it is read and reported
 * once on [quarantined]. Reports wait until something collects them, so one found before the UI is
 * up still reaches the user.
 *
 * Two texts moved aside from the same key in the same millisecond do not overwrite each other. The
 * second goes under the quarantine key of the next free millisecond.
 *
 * A record a newer build wrote is never moved, the way the web stores leave it for that build in
 * another tab. It reads as the default, every update to its key is turned down with
 * [StoreError.Unavailable], and it is reported once per key.
 *
 * @param[now] The time in milliseconds since the epoch, stamped on quarantine keys.
 */
class InMemoryStoreFactory(
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : StoreFactory {
    private val texts = MutableStateFlow<Map<String, String>>(emptyMap())
    private val quarantines = Channel<Quarantined>(Channel.UNLIMITED)
    private val otherTabs = MutableSharedFlow<StorageKey>(extraBufferCapacity = CHANGE_BUFFER)
    private val failures = MutableStateFlow<List<StoreError>>(emptyList())
    private val deleteFailures = MutableStateFlow<List<StoreError>>(emptyList())
    private val beforeUpdates = MutableStateFlow<Map<String, suspend () -> Unit>>(emptyMap())

    // b-301a
    private val newerReported = MutableStateFlow<Set<String>>(emptySet())

    override val externalChanges: Flow<StorageKey> = otherTabs.asSharedFlow()

    override val quarantined: Flow<Quarantined> = quarantines.receiveAsFlow()

    override fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T> = InMemoryStore(key, codec, default)

    /** Put [text] under [key] as it is, the way an older or broken build might have left it. */
    internal fun seed(
        key: String,
        text: String,
    ) {
        texts.update { stored -> stored + (key to text) }
    }

    /** The text under [key], or null when there is none. */
    internal fun textAt(key: String): String? = texts.value[key]

    /** Every key that holds something. */
    internal val keys: Set<String>
        get() = texts.value.keys

    /** Turn down the next [count] updates with [error], whichever stores they go to. */
    internal fun failNextUpdates(
        count: Int,
        error: StoreError,
    ) {
        failures.update { queued -> queued + List(count) { error } }
    }

    /** Turn down the next [count] deletes with [error], whichever stores they go to. */
    internal fun failNextDeletes(
        count: Int,
        error: StoreError,
    ) {
        deleteFailures.update { queued -> queued + List(count) { error } }
    }

    /**
     * Run [action] once, just before the next update to [key] reads what is stored, the way a write
     * from elsewhere could land in between.
     */
    internal fun beforeNextUpdate(
        key: String,
        action: suspend () -> Unit,
    ) {
        beforeUpdates.update { pending -> pending + (key to action) }
    }

    /** Put [text] under [key] the way another tab would, and report the key on [externalChanges]. */
    internal fun writeFromAnotherTab(
        key: String,
        text: String,
    ) {
        seed(key, text)
        StorageKeys.parse(key)?.let(otherTabs::tryEmit)
    }

    // b-301a
    private fun report(quarantined: Quarantined) {
        if (quarantined.reason == QuarantineReason.NewerSchema) {
            var first = false
            newerReported.update { reported ->
                first = quarantined.key !in reported
                reported + quarantined.key
            }
            if (!first) return
        }
        quarantines.trySend(quarantined)
    }

    private fun takeBeforeUpdate(key: String): (suspend () -> Unit)? {
        var taken: (suspend () -> Unit)? = null
        beforeUpdates.update { pending ->
            taken = pending[key]
            pending - key
        }
        return taken
    }

    private inner class InMemoryStore<T>(
        private val key: String,
        private val codec: RecordCodec<T>,
        private val default: T,
    ) : Store<T> {
        override val data: Flow<T> =
            texts
                .map { stored -> stored[key] }
                .distinctUntilChanged()
                .map { read() }
                .distinctUntilChanged()

        override suspend fun get(): T = read()

        override suspend fun update(block: (T) -> T): StoreError? {
            takeBeforeUpdate(key)?.invoke()
            failures.takeFirst()?.let { error -> return error }
            var moved: Quarantined? = null
            var refused = false
            texts.update { stored ->
                val reading = settle(stored)
                moved = reading.quarantined
                refused = reading.refused
                if (refused) stored else reading.texts + (key to codec.encode(block(reading.value)))
            }
            moved?.let(::report)
            return if (refused) StoreError.Unavailable else null
        }

        // b-310aa
        override suspend fun fromNewerBuild(): Boolean {
            val text = texts.value[key] ?: return false
            val outcome = codec.decode(text)
            return outcome is DecodeOutcome.Quarantine && outcome.reason == QuarantineReason.NewerSchema
        }

        override suspend fun delete(): StoreError? {
            deleteFailures.takeFirst()?.let { error -> return error }
            texts.update { stored -> stored - key }
            return null
        }

        private fun read(): T {
            lateinit var reading: Reading<T>
            texts.update { stored ->
                reading = settle(stored)
                reading.texts
            }
            reading.quarantined?.let(::report)
            return reading.value
        }

        // The move and the read happen in one step, so two readers never both report the same text.
        private fun settle(stored: Map<String, String>): Reading<T> {
            val text = stored[key] ?: return Reading(default, stored, quarantined = null)
            return when (val outcome = codec.decode(text)) {
                is DecodeOutcome.Ok -> {
                    Reading(outcome.value, stored, quarantined = null)
                }
                // b-301a
                is DecodeOutcome.Quarantine if outcome.reason == QuarantineReason.NewerSchema -> {
                    Reading(default, stored, Quarantined(key, outcome.reason), refused = true)
                }
                is DecodeOutcome.Quarantine -> {
                    Reading(
                        value = default,
                        texts = stored - key + (freeQuarantineKey(stored) to text),
                        quarantined = Quarantined(key, outcome.reason),
                    )
                }
            }
        }

        private fun freeQuarantineKey(stored: Map<String, String>): String {
            var time = now()
            while (StorageKeys.quarantine(key, time) in stored) time++
            return StorageKeys.quarantine(key, time)
        }
    }
}

/** Take the first queued error off, or null when none is queued. */
private fun MutableStateFlow<List<StoreError>>.takeFirst(): StoreError? {
    var taken: StoreError? = null
    update { queued ->
        taken = queued.firstOrNull()
        queued.drop(1)
    }
    return taken
}

/**
 * What a store holds, and the stored texts once anything unreadable has been moved aside.
 *
 * @property[refused] True when a newer build wrote the record, so no update may land on it.
 */
private class Reading<T>(
    val value: T,
    val texts: Map<String, String>,
    val quarantined: Quarantined?,
    val refused: Boolean = false,
)

private const val CHANGE_BUFFER = 64
